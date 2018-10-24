# OTA升级包制作SOP

### 修改记录
| 版 次  | 修改日期       | 作 者  | 修改内容                  |
| :--- | ---------- | ---- | --------------------- |
| V1.0 | 2011.11.24 | 黄祎平  | 初版                    |
| V2.0 | 2011.12.16 | 汤阳   | 更新内部版本升级并实现内部测试       |
| V3.0 | 2012.03.16 | 汤阳   | 新增支持uboot.img差分升级     |
| V4.0 | 2012.03.28 | 汤阳   | 新增差分包原始包备份事项          |
| V4.1 | 2012.10.12 | 汤阳   | 修改切换到新远程服务器，新增兼容说明    |
| V4.2 | 2013.05.31 | 汤阳   | 新增'-w'参数以及设备软件兼容性注意事项 |
| V5.0 | 2015.11.04 | 苟周平  | 初版                    |
| V6.0 | 2016.08.29 | 陈明   | 更新制作差分包及OTA后台操作，新增FAQ |
| V6.1 | 2016.10.12 | 陈明   | 新增OTA包制作升级流程图         |

[TOC]

### 差分包是什么
全称差分升级包，用于通过Android的Recovery模式来做软件版本升级。标准的方式是手动进入Recovery模式然后在设备存储上选择差分包来进行升级，本文讲的OTA升级是指通过搭建Server 服务器，来让手机端能够访问并且能下载到正确的升级版本的差分包，然后自动进入Recovery模式来升级系统。

差分包是比较两个版本之间的差异而制作的，制作版本A到版本B的差分包之后，能让当前处于A版本的设备通过Recovery模式升级到B版本。

### 使用差分包的好处
* 方便量产后的升级可以在用户手机上继续进行；
* 可以保留全部的用户信息。如果用单机工具做```firmware upgrade```时，用户保留在手机端的私人信息将会全部丢失；
* 减小了升级包的体积，导致减少可能的下载流量。

### 制作 *原始包*
新项目制作原始包前，请先查看

