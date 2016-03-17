package com.waylens.hachi.hardware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.hardware.smartconfig.NetworkUtil;

/**
 * Created by Richard on 3/17/16.
 */
public class NetworkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (NetworkUtil.isWifiConnected(context)) {
            ((Hachi) context.getApplicationContext()).startDeviceScanner();
        }
    }
}
