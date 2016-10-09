package com.waylens.hachi.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.Hachi;

/**
 * Created by Xiaofei on 2016/10/9.
 */

public class ConnectivityHelper {
    private static final String TAG = ConnectivityHelper.class.getSimpleName();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setPreferredNetwork(int desirednetworkType) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) Hachi.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        Network[] network = connectivityManager.getAllNetworks();
        if (network != null && network.length > 0) {
            for (int i = 0; i < network.length; i++) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network[i]);
                int networkType = networkInfo.getType();
                Logger.t(TAG).d("network: " + networkInfo.toString());
                if (desirednetworkType == networkType) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        boolean bind = connectivityManager.bindProcessToNetwork(network[i]);
                        Logger.t(TAG).d("bind result: " + bind);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        boolean bind = connectivityManager.setProcessDefaultNetwork(network[i]);
                        Logger.t(TAG).d("bind result: " + bind);
                    }
                }
            }
        }

    }
}
