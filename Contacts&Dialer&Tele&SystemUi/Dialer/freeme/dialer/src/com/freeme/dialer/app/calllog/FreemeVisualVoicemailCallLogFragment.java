package com.freeme.dialer.app.calllog;

import android.app.KeyguardManager;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.VoicemailContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.dialer.app.R;
import com.android.dialer.app.calllog.CallLogNotificationsService;
import com.android.dialer.app.voicemail.VoicemailAudioManager;
import com.android.dialer.app.voicemail.VoicemailErrorManager;
import com.android.dialer.app.voicemail.VoicemailPlaybackPresenter;
import com.android.dialer.common.LogUtil;
import com.android.dialer.logging.DialerImpression;
import com.android.dialer.logging.Logger;
import com.android.dialer.util.PermissionsUtil;
import com.freeme.dialer.app.list.FreemeListsFragment;

public class FreemeVisualVoicemailCallLogFragment extends FreemeCallLogFragment {

    private final ContentObserver mVoicemailStatusObserver =
            new FreemeCallLogFragment.CustomContentObserver();
    private VoicemailPlaybackPresenter mVoicemailPlaybackPresenter;
    private VoicemailErrorManager mVoicemailErrorManager;

    public FreemeVisualVoicemailCallLogFragment() {
        super(CallLog.Calls.VOICEMAIL_TYPE);
    }

    @Override
    protected VoicemailPlaybackPresenter getVoicemailPlaybackPresenter() {
        return mVoicemailPlaybackPresenter;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mVoicemailPlaybackPresenter =
                VoicemailPlaybackPresenter.getInstance(getActivity(), savedInstanceState);

        if (PermissionsUtil.hasReadVoicemailPermissions(getContext())
                && PermissionsUtil.hasAddVoicemailPermissions(getContext())) {
            getActivity()
                    .getContentResolver()
                    .registerContentObserver(
                            VoicemailContract.Status.CONTENT_URI, true, mVoicemailStatusObserver);
        } else {
            LogUtil.w(
                    "FreemeVisualVoicemailCallLogFragment.onActivityCreated",
                    "read voicemail permission unavailable.");
        }
        super.onActivityCreated(savedInstanceState);
        mVoicemailErrorManager =
                new VoicemailErrorManager(getContext(), getAdapter().getAlertManager(), mModalAlertManager);

        if (PermissionsUtil.hasReadVoicemailPermissions(getContext())
                && PermissionsUtil.hasAddVoicemailPermissions(getContext())) {
            getActivity()
                    .getContentResolver()
                    .registerContentObserver(
                            VoicemailContract.Status.CONTENT_URI,
                            true,
                            mVoicemailErrorManager.getContentObserver());
        } else {
            LogUtil.w(
                    "FreemeVisualVoicemailCallLogFragment.onActivityCreated",
                    "read voicemail permission unavailable.");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mVoicemailPlaybackPresenter.onResume();
        mVoicemailErrorManager.onResume();
    }

    @Override
    public void onPause() {
        mVoicemailPlaybackPresenter.onPause();
        mVoicemailErrorManager.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        getActivity()
                .getContentResolver()
                .unregisterContentObserver(mVoicemailErrorManager.getContentObserver());
        mVoicemailPlaybackPresenter.onDestroy();
        mVoicemailErrorManager.onDestroy();
        getActivity().getContentResolver().unregisterContentObserver(mVoicemailStatusObserver);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mVoicemailPlaybackPresenter.onSaveInstanceState(outState);
    }

    @Override
    public void fetchCalls() {
        super.fetchCalls();
        if (getParentFragment() instanceof FreemeListsFragment) {
            ((FreemeListsFragment) getParentFragment()).updateTabUnreadCounts();
        }
    }

    @Override
    public void onVisible() {
        LogUtil.enterBlock("FreemeVisualVoicemailCallLogFragment.onVisible");
        super.onVisible();
        if (getActivity() != null) {
            Intent intent = new Intent(VoicemailContract.ACTION_SYNC_VOICEMAIL);
            intent.setPackage(getActivity().getPackageName());
            getActivity().sendBroadcast(intent);
            Logger.get(getActivity()).logImpression(DialerImpression.Type.VVM_TAB_VIEWED);
            getActivity().setVolumeControlStream(VoicemailAudioManager.PLAYBACK_STREAM);
        }
    }

    @Override
    public void onNotVisible() {
        LogUtil.enterBlock("FreemeVisualVoicemailCallLogFragment.onNotVisible");
        super.onNotVisible();
        if (getActivity() != null) {
            getActivity().setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            // onNotVisible will be called in the lock screen when the call ends
            if (!getActivity().getSystemService(KeyguardManager.class).inKeyguardRestrictedInputMode()) {
                LogUtil.i("FreemeVisualVoicemailCallLogFragment.onNotVisible", "clearing all new voicemails");
                CallLogNotificationsService.markAllNewVoicemailsAsOld(getActivity());
            }
        }
    }
}
