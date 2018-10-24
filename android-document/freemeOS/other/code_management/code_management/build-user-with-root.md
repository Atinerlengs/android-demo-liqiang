[TOC]

# build user with root in android N

本文根据mtk [FAQ06317][1]修改

## 1，修改alps/build/core/main.mk

```makefile
diff --git a/core/main.mk b/core/main.mk
index cf8146c..ccc8cdf 100644
--- a/core/main.mk
+++ b/core/main.mk
@@ -389,7 +389,7 @@ enable_target_debugging := true
 tags_to_install :=
 ifneq (,$(user_variant))
   # Target is secure in user builds.
-  ADDITIONAL_DEFAULT_PROPERTIES += ro.secure=1
+  ADDITIONAL_DEFAULT_PROPERTIES += ro.secure=0
   ADDITIONAL_DEFAULT_PROPERTIES += security.perf_harden=1

   ifeq ($(user_variant),user)
```

将ADDITIONAL_DEFAULT_PROPERTIES += ro.secure=1 改成 ADDITIONAL_DEFAULT_PROPERTIES += ro.secure=0 即可。

## 2，system/core/adb/Android.mk

在android JB 版本(4.1) 以后，google 从编译上直接去除了adbd 的user 版本root 权限， 为此您要修改system/core/adb/Android.mk 中的编译选项ALLOW_ADBD_ROOT, 如果没有打开这个选项，那么adb.c 中将不会根据ro.secure 去选择root 还是shell 权限，直接返回shell 权限。因此您必须需要Android.mk 中修改：

```
diff --git a/core/adb/Android.mk b/core/adb/Android.mk
index 4b573b4..74511e5 100755
--- a/core/adb/Android.mk
+++ b/core/adb/Android.mk
@@ -319,7 +319,7 @@ LOCAL_CFLAGS := \
     -D_GNU_SOURCE \
     -Wno-deprecated-declarations \

-ifneq (,$(filter userdebug eng,$(TARGET_BUILD_VARIANT)))
+ifneq (,$(filter userdebug user eng,$(TARGET_BUILD_VARIANT)))
 LOCAL_CFLAGS += -DALLOW_ADBD_DISABLE_VERITY=1
 LOCAL_CFLAGS += -DALLOW_ADBD_ROOT=1
 endif
```

## 3，sepolicy/Android.mk

在android L (5.0) 以后, google 默认开启SELinux enforce mode, 需要在user build 上将su label 默认build 进SEPolicy.
放开SELinux 的限制. 更新alps/system/sepolicy/Android.mk 116 行,  将su label 默认编译进入sepolicy.

```
diff --git a/sepolicy/Android.mk b/sepolicy/Android.mk
index c819a39..b2a9766 100644
--- a/sepolicy/Android.mk
+++ b/sepolicy/Android.mk
@@ -96,7 +96,7 @@ $(sepolicy_policy.conf): $(call build_policy, $(sepolicy_build_files))
        @mkdir -p $(dir $@)
        $(hide) m4 $(PRIVATE_ADDITIONAL_M4DEFS) \
                -D mls_num_sens=$(PRIVATE_MLS_SENS) -D mls_num_cats=$(PRIVATE_MLS_CATS) \
-               -D target_build_variant=$(TARGET_BUILD_VARIANT) \
+               -D target_build_variant=eng \
                -s $^ > $@
        $(hide) sed '/dontaudit/d' $@ > $@.dontaudit

@@ -105,7 +105,7 @@ $(LOCAL_BUILT_MODULE): $(sepolicy_policy.conf) $(HOST_OUT_EXECUTABLES)/checkpoli
        $(hide) $(HOST_OUT_EXECUTABLES)/checkpolicy -M -c $(POLICYVERS) -o $@.tmp $<
        $(hide) $(HOST_OUT_EXECUTABLES)/checkpolicy -M -c $(POLICYVERS) -o $(dir $<)/$(notdir $@).dontaudit $<.dontaudit
        $(hide) $(HOST_OUT_EXECUTABLES)/sepolicy-analyze $@.tmp permissive > $@.permissivedomains
-       $(hide) if [ "$(TARGET_BUILD_VARIANT)" = "user" -a -s $@.permissivedomains ]; then \
+#      $(hide) if [ "$(TARGET_BUILD_VARIANT)" = "user" -a -s $@.permissivedomains ]; then \
                echo "==========" 1>&2; \
                echo "ERROR: permissive domains not allowed in user builds" 1>&2; \
                echo "List of invalid domains:" 1>&2; \
```

[1]:https://onlinesso.mediatek.com/Pages/FAQ.aspx?List=SW&FAQID=FAQ06317
