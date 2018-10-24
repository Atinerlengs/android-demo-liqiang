package com.freeme.filemanager.fragment;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.InetAddress;

import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.FileExplorerTabActivity.IBackPressedListener;
import com.freeme.filemanager.app.FTPServerService;
import com.freeme.filemanager.util.EntriesManager;

import org.swiftp.Defaults;
import org.swiftp.Globals;
import org.swiftp.UiUpdater;

public class ServerControlFragment extends BaseFragment implements IBackPressedListener {

    private static final String TAG = "ServerControlFragment";

    private static final int MSG_UPDATE_UI  = 0;
    private static final int MSG_DELAY_LOAD = 1;

    private boolean mPrepared;

    private TextView mIpText;
    private TextView mInstructionText;
    private TextView mInstructionTextPre;

    private View mOptButton;

    private View mRootView;

    private Context mContext;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_UI:
                    removeMessages(MSG_UPDATE_UI);
                    updateUi();
                    break;
                case MSG_DELAY_LOAD:
                    init();
                    break;
                default:
                    break;
            }
        }
    };

    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            Log.d(TAG, "Wifi status broadcast received");
            updateUi();
        }
    };

    public ServerControlFragment() {
        //do nothing
    }

    @Override
    public View onFragmentCreateView(LayoutInflater inflater,
                                     ViewGroup container, Bundle savedInstanceState) {
        mContext = getContext();
        mRootView = inflater.inflate(R.layout.layout_file_explorer_stub, container, false);
        mHandler.sendEmptyMessageDelayed(MSG_DELAY_LOAD, 200);
        return mRootView;
    }

    @Override
    public boolean onBack() {
        return false;
    }

    @Override
    public void fragmentShow() {
        super.fragmentShow();

        if (!mPrepared) {
            return;
        }
        UiUpdater.registerClient(mHandler);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_UI, 200);
    }

    @Override
    public void fragmentHint() {
        super.fragmentHint();
        if (!mPrepared) {
            return;
        }
        UiUpdater.unregisterClient(mHandler);
        Log.d(TAG, "Unregistered for wifi updates");
        if (mContext != null) {
            mContext.unregisterReceiver(mWifiReceiver);
        }
    }

    public void onResume() {
        super.onResume();
        UiUpdater.registerClient(mHandler);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_UI, 200);
    }

    @Override
    public void onStop() {
        super.onStop();
        UiUpdater.unregisterClient(mHandler);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UiUpdater.unregisterClient(mHandler);
    }

    private void init() {
        if (mContext == null) {
            mHandler.sendEmptyMessageDelayed(MSG_DELAY_LOAD, 200);
            return;
        }

        ViewStub stub = (ViewStub) mRootView.findViewById(R.id.viewContaniner);
        stub.setLayoutResource(R.layout.fragment_server_control);
        stub.inflate();

        // Set the application-wide context global, if not already set
        Context myContext = Globals.getContext();
        if (myContext == null && mContext != null) {
            myContext = mContext.getApplicationContext();
            if (myContext == null) {
                return;
            }
            Globals.setContext(myContext);
        } else {
            myContext = ActivityThread.currentApplication();
            Globals.setContext(myContext);
        }

        mIpText = (TextView) mRootView.findViewById(R.id.ip_address);
        mInstructionText = (TextView) mRootView.findViewById(R.id.instruction);
        mInstructionTextPre = (TextView) mRootView.findViewById(R.id.instruction_pre);
        mOptButton = mRootView.findViewById(R.id.server_bottom);

        updateUi();
        UiUpdater.registerClient(mHandler);

        if (mContext != null) {
            Log.d(TAG, "Registered for wifi updates");
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mWifiReceiver, filter);
        }

        mPrepared = true;
    }

    /**
     * This will be called by the static UiUpdater whenever the service has
     * changed state in a way that requires us to update our UI. We can't use
     * any myLog.l() calls in this function, because that will trigger an
     * endless loop of UI updates.
     */
    public void updateUi() {
        Log.d(TAG, "Updating UI");

        if (mContext == null) {
            Log.d(TAG, "Error: mContext is null");
            return;
        }

        final boolean isWifiReady = FTPServerService.isWifiEnabled();

        ImageView wifiImg = (ImageView) mRootView.findViewById(R.id.wifi_state_image);
        wifiImg.setImageResource(isWifiReady ? R.drawable.wifi_state4 : R.drawable.wifi_state0);

        boolean running = FTPServerService.isRunning();
        if (running) {
            Log.d(TAG, "updateUi: server is running");
            // Put correct text in start/stop button
            // Fill in wifi status and address
            InetAddress address = FTPServerService.getWifiIp();
            if (address != null) {
                String port = ":" + FTPServerService.getPort();
                mIpText.setText("ftp://" + address.getHostAddress() + (FTPServerService.getPort() == 21 ? "" : port));
                EntriesManager.initEntries(mContext.getApplicationContext());
            } else {
                // could not get IP address, stop the service
                Intent intent = new Intent(mContext, FTPServerService.class);
                mContext.stopService(intent);
                mIpText.setText("");
                EntriesManager.releaseEntries();
            }
            wifiImg.setImageResource(R.drawable.wifi_state1);
        }

        if (!isWifiReady) {
            mOptButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                }
            });
        } else {
            mOptButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Globals.setLastError(null);
                    File chrootDir = new File(Defaults.chrootDir);

                    Context context = mContext.getApplicationContext();
                    Intent intent = new Intent(context, FTPServerService.class);

                    Globals.setChrootDir(chrootDir);
                    if (!FTPServerService.isRunning()) {
                        warnIfNoExternalStorage();
                        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                            context.startService(intent);
                        }
                    } else {
                        context.stopService(intent);
                    }
                }
            });
        }

        TextView startStopButtonText = (TextView) mRootView.findViewById(R.id.server_operate);
        if (isWifiReady) {
            startStopButtonText.setText(running ? R.string.stop_server : R.string.start_server);
        } else {
            if (FTPServerService.isRunning()) {
                Context context = mContext.getApplicationContext();
                Intent intent = new Intent(context, FTPServerService.class);
                context.stopService(intent);
            }
            startStopButtonText.setText(R.string.no_wifi);
        }
        mInstructionTextPre.setText(isWifiReady? R.string.instruction_pre:R.string.instruction_pre_onwifi);
        mIpText.setVisibility(running ? View.VISIBLE : View.INVISIBLE);
        mInstructionText.setVisibility(running ? View.VISIBLE : View.GONE);
        mInstructionTextPre.setVisibility(running ? View.GONE : View.VISIBLE);
    }

    private void warnIfNoExternalStorage() {
        final String storageState = Environment.getExternalStorageState();
        if (!storageState.equals(Environment.MEDIA_MOUNTED)) {
            Log.i(TAG, "Warning due to storage state " + storageState);
            Toast toast = Toast.makeText(getActivity(), R.string.storage_device_umouonted, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }
}
