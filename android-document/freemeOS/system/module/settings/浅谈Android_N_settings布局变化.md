### 一.概述：
Android N settings界面最直观的改变就是增加了一个侧滑，当手指从屏幕左侧边缘向屏幕中间滑动，即可呼出侧滑菜单，内容基本上和settings主界面一样，只不过没有每个item的详细描述。  
增加侧滑菜单可以让用户在settings二级或者三级菜单的时候迅速回到一级菜单，更加方便！
### 二.布局变化
1. 在Android M 中，settings的布局是写在dashboard_categories.xml中，如

```
<dashboard-categories
&nbsp;&nbsp;&nbsp;&nbsp;xmlns:android="http://schemas.android.com/apk/res/android"
&nbsp;&nbsp;&nbsp;&nbsp;xmlns:settings="http://schemas.android.com/apk/res/com.android.settings">

    <!-- WIRELESS and NETWORKS -->
    <dashboard-category
    &nbsp;&nbsp;&nbsp;&nbsp;android:id="@+id/wireless_section"
    &nbsp;&nbsp;&nbsp;&nbsp;android:key="@string/category_key_wireless"
    &nbsp;&nbsp;&nbsp;&nbsp;android:title="@string/header_category_wireless_networks" >

        <!-- Wifi -->
        <dashboard-tile
                android:id="@+id/wifi_settings"
                android:title="@string/wifi_settings_title"
                android:fragment="com.android.settings.wifi.WifiSettings"
                android:icon="@drawable/ic_settings_wireless"
                />
       </dashboard-category>
    </dashboard-categories>
```

    然后在SettingsActivity中通过XmlResourceParser来解析dashboard_categories.xml文件，生成List<DashboardCategory>，之后在DashboardSummary.java中拿到List<DashboardCategory>数据，生成主界面

2.在Android N 中 settings去掉了dashboard_categories.xml，那么数据如何获取的呢？在Android M 中，SettingsActivity 直接继承 Activity，但是在 N 中，不是直接继承Activity，而是继承SettingsDrawerActivity，在com.android.settingslib包中，SettingsDrawerActivity的作用就是实现侧滑的效果，同时提供数据的获取，getDashboardCategories()。
在SettingsDrawerActivity中:

```
    public List<DashboardCategory> getDashboardCategories() {
        if (sDashboardCategories == null) {
            sTileCache = new HashMap<>();
            sConfigTracker = new InterestingConfigChanges();
            // Apply initial current config.
            sConfigTracker.applyNewConfig(getResources());
            sDashboardCategories = TileUtils.getCategories(this, sTileCache);
        }
        return sDashboardCategories;
    }
```

在TileUtils中：

```
private static final String SETTINGS_ACTION = "com.android.settings.action.SETTINGS";
private static final String SETTING_PKG = "com.android.settings";
```

调用方法 getTilesForAction(context, user, SETTINGS_ACTION, cache, null, tiles, true);

```
    private static void getTilesForAction(Context context,
            UserHandle user, String action, Map<Pair<String, String>, Tile> addedCache,
            String defaultCategory, ArrayList<Tile> outTiles, boolean requireSettings) {
        Intent intent = new Intent(action);
        if (requireSettings) {
            intent.setPackage(SETTING_PKG);
        }
        getTilesForIntent(context, user, intent, addedCache, defaultCategory, outTiles,
                requireSettings, true);
    }

    public static void getTilesForIntent(Context context, UserHandle user, Intent intent,
            Map<Pair<String, String>, Tile> addedCache, String defaultCategory, List<Tile> outTiles,
            boolean usePriority, boolean checkCategory) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> results = pm.queryIntentActivitiesAsUser(intent,
                PackageManager.GET_META_DATA, user.getIdentifier());
                for (ResolveInfo resolved : results) {
            /// M: Extra package white list for add item to Settings
            if (!resolved.system && !ArrayUtils.contains(EXTRA_PACKAGE_WHITE_LIST,
                    resolved.activityInfo.packageName)) {
                // Do not allow any app to add to settings, only system ones.
                continue;
            }
            ActivityInfo activityInfo = resolved.activityInfo;
            Bundle metaData = activityInfo.metaData;
            String categoryKey = defaultCategory;
            if (checkCategory && ((metaData == null) || !metaData.containsKey(EXTRA_CATEGORY_KEY))
                    && categoryKey == null) {
                Log.w(LOG_TAG, "Found " + resolved.activityInfo.name + " for intent "
                        + intent + " missing metadata "
                        + (metaData == null ? "" : EXTRA_CATEGORY_KEY));
                continue;
            } else {
                categoryKey = metaData.getString(EXTRA_CATEGORY_KEY);
                /// M: set default category when extra data not set category
                if (categoryKey == null) {
                    categoryKey = defaultCategory;
                }
            }
            Pair<String, String> key = new Pair<String, String>(activityInfo.packageName,
                    activityInfo.name);
            Tile tile = addedCache.get(key);
            if (tile == null) {
                tile = new Tile();
                tile.intent = new Intent().setClassName(
                        activityInfo.packageName, activityInfo.name);
                tile.category = categoryKey;
                tile.priority = usePriority ? resolved.priority : 0;
                tile.metaData = activityInfo.metaData;
                updateTileData(context, tile, activityInfo, activityInfo.applicationInfo,
                        pm);
                /// M: Drawer plugin support @{
                if (sDrawerExt == null) {
                    sDrawerExt = UtilsExt.getDrawerPlugin(context);
                }
                if (activityInfo.name.endsWith("PrivacySettingsActivity")) {
                    sDrawerExt.setFactoryResetTitle(tile);
                } else if (activityInfo.name.endsWith("SimSettingsActivity")) {
                    tile.title = sDrawerExt.customizeSimDisplayString(tile.title.toString(),
                            SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                }
                addedCache.put(key, tile);
            }
            if (!tile.userHandle.contains(user)) {
                tile.userHandle.add(user);
            }
            if (!outTiles.contains(tile)) {
                outTiles.add(tile);
            }
        }
    }
```

