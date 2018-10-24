/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.freeme.apis.view;

import com.example.freeme.apis.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


/**
 * Demonstrates splitting touch events across multiple views within a view group.
 */
public class ToastTest extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.toast_show_view);
        Button toast_show = (Button) findViewById(R.id.toast_show);
        toast_show.setOnClickListener(itemClickListener);
        //list2.setOnItemClickListener(itemClickListener);
    }

    private int responseIndex = 0;

    private final OnClickListener itemClickListener = new OnClickListener() {
        public  void onClick(View v){
            Toast toasttest = Toast.makeText(getApplicationContext(), "TOAST STYLE TEST", Toast.LENGTH_SHORT);
            toasttest.show();
        }
    };
}
