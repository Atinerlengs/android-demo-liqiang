package com.freeme.filemanager.activity.cleanup;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.BaseActivity;
import com.freeme.filemanager.activity.cleanup.largefiles.LargeFilesActivity;
import com.freeme.filemanager.activity.cleanup.special.WeChatSpecialActivity;
import com.freeme.filemanager.controller.CleanListAdapter;
import com.freeme.filemanager.controller.IBackHandledInterface;
import com.freeme.filemanager.fragment.BaseFragment;
import com.freeme.filemanager.fragment.cleanup.DeepCleanFragment;
import com.freeme.filemanager.util.AsyncGarbageCleanupHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.ListItemTextView;
import com.freeme.filemanager.view.circleprogress.RoundProgressBar;
import com.freeme.filemanager.view.garbage.CleanListItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.freeme.filemanager.FMIntent.EXTRA_BACK_TITLE;
import static com.freeme.filemanager.FMIntent.EXTRA_CHOSE_CLEAN_LIST;
import static com.freeme.filemanager.FMIntent.EXTRA_DEEP_CLEAN;

public class GarbageCleaningActivity extends BaseActivity implements
        IBackHandledInterface, View.OnClickListener,
        AsyncGarbageCleanupHelper.GarbageCleanupStatesListener {

    private Handler mHandler;
    private ActionBar mActionBar;
    private AsyncGarbageCleanupHelper mAsyncGarbageCleanupHelper;
    private CleanListAdapter mAdapter;

    private RoundProgressBar mRoundProgressBar;
    private ListView mListView;
    private Button mCleanUpBtn;
    private List<CleanListItem> mCheckList;
    private List<CleanListItem> mAnimList;
    private boolean mListAnimaFinish;
    private LinearLayout mEntrance;

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_garbage_cleaning);
        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        Util.setBackTitle(mActionBar, getResources().getString(R.string.app_name));

        mCleanUpBtn = (Button)findViewById(R.id.cleanup_button);

        mRoundProgressBar = (RoundProgressBar)findViewById(R.id.roundProgressBar);
        mRoundProgressBar.setOnlyColor(true);
        mRoundProgressBar.setProgress(0);

        Intent intent = this.getIntent();
        mCheckList = (List<CleanListItem>) intent.getSerializableExtra(EXTRA_CHOSE_CLEAN_LIST);

        //ArraryList deep copy
        mAnimList = new ArrayList<>();
        CleanListItem[] tradingArray = new CleanListItem[mCheckList.size()];
        Collections.addAll(mAnimList, mCheckList.toArray(tradingArray));

        mListView = (ListView)findViewById(R.id.clean_list);
        mListView.setVisibility(View.VISIBLE);

        mAdapter = new CleanListAdapter(this, R.layout.layout_cleaning_list_item, mAnimList);
        mListView.setAdapter(mAdapter);

        mHandler = new Handler(getMainLooper());

        mAsyncGarbageCleanupHelper = new AsyncGarbageCleanupHelper(this);

        mAsyncGarbageCleanupHelper.setGarbageCleanupItem(mCheckList);
        mAsyncGarbageCleanupHelper.setGarbageCleanupStatesListener(this);
        mAsyncGarbageCleanupHelper.setState(AsyncGarbageCleanupHelper.STATE_SCAN_FINISH);
        mAsyncGarbageCleanupHelper.cleanUp();

        mCleanUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mAsyncGarbageCleanupHelper != null)
                        && (mAsyncGarbageCleanupHelper.getState() == AsyncGarbageCleanupHelper.STATE_CLEANUP_FINISH)) {
                    finish();
                }
            }
        });

        initDeepCleanEntrance();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void setSelectedFragment(BaseFragment selectedFragment) {
    }

    @Override
    public void onScanGarbageFinish(Map<Integer, List<AsyncGarbageCleanupHelper.GarbageItem>> mChildMap) {
    }

    @Override
    public void onFinish(int i, long l, int j) {
    }

    @Override
    public void onUpdateUI(final int state) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case AsyncGarbageCleanupHelper.STATE_START_SCAN:
                        break;

                    case AsyncGarbageCleanupHelper.STATE_SCAN_FINISH:
                        break;

                    case AsyncGarbageCleanupHelper.STATE_START_CLEANUP:
                        break;

                    case AsyncGarbageCleanupHelper.STATE_CLEANUP_FINISH:
                        mAdapter.notifyDataSetChanged();
                        String cleanMsg0 = "";
                        String cleanMsg1;
                        if (mAsyncGarbageCleanupHelper != null) {
                            long cleanSize = mAsyncGarbageCleanupHelper.getTotalDeletedFileSize();
                            if (cleanSize > 0) {
                                String emptyDir = Util.convertStorage(mAsyncGarbageCleanupHelper.getTotalDeletedFileSize());
                                if (emptyDir.equals("64.0 KB") || emptyDir.equals("32.0 KB")) {
                                    cleanMsg1 = getResources().getString(R.string.no_garbage_result);
                                } else {
                                    cleanMsg0 = getResources().getString(R.string.storage_cleaned);
                                    cleanMsg1 = emptyDir;
                                }
                            } else {
                                cleanMsg1 = getResources().getString(R.string.no_garbage_result);
                            }
                            mAsyncGarbageCleanupHelper.stopRunning();

                            updateLayout(cleanMsg0, cleanMsg1);
                        }
                        mCleanUpBtn.setEnabled(true);
                        mCleanUpBtn.setText(getResources().getString(R.string.garbage_clean_finish));
                        break;
                }
            }
        });

    }

    @Override
    public void updateCleanProgress(int progress) {
        mRoundProgressBar.doAnimation(progress);
        deleteCell(mListView, 0);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.deep_clean: {
                FragmentManager manager = getFragmentManager();
                Fragment fragment = new DeepCleanFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable(EXTRA_DEEP_CLEAN, EXTRA_DEEP_CLEAN);
                fragment.setArguments(bundle);
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.cleaning_main_page, fragment);
                transaction.commit();
            }
                break;
            case R.id.wechat_special: {
                Intent intent = new Intent(this, WeChatSpecialActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(EXTRA_BACK_TITLE, mActionBar.getTitle().toString());
                startActivity(intent);
            }
                break;
            case R.id.clear_large_files: {
                Intent intent = new Intent(this, LargeFilesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(EXTRA_BACK_TITLE, mActionBar.getTitle().toString());
                startActivity(intent);
            }
                break;
        }
    }

    private void updateLayout(final String msg0, final String sizemsg) {
        if (mListAnimaFinish) {
            if (TextUtils.isEmpty(msg0)) {
                mRoundProgressBar.setCenterText(sizemsg);
            } else {
                mRoundProgressBar.setCenterText(sizemsg);
                mRoundProgressBar.setBottomText2(msg0);
            }
            /*RelativeLayout topView = (RelativeLayout)findViewById(R.id.view_top);
            int topHeight = topView.getHeight();
            int progressHeight = mRoundProgressBar.getHeight();
            mRoundProgressBar.animate()
                    .y((topHeight - progressHeight )/2)
                    .setDuration(100)
                    .start();*/
            /*RelativeLayout.LayoutParams Params = (RelativeLayout.LayoutParams) mRoundProgressBar.getLayoutParams();
            Params.addRule(RelativeLayout.CENTER_IN_PARENT);
            mRoundProgressBar.setLayoutParams(Params);*/

            mEntrance.setVisibility(View.VISIBLE);
            mAsyncGarbageCleanupHelper.resetDeletedParam();
        } else {
            mListView.setVisibility(View.GONE);
            mListAnimaFinish = true;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    updateLayout(msg0, sizemsg);
                }
            };
            mHandler.postDelayed(runnable,100);
        }
    }

    private void deleteCell(View v, final int position) {
        Animation.AnimationListener al = new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                mListAnimaFinish = true;
            }
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationStart(Animation animation) {}
        };
        collapse(v, al);
    }

    private void collapse(final View v, Animation.AnimationListener al) {
        final int initialHeight = v.getMeasuredHeight();
        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        if (al != null) {
            anim.setAnimationListener(al);
        }
        anim.setDuration(mRoundProgressBar.DURATION);
        v.startAnimation(anim);
    }

    private void initDeepCleanEntrance() {
        mEntrance = (LinearLayout)findViewById(R.id.deep_clean_entrance);
        ((ListItemTextView) findViewById(R.id.deep_clean)).setOnClickListener(this);
        ((ListItemTextView) findViewById(R.id.wechat_special)).setOnClickListener(this);
        ((ListItemTextView) findViewById(R.id.clear_large_files)).setOnClickListener(this);
    }
}
