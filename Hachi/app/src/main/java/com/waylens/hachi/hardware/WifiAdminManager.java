package com.waylens.hachi.hardware;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/8/12.
 */
public class WifiAdminManager {
    private static final String TAG = WifiAdminManager.class.getSimpleName();
    private static final boolean DEBUG = false;
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
    private void networkStateChanged(WifiAdmin wifiAdmin) {
        if (DEBUG) {
            Log.d(TAG, "networkStateChanged");
        }
        for (WifiCallback callback : mWifiCallbackList) {
            callback.networkStateChanged(wifiAdmin);
        }
    }

    private void wifiScanResult(WifiAdmin wifiAdmin) {
        if (DEBUG) {
            Log.d(TAG, "wifiScanResult");
        }
        for (WifiCallback callback : mWifiCallbackList) {
            callback.wifiScanResult(wifiAdmin);
        }
    }

    private void onConnectError(WifiAdmin wifiAdmin) {
        if (DEBUG) {
            Log.d(TAG, "onConnectError");
        }
        for (WifiCallback callback : mWifiCallbackList) {
            callback.onConnectError(wifiAdmin);
        }
    }

    private void onConnectDone(WifiAdmin wifiAdmin) {
        if (DEBUG) {
            Log.d(TAG, "onConnectDone");
        }
        for (WifiCallback callback : mWifiCallbackList) {
            callback.onConnectDone(wifiAdmin);
        }
    }

    class MyWifiAdmin extends WifiAdmin {

        public MyWifiAdmin(Context context) {
            super(context);
        }

        @Override
        public void networkStateChanged(WifiAdmin wifiAdmin) {
            if (wifiAdmin == mWifiAdmin) {
                WifiAdminManager.this.networkStateChanged(wifiAdmin);
            }
        }

        @Override
        public void wifiScanResult(WifiAdmin wifiAdmin) {
            if (wifiAdmin == mWifiAdmin) {
                WifiAdminManager.this.wifiScanResult(wifiAdmin);
            }
        }

        @Override
        public void onConnectError(WifiAdmin wifiAdmin) {
            if (wifiAdmin == mWifiAdmin) {
                WifiAdminManager.this.onConnectError(wifiAdmin);
            }
        }

        @Override
        public void onConnectDone(WifiAdmin wifiAdmin) {
            if (wifiAdmin == mWifiAdmin) {
                WifiAdminManager.this.onConnectDone(wifiAdmin);
            }
        }

    }

    public WifiAdmin attachWifiAdmin(WifiCallback callback) {
        if (DEBUG) {
            Log.d(TAG, "attachWifiAdmin " + callback);
        }
        mWifiCallbackList.add(callback);
        if (mWifiAdmin == null) {
            if (DEBUG) {
                Log.d(TAG, "start WifiAdmin");
            }
            mWifiAdmin = new MyWifiAdmin(mContext);
            mWifiAdmin.init();
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

    // API
    public void detachWifiAdmin(WifiCallback callback, boolean bCancelConnect) {
        mWifiCallbackList.remove(callback);
        if (bCancelConnect) {
            mWifiAdmin.cancelConnect();
        }
        if (mWifiCallbackList.size() == 0) {
            if (DEBUG) {
                Log.d(TAG, "stop WifiAdmin");
            }
            if (mScanWifiAction != null) {
                mHandler.removeCallbacks(mScanWifiAction);
                mScanWifiAction = null;
            }
            mWifiAdmin.release();
            mWifiAdmin = null;
        }
        if (DEBUG) {
            Log.d(TAG, "detachWifiAdmin " + callback);
        }
    }

    // API
    public WifiAdmin getWifiAdmin() {
        return mWifiAdmin;
    }
}
