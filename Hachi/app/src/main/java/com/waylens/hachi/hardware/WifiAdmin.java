package com.waylens.hachi.hardware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.orhanobut.logger.Logger;
import com.transee.common.Utils;

import java.util.List;


public class WifiAdmin {
    private static final String TAG = WifiAdmin.class.getSimpleName();

    private static final int STATE_IDLE = 0;
    private static final int STATE_CONNECTING = 1;

    private int mState = STATE_IDLE;

    private final Context mContext;
    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;
    private WifiStatusBroadcastReceiver mReceiver;
    private String mCurrSSID = "";


    private NetworkInfo mNetworkInfo;

    private List<ScanResult> mScanResult;

    // connection
    private String mTargetSSID;
    private String mTargetPassword;

    private WifiConfiguration mWifiConfig; // connecting if != null
    private WifiAdminListener mListener = null;

    public interface WifiAdminListener {
        void networkStateChanged(WifiAdmin wifiAdmin);

        void wifiScanResult(WifiAdmin wifiAdmin);

        void ConnectError(WifiAdmin wifiAdmin);

        void ConnectDone(WifiAdmin wifiAdmin);
    }


    public void setListener(WifiAdminListener listener) {
        this.mListener = listener;
    }


    public WifiAdmin(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    // API
    public void init() {
        mNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        mScanResult = mWifiManager.getScanResults();
        updateWifiSSID(); // init wifi SSID


        Logger.t(TAG).d("startTrackWifiState");

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        // filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        mReceiver = new WifiStatusBroadcastReceiver();
        mContext.registerReceiver(mReceiver, filter);
    }

    // API
    public void release() {
        if (mReceiver != null) {
            Logger.t(TAG).d("stopTrackWifiState");
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    public WifiManager getWifiManager() {
        return mWifiManager;
    }


    public List<ScanResult> getScanResult() {
        return mScanResult;
    }

    public NetworkInfo getNetworkInfo() {
        return mNetworkInfo;
    }

    public String getCurrSSID() {
        return mCurrSSID;
    }

    public String getTargetSSID() {
        return mTargetSSID;
    }


    public void scan() {
        Logger.t(TAG).d("call scan()");
        mWifiManager.startScan();
    }

    public void connectTo(String ssid, String password) {
        cancelConnect();

        mTargetSSID = ssid;
        mTargetPassword = password;

        mWifiConfig = new WifiConfiguration();
        mWifiConfig.allowedAuthAlgorithms.clear();
        mWifiConfig.allowedGroupCiphers.clear();
        mWifiConfig.allowedKeyManagement.clear();
        mWifiConfig.allowedPairwiseCiphers.clear();
        mWifiConfig.allowedProtocols.clear();

        mWifiConfig.SSID = "\"" + mTargetSSID + "\"";
        mWifiConfig.preSharedKey = "\"" + mTargetPassword + "\"";
        mWifiConfig.hiddenSSID = true;
        mWifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        mWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        mWifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        mWifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        mWifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        mWifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        mWifiConfig.status = WifiConfiguration.Status.ENABLED;


        Logger.t(TAG).d("=== connecTo(" + ssid + ") ===");

        mState = STATE_CONNECTING;
        startTargetWifi();
    }


    public void cancelConnect() {
        mState = STATE_IDLE;
        mWifiConfig = null;
    }

    public boolean isConnecting() {
        return mState == STATE_CONNECTING;
    }

    // check to call addNetwork()/enableNetwork()
    private void startTargetWifi() {
        if (!mNetworkInfo.isAvailable()) {

            Logger.t(TAG).d("call setWifiEnabled");
            if (!mWifiManager.setWifiEnabled(true)) {
                Logger.t(TAG).w("setWifiEnabled failed");
            }
        } else {
            NetworkInfo.State state = mNetworkInfo.getState();
            switch (state) {
                case CONNECTING:
                case CONNECTED:
                    if (!mCurrSSID.equals(mTargetSSID)) {

                        Logger.t(TAG).d("connecting/connected to " + mCurrSSID + ", not target " + mTargetSSID);
                        Logger.t(TAG).d("call disconnect()");
                        mWifiManager.disconnect();
                    } else {
                        Logger.t(TAG).d("connecting/connected to target " + mTargetSSID);
                        mWifiConfig = null; // no further action
                        if (state == NetworkInfo.State.CONNECTED) {
                            mState = STATE_IDLE;
                            if (mListener != null) {
                                mListener.ConnectDone(this);
                            }
                        }
                    }
                    break;
                case DISCONNECTED:
                    if (mWifiConfig != null) {
                        int netId = addTargetNetwork(mWifiConfig);
                        Logger.t(TAG).d("Wifi is enabled and disconnected");
                        Logger.t(TAG).d("addNetwork, netID: " + netId);
                        Logger.t(TAG).d("enableNetwork " + mWifiConfig.SSID);
                        if (netId >= 0 && mWifiManager.enableNetwork(netId, true)) {

                            Logger.t(TAG).d("enableNetwork OK");
                            mWifiConfig = null; // no further action
                        }
                    } else {
                        Logger.t(TAG).d("disconnected state, currSSID: " + mCurrSSID);
                        if (mCurrSSID.equals(mTargetSSID)) {
                            mState = STATE_IDLE;
                            if (mListener != null) {
                                mListener.ConnectError(this);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private int addTargetNetwork(WifiConfiguration wifiConfig) {
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                String ssid = Utils.normalizeNetworkName(config.SSID);
                if (ssid.equals(mTargetSSID)) {
                    Logger.t(TAG).d("updateNetwork: " + mTargetSSID);
                    wifiConfig.networkId = config.networkId;
                    return mWifiManager.updateNetwork(wifiConfig);
                }
            }
        }
        return mWifiManager.addNetwork(wifiConfig);
    }

    private void updateWifiSSID() {
        WifiInfo info = mWifiManager.getConnectionInfo();
        if (info == null || info.getNetworkId() < 0) {
            mCurrSSID = "";
        } else {
            mCurrSSID = info.getSSID();
            mCurrSSID = Utils.normalizeNetworkName(mCurrSSID);
            if (mCurrSSID == null)
                mCurrSSID = "";

            Logger.t(TAG).d("SSID: " + mCurrSSID);
        }
    }

    private void networkStateChanged(NetworkInfo networkInfo) {
        mNetworkInfo = networkInfo;
        if (mState == STATE_CONNECTING) {
            startTargetWifi();
        }
        if (mListener != null) {
            mListener.networkStateChanged(this);
        }
    }

    private void onWifiScanResult(Intent intent) {
        mScanResult = mWifiManager.getScanResults();
        if (mListener != null) {
            mListener.wifiScanResult(this);
        }
    }

    private static String getWifiStateName(int state) {
        switch (state) {
            default:
            case WifiManager.WIFI_STATE_UNKNOWN:
                return "unknown";
            case WifiManager.WIFI_STATE_DISABLED:
                return "disabled";
            case WifiManager.WIFI_STATE_DISABLING:
                return "disabling";
            case WifiManager.WIFI_STATE_ENABLED:
                return "enabled";
            case WifiManager.WIFI_STATE_ENABLING:
                return "enabling";
        }
    }

    class WifiStatusBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Logger.t(TAG).d("=== receive " + action);

            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                int prevState = intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN);

                Logger.t(TAG).d("WIFI_STATE_CHANGED_ACTION: " + getWifiStateName(state) + ", prev: "
                    + getWifiStateName(prevState));
                return;
            }

            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                android.net.wifi.SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                int supplicantError = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0);

                Logger.t(TAG).d("SUPPLICANT_STATE_CHANGED_ACTION: newState=" + state + ", supplicantError="
                    + supplicantError);
                return;
            }

            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {

                Logger.t(TAG).d("SCAN_RESULTS_AVAILABLE_ACTION");
                onWifiScanResult(intent);
                return;
            }

            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getExtras().get(WifiManager.EXTRA_NETWORK_INFO);

                Logger.t(TAG).d("NETWORK_STATE_CHANGED_ACTION: " + networkInfo.toString());
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    updateWifiSSID();
                    networkStateChanged(networkInfo);
                }
                return;
            }
        }
    }
}