在PKMS中获取指定包名的List<ResolveInfo>，然后在getTilesForIntent方法中循环遍历，生成对应的Tile，然后将Tile添加到outTiles(ArrayList<Tile>)中去。获取到ArrayList<Tile>之后，回到前面的sDashboardCategories = TileUtils.getCategories(this, sTileCache);在TileUtils中，如下：

```
    public static List<DashboardCategory> getCategories(Context context,
            HashMap<Pair<String, String>, Tile> cache) {
            ...
                    HashMap<String, DashboardCategory> categoryMap = new HashMap<>();
        for (Tile tile : tiles) {
            DashboardCategory category = categoryMap.get(tile.category);
            if (category == null) {
                category = createCategory(context, tile.category);
                if (category == null) {
                    Log.w(LOG_TAG, "Couldn't find category " + tile.category);
                    continue;
                }
                categoryMap.put(category.key, category);
            }
            category.addTile(tile);
        }
        ArrayList<DashboardCategory> categories = new ArrayList<>(categoryMap.values());
        for (DashboardCategory category : categories) {
            Collections.sort(category.tiles, TILE_COMPARATOR);
        }
        Collections.sort(categories, CATEGORY_COMPARATOR);
        if (DEBUG_TIMING) Log.d(LOG_TAG, "getCategories took "
                + (System.currentTimeMillis() - startTime) + " ms");
        return categories;
    }
```

遍历tiles，封装成对应的DashboardCategory，最终返回给DashboardSummary.java，把数据传给DashboardAdapter，生成主界面。

### 三.如何在Settings主界面添加item

1. 在PKMS中是通过解析对应package的AndroidManifest.xml文件来获取要显示的Activity信息的，在前面的TileUtils中定义了private static final String SETTINGS_ACTION = "com.android.settings.action.SETTINGS"，并且把这个作为参数传递给PKMS，所以新增的Activity属性中，要加上这个。
2. 新增的“智能辅助”在AndroidManifest.xml中添加，如下

```
        <activity android:name="Settings$FreemeIntelligenceAssistantActivity"
               android:label="@string/freeme_settings_intelligence_assistant_title"
               android:icon="@drawable/freeme_settings_ic_intelligence_assistant_title"
               android:taskAffinity=""
               android:parentActivityName="Settings" >
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="com.android.settings.SHORTCUT" />
            </intent-filter>
            <intent-filter android:priority="8">
                <action android:name="com.android.settings.action.SETTINGS" />
            </intent-filter>
            <meta-data android:name="com.android.settings.category"
                       android:value="com.android.settings.category.device" />
            <meta-data android:name="com.android.settings.FRAGMENT_CLASS"
                android:value="com.freeme.settings.FreemeIntelligenceAssistant" />
            <meta-data android:name="com.android.settings.PRIMARY_PROFILE_CONTROLLED"
                android:value="true" />
        </activity>
```

这样在Settings主界面和侧滑菜单中就可以看到“智能辅助”了。

