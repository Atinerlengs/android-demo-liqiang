package com.freeme.dialer.contactsfragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.contacts.common.preference.ContactsPreferences;
import com.android.dialer.app.list.DialtactsPagerAdapter;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.contactsfragment.ContactsFragment;
import com.android.dialer.contactsfragment.R;
import com.android.dialer.performancereport.PerformanceReport;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.IntentUtil;
import com.android.dialer.util.PermissionsUtil;
import com.freeme.contacts.common.utils.FreemeBottomSelectedController;
import com.freeme.contacts.common.utils.FreemeCommonFeatureOptions;
import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.freeme.contacts.common.widgets.FreemeBottomSelectedView;
import com.freeme.contacts.common.widgets.FreemeEmptyContentView;
import com.freeme.contacts.common.widgets.FreemeIndexScrollView;
import com.freeme.dialer.app.FreemeDialtactsActivity;
import com.freeme.dialer.app.list.FreemeListsFragment;
import com.freeme.dialer.callback.IFreemeMultiSelectCallBack;
import com.freeme.dialer.contacts.list.service.FreemeMultiChoiceService;
import com.freeme.dialer.contacts.list.service.FreemeMultiDeletionInteraction;
import com.freeme.dialer.contacts.list.service.FreemeMultiDeletionInteraction.MultiContactDeleteListener;
import com.freeme.dialer.contacts.merge.FreemeContactsMerge;
import com.freeme.dialer.utils.FreemeShareContacts;

import java.util.ArrayList;
import java.util.Arrays;

