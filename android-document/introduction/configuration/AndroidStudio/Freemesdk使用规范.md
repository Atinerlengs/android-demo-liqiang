[toc]
# FreemeSdk使用


## build.gradle配置

```
buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.0.0'
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}


// *******以上部分android—studio自动生成*******
// --------------------------------------------
// **************以下部分需自己配置************
ext {
    compileSdkVersion = 24
    buildToolsVersion = '26.0.2'
    minSdkVersion = 24
    targetSdkVersion = 25
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7

    useCertificate = 'platform'
    useFreemePlarformRes = true
    if (rootProject.file('local.properties').exists()) {
        Properties localProperties = new Properties()
        localProperties.load(rootProject.file('local.properties').newDataInputStream())

        sdkDir = localProperties.getProperty('sdk.dir')
        sdkFreemeDir = file("${sdkDir}/extras/freeme").path
        sdkFreemeSigningsDir = file("${sdkFreemeDir}/signings").path
        sdkFreemePlatformDir = file("${sdkFreemeDir}/platforms/android-v${compileSdkVersion}").path

        // signing
        if (file("${sdkFreemeSigningsDir}/signings.properties").exists()) {
            Properties signingsProperties = new Properties()
            signingsProperties.load(file("${sdkFreemeSigningsDir}/signings.properties").newDataInputStream())

            storeFile = file("${sdkFreemeSigningsDir}/${signingsProperties["key.${useCertificate}.store"]}").path
            storePassword = signingsProperties["key.${useCertificate}.store.password"]
            keyAlias = signingsProperties["key.${useCertificate}.alias"]
            keyPassword = signingsProperties["key.${useCertificate}.password"]
        }
        // sdk
        if (file("${sdkFreemePlatformDir}").exists()) {
            if (useFreemePlarformRes && file("${sdkFreemePlatformDir}/freeme-framework-res.apk").exists()) {
                sdkFreemePlatformRes = file("${sdkFreemePlatformDir}/freeme-framework-res.apk").path
            }
        }
    }
}

apply plugin: 'com.android.application'

android {
    compileSdkVersion rootProject.compileSdkVersion
    buildToolsVersion rootProject.buildToolsVersion
    defaultConfig {
        minSdkVersion rootProject.minSdkVersion
        targetSdkVersion rootProject.targetSdkVersion
        applicationId 'com.freeme.hongbaoassistant'
        versionCode 1
        versionName '1.0'
    }
    compileOptions {
        sourceCompatibility rootProject.sourceCompatibility
        targetCompatibility rootProject.targetCompatibility
    }
    signingConfigs {
        release {
            storeFile file(rootProject.storeFile)
            storePassword rootProject.storePassword
            keyAlias rootProject.keyAlias
            keyPassword rootProject.keyPassword
        }
    }

    sourceSets {
        main {
            manifest.srcFile 'AndroidManifest.xml'
            java.srcDirs = ['src']
            resources.srcDirs = ['src']
            aidl.srcDirs = ['src']
            renderscript.srcDirs = ['src']
            res.srcDirs = ['res']
            assets.srcDirs = ['assets']
        }
    }
    buildTypes {
        debug {
            minifyEnabled false
            signingConfig signingConfigs.release
        }
        release {
            minifyEnabled true
            shrinkResources true
            zipAlignEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard.flags'
            signingConfig signingConfigs.release
        }
    }
    aaptOptions {
        if (rootProject.sdkFreemePlatformRes) {
            additionalParameters '-I', rootProject.sdkFreemePlatformRes
        }
    }
}

dependencies {
    implementation project(':settings_common')
    implementation fileTree(dir: 'libs', include: ['*.jar'])
}

```

## 获取freemesdk

```
git clone http://192.168.48.11/biantao/android-sdk-extra-freeme.git
```

## 添加到Android SDK中

将android-sdk-extra-freeme添加到我们Android Sdk的extras中

例如：

```
mv ./android-sdk-extra-freeme ~/Android/Sdk/extras/freeme
```

注：每个人的Android sdk路径可能不一样，请自寻查找下对应的路径。
## 替换android.jar包


- **原则**：

对应的版本，替换对应的android.jar包

- **举例**：

如果所对应的编译版本compileSdkVersion = 25
则将freeme/platforms对应的android-v25中的android.jar与Sdk/platforms中android-25中的替换

![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/buildgradle.png)

## 打包apk

- **Build**

因为apk的签名已经在build.gradle中配好,所以直接点击==Build==中的==Build APK==
![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/buildapk.png)

然后我们看到==APK(s) generated successfully==表示编译成功了

![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/buildSuccess.png)

点击==locate==我们可以看到生成了==xxx-debug.apk==
![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/debug-apk.png)


