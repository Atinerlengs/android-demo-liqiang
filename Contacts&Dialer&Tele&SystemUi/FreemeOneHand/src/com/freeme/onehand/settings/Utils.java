package com.freeme.onehand.settings;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;

public final class Utils {

    public static boolean isRTL(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)
                == Configuration.SCREENLAYOUT_LAYOUTDIR_RTL;
    }

    public static boolean hasPackage(Context context, String pkg) {
        if (context == null) {
            return false;
        }
        try {
            context.getPackageManager().getApplicationInfo(pkg,
                    PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isGoogleTalkBackEnabled(Context context) {
        String accesibilityService = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return accesibilityService != null && accesibilityService
                .matches("(?i).*com.google.android.marvin.talkback.TalkBackService.*");
    }

    public static void turnOffGoogleTalkBack(Context context) {
        ContentResolver resolver = context.getContentResolver();
        final TextUtils.SimpleStringSplitter splitter =
                new TextUtils.SimpleStringSplitter(':');

        ArraySet<ComponentName> enabledServices = new ArraySet<>();
        String enabledServicesSetting = Settings.Secure.getString(resolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabledServicesSetting)) {
            return;
        }
        splitter.setString(enabledServicesSetting);
        while (splitter.hasNext()) {
            enabledServices.add(ComponentName.unflattenFromString(splitter.next()));
        }

        enabledServices.remove(ComponentName.unflattenFromString(
                "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService"));

        StringBuilder enabledServicesSettingBuilder = new StringBuilder();
        for (ComponentName cn : enabledServices) {
            if (enabledServicesSettingBuilder.length() != 0) {
                enabledServicesSettingBuilder.append(':');
            }
            enabledServicesSettingBuilder.append(cn.flattenToString());
        }
        Settings.Secure.putString(resolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                enabledServicesSettingBuilder.toString());
        Settings.Secure.putInt(resolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                (enabledServicesSettingBuilder.length() != 0) ? 1 : 0);
    }

    public static boolean hasNavigationBar() {
        Resources resources = Resources.getSystem();
        int resId = resources.getIdentifier("config_showNavigationBar", "bool",
                "android");
        boolean hasNavigationBar = resources.getBoolean(resId);
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
        if ("1".equals(navBarOverride)) {
            hasNavigationBar = false;
        } else if ("0".equals(navBarOverride)) {
            hasNavigationBar = true;
        }
        return hasNavigationBar;
    }

    public static final boolean HAS_TRIPLY_HOMEKEY = false;

    private Utils() {
        // Do not initialize
    }
}
