package com.freeme.filemanager.activity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freeme.filemanager.FMApplication;
import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.cleanup.CleanupMainActivity;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.PermissionUtil;
import com.freeme.filemanager.util.StorageHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.util.FileCategoryHelper.CategoryInfo;
import com.freeme.filemanager.util.FileCategoryHelper.FileCategory;

import com.freeme.filemanager.view.FileCategoryItem;
import com.freeme.filemanager.view.StorageCategoryItem;
import com.freeme.filemanager.view.TabButton;
import com.freeme.filemanager.view.circleprogress.RoundProgressBar;

import static com.freeme.filemanager.FMIntent.EXTRA_CATEGORY_INFO;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_TYPE;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_TOTAL;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_USE;

public class StorageCategoryActivity extends Activity implements FMApplication.SDCardChangeListener,
        View.OnClickListener {
    private static final String TAG = "MoneyInfoActivity";
    private static List<FileCategoryItem> mFileCategoryItems
                = new ArrayList<FileCategoryItem>();
    private static int[] icons = new int[] {
            R.color.grid_horizontal_music_color,
            R.color.grid_horizontal_video_color,
            R.color.grid_horizontal_image_color,
            R.color.grid_horizontal_document_color,
            R.color.grid_horizontal_apk_color,
            R.color.grid_horizontal_other_color};
    private static int[] mTexts = new int[] { R.string.category_music,
            R.string.category_video, R.string.category_picture,
            R.string.category_document,R.string.category_apk,
            R.string.category_other };
    private static FileCategory[] categories = { FileCategory.Music,
            FileCategory.Video, FileCategory.Picture, FileCategory.Doc,
            FileCategory.Apk,FileCategory.Other };

    private static final int STORAGE_TABS_ONE = 1;
    private static final int STORAGE_TABS_TWO = 2;

    private long mTotalSize;
    private long mUseSize;

    private long mTotalSizeAll;
    private long mUseSizeAll;

    private int mStorageType;

    private FileCategoryHelper mFileCagetoryHelper;

    private RoundProgressBar mRoundProgressBar;

    private AsyncTask<Void, Void, Object> mRefreshCategoryInfoTask;

    private CategoryItemAdapter mCategoryAdapter;

    private Map<String, StorageCategoryItem> mCategoryStorageItems = new LinkedHashMap<>();

    String mInternalStorageTitle;
    String mSdcardTitle;
    String mUsbStorageTitle;
    long mLastClickTime;

    private LinearLayout mTabBtnLinear;
    private View mView0;
    private View mView1;
    private TabButton mLeftBtn;
    private TabButton mCenterBtn;
    private TabButton mRrightBtn;
    private int mTabCnt = STORAGE_TABS_ONE;
    private int mSeletctId;

    private int[] iArrProgress = {0, 0, 0, 0, 0, 0};

    static {
        for (int i = 0; i < mTexts.length; i++) {
            FileCategoryItem item = new FileCategoryItem();
            item.iconId = icons[i];
            item.textStringId = mTexts[i];
            item.category = categories[i];
            mFileCategoryItems.add(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_circle_progressbar);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        Util.setBackTitle(actionBar, getString(R.string.app_name));
        setTitle(R.string.all_storage);
        actionBar.setDisplayShowHomeEnabled(false);

        mTabBtnLinear = (LinearLayout) findViewById(R.id.tab_btn_layout);
        mLeftBtn = (TabButton) findViewById(R.id.btn_left);
        mLeftBtn.setOnClickListener(this);
        mView0 = (View) findViewById(R.id.view0);

        mCenterBtn = (TabButton) findViewById(R.id.btn_mid);
        mCenterBtn.setOnClickListener(this);

        mView1 = (View) findViewById(R.id.view1);
        mRrightBtn = (TabButton) findViewById(R.id.btn_right);
        mRrightBtn.setOnClickListener(this);
        mRoundProgressBar = (RoundProgressBar) findViewById(R.id.roundProgressBar);

        mTotalSize = getIntent().getLongExtra(EXTRA_STORAGE_TOTAL, 0);
        mUseSize = getIntent().getLongExtra(EXTRA_STORAGE_USE, 0);
        mStorageType = getIntent().getIntExtra(EXTRA_STORAGE_TYPE, 0);
        mTotalSizeAll = mTotalSize;
        mUseSizeAll = mUseSize;

        mFileCagetoryHelper = new FileCategoryHelper(this);

        initDeviceInfo();

        mCategoryAdapter = new CategoryItemAdapter(this, mFileCategoryItems);
        GridView gridView = (GridView) findViewById(R.id.category_buttons);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                long currentTime = Calendar.getInstance().getTimeInMillis();
                if (currentTime - mLastClickTime > Util.MIN_CLICK_DELAY_TIME) {
                    mLastClickTime = currentTime;
                    FileCategoryItem selectItem = mFileCategoryItems.get(position);
                    Intent intent = new Intent(StorageCategoryActivity.this, StorageFileListActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra(EXTRA_STORAGE_TYPE, mStorageType);
                    intent.putExtra(EXTRA_CATEGORY_INFO, selectItem.category);
                    startActivity(intent);
                }
            }
        });
        gridView.setFocusable(false);
        gridView.setAdapter(mCategoryAdapter);

        final RelativeLayout fileCleanRl = (RelativeLayout) findViewById(R.id.storage_file_clean);
        fileCleanRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent info = new Intent(StorageCategoryActivity.this, CleanupMainActivity.class);
                info.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                info.putExtra(EXTRA_STORAGE_TOTAL, mTotalSizeAll);
                info.putExtra(EXTRA_STORAGE_USE, mUseSizeAll);
                info.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(info);
                finish();
            }
        });

        FMApplication app = (FMApplication) getApplicationContext();
        app.addSDCardChangeListener(this);
    }

    private void initDeviceInfo() {
        mInternalStorageTitle = getResources().getString(R.string.interior_info_storage);
        mSdcardTitle = getResources().getString(R.string.storage_sd_card);
        mUsbStorageTitle = getResources().getString(R.string.storage_external_usb);

        final Util.SDCardInfo sdCardInfo = Util.getSDCardInfo();
        if (sdCardInfo != null && sdCardInfo.total >= sdCardInfo.free) {
            mCategoryStorageItems.put(mSdcardTitle,
                    new StorageCategoryItem(mSdcardTitle));
            mTabCnt++;
            mTotalSizeAll = mTotalSizeAll + sdCardInfo.total;
            mUseSizeAll = mUseSizeAll + (sdCardInfo.total - sdCardInfo.free);
        } else {
            if (mCategoryStorageItems.keySet().contains(mSdcardTitle)) {
                mCategoryStorageItems.remove(mSdcardTitle);
                mTabCnt--;
            }
        }

        final Util.UsbStorageInfo usbStorageInfo = Util.getUsbStorageInfo();
        if (usbStorageInfo != null && usbStorageInfo.total >= usbStorageInfo.free) {
            mCategoryStorageItems.put(mUsbStorageTitle,
                    new StorageCategoryItem(mUsbStorageTitle));
            mTabCnt++;
            mTotalSizeAll = mTotalSizeAll + usbStorageInfo.total;
            mUseSizeAll = mUseSizeAll + (usbStorageInfo.total - usbStorageInfo.free);
        } else {
            if (mCategoryStorageItems.keySet().contains(mUsbStorageTitle)) {
                mCategoryStorageItems.remove(mUsbStorageTitle);
                mTabCnt--;
            }
        }
        mSeletctId = R.id.btn_left;
        resetState(mSeletctId);
    }

    public void onClick(View v) {
        mSeletctId = v.getId();
        resetState(mSeletctId);
    }

    private void resetState(int id) {
        mLeftBtn.setSelected(false);
        mCenterBtn.setSelected(false);
        mRrightBtn.setSelected(false);
        String totalString;
        boolean hasSdcard = mCategoryStorageItems.keySet().contains(mSdcardTitle);

        switch (mTabCnt) {
            case STORAGE_TABS_ONE:
                mTabBtnLinear.setVisibility(View.GONE);
                mStorageType = GlobalConsts.IS_MEMORY_CARD;
                break;

            case STORAGE_TABS_TWO:
                mTabBtnLinear.setVisibility(View.VISIBLE);
                mLeftBtn.setVisibility(View.VISIBLE);
                mView0.setVisibility(View.GONE);
                mCenterBtn.setVisibility(View.GONE);
                mView1.setVisibility(View.VISIBLE);
                mRrightBtn.setVisibility(View.VISIBLE);
                mRrightBtn.setText(hasSdcard ? R.string.storage_sd_card : R.string.storage_external_usb);
                break;

            default:
                mTabBtnLinear.setVisibility(View.VISIBLE);
                mLeftBtn.setVisibility(View.VISIBLE);
                mView0.setVisibility(View.VISIBLE);
                mCenterBtn.setVisibility(View.VISIBLE);
                mView1.setVisibility(View.VISIBLE);
                mRrightBtn.setVisibility(View.VISIBLE);
                break;
        }

        switch (id) {
            case R.id.btn_left:
                mLeftBtn.setSelected(true);
                mStorageType = GlobalConsts.IS_MEMORY_CARD;
                break;
            case R.id.btn_mid:
                mCenterBtn.setSelected(true);
                mStorageType = GlobalConsts.IS_SD_CARD;
                break;
            case R.id.btn_right:
                mRrightBtn.setSelected(true);
                if (mTabCnt == STORAGE_TABS_TWO) {
                    mStorageType = hasSdcard ? GlobalConsts.IS_SD_CARD:GlobalConsts.IS_USBOTG_CARD;
                } else { //three storage tabs
                    mStorageType = GlobalConsts.IS_USBOTG_CARD;
                }
                break;
            default:
                break;
        }

        totalString = getSizeString(mStorageType);

        refreshCategoryInfo();
        mRoundProgressBar.setBottomText(mInternalStorageTitle);
        mRoundProgressBar.setBottomText2(Formatter.formatFileSize(this, mUseSize) + "/"
                + totalString);
        refreshProcess(mUseSize, mTotalSize, mRoundProgressBar.getMax());
    }

    private String getSizeString(int mStorageType) {
        String totalString;
        switch (mStorageType) {
            case GlobalConsts.IS_MEMORY_CARD:
                final Util.MemoryCardInfo memoryCardInfo = Util.getMemoryCardInfo();
                mTotalSize = memoryCardInfo.total;
                mUseSize = mTotalSize - memoryCardInfo.free;
                totalString = Util.convertStorage(mTotalSize);
                break;
            case GlobalConsts.IS_SD_CARD:
                final Util.SDCardInfo sdCardInfo = Util.getSDCardInfo();
                mTotalSize = sdCardInfo.total;
                mUseSize = mTotalSize - sdCardInfo.free;
                totalString = Formatter.formatFileSize(this, mTotalSize);
                break;
            case GlobalConsts.IS_USBOTG_CARD:
                final Util.UsbStorageInfo usbStorageInfo = Util.getUsbStorageInfo();
                mTotalSize = usbStorageInfo.total;
                mUseSize = mTotalSize - usbStorageInfo.free;
                totalString = Formatter.formatFileSize(this, mTotalSize);
                break;
            default:
                totalString = "";
                break;
        }
        return totalString;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCategoryInfo();
    }

    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("select_tab", mSeletctId);
        super.onSaveInstanceState(outState);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mSeletctId = savedInstanceState.getInt("select_tab");
        resetState(mSeletctId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMountStateChange(int flag) {
        if (flag == FMApplication.SDCardChangeListener.flag_UMMOUNT) {
            finish();
        } else if (flag == FMApplication.SDCardChangeListener.flag_INJECT) {
            if (!mCategoryStorageItems.keySet().contains(mSdcardTitle)) {
                mCategoryStorageItems.put(mSdcardTitle, new StorageCategoryItem(mSdcardTitle));
                mTabCnt++;
                final Util.SDCardInfo sdCardInfo = Util.getSDCardInfo();
                mTotalSizeAll = mTotalSizeAll + sdCardInfo.total;
                mUseSizeAll = mUseSizeAll + (sdCardInfo.total - sdCardInfo.free);
                mSeletctId = R.id.btn_left;
                resetState(mSeletctId);
            }
        }
    }

    private void refreshProcess(final long process, final long total, final int max) {
        final int N = (int) ((float)process / total * max);
        mRoundProgressBar.setProgress(N);
        /*new Thread(new Runnable() {
            private int p = 0;

            @Override
            public void run() {
                mRoundProgressBar.setProgress(p);
                while (p < N) {
                    p += 1;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mRoundProgressBar.setProgress(p);
                        }
                    });

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();*/
    }

    private void refreshCategoryInfo() {
        if (!PermissionUtil.hasStoragePermissions(this)) {
            this.finish();
            return;
        }
        mRefreshCategoryInfoTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object... arg0) {
                mFileCagetoryHelper.refreshCategoryInfo(mStorageType, true);
                return null;
            }

            protected void onPostExecute(Object paramVoid) {
                setCategoryInfo();
            }
        };
        mRefreshCategoryInfoTask.execute(new Void[0]);
    }

    private void setCategoryInfo() {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            StorageHelper.MountedStorageInfo mountedStorageInfo = StorageHelper.getInstance(this).getMountedStorageInfo();
            if (mountedStorageInfo != null) {
                long useSize = 0;
                if (FileCategoryHelper.sCategories != null) {
                    int i = 0;
                    for (FileCategoryItem fc : mFileCategoryItems) {
                        if (!fc.category.equals(FileCategory.Other)) {
                            CategoryInfo categoryInfo = mFileCagetoryHelper.getCategoryInfos().get(fc.category);
                            if (categoryInfo != null) {
                                fc.size = categoryInfo.size;
                                useSize += categoryInfo.size;
                                iArrProgress[i] = (int)(categoryInfo.size*mRoundProgressBar.getMax()/mTotalSize);
                                i++;
                            }
                        } else {
                            fc.size = mUseSize - useSize;
                            iArrProgress[i] = (int) ((mUseSize - useSize)* mRoundProgressBar.getMax()/mTotalSize );
                        }
                    }
                    mRoundProgressBar.setAllTypeProgress(iArrProgress);
                    mCategoryAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private class CategoryItemAdapter extends BaseAdapter {

        private List<FileCategoryItem> mItems = null;
        private Context mContext;

        public CategoryItemAdapter(Context context, List<FileCategoryItem> items) {
            mItems = items;
            mContext = context;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(mContext).inflate(
                    R.layout.layout_fast_category_item_horizontal, null);
            ImageView imageView = (ImageView) view
                    .findViewById(R.id.category_icon);
            TextView tvName = (TextView) view.findViewById(R.id.category_text);
            TextView tvConunt = (TextView) view
                    .findViewById(R.id.category_count_tv);
            FileCategoryItem item = mItems.get(position);
            imageView.setImageResource(icons[position]);
            tvName.setText(mItems.get(position).textStringId);
            tvConunt.setText(Formatter.formatFileSize(mContext, item.size));
            ImageView rightArrow = (ImageView) view.findViewById(R.id.dir_arrow);
            view.setTag(item);

            if(item.category == FileCategory.Other) {
                tvName.setEnabled(false);
                tvConunt.setEnabled(false);
                rightArrow.setVisibility(View.GONE);
                view.setOnClickListener(null);
            }
            return view;
        }
    }
}
