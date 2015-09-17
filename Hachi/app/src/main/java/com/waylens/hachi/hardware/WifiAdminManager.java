package com.waylens.hachi.hardware;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/8/12.
 */
public class WifiAdminManager {
    private static final String TAG = WifiAdminManager.class.getSimpleName();

    public static final int WIFI_SCAN_INTERVAL = 3000;
    private WifiAdmin mWifiAdmin;

    private Runnable mScanWifiAction;

    private Handler mHandler = new Handler();

    private List<WifiCallback> mWifiCallbackList = new ArrayList<>();


    private static WifiAdminManager mSharedManager = null;

    private WifiAdminManager() {

    }

    private static Context mContext;

    public static void initialize(Context context) {
        mContext = context;
    }

    public static WifiAdminManager getManager() {
        if (mSharedManager == null) {
            mSharedManager = new WifiAdminManager();
        }

        return mSharedManager;
    }

    public interface WifiCallback {
        void networkStateChanged(WifiAdmin wifiAdmin);

        void wifiScanResult(WifiAdmin wifiAdmin);

        void onConnectError(WifiAdmin wifiAdmin);

        void onConnectDone(WifiAdmin wifiAdmin);
    }

    private void onNetworkStateChanged(WifiAdmin wifiAdmin) {

        Logger.t(TAG).d("networkStateChanged");
        for (WifiCallback callback : mWifiCallbackList) {
            callback.networkStateChanged(wifiAdmin);
        }
    }

    private void onWifiScanResult(WifiAdmin wifiAdmin) {

        Logger.t(TAG).d("wifiScanResult");

        for (WifiCallback callback : mWifiCallbackList) {
            callback.wifiScanResult(wifiAdmin);
        }
    }

    private void onConnectError(WifiAdmin wifiAdmin) {

        Logger.t(TAG).d("onConnectError");
        for (WifiCallback callback : mWifiCallbackList) {
            callback.onConnectError(wifiAdmin);
        }
    }

    private void onConnectDone(WifiAdmin wifiAdmin) {

        Logger.t(TAG).d("onConnectDone");
        for (WifiCallback callback : mWifiCallbackList) {
            callback.onConnectDone(wifiAdmin);
        }
    }


    public WifiAdmin attachWifiAdmin(WifiCallback callback) {

        Logger.t(TAG).d("attachWifiAdmin " + callback);
        mWifiCallbackList.add(callback);
        if (mWifiAdmin == null) {

            Logger.t(TAG).d("start WifiAdmin");
            mWifiAdmin = new WifiAdmin(mContext);
            mWifiAdmin.init();
            mWifiAdmin.setListener(new WifiAdmin.WifiAdminListener() {
                @Override
                public void networkStateChanged(WifiAdmin wifiAdmin) {
                    onNetworkStateChanged(wifiAdmin);
                }

                @Override
                public void wifiScanResult(WifiAdmin wifiAdmin) {
                    onWifiScanResult(wifiAdmin);
                }

                @Override
                public void ConnectError(WifiAdmin wifiAdmin) {
                    onConnectError(wifiAdmin);
                }

                @Override
                public void ConnectDone(WifiAdmin wifiAdmin) {
                    onConnectDone(wifiAdmin);
                }
            });
            mScanWifiAction = new Runnable() {
                @Override
                public void run() {
                    requestScan();
                }
            };
            requestScan();
        }
        return mWifiAdmin;
    }

    private void requestScan() {
        if (mWifiAdmin != null && mScanWifiAction != null) {
            mWifiAdmin.scan();
            if (mScanWifiAction != null) {
                mHandler.postDelayed(mScanWifiAction, WIFI_SCAN_INTERVAL);
            }
        }
    }

    public void detachWifiAdmin(WifiCallback callback, boolean bCancelConnect) {
        mWifiCallbackList.remove(callback);
        if (bCancelConnect) {
            mWifiAdmin.cancelConnect();
        }
        if (mWifiCallbackList.size() == 0) {
            Logger.t(TAG).d("stop WifiAdmin");
            if (mScanWifiAction != null) {
                mHandler.removeCallbacks(mScanWifiAction);
                mScanWifiAction = null;
            }
            mWifiAdmin.release();
            mWifiAdmin = null;
        }

        Logger.t(TAG).d("detachWifiAdmin " + callback);
    }


    public WifiAdmin getWifiAdmin() {
        return mWifiAdmin;
    }
}