public class FreemeContactsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        View.OnScrollChangeListener,
        FreemeIndexScrollView.OnIndexSelectedListener,
        FreemeEmptyContentView.OnEmptyViewActionButtonClickedListener,
        ContactsPreferences.ChangeListener,
        TextWatcher,
        View.OnClickListener {

    private static final String TAG = "FreemeContactsFragment";
    private static final int LOADER_ID_LOAD_CONTACTS_DATA = 0x1101;
    public static final int LOADER_ID_LOAD_PROFIIE_DATA = 0x1102;

    private LinearLayout anchoredHeaderContainer;
    private TextView anchoredHeader;
    private RecyclerView recyclerView;
    private LinearLayoutManager manager;
    private FreemeContactsAdapter adapter;
    private FreemeEmptyContentView emptyContentView;
    private ImageView mAddNewContact;
    private EditText mSearchEdit;
    private FreemeIndexScrollView mIndexScrollView;

    private ContactsPreferences contactsPrefs;

    private String[] mChars;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactsPrefs = new ContactsPreferences(getContext());
        contactsPrefs.registerChangeListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.freeme_fragment_contacts, container, false);

        mSarchContainer = view.findViewById(R.id.freeme_contacts_search);
        mAddNewContact = mSarchContainer.findViewById(R.id.add_new_contacts);
        mAddNewContact.setOnClickListener(this);
        mSearchEdit = mSarchContainer.findViewById(R.id.freeme_search_edit);
        mSearchEdit.addTextChangedListener(this);

        anchoredHeaderContainer = view.findViewById(R.id.header_layout);
        view.findViewById(R.id.header_top_line).setVisibility(View.GONE);
        anchoredHeader = view.findViewById(R.id.header);
        recyclerView = view.findViewById(R.id.recycler_view);

        mIndexScrollView = view.findViewById(R.id.index_scroll_view);
        mChars = getResources().getStringArray(R.array.index_handle_chars);
        mIndexScrollView.setIndexHandleChar(mChars);
        mIndexScrollView.setOnIndexSelectedListener(this);
        ColorDrawable cd = new ColorDrawable(
                getResources().getColor(R.color.freeme_content_bg_color_light));
        mIndexScrollView.setIndexHandleBackground(cd, cd);

        emptyContentView = view.findViewById(R.id.empty_list_view);
        emptyContentView.setImage(R.drawable.freeme_empty_icon_contacts);
        emptyContentView.setActionLabel(FreemeEmptyContentView.NO_LABEL);
        emptyContentView.setActionClickedListener(this);

        FreemeBottomSelectedView bottomContainer = view.findViewById(R.id.bottom_container);
        mBottomSelectedController = new FreemeBottomSelectedController(bottomContainer);

        boolean hasContactsPermissions = PermissionsUtil.hasContactsReadPermissions(getContext());
        if (hasContactsPermissions) {
            mIndexScrollView.setVisibility(View.VISIBLE);
            mSarchContainer.setVisibility(View.VISIBLE);

            getLoaderManager().initLoader(LOADER_ID_LOAD_CONTACTS_DATA, null, this);
        } else {
            emptyContentView.setDescription(R.string.permission_no_contacts);
            emptyContentView.setActionLabel(R.string.permission_single_turn_on);
            emptyContentView.setVisibility(View.VISIBLE);

            mIndexScrollView.setVisibility(View.GONE);
            mSarchContainer.setVisibility(View.GONE);
            anchoredHeaderContainer.setVisibility(View.GONE);
        }

        mFirstCompletelyPosition = -1;

        adapter = new FreemeContactsAdapter(getContext(), getLoaderManager(), null);
        adapter.setHasContactsPermissions(hasContactsPermissions);
        adapter.setFreemeMultiSelectCallBack(new IFreemeMultiSelectCallBack() {
            @Override
            public void isInMulitMode(boolean isMultiMode) {
                Activity activity = getActivity();
                if (activity instanceof FreemeDialtactsActivity) {
                    ((FreemeDialtactsActivity) activity).setMultiSelectMode(isMultiMode,
                            DialtactsPagerAdapter.TAB_INDEX_ALL_CONTACTS);
                }
                if (isMultiMode) {
                    if (FreemeCommonFeatureOptions.isSuperPowerModeOn(getContext())) {
                        mBottomSelectedController.showActions(ACTION_NAMES_POWERSAVER,
                                ACTION_CODES_POWERSAVER, mCallBack);
                    } else {
                        mBottomSelectedController.showActions(ACTION_NAMES, ACTION_CODES, mCallBack);
                    }
                } else {
                    mBottomSelectedController.hideActions();
                }
                mSarchContainer.setVisibility(isMultiMode ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onSelectedCount(int count) {
                Activity activity = getActivity();
                if (activity instanceof FreemeDialtactsActivity) {
                    ((FreemeDialtactsActivity) activity).updateActionBarTitle(count,
                            DialtactsPagerAdapter.TAB_INDEX_ALL_CONTACTS);
                    ((FreemeDialtactsActivity) activity).setIsAllContactsSelected(
                            count > 0 && count == adapter.getItemCount());
                }
                boolean enable = count > 0;
                mBottomSelectedController.updateActionEnabled(enable, ACTION_CODE_DELETE);
                mBottomSelectedController.updateActionEnabled(enable, ACTION_CODE_SHARE);
                mBottomSelectedController.updateActionEnabled(count >= MIN_MERGE_SIZE && count <= MAX_MERGE_SIZE, ACTION_CODE_MERGE);
            }
        });

        if (savedInstanceState != null) {
            adapter.onRestoreInstanceState(savedInstanceState);
            mFirstCompletelyPosition = savedInstanceState.getInt(KEY_LIST_SCROLL_POSITION);
            long[] ids = savedInstanceState.getLongArray(KEY_CONTACT_ID_LIST);
            if (ids != null && ids.length > 0) {
                if (mSelectedContactIdList == null) {
                    mSelectedContactIdList = new ArrayList<>();
                } else {
                    mSelectedContactIdList.clear();
                }
                for (long id : ids) {
                    mSelectedContactIdList.add(id);
                }
            }
        }

        manager = new LinearLayoutManager(getContext()) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler,
                                         RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                int itemsShown = findLastVisibleItemPosition() - findFirstVisibleItemPosition() + 1;
                if (adapter.getItemCount() > itemsShown) {
                    recyclerView.setOnScrollChangeListener(FreemeContactsFragment.this);
                } else {
                    anchoredHeaderContainer.setVisibility(View.GONE);
                }
            }
        };

        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onChange() {
        if (getActivity() != null && isAdded()) {
            getLoaderManager().restartLoader(LOADER_ID_LOAD_CONTACTS_DATA, null, this);
        }
    }

    /**
     * @return a loader according to sort order and display order.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        boolean sortOrderPrimary =
                (contactsPrefs.getSortOrder() == ContactsPreferences.SORT_ORDER_PRIMARY);
        boolean displayOrderPrimary =
                (contactsPrefs.getDisplayOrder() == ContactsPreferences.DISPLAY_ORDER_PRIMARY);
        String sortKey = sortOrderPrimary
                ? ContactsContract.Contacts.SORT_KEY_PRIMARY
                : ContactsContract.Contacts.SORT_KEY_ALTERNATIVE;
        return FreemeContactsCursorLoader
                .createInstance(displayOrderPrimary, getContext(), sortKey, mQueryString);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.setHasContactsPermissions(true);
        adapter.changeCursor(cursor);
        //M: the cursor is null at sometime, so add null checking when use is.
        if (cursor == null || cursor.getCount() == 0) {
            emptyContentView.setActionLabel(FreemeEmptyContentView.NO_LABEL);
            if (isSearchMode) {
                emptyContentView.setDescription(R.string.freeme_no_match_contact);
            } else {
                emptyContentView.setDescription(R.string.all_contacts_empty);
            }
            emptyContentView.setVisibility(View.VISIBLE);
            anchoredHeaderContainer.setVisibility(View.GONE);
            mIndexScrollView.setVisibility(View.GONE);

            boolean isMultiWindow = getActivity().isInMultiWindowMode();
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) emptyContentView.getLayoutParams();
            params.gravity = isMultiWindow ? Gravity.BOTTOM : Gravity.CENTER;
            params.bottomMargin = isMultiWindow ? 30 : 0;
            emptyContentView.setLayoutParams(params);
        } else {
            emptyContentView.setVisibility(View.GONE);
            adapter.setMultiMode(adapter.isMultiMode());

            PerformanceReport.logOnScrollStateChange(recyclerView);

            if (mSelectedContactIdList != null && mSelectedContactIdList.size() > 0) {
                adapter.setSelectedList(mSelectedContactIdList);
            }
            if (mFirstCompletelyPosition != -1) {
                manager.scrollToPositionWithOffset(mFirstCompletelyPosition, 0);
                adapter.refreshHeaders();
                mFirstCompletelyPosition = -1;
            }

            mIndexScrollView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        recyclerView.setAdapter(null);
        recyclerView.setOnScrollChangeListener(null);
        adapter = null;
        contactsPrefs.unregisterChangeListener();
    }

    /*
     * When our recycler view updates, we need to ensure that our row headers and anchored header
     * are in the correct state.
     *
     * The general rule is, when the row headers are shown, our anchored header is hidden. When the
     * recycler view is scrolling through a sublist that has more than one element, we want to show
     * out anchored header, to create the illusion that our row header has been anchored. In all
     * other situations, we want to hide the anchor because that means we are transitioning between
     * two sublists.
     */
    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        int firstVisibleItem = manager.findFirstVisibleItemPosition();
        int firstCompletelyVisible = manager.findFirstCompletelyVisibleItemPosition();
        if (firstCompletelyVisible == RecyclerView.NO_POSITION) {
            // No items are visible, so there are no headers to update.
            return;
        }

        mFirstCompletelyPosition = firstCompletelyVisible;

        String anchoredHeaderString = adapter.getHeaderString(firstCompletelyVisible);

        if (adapter.isMultiMode()) {
            anchoredHeader.setText(anchoredHeaderString);
            anchoredHeaderContainer.setVisibility(View.VISIBLE);
            return;
        }

        // If the user swipes to the top of the list very quickly, there is some strange behavior
        // between this method updating headers and adapter#onBindViewHolder updating headers.
        // To overcome this, we refresh the headers to ensure they are correct.
        if (firstVisibleItem == 0) {
            adapter.refreshHeaders();
            anchoredHeaderContainer.setVisibility(View.GONE);
        } else {
            if (adapter.getHeaderString(firstVisibleItem).equals(anchoredHeaderString)
                    || adapter.isLastItemInGroup(firstVisibleItem)) {
                anchoredHeader.setText(anchoredHeaderString);
                anchoredHeaderContainer.setVisibility(View.VISIBLE);
            } else {
                anchoredHeaderContainer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onEmptyViewActionButtonClicked() {
        if (emptyContentView.getActionLabel() == R.string.permission_single_turn_on) {
            String[] deniedPermissions = PermissionsUtil.getPermissionsCurrentlyDenied(
                    getContext(), PermissionsUtil.allContactsGroupPermissionsUsedInDialer);
            if (deniedPermissions.length > 0) {
                LogUtil.i("ContactsFragment.onEmptyViewActionButtonClicked",
                        "Requesting permissions: " + Arrays.toString(deniedPermissions));
                FragmentCompat.requestPermissions(this, deniedPermissions,
                        ContactsFragment.READ_CONTACTS_PERMISSION_REQUEST_CODE);
            }
        } else if (emptyContentView.getActionLabel()
                == R.string.all_contacts_empty_add_contact_action) {
            // Add new contact
            DialerUtils.startActivityWithErrorToast(getContext(), IntentUtil.getNewContactIntent(),
                    R.string.add_contact_not_available);
            ///M: disable view's clickable in 1000ms to avoid double or trible click.
            DialerUtils.disableViewClickableInDuration(emptyContentView, 1000 /*ms*/);
        } else {
            throw Assert.createIllegalStateFailException("Invalid empty content view action label.");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == ContactsFragment.READ_CONTACTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length >= 1 && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                // Force a refresh of the data since we were missing the permission before this.
                emptyContentView.setVisibility(View.GONE);

                mIndexScrollView.setVisibility(View.VISIBLE);
                mSarchContainer.setVisibility(View.VISIBLE);

                getLoaderManager().initLoader(LOADER_ID_LOAD_CONTACTS_DATA, null, this);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (emptyContentView.getActionLabel() == R.string.permission_single_turn_on &&
                (emptyContentView.getVisibility() == View.VISIBLE)) {
            // We didn't have the permission before, and now we do. need initLoader to load contacts.
            if (PermissionsUtil.hasContactsReadPermissions(getContext())) {
                getLoaderManager().initLoader(LOADER_ID_LOAD_CONTACTS_DATA, null, this);
                emptyContentView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        DialerUtils.startActivityWithErrorToast(getContext(), IntentUtil.getNewContactIntent(),
                R.string.add_contact_not_available);
        ///M: disable view's clickable in 1000ms to avoid double or trible click.
        DialerUtils.disableViewClickableInDuration(v, 1000 /*ms*/);
    }

    @Override
    public void onIndexSelected(String section) {
        if (adapter == null) return;
        int tempIdx = Arrays.asList(mChars).indexOf(section);
        final String[] sections = adapter.getHeaders();
        int index = Arrays.asList(sections).indexOf(section);
        if (index != -1) {
            // index plus 1 because the list has a head view
            manager.scrollToPositionWithOffset(adapter.getHeaderPosition(index), 0);
            adapter.refreshHeaders();
        } else if (tempIdx > 0) {
            tempIdx--;
            onIndexSelected(mChars[tempIdx]);
        }
    }

    private static final String KEY_LIST_SCROLL_POSITION = "list_scroll_position";
    private static final String KEY_CONTACT_ID_LIST = "contact_id_list";
    private int mFirstCompletelyPosition = 0;
    private ArrayList<Long> mSelectedContactIdList;
    private View mSarchContainer;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null) {
            adapter.onSaveInstanceState(outState);
        }
        outState.putInt(KEY_LIST_SCROLL_POSITION, mFirstCompletelyPosition);
        mSelectedContactIdList = adapter.getSelectedContactIdList();
        if (mSelectedContactIdList.size() > 0) {
            long[] ids = mSelectedContactIdList.stream().mapToLong(t -> t.longValue()).toArray();
            outState.putLongArray(KEY_CONTACT_ID_LIST, ids);
        }
    }

    public boolean isNeedExitMultiMode() {
        if (adapter != null && adapter.isMultiMode()) {
            adapter.setMultiMode(false);
            adapter.notifyDataSetChanged();
            return true;
        }
        return false;
    }

    private FreemeBottomSelectedController mBottomSelectedController;
    private static final int ACTION_CODE_SHARE = 0x100;
    private static final int ACTION_CODE_DELETE = 0x200;
    private static final int ACTION_CODE_MERGE = 0x300;
    private static final int MIN_MERGE_SIZE = 2;
    private static final int MAX_MERGE_SIZE = 10;
    private static final int[] ACTION_NAMES = new int[]{
            R.string.freeme_menu_merge_contacts,
            R.string.menu_share,
            R.string.recentCalls_delete};
    private static final int[] ACTION_CODES = new int[]{
            ACTION_CODE_MERGE,
            ACTION_CODE_SHARE,
            ACTION_CODE_DELETE};
    private static final int[] ACTION_NAMES_POWERSAVER = new int[]{
            R.string.freeme_menu_merge_contacts,
            R.string.recentCalls_delete};
    private static final int[] ACTION_CODES_POWERSAVER = new int[]{
            ACTION_CODE_MERGE,
            ACTION_CODE_DELETE};

    private FreemeBottomSelectedView.IFreemeBottomActionCallBack mCallBack = (int actionCode) -> {
        switch (actionCode) {
            case ACTION_CODE_SHARE:
                new FreemeShareContacts(getContext()).doShare(
                        adapter.getSelectedContactUriList(), getActivity().getClass().getName());
                break;
            case ACTION_CODE_DELETE: {
                if (!FreemeMultiChoiceService.isCanDelete()) {
                    return;
                }
                FreemeLogUtils.d(TAG, "[deleteSelectedContacts]...");
                FreemeMultiDeletionInteraction
                        .start(this, adapter.getSelectedContactIdList())
                        .setListener(new MultiContactDeleteListener() {
                            @Override
                            public void onDeletionFinished() {
                                if (adapter != null) {
                                    adapter.setMultiMode(false);
                                }

                                if (getParentFragment() instanceof FreemeListsFragment) {
                                    ((FreemeListsFragment) getParentFragment()).updateCalllogInfo();
                                }
                            }

                            @Override
                            public void onDeletionCancelled() {
                                FreemeMultiChoiceService.STATUS = FreemeMultiChoiceService.STATUS_IDLE;
                            }
                        });
                break;
            }
            case ACTION_CODE_MERGE:
                ArrayList<Long> contactIds = adapter.getSelectedContactIdList();
                if (contactIds != null && contactIds.size() > 1) {
                    ArrayList<String> list = new ArrayList<>();
                    for (Long id : contactIds) {
                        list.add(id + "");
                    }
                    new FreemeContactsMerge().doMerge(getActivity(), list);
                }

                break;
        }
    };

    private String mQueryString;
    private boolean isSearchMode;

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.equals(mQueryString)) {
            return;
        }
        isSearchMode = !TextUtils.isEmpty(s);
        if (adapter != null) {
            adapter.setSearchMode(isSearchMode);
        }
        if (!TextUtils.equals(mQueryString, s)) {
            mQueryString = s.toString();
            if (PermissionsUtil.hasContactsReadPermissions(getContext())) {
                getLoaderManager().restartLoader(LOADER_ID_LOAD_CONTACTS_DATA, null, this);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    public void onAllSelected() {
        adapter.allSelecteOrNot();
    }

    public boolean inSearchContactorMode() {
        return !TextUtils.isEmpty(mQueryString);
    }

    public void clearSearchContactorFocus() {
        mSearchEdit.clearFocus();
        mSearchEdit.setText(null);
    }
}
