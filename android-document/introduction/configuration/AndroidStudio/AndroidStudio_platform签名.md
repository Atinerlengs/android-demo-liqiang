# Android Studio platform签名

[TOC]

### 生成keyStore

github 有开源的生成工具：[keytool-importkeypai](https://github.com/getfatday/keytool-importkeypair)

命令：

```
keytool-importkeypair -k your_keystore_name -p keystore_password -pk8 your.pk8 -cert your.pem -alias platform
```

譬如：

```
keytool-importkeypair -k debug.keystore -p android -pk8 platform.pk8 -cert platform.x509.pem -alias platform
```

### 集成keyStore

将生成的debug.keystore 拷贝到项目源码下。

gradle的配置文件一般以.properties结束，新建一个signing.properties文件，内容如下：

```sh
# 格式：
STORE_FILE=yourapp.keystore
STORE_PASSWORD=your password
KEY_ALIAS=your alias
KEY_PASSWORD=your password
```

```sh
# 举例：
STORE_FILE=debug.keystore
STORE_PASSWORD=android
KEY_ALIAS=platform
KEY_PASSWORD=android    #默认为android，必填
```

在gradle部分添加如下代码即可

```
android {
	...
  signingConfigs {
        debug {
            storeFile
            storePassword
            keyAlias
            keyPassword
        }

        release {
            storeFile
            storePassword
            keyAlias
            keyPassword
        }
    }

    getSigningProperties()
    ...
}

//读取签名配置文件
def getSigningProperties(){

    def propFile = file('signing.properties')
    if (propFile.canRead()){
        def Properties props = new Properties()
        props.load(new FileInputStream(propFile))
        if (props!=null && props.containsKey('STORE_FILE') && props.containsKey('STORE_PASSWORD') &&
                props.containsKey('KEY_ALIAS') && props.containsKey('KEY_PASSWORD')) {
            android.signingConfigs.debug.storeFile = file(props['STORE_FILE'])
            android.signingConfigs.debug.storePassword = props['STORE_PASSWORD']
            android.signingConfigs.debug.keyAlias = props['KEY_ALIAS']
            android.signingConfigs.debug.keyPassword = props['KEY_PASSWORD']
        } else {
            println 'signing.properties found but some entries are missing'
            android.buildTypes.release.signingConfig = null
        }
    }else {
        println 'signing.properties not found'
        android.buildTypes.release.signingConfig = null
    }
}
```

此处为debug模式签名，如果是release模式则将 ```getSigningProperties()``` 函数中的 ```android.signingConfigs.debug.``` 改成 ```android.signingConfigs.release.```



最后运行项目即可。

### FAQ

#### target SDK问题

```
Error:Execution failed for task ':installDebug'.

com.android.builder.testing.api.DeviceException: com.android.ddmlib.InstallException: Failed to finalize session : -26: Package com.freeme.ota new target SDK 21 doesn't support runtime permissions but the old target SDK 23 does.
```

出现该问题为，设备上应用目前target SDK 为23，而项目本身为21。**一般由于只设置minSdkVersion导致**

**解决方案**：

- 删除设备端原始系统APK
- 在项目中设置target SDK 为23，即可覆盖安装

#### sharedUserId 问题

```
Error:Execution failed for task ':installDebug'.

com.android.builder.testing.api.DeviceException: com.android.ddmlib.InstallException: Failed to finalize session : INSTALL_FAILED_SHARED_USER_INCOMPATIBLE: Forbidding shared user change from SharedUserSetting{86535d0 android.uid.system/1000} to SharedUserSetting{85d79c9 android.uid.system.ota/10104}
```

解决方案：在AndroidMainfast，中添加

```
android:sharedUserId="android.uid.system"
```

http://192.168.3.179/biantao/FreemeFactoryTest
