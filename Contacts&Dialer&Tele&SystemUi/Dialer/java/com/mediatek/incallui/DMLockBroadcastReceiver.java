package com.mediatek.incallui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.android.incallui.call.DialerCall;
import com.android.incallui.call.CallList;
import com.android.incallui.InCallPresenter;
import com.android.incallui.Log;
import com.android.incallui.R;

public class DMLockBroadcastReceiver extends BroadcastReceiver {
    public static DMLockBroadcastReceiver sDMLockBroadcastReceiver;

    private DMLockBroadcastReceiver(Context context) {
        mStatusBarHelper = new StatusBarHelper(context);
    }

    // Only one this receiver can be registered/unregisted
    public synchronized static DMLockBroadcastReceiver getInstance(Context context) {
        if (sDMLockBroadcastReceiver == null && context != null) {
            sDMLockBroadcastReceiver = new DMLockBroadcastReceiver(context.getApplicationContext());
        }

        return sDMLockBroadcastReceiver;
    }

    /// DM lock @{
    private final static String ACTION_LOCKED = "com.mediatek.dm.LAWMO_LOCK";
    private final static String ACTION_UNLOCK = "com.mediatek.dm.LAWMO_UNLOCK";
    /// @}

    /// privacy protect @{
    private final static String NOTIFY_LOCKED = "com.mediatek.ppl.NOTIFY_LOCK";
    private final static String NOTIFY_UNLOCK = "com.mediatek.ppl.NOTIFY_UNLOCK";
    /// @}

    private StatusBarHelper mStatusBarHelper;

    public void register(Context context) {
        IntentFilter lockFilter = new IntentFilter(ACTION_LOCKED);
        lockFilter.addAction(ACTION_UNLOCK);

        Log.d(this, "register ppl lock message");
        lockFilter.addAction(NOTIFY_LOCKED);
        lockFilter.addAction(NOTIFY_UNLOCK);
        context.registerReceiver(this, lockFilter);
    }

    public void unregister(Context context) {
        // reset privacyProtectEnable
        Log.d(this, "Disable privacy protect and enable system bar in onDestroy()");
        /// M: For ALPS1561354. The on-going call would be hung up as NOTIFY_LOCK arrives.
        // At this point, this activity destroyed and unexpectly unregister the broadcast
        // receiver which is suppose to receive the subsequent NOTIFY_UNLOCK. This change
        // insure to do the reverse operation for the NOTIFY_LOCK in this case
        mStatusBarHelper.enableSystemBarNavigation(true);
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(this, "action: " + action);
        /// if privacy protect open,should disable NavigationBar. For ALPS01414144 @
        if (action.equals(NOTIFY_LOCKED)) {
            mStatusBarHelper.enableSystemBarNavigation(false);
        } else if (action.equals(NOTIFY_UNLOCK)) {
            mStatusBarHelper.enableSystemBarNavigation(true);
        }
        /// @}
        DialerCall call = CallList.getInstance().getActiveOrBackgroundCall();
        if (call == null || !DialerCall.State.isConnectingOrConnected(call.getState())) {
            Log.d(this, "mDMLockReceiver , return");
            return;
        }
        if (action.equals(ACTION_LOCKED) || action.equals(NOTIFY_LOCKED)) {
            int msg = R.string.dm_lock;
            if (call.getState() == DialerCall.State.IDLE) {
                return;
            } else {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            }
        } else if (action.equals(ACTION_UNLOCK) || action.equals(NOTIFY_UNLOCK)) {
            int msg = R.string.dm_unlock;
            if (call.getState() == DialerCall.State.IDLE) {
                return;
            } else {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            }
        }

        InCallPresenter.getInstance().onCallListChange(CallList.getInstance());
    }
}
