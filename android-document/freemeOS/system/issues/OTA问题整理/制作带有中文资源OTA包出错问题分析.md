# 制作带有中文资源OTA包出错问题分析

#### 问题现象

庄毅的服务器编译v9项目时，加入中文文件名的资源，制作OTA包时编译出错，必现，log如下

```verilog
++++ system ++++
creating system.img...
Running:  mkuserimg.sh -s /tmp/targetfiles-HPiLbl/system /tmp/system-AoMdO1.img ext4 system 2704793600 -T 1474900649 -C /tmp/targetfiles-HPiLbl/META/filesystem_config.txt -B /tmp/system-blocklist-xDKPz6.map -L system /tmp/targetfiles-HPiLbl/BOOT/RAMDISK/file_contexts
make_ext4fs -s -T 1474900649 -S /tmp/targetfiles-HPiLbl/BOOT/RAMDISK/file_contexts -C /tmp/targetfiles-HPiLbl/META/filesystem_config.txt -B /tmp/system-blocklist-xDKPz6.map -L system -l 2704793600 -a system /tmp/system-AoMdO1.img /tmp/targetfiles-HPiLbl/system
【failed to find [/system/presets/Movies/美食.mp4] in canned fs_config】
loaded 2249 fs_config entries
Creating filesystem with parameters:
    Size: 2704793600
    Block size: 4096
    Blocks per group: 32768
    Inodes per group: 7872
    Inode size: 256
    Journal blocks: 10317
    Label: system
    Blocks: 660350
    Block groups: 21
    Reserved block group size: 167
Traceback (most recent call last):
  File "./build/tools/releasetools/add_img_to_target_files", line 364, in <module>
    main(sys.argv[1:])
  File "./build/tools/releasetools/add_img_to_target_files", line 358, in main
    AddImagesToTargetFiles(args[0])
  File "./build/tools/releasetools/add_img_to_target_files", line 322, in AddImagesToTargetFiles
    AddSystem(output_zip, recovery_img=recovery_image, boot_img=boot_image)
  File "./build/tools/releasetools/add_img_to_target_files", line 65, in AddSystem
    block_list=block_list)
  File "./build/tools/releasetools/add_img_to_target_files", line 80, in BuildSystem
    return CreateImage(input_dir, info_dict, "system", block_list=block_list)
  File "./build/tools/releasetools/add_img_to_target_files", line 171, in CreateImage
    assert succ, "build " + what + ".img image failed"
AssertionError: build system.img image failed
make: *** [out/target/product/v981q_ada/obj/PACKAGING/target_files_intermediates/full_v981q_ada-target_files-1474944850.zip] Error 1
make: *** Deleting file `out/target/product/v981q_ada/obj/PACKAGING/target_files_intermediates/full_v981q_ada-target_files-1474944850.zip'
```



#### 分析过程

###### Step 1

可见上述log中 出错语句 ```failed to find [/system/presets/Movies/美食.mp4] in canned fs_config```

在 **opengrok** ，搜索关键字 " **in canned fs_config** "，

定位到该错误日志位置：

```c++
void canned_fs_config(const char* path, int dir, const char* target_out_path,
					  unsigned* uid, unsigned* gid, unsigned* mode, uint64_t* capabilities) {
	Path key;
	key.path = path+1;   // canned paths lack the leading '/'
	Path* p = (Path*) bsearch(&key, canned_data, canned_used, sizeof(Path), path_compare);
	if (p == NULL) {
		fprintf(stderr, "failed to find [%s] in canned fs_config\n", path);
		exit(1);
	}
	*uid = p->uid;
	*gid = p->gid;
	*mode = p->mode;
	*capabilities = p->capabilities;
```

###### Step 2

进一步分析该文件所在目录的Android.mk文件，可知，该文件编译

```sh
include $(CLEAR_VARS)
LOCAL_SRC_FILES := make_ext4fs_main.c canned_fs_config.c
LOCAL_MODULE := make_ext4fs
LOCAL_SHARED_LIBRARIES += libcutils
LOCAL_STATIC_LIBRARIES += \
    libext4_utils_host \
    libsparse_host \
    libz
ifeq ($(HOST_OS),windows)
  LOCAL_LDLIBS += -lws2_32
else
  LOCAL_SHARED_LIBRARIES += libselinux
  LOCAL_CFLAGS := -DHOST
endif
include $(BUILD_HOST_EXECUTABLE)
```

通过对 ```make_ext4fs_main.c canned_fs_config.c``` 代码详细分析，可知make_ext4fs传入的参数是：

```c++
static void usage(char *path)
{
	fprintf(stderr, "%s [ -l <len> ] [ -j <journal size> ] [ -b <block_size> ]\n", basename(path));
	fprintf(stderr, "    [ -g <blocks per group> ] [ -i <inodes> ] [ -I <inode size> ]\n");
	fprintf(stderr, "    [ -L <label> ] [ -f ] [ -a <android mountpoint> ] [ -u ]\n");
	fprintf(stderr, "    [ -S file_contexts ] [ -C fs_config ] [ -T timestamp ]\n");
	fprintf(stderr, "    [ -z | -s ] [ -w ] [ -c ] [ -J ] [ -v ] [ -B <block_list_file> ]\n");
	fprintf(stderr, "    <filename> [[<directory>] <target_out_directory>]\n");
}
```

编译system.img 传入实际参数如下：

```sh
 make_ext4fs -s 
-T 1474993762
-S /tmp/targetfiles-4II5Pd/BOOT/RAMDISK/file_contexts
-C /tmp/targetfiles-4II5Pd/META/filesystem_config.txt   【fs_config】
-B /tmp/system-blocklist-moQHHS.map
-L system
-l 2704793600
-a system
/tmp/system-yaf4kX.img <filename>
/tmp/targetfiles-4II5Pd/system  【target_out_directory】
```

该命令作用为：打包 system.img 并会遍历target_out_directory 目录中所有文件是否存在于 fs_config 中

###### Step 3

出错原因是 **fs_config** 文件中无法查找到 ```target_out_directory/system/presets/Movies/美食.mp4```

根据前面分析 **fs_config** 文件即 ```/tmp/targetfiles-4II5Pd/META/filesystem_config.txt```

在ubuntu机器上，中文使用utf-8编码，而初步使用命令

file /tmp/targetfiles-4II5Pd/META/filesystem_config.txt 显示该文件是ASCII码文件，

查看该文件，发现其中 ```/system/presets/Movies/美食.mp4``` 路径对应于 ```/system/presets/Movies/??????.mp4```

路径没有被正确写入（文件名没有被正确解析），利用Opengrok搜索关键字"filesystem_config.txt"，可知该文件由 ```build/core/Makefile``` 中以下命令生成：

```
$(hide) zipinfo -1 $@ | awk 'BEGIN { FS="SYSTEM/" } /^SYSTEM\// {print "system/" $$2}' | $(HOST_OUT_EXECUTABLES)/fs_config -C -D $(TARGET_OUT) -S $(SELINUX_FC) > $(zip_root)/META/filesystem_config.txt
```

其原理通过zipinfo 命令把 临时升级包 


```
out/target/product/v981q_ada/obj/PACKAGING/target_files_intermediates/full_v981q_ada-target_files-1474944850.zip
```

中system目录下所有的文件路径导入进filesystem_config.txt中。

###### Step 4

怀疑zipinfo 没有正确解析zip包，进一步测试，发现ubuntu12.04的zipinfo 命令在解析带中文资源的zip包时乱码。log如下：

```shell
program62@tyd2015472-Z9NA-D6C:~$ zipinfo test.zip
Archive:  test.zip
Zip file size: 3849427 bytes, number of entries: 6
drwxrwxr-x  3.0 unx        0 bx stor 16-Sep-28 15:20 123/
-rw-rw-r--  3.0 unx      438 tx defN 16-Sep-28 15:29 1.txt
-rw-rw-r--  3.0 unx      268 tx defN 16-Sep-28 15:20 2
-rw-rw-r--  3.0 unx   345563 tx defN 16-Sep-28 15:24 2.txt
-rw-rw-r--  3.0 unx      440 tx defN 16-Sep-28 15:30 3.txt
-rwxr--r--  3.0 unx  3812562 bx defN 16-Sep-28 15:17 ????????????.mp4
6 files, 4159271 bytes uncompressed, 3848565 bytes compressed:  7.5%
```

 而在笔者的机器上（ubuntu 14.04）却是：

```shell
program62@tyd2015472-Z9NA-D6C:~$ zipinfo test.zip
Archive:  test.zip
Zip file size: 3849427 bytes, number of entries: 6
drwxrwxr-x  3.0 unx        0 bx stor 16-Sep-28 15:20 123/
-rw-rw-r--  3.0 unx      438 tx defN 16-Sep-28 15:29 1.txt
-rw-rw-r--  3.0 unx      268 tx defN 16-Sep-28 15:20 2
-rw-rw-r--  3.0 unx   345563 tx defN 16-Sep-28 15:24 2.txt
-rw-rw-r--  3.0 unx      440 tx defN 16-Sep-28 15:30 3.txt
-rwxr--r--  3.0 unx  3812562 bx defN 16-Sep-28 15:17 测试视频.mp4
6 files, 4159271 bytes uncompressed, 3848565 bytes compressed:  7.5%
```

确认分析遂将14.04的zipinfo覆盖12.04同名文件，zipinfo乱问题解决。OTA包也可正常制作。



#### 总结

Ubuntu 12.04 的zipinfo 解析内容含有中文名称的zip包，可能出现乱码，请使用ubuntu14.04 中zipinfo覆盖系统/usr/bin/zipinfo。

