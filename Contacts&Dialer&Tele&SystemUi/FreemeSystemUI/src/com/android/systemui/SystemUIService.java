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

package com.android.systemui;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemProperties;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.freeme.recents.RecentsUtils;

public class SystemUIService extends Service {

    //*/ freeme.lishoubo, 20180125. recents surface.
    final private Handler mHandler = new Handler();
    final private Messenger mMessenger = new Messenger(new IncomingHandler());
    //*/
    @Override
    public void onCreate() {
        super.onCreate();
        ((SystemUIApplication) getApplication()).startServicesIfNeeded();

        // For debugging RescueParty
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("debug.crash_sysui", false)) {
            throw new RuntimeException();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        /*/ freeme.lishoubo, 20180125. recents surface.
        return null;
        /*/
        return mMessenger.getBinder();
        //*/
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        SystemUI[] services = ((SystemUIApplication) getApplication()).getServices();
        if (args == null || args.length == 0) {
            for (SystemUI ui: services) {
                pw.println("dumping service: " + ui.getClass().getName());
                ui.dump(fd, pw, args);
            }
        } else {
            String svc = args[0];
            for (SystemUI ui: services) {
                String name = ui.getClass().getName();
                if (name.endsWith(svc)) {
                    ui.dump(fd, pw, args);
                }
            }
        }
    }
    //*/ freeme.lishoubo, 20180125. recents surface.
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            RecentsComponent recents = ((SystemUIApplication) getApplication()).getComponent(Recents.class);
            switch (msg.what) {
                case RecentsUtils.FREEME_SHOW_SCREEN_PIN_REQUEST:
                    EventBus.getDefault().send(new ScreenPinningRequestEvent(SystemUIService.this, msg.arg1));
                    break;
                case RecentsUtils.FREEME_RECENTS_DRAWN:
                    EventBus.getDefault().send(new RecentsDrawnEvent());
                    break;
                default:
                    break;
            }
        }
    }
    //*/
}

