package com.example.freeme.apis.effect;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import com.example.freeme.apis.R;
import android.widget.Button;
import java.util.Calendar;

public class Datepicker1 extends Activity implements View.OnClickListener {
    private DatePicker datePicker;
    private Calendar calendar;
    private Button showDialog;

    private int mYear;
    private int mMonth;
    private int mDay;
    private int mHour;
    private int mMinute;
    static final int TIME_12_DIALOG_ID = 0;
    static final int TIME_24_DIALOG_ID = 1;
    static final int DATE_DIALOG_ID = 2;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datepicker);
        showDialog = (Button) findViewById(R.id.showDialog);
        showDialog.setOnClickListener(this);
        // 获取日历对象
        calendar = Calendar.getInstance();
// 获取当前对应的年、月、日的信息
        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH) + 1;
        mDay = calendar.get(Calendar.DAY_OF_MONTH);
        mHour = calendar.get(Calendar.HOUR_OF_DAY);
        mMinute = calendar.get(Calendar.MINUTE);
        //setDialogOnClickListener(R.id.showDialog, DATE_DIALOG_ID);
        datePicker = (DatePicker) this.findViewById(R.id.myDatePicker);

// dataPicker初始化
        datePicker.init(mYear, mMonth, mDay, new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                setTitle(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
            }
        });


    }

    @Override
    public void onClick(View v) {
        showDialog(DATE_DIALOG_ID);
    }
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case TIME_12_DIALOG_ID:
            case TIME_24_DIALOG_ID:
/*
                return new TimePickerDialog(this,
                        mTimeSetListener, mHour, mMinute, id == TIME_24_DIALOG_ID);
*/
            case DATE_DIALOG_ID:
                return new DatePickerDialog(this,
                        mDateSetListener,
                        mYear, mMonth, mDay);
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case TIME_12_DIALOG_ID:
            case TIME_24_DIALOG_ID:
                ((TimePickerDialog) dialog).updateTime(mHour, mMinute);
                break;
            case DATE_DIALOG_ID:
                ((DatePickerDialog) dialog).updateDate(mYear, mMonth, mDay);
                break;
        }
    }


    private DatePickerDialog.OnDateSetListener mDateSetListener =
            new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, int monthOfYear,
                                      int dayOfMonth) {
                    mYear = year;
                    mMonth = monthOfYear;
                    mDay = dayOfMonth;
                    //updateDisplay();
                }
            };

}
