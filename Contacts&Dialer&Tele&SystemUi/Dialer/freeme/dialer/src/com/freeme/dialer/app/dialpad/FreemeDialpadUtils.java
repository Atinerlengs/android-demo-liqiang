package com.freeme.dialer.app.dialpad;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.android.contacts.common.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.common.list.PhoneNumberListAdapter;
import com.android.contacts.common.list.PhoneNumberPickerFragment;
import com.android.contacts.common.widget.FloatingActionButtonController;
import com.android.dialer.animation.AnimUtils;
import com.android.dialer.animation.AnimationListenerAdapter;
import com.android.dialer.app.Bindings;
import com.android.dialer.app.R;
import com.android.dialer.app.list.DialtactsPagerAdapter;
import com.android.dialer.app.list.RegularSearchFragment;
import com.android.dialer.app.list.SearchFragment;
import com.android.dialer.app.list.SmartDialSearchFragment;
import com.android.dialer.callintent.CallIntentBuilder;
import com.android.dialer.callintent.CallSpecificAppData;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.configprovider.ConfigProviderBindings;
import com.android.dialer.interactions.PhoneNumberInteraction;
import com.android.dialer.logging.Logger;
import com.android.dialer.logging.ScreenEvent;
import com.android.dialer.p13n.inference.P13nRanking;
import com.android.dialer.p13n.inference.protocol.P13nRanker;
import com.android.dialer.p13n.logging.P13nLogger;
import com.android.dialer.p13n.logging.P13nLogging;
import com.android.dialer.searchfragment.list.NewSearchFragment;
import com.android.dialer.smartdial.SmartDialNameMatcher;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.ViewUtil;
import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.freeme.dialer.app.FreemeDialtactsActivity;
import com.freeme.dialer.app.calllog.FreemeCallLogFragment;
import com.freeme.dialer.app.list.FreemeListsFragment;
import com.mediatek.contacts.simcontact.GlobalEnv;
import com.mediatek.dialer.util.DialerFeatureOptions;

public class FreemeDialpadUtils implements OnPhoneNumberPickerActionListener {

    private static final String TAG = "FreemeDialpadUtils";
    private static final boolean DEBUG = FreemeLogUtils.DEBUG;

    @VisibleForTesting
    private static final String TAG_DIALPAD_FRAGMENT = "dialpad";
    private static final String KEY_IN_REGULAR_SEARCH_UI = "in_regular_search_ui";
    private static final String KEY_IN_DIALPAD_SEARCH_UI = "in_dialpad_search_ui";
    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final String KEY_IS_DIALPAD_SHOWN = "is_dialpad_shown";
    private static final String TAG_NEW_SEARCH_FRAGMENT = "new_search";
    private static final String TAG_REGULAR_SEARCH_FRAGMENT = "search";
    private static final String TAG_SMARTDIAL_SEARCH_FRAGMENT = "smartdial";

    private static final int FAB_SCALE_IN_DELAY_MS = 300;

    private FreemeListsFragment mListsFragment;
    private FreemeCallLogFragment mCalllogFragment;
    private FreemeDialpadFragment mDialpadFragment;
    private FloatingActionButtonController mFloatingActionButtonController;
    /**
     * Fragment for searching phone numbers using the alphanumeric keyboard.
     */
    private RegularSearchFragment mRegularSearchFragment;

    /**
     * Fragment for searching phone numbers using the dialpad.
     */
    private SmartDialSearchFragment mSmartDialSearchFragment;

    /**
     * new Fragment for search phone numbers using the keyboard and the dialpad.
     */
    private NewSearchFragment mNewSearchFragment;
    /**
     * Animation that slides in.
     */
    private Animation mSlideIn;

    /**
     * Animation that slides out.
     */
    private Animation mSlideOut;

    private boolean mStateSaved;

    private boolean mInDialpadSearch;
    private boolean mInRegularSearch;
    private boolean mClearSearchOnPause;
    private boolean mIsDialpadShown;
    private boolean mShowDialpadOnResume;
    /**
     * Whether or not the device is in landscape orientation.
     */
    private boolean mIsLandscape;
    /**
     * True if the dialpad is only temporarily showing due to being in call
     */
    private boolean mInCallDialpadUp;

    private String mSearchQuery = "";
    private String mDialpadQuery;

    private P13nLogger mP13nLogger;
    private P13nRanker mP13nRanker;

    private View mParentLayout;

