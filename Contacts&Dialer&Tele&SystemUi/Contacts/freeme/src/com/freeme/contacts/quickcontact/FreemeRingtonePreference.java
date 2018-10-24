package com.freeme.contacts.quickcontact;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings.System;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;

/**
 * support select custom ringtone.
 *
 * A {@link Preference} that allows the user to choose a ringtone from those on the device.
 * The chosen ringtone's URI will be persisted as a string.
 * <p>
 * If the user chooses the "Default" item, the saved string will be one of
 * {@link System#DEFAULT_RINGTONE_URI},
 * {@link System#DEFAULT_NOTIFICATION_URI}, or
 * {@link System#DEFAULT_ALARM_ALERT_URI}. If the user chooses the "Silent"
 * item, the saved string will be an empty string.
 *
 * @attr ref android.R.styleable#RingtonePreference_ringtoneType
 * @attr ref android.R.styleable#RingtonePreference_showDefault
 * @attr ref android.R.styleable#RingtonePreference_showSilent
 * <p>
 * Based of frameworks/base/core/java/android/preference/FreemeRingtonePreference.java
 * but extends android.support.v7.preference.Preference instead.
 */
public class FreemeRingtonePreference extends Preference {

    private static final String TAG = "FreemeRingtonePreference";

    private int mRingtoneType;
    private boolean mShowDefault;
    private boolean mShowSilent;

    private Uri mCheckedUri;

    public FreemeRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.RingtonePreference, 0, 0);
        mRingtoneType = a.getInt(com.android.internal.R.styleable.RingtonePreference_ringtoneType,
                RingtoneManager.TYPE_ALARM);
        mShowDefault = a.getBoolean(com.android.internal.R.styleable.RingtonePreference_showDefault,
                true);
        mShowSilent = a.getBoolean(com.android.internal.R.styleable.RingtonePreference_showSilent,
                true);
        setIntent(new Intent(RingtoneManager.ACTION_RINGTONE_PICKER));
        a.recycle();
    }

    /**
     * Returns the sound type(s) that are shown in the picker.
     *
     * @return The sound type(s) that are shown in the picker.
     * @see #setRingtoneType(int)
     */
    public int getRingtoneType() {
        return mRingtoneType;
    }

    /**
     * Sets the sound type(s) that are shown in the picker.
     *
     * @param type The sound type(s) that are shown in the picker.
     * @see RingtoneManager#EXTRA_RINGTONE_TYPE
     */
    public void setRingtoneType(int type) {
        mRingtoneType = type;
    }

    /**
     * Returns whether to a show an item for the default sound/ringtone.
     *
     * @return Whether to show an item for the default sound/ringtone.
     */
    public boolean getShowDefault() {
        return mShowDefault;
    }

    /**
     * Sets whether to show an item for the default sound/ringtone. The default
     * to use will be deduced from the sound type(s) being shown.
     *
     * @param showDefault Whether to show the default or not.
     * @see RingtoneManager#EXTRA_RINGTONE_SHOW_DEFAULT
     */
    public void setShowDefault(boolean showDefault) {
        mShowDefault = showDefault;
    }

    /**
     * Returns whether to a show an item for 'Silent'.
     *
     * @return Whether to show an item for 'Silent'.
     */
    public boolean getShowSilent() {
        return mShowSilent;
    }

    /**
     * Sets whether to show an item for 'Silent'.
     *
     * @param showSilent Whether to show 'Silent'.
     * @see RingtoneManager#EXTRA_RINGTONE_SHOW_SILENT
     */
    public void setShowSilent(boolean showSilent) {
        mShowSilent = showSilent;
    }


    /**
     * Prepares the intent to launch the ringtone picker. This can be modified
     * to adjust the parameters of the ringtone picker.
     *
     * @param ringtonePickerIntent The ringtone picker intent that can be
     *                             modified by putting extras.
     */
    public void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {

        /*ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                onRestoreRingtone());*/

        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, mShowDefault);
        if (mShowDefault) {
            ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    RingtoneManager.getDefaultUri(getRingtoneType()));
        }

        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, mShowSilent);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, mRingtoneType);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getTitle());
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS,
                AudioAttributes.FLAG_BYPASS_INTERRUPTION_POLICY);
    }

    /**
     * Called when a ringtone is chosen.
     * <p>
     * By default, this saves the ringtone URI to the persistent storage as a
     * string.
     *
     * @param ringtoneUri The chosen ringtone's {@link Uri}. Can be null.
     */
    protected void onSaveRingtone(Uri ringtoneUri) {
        setCheckedUri(ringtoneUri);
    }

    public Uri getCheckedUri() {
        return mCheckedUri;
    }

    public void setCheckedUri(Uri checkedUri) {
        mCheckedUri = checkedUri;
    }

    /**
     * Called when the chooser is about to be shown and the current ringtone
     * should be marked. Can return null to not mark any ringtone.
     * <p>
     * By default, this restores the previous ringtone URI from the persistent
     * storage.
     *
     * @return The ringtone to be marked as the current ringtone.
     */
    protected Uri onRestoreRingtone() {
        /*final String uriString = getPersistedString(null);
        return !TextUtils.isEmpty(uriString) ? Uri.parse(uriString) : null;*/
        return getCheckedUri();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValueObj) {
        String defaultValue = (String) defaultValueObj;

        /*
         * This method is normally to make sure the internal state and UI
         * matches either the persisted value or the default value. Since we
         * don't show the current value in the UI (until the dialog is opened)
         * and we don't keep local state, if we are restoring the persisted
         * value we don't need to do anything.
         */
        if (restorePersistedValue) {
            return;
        }

        // If we are setting to the default value, we should persist it.
        if (!TextUtils.isEmpty(defaultValue)) {
            onSaveRingtone(Uri.parse(defaultValue));
        }
    }

    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

            if (callChangeListener(uri != null ? uri.toString() : "")) {
                onSaveRingtone(uri);
            }
        }

        return true;
    }

}
