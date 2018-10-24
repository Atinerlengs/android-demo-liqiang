package com.freeme.dialer.contactsfragment;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.dialer.R;
import com.android.dialer.util.PermissionsUtil;
import com.freeme.contacts.common.utils.FreemeIntentUtils;

class FreemeContactsListHeaderViewHolder extends ViewHolder implements View.OnClickListener {

    private final Context mContext;
    private FrameLayout mMyProfile;
    private FrameLayout mContactsGroup;

    private boolean mHasProfile = false;
    private long mContactId = -1;

    FreemeContactsListHeaderViewHolder(LoaderManager loaderManager, View view) {
        super(view);
        mContext = view.getContext();

        mMyProfile = view.findViewById(R.id.freeme_my_profile);
        ((TextView) mMyProfile.findViewById(R.id.header_text)).setText(R.string.freeme_my_profile);
        mMyProfile.setOnClickListener(this);

        mContactsGroup = view.findViewById(R.id.freeme_contacts_group);
        ((TextView) mContactsGroup.findViewById(R.id.header_text)).setText(R.string.freeme_my_contacts_group);
        mContactsGroup.setOnClickListener(this);

        if (PermissionsUtil.hasContactsReadPermissions(mContext)) {
            loaderManager.initLoader(FreemeContactsFragment.LOADER_ID_LOAD_PROFIIE_DATA,
                    null, mProfileLoaderListener);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.freeme_my_profile:
                FreemeIntentUtils.startActivityWithErrorToast(mContext,
                        FreemeIntentUtils.getMyProfileIntent(mHasProfile, mContactId,
                                mHasProfile ? mContext.getString(R.string.freeme_user_profile)
                                        : mContext.getString(R.string.freeme_modify_my_card)));
                break;
            case R.id.freeme_contacts_group:
                FreemeIntentUtils.startActivityWithErrorToast(mContext,
                        FreemeIntentUtils.getBrowseContactsGroupIntent(mContext));
                break;
        }
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mProfileLoaderListener =
            new LoaderManager.LoaderCallbacks<Cursor>() {
                @Override
                public CursorLoader onCreateLoader(int id, Bundle args) {
                    final CursorLoader loader = createCursorLoader(mContext);
                    loader.setUri(ContactsContract.Profile.CONTENT_URI);
                    loader.setProjection(new String[]{
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.IS_USER_PROFILE
                    });
                    return loader;
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                    if (cursor != null && cursor.moveToFirst()) {
                        mContactId = cursor.getLong(0);
                        mHasProfile = cursor.getInt(1) == 1;
                    } else {
                        mHasProfile = false;
                    }
                }

                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    private CursorLoader createCursorLoader(Context context) {
        return new CursorLoader(context) {
            @Override
            protected Cursor onLoadInBackground() {
                try {
                    return super.onLoadInBackground();
                } catch (RuntimeException e) {
                    return null;
                }
            }
        };
    }
}