- [OTA制作升级流程](#jumps1)
- [FAQ：如何配置新项目 OTA 所需系统参数？](#jumps2)
- [FAQ：如何配置OTA 所需cache分区大小？](#jumps3)

原始包需编译产生。如下以制作K506项目的*原始包*为例：

```
. build/envsetup.sh
lunch full_k506-user
make ...
make ... otapackage
```

 *Mediatek平台编译命令：*

```
mk -o=TARGET_BUILD_VARIANT=user k506 new
mk -o=TARGET_BUILD_VARIANT=user k506 otapackage
```

> ###### 说明
> 命令 ```make ... otapackage``` 在生成 *原始包* 时，还会重新生成配套的带校验字的二进制镜像文件（boot.img/lk.bin/etc.）。所以 **应该在制作完 *原始包* 后，再发布新的系统版本二进制镜像文件** 。

生成文件：

    out/target/product/k506/obj/PACKAGING/target_files_intermediates/full_k506-target_files-xxx.zip

> ###### 注意
> 因项目、编译主机、发布版本（eng/user/userdebug）的不同，文件路径也会不同。请根据项目的变动使用以上命令。

### 制作 *差分包*
通过差分脚本工具，比对 **两个** 版本 *原始包* 文件的差异，按规则生成 *差分包* 。
如下是制作方法：

- L 及以下版本

```
build/tools/releasetools/ota_from_target_files –u <target-secondary_boot> –i <original-ota_package> <target-ota_package> update.zip
```

  > ###### 说明
  > ```<target-secondary_boot>``` ：目标版本的uboot/lk二进制文件；
  >
  > ```<original-ota_package>``` ： *源* 版本 *原始包* 文件；
  >
  > ```<target-ota_package>``` ： *目标* 版本 *原始包* 文件；
  >
  > ```update.zip``` ：将在当前目录中生成的差分包文件，该文件即是从 *源* 版本到 *目标* 版本的 *差分包* 。然后，将此文件拷贝到设备存储中，可以通过标准的Recovery模式进行手动升级。
  >
  > ```package.zip``` ：FreemeOS中，与 ```update.zip``` 同级目录会生成此包。该包用以上传至OTA服务器，供在线升级使用。

- M 版本

  ```
  build/tools/releasetools/ota_from_target_files –s <external-script> --block -i <original-ota_package> <target-ota_package> update.zip
  ```
  > ######说明
  > ``` <external-script>```：更新目标uboot/lk相关，（默认为：
  > ./device/mediatek/build/releasetools/mt_ota_from_target_files）配置前请参看[FAQ：如何升级Android M 版本lk?](#jumps4)
  >
  > ```<original-ota_package>```：*源*版本*原始包*文件；
  >
  > ```<target-ota_package>```：*目标*版本*原始包*文件；
  >
  > ```update.zip```：将在当前目录中生成的差分包文件，该文件即是从*源*版本到*目标*版本的*差分包*。然后，将此文件拷贝到设备存储中，可以通过标准的Recovery模式进行手动升级。
  >
  > ```package.zip```：FreemeOS中，与```update.zip```同级目录会生成此包。该包用以上传至OTA服务器，供在线升级使用。

> ###### **注

> ```package.zip``` 结构：

- update.zip
- md5.txt
>
> ```update.zip``` 和 ```md5.txt``` 将在上传至OTA服务器时，被分别录入到后端数据库中，共OTA客户端查询下载。

### 软件兼容性建议及 ```-w``` 参数的使用
OTA升级之后，在不恢复出厂设置的情况下，某些应用可能会无法正常使用。原因是现有的OTA机制，默认不会对```userdata```分区中的用户数据进行处理，其中的应用私有数据因为应用的升级导致*新*版本不识*旧*版本遗留的数据，从而导致异常。
为了应对该问题的出现，需要在开发过程中遵守如下原则：

- 对于数据库进行**结构性**修改，需要将数据库版本号增加（当应用通过OTA升级到新版本后，如果发现该应用遗留的私有数据库为*老版本*时，会执行```upgrade```调用。一般```ContentProvider```里面的```upgrade```方法，在大多数情况下默认会丢弃遗留数据库，并重建）。
- 对于修改源版本的```SharedPreference```，应该注意尽量新增我们自己的```SharedPreference```项目；如果要用到现有的项目时，应保留其原有的逻辑不变。
- 某些情况下，必须**强制**清除用户的```userdata```分区。首先要判断升级包，是否有擦除```userdata```的属性（使用“```-w```”参数来制作*差分包*），然后使用对话框通知用户，待用户确认后方可升级。

### OTA后台操作
- 国内地址：http://ota.yy845.com:2970
- 海外地址：http://ota.dd351.com:2970

登录成功之后的界面如图1所示。

![tu1](res/pic1.jpg)

新项目需点击“添加新型号”

![tu2_1](res/pic2.jpg)

新增升级项目，请点击"OTA管理 - OTA管理"，如图2所示。

![tu2](res/pic2_1.jpg)

新建项目需点击"新增配置"，之后出现界面如图3所示。

![tu2](res/pic3.jpg)

注意各个项目负责人仅处理自己的项目，一定要小心，避免误操作。ProductNo号为项目匹配的关键信息，由四个值拼接组成，需跟添加新型号时候填写的值保持一致。ProductNo号的拼接方式为："集成商"+"型号名"+"扩展项"+"Local"，同时需要跟源码build/tools/buildinfo里面的 ro.build.ota.product 属性值相对应

```sh
echo "ro.build.ota.product=${PRODUCTMANUFACTURER}_${FREEME_PRODUCT_DEVICE}_${FREEME_OTA_LANGUAGE}_${FREEME_OTA_FLASH}"
```
依赖于项目宏参数定义 ：

```
PRODUCT_MANUFACTURER  [1*]
FREEME_PRODUCT_DEVICE [2*]
FREEME_OTA_LANGUAGE   [3*]
FREEME_OTA_FLASH      [4*]
```

> 注1： 代表设备生产商，原生Android自有定义。
> 注2： 代表设备型号，与原生Android自有的TARGET_DEVICE稍有不同。
> 注3： 代表产品销售区域，如zh/tw/us/...，可自定义。
> 注4： 代表产品附加参数，如运营商(cmcc/cu/...)/...，可自定义。
> 注5： 完整的事例如: freeme_X1_zh_freeme6。

注意如果在同一个项目中发生不兼容的情况时，通过调整宏"FREEME_OTA_LANGUAGE"和"FREEME_OTA_FLASH"来做不兼容的区分，不要调整型号名(PRODUCT_MANUFACTURER)如图3，3_1所示。

![tu3_1](res/pic3_1.jpg)

如图所示新建 亮剑X1的测试用版本，如图4所示。

![tu4](res/pic4.jpg)

确认添加之后如图5所示。

![tu5](res/pic5.jpg)

双击图5中的任意一项的ProductNo号，可以看到项目已经建立好的版本列表，如图6所示。

![tu6](res/pic6.jpg)

点击对话框中的"新增配置"，如图7所示。

![tu7](res/pic7.jpg)

注意这个时候，需填写完整的版本号名称，如："FreemeOs6.1.V3.20"。版本号须跟Build/tools/buildinfo里面的 ro.build.display.id 属性值相对应

```sh
echo "ro.build.display.id=$FREEME_CUSTOM_SW_BUILD_VERNO"
```

宏"FREEME_CUSTOM_SW_BUILD_VERNO"一般在tyd目录下客制化定制。如下：

![tu14](res/pic7_1.jpg)

创建好"FreemeOs6.1.V3.20"跟"FreemeOs6.1.V3.21"之后出现以下界面，如图8所示。

![tu8](res/pic8.jpg)

单击选中需要升级的版本即"FreemeOs6.1.V3.20"，点击右上方的"上传差分包"如图9所示。

![tu9](res/pic9.jpg)

点击完"上传差分包"之后，会出现一个对话界面，如图10所示。

![tu10](res/pic10.jpg)

选择升级到的目标版本号，注意项目可以进行多目的版本升级，比如"FreemeOs6.1.V3.20"可以做升级到3.21，3.22，3.23也就是说，可以直升。也可进行回退升级专门进行OTA测试，如3.21 -> 3.20。点击"选择文件"选取之前已经做好的package.zip文件，然后点击"开始上传"，稍等一会之后看到如下界面，如图11所示。

![tu11](res/pic11.jpg)

此时版本升级状态处于默认的"测试发布"状态，这样就可以做内部测试了，内部测试仅仅对白名单机器（如图12）开放升级通路，也就是说只有白名单机器，可以在系统升级界面中发现新版本。

![tu12](res/pic12.jpg)

这个时候还属于内部测试阶段， **若点击"测试发布"，会自动切换至"正式发布"** ，"正式发布"阶段的选择如图13所示。

![tu13](res/pic13.jpg)

 **注意"正式发布"这个选择，只能让测试负责人统一来正式释放，软件永远也不要做这个动作。** 流程上，量产版本发布之后，深圳使用之后确认没有问题，才会通知测试这边统一来安排正式发布。


### 备份量产版本OTA全包
综上所述，差分包需要每个版本的OTA全包，这个全包并没有随版本发布的时候放到FTP上，通常都在项目负责人本地，为了更好的维护项目，防止本地数据的丢失，量产之后开始需要将备份上传到专门的FTP上。(注意此账号密码保密)

ftp://192.168.0.6
user: otauser
password: tydsw2012

###OTA制作升级流程图

<a name="jumps1"></a>

![tu15](res/pic15.png)

### FAQ

- **如何配置新项目 OTA 所需系统参数**？<a name="jumps2"></a>
  **答** ：新项目制作原始包前，请进行以下配置：
  - ```ro.build.ota.product``` ， *表示项目号* ，集成源码 vendor/droi/freeme/build/tools/buildinfo中，如下：

    ```sh
    echo   "ro.build.ota.product=${PRODUCT_MANUFACTURER}_${FREEME_PRODUCT_DEVICE}_${FREEME_OTA_LANGUAGE}_${FREEME_OTA_FLASH}"
    ```

    依赖项目宏参数定义，在源码 tyd/${具体项目}/ProjectConfig.mk 客制化。如下：

    ```
    PRODUCT_MANUFACTURER  [1*]
    FREEME_PRODUCT_DEVICE [2*]
    FREEME_OTA_LANGUAGE   [3*]
    FREEME_OTA_FLASH      [4*]
    ```

    > 注1： 代表设备生产商，原生Android自有定义。
    >
    > 注2： 代表设备型号，与原生Android自有的TARGET_DEVICE稍有不同。
    >
    >  注3： 代表产品销售区域，如zh/tw/us/...，可自定义。
    >
    > 注4： 代表产品附加参数，如运营商(cmcc/cu/...)/...，可自定义。
    > 注5： 完整的事例如: NOSSON_V9_zh_freeme6。

  -  ```ro.build.display.id``` ， *表示版本号* ，集成于源码 build/tools/buildinfo 中，如下：

    ```sh
    echo "ro.build.display.id=$FREEME_CUSTOM_SW_BUILD_VERNO"
    ```

    依赖项目宏参数定义，在源码 tyd/${具体项目}/ProjectConfig.mk 客制化。如下：

    ```sh
    FREEME_CUSTOM_SW_BUILD_VERNO = V9C81Q.ADA.GAL1SAG1.0725.V2.04
    ```

- **如何配置OTA 所需cache分区大小？** <a name="jumps3"></a>

  **答：新项目配置分区表时，须注意cache分区大小：**

  - OTA升级时，所需cache分区一般为**OTA包的3~4倍**，譬如20M的OTA包则需要100M左右的cache分区。
  - 如果项目的整体容量较小（譬如4 GB存储），建议设置最小`cache分区 100M`

- **如何升级Android M 版本 lk** ？<a name="jumps4"></a>
  **答** ：以下方法仅适用 *MTK 6735,6580,6737* 等平台，具体详情参考[\[FAQ18188]Android M 版本如何升级lk ](https://onlinesso.mediatek.com/Pages/FAQ.aspx?List=SW&FAQID=FAQ17441)
   查看MTXXXX_Android_scatter.txt文件并搜索关键字 ```is_upgradable```

  - **不存在 ```is_upgradable``` 关键字**
    修改方法：

    1. **制作原始包前，请修改build/core/Makefile** 

       修改 **前** ：

       ```mk
        $(hide) ./device/mediatek/build/releasetools/mt_ota_preprocess.py $(zip_root) $(PRODUCT_OUT) $(PRODUCT_OUT)/ota_update_list.txt 		  
       ```

       修改**后**：

       ```mk
        $(hide) MTK_LOADER_UPDATE=yes MTK_PRELOADER_OTA_BACKUP=no  ./device/mediatek/build/releasetools/mt_ota_preprocess.py $(zip_root) $(PRODUCT_OUT) $(PRODUCT_OUT)/ota_update_list.txt
       ```

    2. **执行编译差分升级包的命令并指定 -s 参数** ，如下：
        ./build/tools/releasetools/ota_from_target_files  **-s**

        **./device/mediatek/build/releasetools/mt_ota_from_target_files** -block -k <key_path> -i V2_org.zip V4_new.zip   V2_4.zip

  - **存在 ```is_upgradable``` 关键字**

    比如：

    ![](res/pic14.jpg)

    ```is_upgradable``` 的值决定是否升级对应分区，true表示升级，false表示不升级。这种情况下lk和preloader是默认升级的，不需要修改文件。但是如果修改了分区表等情况，可能会改变默认的设置，所以这一步一定要确定：preloader，lk和lk2三个分区的is_upgradable都是true，如果为false，请修改分区表```OTA_Update```字段（具体详情参考[\[FAQ18188]Android M 版本如何升级logo等rawdata分区方法](https://onlinesso.mediatek.com/Pages/FAQ.aspx?List=SW&FAQID=FAQ18188)）
