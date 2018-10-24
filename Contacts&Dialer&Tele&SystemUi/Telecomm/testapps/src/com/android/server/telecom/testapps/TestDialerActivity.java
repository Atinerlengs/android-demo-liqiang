package com.android.server.telecom.testapps;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog.Calls;
// M: RTT audio mode.
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
// M: Video call.
import android.telecom.VideoProfile;
// M: Set input type to EditText.
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
// M: RTT audio mode. @{
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
// M: @}
import android.widget.CheckBox;
import android.widget.EditText;
// M: RTT audio mode.
import android.widget.Spinner;
import android.widget.Toast;

//M: RTT audio mode.
import com.mediatek.provider.MtkSettingsExt;

public class TestDialerActivity extends Activity {
    private static final int REQUEST_CODE_SET_DEFAULT_DIALER = 1;

    private EditText mNumberView;
    private CheckBox mRttCheckbox;
    /// M: Initial video call.
    private CheckBox mVideoCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testdialer_main);
        findViewById(R.id.set_default_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setDefault();
            }
        });

        findViewById(R.id.place_call_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                placeCall();
            }
        });

        // M: Currently unused. @{
        /*findViewById(R.id.test_voicemail_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                testVoicemail();
            }
        });

        findViewById(R.id.cancel_missed_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelMissedCallNotification();
            }
        });*/
        // M: @}

        // M: RTT audio mode selection. @{
        Spinner rttAudioModeSelector = (Spinner) findViewById(R.id.rtt_audio_mode_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.rtt_audio_mode_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rttAudioModeSelector.setAdapter(adapter);
        rttAudioModeSelector.setSelection(Settings.Global.getInt(
                TestDialerActivity.this.getContentResolver(),
                MtkSettingsExt.Global.TELECOM_RTT_AUDIO_MODE, 0));
        rttAudioModeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CharSequence selection = (CharSequence) parent.getItemAtPosition(position);
                switch (selection.toString()) {
                    case "Normal voice":
                        Settings.Global.putInt(TestDialerActivity.this.getContentResolver(),
                                MtkSettingsExt.Global.TELECOM_RTT_AUDIO_MODE, 0);
                        break;
                    case "Reduced voice":
                        Settings.Global.putInt(TestDialerActivity.this.getContentResolver(),
                                MtkSettingsExt.Global.TELECOM_RTT_AUDIO_MODE, 1);
                        break;
                    default:
                        Log.i(TestDialerActivity.class.getSimpleName(),
                                "Bad name for rtt audio mode = " + selection.toString());
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // M: @}

        mNumberView = (EditText) findViewById(R.id.number);
        // M: Set input type to make user can input '+' for country number.
        mNumberView.setInputType(InputType.TYPE_CLASS_PHONE);
        mRttCheckbox = (CheckBox) findViewById(R.id.call_with_rtt_checkbox);
        /// M: Initial video call. @{
        mVideoCheckbox = (CheckBox) findViewById(R.id.call_with_video_checkbox);
        if (MtkTelecomTestappsGlobals.isAdvancedFeatureSupport(this)) {
            mVideoCheckbox.setVisibility(View.VISIBLE);
        }
        /// @}
        updateMutableUi();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER) {
            if (resultCode == RESULT_OK) {
                showToast("User accepted request to become default dialer");
            } else if (resultCode == RESULT_CANCELED) {
                showToast("User declined request to become default dialer");
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateMutableUi();
    }

    private void updateMutableUi() {
        Intent intent = getIntent();
        if (intent != null) {
            mNumberView.setText(intent.getDataString());
            mRttCheckbox.setChecked(
                    intent.getBooleanExtra(TelecomManager.EXTRA_START_CALL_WITH_RTT, false));
        }
    }

    private void setDefault() {
        final Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
        intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, getPackageName());
        startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER);
    }

    private void placeCall() {
        final TelecomManager telecomManager =
                (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        /// M: Initial video call @{
        if (mRttCheckbox.isChecked() && mVideoCheckbox.isChecked()) {
            showToast("Not allowed to enable both RTT and Video!");
            return;
        }

        Bundle bundle = createCallIntentExtras();
        // Video call can't be setup with TelecomManager.placeCall because
        // this function can't set intent video parameters.
        if (mVideoCheckbox.isChecked()) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.fromParts(
                    PhoneAccount.SCHEME_TEL, mNumberView.getText().toString(), null));
            intent.putExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                    VideoProfile.STATE_BIDIRECTIONAL);
            intent.putExtras(bundle);
            startActivity(intent);
        } else {
            telecomManager.placeCall(Uri.fromParts(PhoneAccount.SCHEME_TEL,
                    mNumberView.getText().toString(), null), bundle);
        }
        /// @}
    }

    private void testVoicemail() {
        try {
            // Test read
            getContentResolver().query(Calls.CONTENT_URI_WITH_VOICEMAIL, null, null, null, null);
            // Test write
            final ContentValues values = new ContentValues();
            values.put(Calls.CACHED_NAME, "hello world");
            getContentResolver().update(Calls.CONTENT_URI_WITH_VOICEMAIL, values, "1=0", null);
        } catch (SecurityException e) {
            showToast("Permission check failed");
            return;
        }
        showToast("Permission check succeeded");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void cancelMissedCallNotification() {
        try {
            final TelecomManager tm = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            tm.cancelMissedCallsNotification();
        } catch (SecurityException e) {
            Toast.makeText(this, "Privileged dialer operation failed", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Privileged dialer operation succeeded", Toast.LENGTH_SHORT).show();
    }

    private Bundle createCallIntentExtras() {
        Bundle extras = new Bundle();
        extras.putString("com.android.server.telecom.testapps.CALL_EXTRAS", "Hall was here");
        if (mRttCheckbox.isChecked()) {
            extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_RTT, true);
        }

        Bundle intentExtras = new Bundle();
        intentExtras.putBundle(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, extras);
        Log.i("Santos xtr", intentExtras.toString());
        return intentExtras;
    }
}
