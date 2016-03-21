package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.integrity_project.smartconfiglib.SmartConfig;
import com.integrity_project.smartconfiglib.SmartConfigListener;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.smartconfig.NetworkUtil;
import com.waylens.hachi.hardware.smartconfig.SmartConfigConstants;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/3/15.
 */
public class SmartConfigActivity extends BaseActivity {
    private static final String TAG = SmartConfigActivity.class.getSimpleName();
    int runTime;
    boolean isPasswordShown = false;
    boolean isStartClicked = false;
    boolean waitForScanFinish = false;
    boolean foundNewDevice = false;

    SmartConfig smartConfig;
    SmartConfigListener smartConfigListener;

    byte[] freeData;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, SmartConfigActivity.class);
        activity.startActivity(intent);
    }


    @Bind(R.id.smartconfig_network_name_field)
    EditText mEtNetworkName;

    @Bind(R.id.smartconfig_network_pass_field)
    EditText mEtNetworkPasswd;

    @Bind(R.id.smartconfig_progressbar)
    ProgressBar smartconfig_progressbar;

    @OnClick(R.id.smartconfig_start)
    public void smartconfig_start() {
        if (isStartClicked) {
            foundNewDevice = true;
            runTime = SmartConfigConstants.SC_RUNTIME; // stop SmartConfig and reset the progress bar
        } else {

            startSmartConfig();
        }
    }


    BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mEtNetworkName.setText(NetworkUtil.getWifiName(SmartConfigActivity.this));
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(networkChangeReceiver, new IntentFilter(SmartConfigConstants.NETWORK_CHANGE_BROADCAST_ACTION));
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_smart_config);
    }

    @Override
    public void setupToolbar() {
        mToolbar.setTitle(R.string.title_smartconfig);
        mToolbar.setNavigationIcon(R.drawable.navbar_close);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        super.setupToolbar();
    }

    private void startSmartConfig() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                runProgressBar();
            }
        }).start();

        Logger.t(TAG).d("start smart config");

        foundNewDevice = false;
        String passwordKey = mEtNetworkPasswd.getText().toString().trim();
        byte[] paddedEncryptionKey;
        String SSID = mEtNetworkName.getText().toString().trim();
        String gateway = NetworkUtil.getGateway(this);
        paddedEncryptionKey = null;


        freeData = new byte[1];
        freeData[0] = 0x03;

        smartConfig = null;
        smartConfigListener = new SmartConfigListener() {
            @Override
            public void onSmartConfigEvent(SmtCfgEvent event, Exception e) {}
        };
        try {
            smartConfig = new SmartConfig(smartConfigListener, freeData, passwordKey, paddedEncryptionKey, gateway, SSID, (byte) 0, "");
            smartConfig.transmitSettings();
            //lookForNewDevice();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void runProgressBar() {
        isStartClicked = true;
        runTime = 0;
//        hideTabs();
        try {
            while (runTime < SmartConfigConstants.SC_RUNTIME) {
                if ((runTime > 0) && ((runTime % SmartConfigConstants.SC_MDNS_INTERVAL) == 0)) {
                    System.out.println("Pausing MDNS...");
//                    pauseMDNS();
                }
                Thread.sleep(SmartConfigConstants.SC_PROGRESSBAR_INTERVAL);
                runTime += SmartConfigConstants.SC_PROGRESSBAR_INTERVAL;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateProgressBar(runTime);
                    }
                });

            }
        } catch (InterruptedException e) {
        } finally {
            isStartClicked = false;
            waitForScanFinish = false;
            if (!foundNewDevice) {
//                notifyNotFoundNewDevice(); // haven't found new device
            }
//            resetProgressBar();
//            stopSmartConfig();
        }
    }

    void updateProgressBar(int runTime) {
        smartconfig_progressbar.setVisibility(View.VISIBLE);
        if (smartconfig_progressbar != null) {
            smartconfig_progressbar.setProgress(smartconfig_progressbar.getMax() * runTime / SmartConfigConstants.SC_RUNTIME);
        }
    }
}
