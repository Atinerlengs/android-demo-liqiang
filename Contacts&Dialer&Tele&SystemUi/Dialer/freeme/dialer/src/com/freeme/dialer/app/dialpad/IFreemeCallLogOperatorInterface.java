package com.freeme.dialer.app.dialpad;

import com.android.dialer.app.list.DragDropController;

public interface IFreemeCallLogOperatorInterface {
    boolean isDialpadVisible();

    boolean onBackPress();

    boolean isInSearchUi();

    void exitSearchUi();

    boolean hasSearchQuery();

    void onDialpadQueryChanged(String query, String normalizedQuery);

    void showDialpadFragment(boolean animate);

    void setStartedFromNewIntent(boolean value);

    int getDialpadHeight();

    void hideDialpadFragment(boolean animate, boolean clearDialpad);

    boolean isDialpadShown();

    void onDialpadShown();

    void clearDialpad();

    void enableFloatingButton(boolean enabled);

    void setFloatingButtonVisible(boolean visible);

    void setInCallDialpadUp(boolean inCallDialpadUp);

    boolean isInCallDialpadUp();

    void setShowDialpadOnResume(boolean showDialpadOnResume);

    boolean isShowDialpadOnResume();

    void setClearSearchOnPause(boolean clearSearchOnPause);

    boolean isClearSearchOnPause();

    String getSearchQuery();

    void setDragDropController(DragDropController dragController);

    void onResume();

    void onPause();
}