    /**
     * Listener for after slide out animation completes on dialer fragment.
     */
    AnimationListenerAdapter mSlideOutListener =
            new AnimationListenerAdapter() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    commitDialpadFragmentHide();
                }
            };

    public FreemeDialpadUtils(FreemeCallLogFragment calllogFragment) {
        this.mCalllogFragment = calllogFragment;

        Context context = GlobalEnv.getApplicationContext();
        mP13nLogger = P13nLogging.get(context);
        mP13nRanker = P13nRanking.get(context);
    }

    public void init(FloatingActionButtonController floatingActionButtonController,
                          View parentLayout) {
        this.mFloatingActionButtonController = floatingActionButtonController;
        this.mParentLayout = parentLayout;

        Fragment fragment = mCalllogFragment.getParentFragment();
        if (fragment instanceof FreemeListsFragment) {
            mListsFragment = (FreemeListsFragment) fragment;
        }

        Context context = fragment.getContext();
        mIsLandscape = context.getResources().getConfiguration()
                .orientation == Configuration.ORIENTATION_LANDSCAPE;
        final boolean isLayoutRtl = ViewUtil.isRtl();
        if (mIsLandscape) {
            mSlideIn = AnimationUtils.loadAnimation(context,
                    isLayoutRtl ? R.anim.dialpad_slide_in_left
                            : R.anim.dialpad_slide_in_right);
            mSlideOut = AnimationUtils.loadAnimation(context,
                    isLayoutRtl ? R.anim.dialpad_slide_out_left
                            : R.anim.dialpad_slide_out_right);
        } else {
            mSlideIn = AnimationUtils.loadAnimation(context, R.anim.dialpad_slide_in_bottom);
            mSlideOut = AnimationUtils.loadAnimation(context, R.anim.dialpad_slide_out_bottom);
        }

        mSlideIn.setInterpolator(AnimUtils.EASE_IN);
        mSlideOut.setInterpolator(AnimUtils.EASE_OUT);

        mSlideOut.setAnimationListener(mSlideOutListener);
    }

    public void onAttachFragment(final Fragment fragment) {
        if (fragment instanceof FreemeDialpadFragment) {
            mDialpadFragment = (FreemeDialpadFragment) fragment;
            if (!mIsDialpadShown && !mShowDialpadOnResume) {
                final FragmentTransaction transaction = mCalllogFragment.getChildFragmentManager()
                        .beginTransaction();
                transaction.hide(mDialpadFragment);
                transaction.commit();
                LogUtil.i("FreemeDialtactsActivity.onAttachFragment", "hideDialPad: " + "mIsDialPadShow = " +
                        mIsDialpadShown + ", mShowDialpadOnResume = " + mShowDialpadOnResume);
            }
        } else if (fragment instanceof SmartDialSearchFragment) {
            mSmartDialSearchFragment = (SmartDialSearchFragment) fragment;
            mSmartDialSearchFragment.setOnPhoneNumberPickerActionListener(this);
            if (!TextUtils.isEmpty(mDialpadQuery)) {
                mSmartDialSearchFragment.setAddToContactNumber(mDialpadQuery);
            }
            /**M: need setQueryString when change search mode from regular search to smart  @{ */
            if (mIsDialpadShown && mInDialpadSearch && mSearchQuery != null) {
                mSmartDialSearchFragment.setQueryString(mSearchQuery);
            }
            /** @} */
        } else if (fragment instanceof RegularSearchFragment) {
            mRegularSearchFragment = (RegularSearchFragment) fragment;
            mRegularSearchFragment.setOnPhoneNumberPickerActionListener(this);
        } else if (fragment instanceof NewSearchFragment) {
            mNewSearchFragment = (NewSearchFragment) fragment;
        }
        if (fragment instanceof SearchFragment) {
            final SearchFragment searchFragment = (SearchFragment) fragment;
            searchFragment.setReranker(new PhoneNumberPickerFragment.CursorReranker() {
                @Override
                @MainThread
                public Cursor rerankCursor(Cursor data) {
                    Assert.isMainThread();
                    String queryString = searchFragment.getQueryString();
                    return mP13nRanker.rankCursor(data, queryString == null
                            ? 0 : queryString.length());
                }
            });
            searchFragment.addOnLoadFinishedListener(
                    new PhoneNumberPickerFragment.OnLoadFinishedListener() {
                        @Override
                        public void onLoadFinished() {
                            mP13nLogger.onSearchQuery(searchFragment.getQueryString(),
                                    (PhoneNumberListAdapter) searchFragment.getAdapter());
                        }
                    });
            ///M: fix for ALPS03446728 {
            searchFragment.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Show the FAB when the user touches the lists fragment and the soft
                    // keyboard is hidden.
                    if (!mFloatingActionButtonController.isVisible()) {
                        Log.d(TAG, "searchFragment ontouch, showFAB");
                        hideDialpadFragment(true, false);
                    }
                    return false;
                }
            });
            ///@}
        }
    }

    public int getFabAlignment() {
        if (mIsLandscape) {
            return FloatingActionButtonController.ALIGN_END;
        } else {
            return FloatingActionButtonController.ALIGN_MIDDLE;
        }
    }

    public void showDialpadFragment(boolean animate) {
        if (mSlideOut != null) {
            mSlideOut.setAnimationListener(null);
            mSlideOut.detach();
            mSlideOut.setAnimationListener(mSlideOutListener);
        }

        LogUtil.i("DialtactActivity.showDialpadFragment", "animate: %b", animate);
        if (mIsDialpadShown || mStateSaved ||
                (mDialpadFragment != null && mDialpadFragment.isVisible())) {
            return;
        }
        Log.d(TAG, " showDialpadFragment , animate = " + animate);
        mIsDialpadShown = true;

        final FragmentManager fm = mCalllogFragment.getChildFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        if (mDialpadFragment == null) {
            mDialpadFragment = new FreemeDialpadFragment();
            ft.add(R.id.freeme_call_log_fragment_layout, mDialpadFragment, TAG_DIALPAD_FRAGMENT);
        } else {
            ft.show(mDialpadFragment);
        }

        mDialpadFragment.setAnimate(animate);
        Logger.get(mCalllogFragment.getContext())
                .logScreenView(ScreenEvent.Type.DIALPAD, mCalllogFragment.getActivity());
        ft.commit();

        if (animate) {
            mFloatingActionButtonController.scaleOut();
        } else {
            mFloatingActionButtonController.setVisible(false);
        }

        if (mListsFragment != null) {
            mListsFragment.onDialpadUp();
            if (mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_HISTORY)) {
                mListsFragment.setEnableSwipingPages(false);
            }
        }
    }

    public int getDialpadHeight() {
        int h = 0;
        if (mDialpadFragment != null) {
            h = mDialpadFragment.getDialpadHeight();
        }
        return h;
    }

    /**
     * Callback from child FreemeDialpadFragment when the dialpad is shown.
     */
    public void onDialpadShown() {
        LogUtil.i("FreemeDialtactsActivity.onDialpadShown", "");
        Assert.isNotNull(mDialpadFragment);
        if (mDialpadFragment.getAnimate()) {
            Assert.isNotNull(mDialpadFragment.getView()).startAnimation(mSlideIn);
        } else {
            mDialpadFragment.setYFraction(0);
        }

        updateSearchFragmentPosition();
    }

    public void hideDialpadFragment(boolean animate, boolean clearDialpad) {
        if (mDialpadFragment == null || mDialpadFragment.getView() == null) {
            return;
        }
        LogUtil.i("DialtactActivity.hideDialpadFragment", "animate = " + animate +
                ", clearDialpad = " + clearDialpad);
        if (clearDialpad) {
            clearDialpad();
        }
        if (!mIsDialpadShown) {
            return;
        }
        mIsDialpadShown = false;
        mDialpadFragment.setAnimate(animate);

        updateSearchFragmentPosition();

        if (animate) {
            mDialpadFragment.getView().startAnimation(mSlideOut);
        } else {
            commitDialpadFragmentHide();
        }

        if (mListsFragment != null) {
            mListsFragment.sendScreenViewForCurrentPosition();
            mListsFragment.onDialpadDown();
        }

        if (isInSearchUi() && TextUtils.isEmpty(mSearchQuery)) {
            exitSearchUi();
        }
    }

    public boolean isDialpadShown() {
        return mIsDialpadShown;
    }

    public void clearDialpad() {
        if (mDialpadFragment != null) {
            // Temporarily disable accessibility when we clear the dialpad, since it should be
            // invisible and should not announce anything.
            mDialpadFragment
                    .getDigitsWidget()
                    .setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            mDialpadFragment.clearDialpad();
            mDialpadFragment
                    .getDigitsWidget()
                    .setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        }
    }

    private void updateSearchFragmentPosition() {
        SearchFragment fragment = null;
        if (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isAdded()) {
            fragment = mSmartDialSearchFragment;
        } else if (mRegularSearchFragment != null && mRegularSearchFragment.isAdded()) {
            fragment = mRegularSearchFragment;
        }
        LogUtil.i(
                "FreemeDialtactsActivity.updateSearchFragmentPosition",
                "fragment: %s, isVisible: %b",
                fragment,
                fragment != null && fragment.isVisible());
        if (fragment != null) {
            // We need to force animation here even when fragment is not visible since it might not be
            // visible immediately after screen orientation change and dialpad height would not be
            // available immediately which is required to update position. By forcing an animation,
            // position will be updated after a delay by when the dialpad height would be available.
            fragment.updatePosition(true /* animate */);
        }
    }

    /**
     * Finishes hiding the dialpad fragment after any animations are completed.
     */
    private void commitDialpadFragmentHide() {
        Log.d(TAG, "commitDialpadFragmentHide() enter");
        if (!mStateSaved
                && mDialpadFragment != null
                && !mDialpadFragment.isHidden()
                && mCalllogFragment.getActivity() != null
                && !mCalllogFragment.getActivity().isDestroyed()) {
            final FragmentTransaction ft = mCalllogFragment.getChildFragmentManager()
                    .beginTransaction();
            ft.hide(mDialpadFragment);
            ft.commit();
            Log.d(TAG, "ft.commit()");
        }

        if (!isDialpadShown() && !mFloatingActionButtonController.isVisible()) {
            mFloatingActionButtonController.scaleIn(AnimUtils.NO_DELAY);
            LogUtil.i("DialtactActivity.commitDialpadFragmentHide", "FAB scaleIn: ");
        }

        if (mListsFragment != null) {
            mListsFragment.setEnableSwipingPages(!isInSearchUi());
        }
    }

    private void hideDialpadAndSearchUi() {
        if (mIsDialpadShown) {
            hideDialpadFragment(false, true);
        } else {
            exitSearchUi();
        }
    }

    public boolean hasSearchQuery() {
        return !TextUtils.isEmpty(mSearchQuery);
    }

    /**
     * Shows the search fragment
     */
    private void enterSearchUi(boolean smartDialSearch, String query, boolean animate) {
        if (mStateSaved) {
            // Weird race condition where fragment is doing work after the activity is destroyed
            // due to talkback being on (b/10209937). Just return since we can't do any
            // constructive here.
            return;
        }

        if (mCalllogFragment == null) {
            return;
        }

        FragmentManager fm = mCalllogFragment.getChildFragmentManager();
        if (fm.isDestroyed()) {
            return;
        }

        Context context = mCalllogFragment.getContext();

        if (DEBUG) {
            LogUtil.i("FreemeDialtactsActivity.enterSearchUi", "smart dial " + smartDialSearch);
        }

        final FragmentTransaction transaction = fm.beginTransaction();
        if (mInDialpadSearch && mSmartDialSearchFragment != null) {
            Log.d(TAG, "mSmartDialSearchFragment != null, remove it");
            transaction.remove(mSmartDialSearchFragment);
        }
        if (mInRegularSearch && mRegularSearchFragment != null) {
            Log.d(TAG, "mRegularSearchFragment != null, remove it");
            transaction.remove(mRegularSearchFragment);
        }
        final String tag;
        boolean useNewSearch = ConfigProviderBindings.get(context)
                .getBoolean("enable_new_search_fragment", false);
        if (useNewSearch) {
            tag = TAG_NEW_SEARCH_FRAGMENT;
        } else if (smartDialSearch) {
            tag = TAG_SMARTDIAL_SEARCH_FRAGMENT;
        } else {
            tag = TAG_REGULAR_SEARCH_FRAGMENT;
        }
        mInDialpadSearch = smartDialSearch;
        mInRegularSearch = !smartDialSearch;

        if (animate) {
            transaction.setCustomAnimations(android.R.animator.fade_in, 0);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_NONE);
        }

        Fragment fragment = fm.findFragmentByTag(tag);
        if (fragment != null) {
            transaction.remove(fragment);
        }

        if (useNewSearch) {
            fragment = new NewSearchFragment();
        } else if (smartDialSearch) {
            fragment = new SmartDialSearchFragment();
        } else {
            fragment = Bindings.getLegacy(context).newRegularSearchFragment();
        }
        transaction.add(R.id.freeme_search_fragment_container, fragment, tag);

        // FreemeDialtactsActivity will provide the options menu
        fragment.setHasOptionsMenu(false);

        // Will show empty list if P13nRanker is not enabled. Else, re-ranked list by the ranker.
        if (!useNewSearch) {
            ((SearchFragment) fragment)
                    .setShowEmptyListForNullQuery(mP13nRanker.shouldShowEmptyListForNullQuery());
        } else {
            // TODO(calderwoodra): add p13n ranker to new search.
        }

        if (useNewSearch) {
            ((NewSearchFragment) fragment).setQuery(query);
        } else {
            ((SearchFragment) fragment).setQueryString(query);
        }
        transaction.commit();

        if (smartDialSearch) {
            Logger.get(context).logScreenView(
                    ScreenEvent.Type.SMART_DIAL_SEARCH, mCalllogFragment.getActivity());
        } else {
            Logger.get(context).logScreenView(
                    ScreenEvent.Type.REGULAR_SEARCH, mCalllogFragment.getActivity());
        }
    }

    public void exitSearchUi() {
        // See related bug in enterSearchUI();
        if (mStateSaved) {
            return;
        }

        if (mCalllogFragment == null) {
            return;
        }

        FragmentManager fm = mCalllogFragment.getChildFragmentManager();
        if (fm.isDestroyed()) {
            return;
        }

        if (mDialpadFragment != null) {
            mDialpadFragment.clearDialpad();
        }

        setNotInSearchUi();

        final FragmentTransaction transaction = fm.beginTransaction();
        if (mSmartDialSearchFragment != null) {
            transaction.remove(mSmartDialSearchFragment);
        }
        if (mRegularSearchFragment != null) {
            transaction.remove(mRegularSearchFragment);
        }
        if (mNewSearchFragment != null) {
            transaction.remove(mNewSearchFragment);
        }
        transaction.commit();

        if (mDialpadFragment == null || !mDialpadFragment.isVisible()) {
            // If the dialpad fragment wasn't previously visible, then send a screen view because
            // we are exiting regular search. Otherwise, the screen view will be sent by
            // {@link #hideDialpadFragment}.
            if (mListsFragment != null) {
                mListsFragment.sendScreenViewForCurrentPosition();
            }
        }

        if (mListsFragment != null) {
            mListsFragment.setEnableSwipingPages(!isDialpadShown());
        }

        if (!isDialpadShown() && !mFloatingActionButtonController.isVisible()) {
            mFloatingActionButtonController.scaleIn(FAB_SCALE_IN_DELAY_MS);
        }
    }

    /**
     * @return True if the search UI was exited, false otherwise
     */
    private boolean maybeExitSearchUi() {
        if (isInSearchUi() && TextUtils.isEmpty(mSearchQuery)) {
            exitSearchUi();
            DialerUtils.hideInputMethod(mParentLayout);
            return true;
        }
        return false;
    }

    public boolean isInSearchUi() {
        return mInDialpadSearch || mInRegularSearch;
    }

    private void setNotInSearchUi() {
        mInDialpadSearch = false;
        mInRegularSearch = false;
    }

    public boolean isDialpadVisible() {
        return mDialpadFragment != null && mDialpadFragment.isVisible();
    }

    @Override
    public void onPickDataUri(Uri dataUri, boolean isVideoCall,
                              CallSpecificAppData callSpecificAppData) {
        mClearSearchOnPause = true;
        PhoneNumberInteraction.startInteractionForPhoneCall(
                (FreemeDialtactsActivity) mCalllogFragment.getActivity(), dataUri,
                isVideoCall, callSpecificAppData);
    }

    @Override
    public void onPickPhoneNumber(String phoneNumber, boolean isVideoCall,
                                  CallSpecificAppData callSpecificAppData) {
        if (phoneNumber == null) {
            // Invalid phone number, but let the call go through so that InCallUI can show
            // an error message.
            phoneNumber = "";
        }

        Intent intent = new CallIntentBuilder(phoneNumber, callSpecificAppData)
                .setIsVideoCall(isVideoCall).build();
        DialerUtils.startActivityWithErrorToast(mCalllogFragment.getActivity(), intent);
        mClearSearchOnPause = true;
    }

    @Override
    public void onHomeInActionBarSelected() {
        exitSearchUi();
    }

    public void onDialpadQueryChanged(String query, String normalizedQuery) {
        mDialpadQuery = query;
        if (mSmartDialSearchFragment != null) {
            mSmartDialSearchFragment.setAddToContactNumber(query);
        }

        if (DEBUG) {
            LogUtil.i("FreemeDialtactsActivity.onDialpadQueryChanged",
                    "called with new query: " + normalizedQuery);
            LogUtil.i("FreemeDialtactsActivity.onDialpadQueryChanged",
                    "previous query: " + mSearchQuery);
        }

        if (!TextUtils.equals(normalizedQuery, mSearchQuery)) {
            mSearchQuery = normalizedQuery;

            boolean isNeedEnterSearch = !TextUtils.isEmpty(normalizedQuery);

            if (isNeedEnterSearch && !isInSearchUi()) {
                enterSearchUi(true, mSearchQuery, false);
            }

            if (mCalllogFragment != null) {
                mCalllogFragment.updateSearchUI(isNeedEnterSearch);
            }

            if (mListsFragment != null) {
                mListsFragment.setSearchQuery(normalizedQuery);
                mListsFragment.updateSearchUI(isNeedEnterSearch);
                mListsFragment.setEnableSwipingPages(!isNeedEnterSearch && !isDialpadShown());
            }

            if (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isVisible()) {
                mSmartDialSearchFragment.setQueryString(normalizedQuery);
            } else if (mRegularSearchFragment != null && mRegularSearchFragment.isVisible()) {
                mRegularSearchFragment.setQueryString(normalizedQuery);
            } else if (mNewSearchFragment != null) {
                mNewSearchFragment.setQuery(normalizedQuery);
            }

            if (!isNeedEnterSearch) {
                maybeExitSearchUi();
            }
        }

        try {
            if (mDialpadFragment != null && mDialpadFragment.isVisible()) {
                mDialpadFragment.process_quote_emergency_unquote(normalizedQuery);
            }
        } catch (Exception ignored) {
            // Skip any exceptions for this piece of code
        }
    }

    public void onResume() {
        mP13nLogger.reset();
        mP13nRanker.refresh(new P13nRanker.P13nRefreshCompleteListener() {
            @Override
            public void onP13nRefreshComplete() {
                // TODO(strongarm): make zero-query search results visible
            }
        });
    }

    public void onPause() {
        if (mClearSearchOnPause) {
            hideDialpadAndSearchUi();
            mClearSearchOnPause = false;
        }
        if (mSlideOut.hasStarted() && !mSlideOut.hasEnded()) {
            commitDialpadFragmentHide();
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_SEARCH_QUERY, mSearchQuery);
        outState.putBoolean(KEY_IN_REGULAR_SEARCH_UI, mInRegularSearch);
        outState.putBoolean(KEY_IN_DIALPAD_SEARCH_UI, mInDialpadSearch);
        outState.putBoolean(KEY_IS_DIALPAD_SHOWN, mIsDialpadShown);
        mStateSaved = true;
    }

    public boolean onBackPress() {
        if (mIsDialpadShown) {
            if (!hasSearchQuery() ||
                    (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isVisible()
                            && mSmartDialSearchFragment.getAdapter().getCount() == 0)) {
                exitSearchUi();
            }
            hideDialpadFragment(true, false);
            return true;
        }
        return false;
    }

    public boolean isInCallDialpadUp() {
        return mInCallDialpadUp;
    }

    public void setInCallDialpadUp(boolean inCallDialpadUp) {
        this.mInCallDialpadUp = inCallDialpadUp;
    }

    public boolean isShowDialpadOnResume() {
        return mShowDialpadOnResume;
    }

    public void setShowDialpadOnResume(boolean showDialpadOnResume) {
        this.mShowDialpadOnResume = showDialpadOnResume;
    }

    public boolean isClearSearchOnPause() {
        return mClearSearchOnPause;
    }

    public void setClearSearchOnPause(boolean clearSearchOnPause) {
        this.mClearSearchOnPause = clearSearchOnPause;
    }

    public boolean isInDialpadSearch() {
        return mInDialpadSearch;
    }

    public void setInDialpadSearch(boolean inSearch) {
        this.mInDialpadSearch = inSearch;
    }

    public boolean isInRegularSearch() {
        return mInRegularSearch;
    }

    public void setInRegularSearch(boolean inSearch) {
        this.mInRegularSearch = inSearch;
    }

    public String getSearchQuery() {
        return mSearchQuery;
    }

    public void setSearchQuery(String query) {
        this.mSearchQuery = query;
        if (mDialpadFragment != null) {
            mDialpadFragment.setSearchQuery(query);
            if (mDialpadFragment.getDigitsWidget() != null) {
                mDialpadFragment.getDigitsWidget().setText(query);
            }
        }
    }

    public void setStartedFromNewIntent(boolean value) {
        if (mDialpadFragment != null) {
            mDialpadFragment.setStartedFromNewIntent(value);
        }
    }
}
