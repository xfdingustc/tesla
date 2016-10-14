package com.waylens.hachi.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.widget.Toast;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;

import static android.content.Context.WIFI_SERVICE;

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
                if (networkInfo == null) {
                    continue;
                }
//                Logger.t(TAG).d("networkInfo: " + networkInfo.toString());
                int networkType = networkInfo.getType();
                if (desirednetworkType == networkType) {
                    setAppNetwork(connectivityManager, network[i]);
                }
            }
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Network getCelullarNetwork() {
        final ConnectivityManager connectivityManager = (ConnectivityManager) Hachi.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);


        Network[] network = connectivityManager.getAllNetworks();
        if (network != null && network.length > 0) {
            for (int i = 0; i < network.length; i++) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network[i]);
                if (networkInfo == null) {
                    continue;
                }
                Logger.t(TAG).d("networkInfo: " + networkInfo.toString());
                int networkType = networkInfo.getType();
                if (ConnectivityManager.TYPE_MOBILE == networkType) {
                    return network[i];
                }
            }
        }
        return null;
    }


    public static boolean isConnected2VdtCamera() {
        WifiManager wifiManager = (WifiManager) Hachi.getContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null && wifiInfo.getSSID().contains("C9J")) {
            return true;
        }
        return false;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void requestInternetNetwork() {
//        final ConnectivityManager connectivityManager = (ConnectivityManager) Hachi.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
//
//        NetworkRequest.Builder builder = new NetworkRequest.Builder();
//        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
//        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
//        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
//
//        NetworkRequest networkRequest = builder.build();
//        connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback(){
//            @Override
//            public void onAvailable(Network network) {
//                super.onAvailable(network);
//                setAppNetwork(connectivityManager, network);
//            }
//        });

        WifiManager wifiManager = (WifiManager) Hachi.getContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null && wifiInfo.getSSID().contains("C9J")) {
//            setPreferredNetwork(ConnectivityManager.TYPE_MOBILE);
        } else {
            Logger.t(TAG).d("ssid: " + wifiInfo.getSSID());
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void setAppNetwork(ConnectivityManager manager, Network network) {
        NetworkInfo networkInfo = manager.getNetworkInfo(network);
        Logger.t(TAG).d("bind process network: " + networkInfo.toString());
        if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            Toast.makeText(Hachi.getContext(), R.string.using_cellular, Toast.LENGTH_LONG).show();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            boolean bind = manager.bindProcessToNetwork(network);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            boolean bind = ConnectivityManager.setProcessDefaultNetwork(network);
        }
    }
}
