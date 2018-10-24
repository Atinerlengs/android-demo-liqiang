# 一、功能描述
通过添加一个按钮实现settings整体布局的切换，并在grid布局中添加三个常用功能。
# 二、详细实现
## 1.切换按钮的实现

//设置切换按钮的背景图片以及可见性

```
public boolean onCreateOptionsMenu(Menu menu) {
    mChangeModeMenuItem = menu.findItem(R.id.mode_change);
        if(mChangeModeMenuItem != null && ifGridMode){
            mChangeModeMenuItem.setIcon(R.drawable.ic_mode_list);
        }else if(mChangeModeMenuItem != null && !ifGridMode){
            mChangeModeMenuItem.setIcon(R.drawable.ic_mode_grid);
        }
        if(this.getClass().getName().equals("com.android.settings.SubSettings") 
            && mChangeModeMenuItem != null){
            mChangeModeMenuItem.setVisible(false);
        }else 			if(this.getClass().getName().equals("com.android.settings.Settings")
            && mChangeModeMenuItem != null){
            mChangeModeMenuItem.setVisible(true);
        }
}
```

//数据库的写入和刷新页面

```
public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
            case R.id.mode_change:
                gridSp.edit().putBoolean(IFGRIDMODESP, ifGridMode ? false : true).commit();
                refresh();
                return true;
            }
            return false;
        }
```


##  2.Grid view
SettingsActivity.java

整体布局：loadCategoriesFromResource(R.xml.dashboard_categories_2, categories, this);

DashboardSummary.java

rebuildUI()

//grid与listview下不同的category布局

```
if(!ifGridMode){
    categoryView=mLayoutInflater.inflate(R.layout.dashboard_category_listmode,mDashboard,false);
}else{
    categoryView = mLayoutInflater.inflate(R.layout.dashboard_category, 
    mDashboard,false);
}
```

//gridview设置显示的最大数

```
if(ifGridMode){
    tileList.add(tileView.getTitleTextView().getText().toString());
    if (categoryContent.getChildCount() >= MAX_TILE_NUM) {
        break;
     }
}

```

//gridview下添加DeviceInfoSettings和MoreSystemSettings

```
if((n == count - 1) && ifGridMode) {
    DashboardTileView tileAboutView = new DashboardTileView(context);
    DashboardTile tileAboutSetting = new DashboardTile();
    tileAboutSetting.title = getString(R.string.about_settings);
    tileAboutSetting.iconRes = R.drawable.ic_settings_about_2;
    tileAboutSetting.fragment = "com.android.settings.DeviceInfoSettings";
    updateTileView(context, res, tileAboutSetting, tileAboutView.getImageView(),
                        tileAboutView.getTitleTextView(), 		tileAboutView.getStatusTextView());
    tileAboutView.setTile(tileAboutSetting);
   	categoryContent.addView(tileAboutView);
    String[] titles = new String[MAX_TILE_NUM];
    for(int i = 0; i < tileList.size(); i++) {
          titles[i] = tileList.get(i);
    }
    Bundle bundle = new Bundle();
    bundle.putStringArray("dashBoardTile", titles);
    DashboardTileView tileMoreView = new DashboardTileView(context);
    DashboardTile tileMoreSetting = new DashboardTile();
    tileMoreSetting.title = getString(R.string.radio_controls_title);
    tileMoreSetting.iconRes = R.drawable.ic_system_settings_more_2;
    tileMoreSetting.fragment = "com.android.settings.MoreSystemSettings";
    tileMoreSetting.fragmentArguments = bundle;
    updateTileView(context, res, tileMoreSetting, tileMoreView.getImageView(),
                        tileMoreView.getTitleTextView(), 	tileMoreView.getStatusTextView());
    tileMoreView.setTile(tileMoreSetting);
    categoryContent.addView(tileMoreView);
  }
```

DashboardTileView.java

//grid与listview下不同的TileView

```
if(!ifGridMode){
 	view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile_listmode, this);
}else{
   	view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile, this);
}
```

## 3.Grid view下三个常用功能
DashboardSummary.java

 //三个常用功能获取与设置监听

```
    commonAccessibilityExt = (TextView)rootView.findViewById(R.id.common_accessibility_ext);
  commonAccessibilityExt.setOnClickListener(this);
  commonLock = (TextView)rootView.findViewById(R.id.common_lock);
  commonLock.setOnClickListener(this);
  commonNotification = (TextView)rootView.findViewById(R.id.common_notification);
  commonNotification.setOnClickListener(this);
```

//三个常用功能的相关信息获取

```
  CustomHobbyService mService=new CustomHobbyService(getActivity());
  values= mService.queryTopThree();
  switch (values.size()) {
            case 3:
            thirdLink= (String)values.get(2).get(CustomHobbyUtils.LINK);
                thirdTitleId=  (int) values.get(2).get(CustomHobbyUtils.CONTENT);
                thirdParentId=  (int) values.get(2).get(CustomHobbyUtils.PARENT_TITLE);
                thirdComment= (String)values.get(2).get(CustomHobbyUtils.COMMENT);
                setTitleText(commonLock, thirdParentId, thirdTitleId);
            case 2:
                secondLink= (String)values.get(1).get(CustomHobbyUtils.LINK);
                secondTitleId=  (int) values.get(1).get(CustomHobbyUtils.CONTENT);
                secondParentId=  (int) values.get(1).get(CustomHobbyUtils.PARENT_TITLE);
                secondComment= (String)values.get(1).get(CustomHobbyUtils.COMMENT);
                setTitleText(commonNotification, secondParentId, secondTitleId);
            case 1:
                firstLink= (String)values.get(0).get(CustomHobbyUtils.LINK);
                firstTitleId=  (int) values.get(0).get(CustomHobbyUtils.CONTENT);
                firstParentId=  (int) values.get(0).get(CustomHobbyUtils.PARENT_TITLE);
                firstComment=(String)values.get(0).get(CustomHobbyUtils.COMMENT);
                setTitleText(commonAccessibilityExt, firstParentId, firstTitleId);
                break;
            default:
               break;
    }
```

//三个常用功能的启动

```
private void startLink(String link,int parentid,int titleid,String comment){.....}
```
