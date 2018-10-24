package com.example.freeme.apis.app;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.freeme.actionbar.app.FreemeActionBarUtil;

public class FreemeActionBar extends Activity {
    final int TEST_COLOR = 0xFF00FF00;
    private ColorStateList mColorList = ColorStateList.valueOf(TEST_COLOR);

    private ActionMode mActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        setTitleText("TITLE");
        setBackTitleText("LEFT");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // MAX item count is 5 in freeme style.
        MenuItem actionItem1 = addOptionsMenu(menu, "RIGHT");
        actionItem1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        // action mode default mode is SHOW_AS_ACTION_NEVER.
        addOptionsMenu(menu, "SET COLOR");
        addOptionsMenu(menu, "DISABLED").setEnabled(false);
        addOptionsMenu(menu, "START ACTIONMODE");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getTitle().equals("SET COLOR")) {
            setTitleColor();
            setBackTitleColor();
            setActionMenuTextColor();
            setUpViewColor();
        } else if (item.getTitle().equals("START ACTIONMODE")) {
            startActionMode();
        } else {
            Toast.makeText(this, "Selected Item: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private class MyActionModeCallback implements ActionMode.Callback {
        @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle("ACTION MODE");
            mode.setSubtitle(null);
            mode.setTitleOptionalHint(false);
            MenuItem actionItem1 = menu.add("DISABLED").setEnabled(false);
            actionItem1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            // action mode default mode is SHOW_AS_ACTION_IF_ROOM.
            menu.add("STOP").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menu.add("STOP").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menu.add("STOP").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            return true;
        }

        @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getTitle().equals("STOP")) {
                stopActionMode();
            }
            return true;
        }

        @Override public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }
    }

    public void startActionMode() {
        if (mActionMode == null) {
            ActionMode.Callback cb = new MyActionModeCallback();
            mActionMode = startActionMode(cb);
        }
    }

    public void stopActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    private MenuItem addOptionsMenu(Menu menu, String title) {
        return menu.add(title);
    }

    private void setTitleText(String title) {
        getActionBar().setTitle(title);
    }

    private void setBackTitleText(String title) {
        FreemeActionBarUtil.setBackTitle(getActionBar(), title);
    }

    private void setTitleColor() {
        FreemeActionBarUtil.setTitleTextColor(getActionBar(), TEST_COLOR);
    }

    private void setBackTitleColor() {
        FreemeActionBarUtil.setBackTitleTextColor(getActionBar(), mColorList);
    }

    private void setActionMenuTextColor() {
        FreemeActionBarUtil.setActionMenuTextColor(getActionBar(), mColorList);
    }

    private void setUpViewColor() {
        Drawable drawable = getBaseContext().getDrawable(com.freeme.internal.R.drawable.ic_ab_back_freeme);
        drawable.setTintList(mColorList);
        getActionBar().setHomeAsUpIndicator(drawable);
    }
}
