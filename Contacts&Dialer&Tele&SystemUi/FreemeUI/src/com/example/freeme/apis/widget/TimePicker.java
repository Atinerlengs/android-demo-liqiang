package com.example.freeme.apis.widget;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.freeme.widget.FreemeTimePicker;

import com.example.freeme.apis.R;

public class TimePicker extends Activity {
    private static final String TAG = "TimePickerTest";

    private FreemeTimePicker mTimePicker;
    private EditText mHourSpinnerInput;
    private EditText mMinuteSpinnerInput;
    private EditText mAmPmInput;

    private int mHourValue;
    private int mMinuteValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timepicker_0);

        mTimePicker = (FreemeTimePicker) findViewById(R.id.timepicker);
        setEditTimePicker();
    }

    private void setEditTimePicker() {
        mTimePicker.setOnTimeChangedListener(new FreemeTimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(FreemeTimePicker view, int hour, int min) {
                mHourValue = hour;
                mMinuteValue = min;
            }
        });
        mTimePicker.setOnEditTextModeChangedListener(new FreemeTimePicker.OnEditTextModeChangedListener() {
            @Override
            public void onEditTextModeChanged(FreemeTimePicker view, boolean editTextMode) {
                mHourValue = view.getHour();
                mMinuteValue = view.getMinute();
            }
        });
        mTimePicker.setIs24HourView(DateFormat.is24HourFormat(this));

        mHourSpinnerInput = mTimePicker.getEditText(FreemeTimePicker.PICKER_HOUR);
        mMinuteSpinnerInput = mTimePicker.getEditText(FreemeTimePicker.PICKER_MINUTE);
        mAmPmInput = mTimePicker.getEditText(FreemeTimePicker.PICKER_AMPM);

        mHourSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_NEXT
                | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        mMinuteSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_DONE
                | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        mHourSpinnerInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 1) {
                    mMinuteSpinnerInput.requestFocus();
                    mMinuteSpinnerInput.selectAll();
                }
            }
        });
        mMinuteSpinnerInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                final int txtlen;
                if ((txtlen = s.toString().length()) > 0) {
                    if (txtlen > 1) {
                        mMinuteSpinnerInput.selectAll();
                    }
                    try {
                        mMinuteValue = Integer.parseInt(s.toString());
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "NumberFormatException ");
                    }
                }
            }
        });
    }
}
