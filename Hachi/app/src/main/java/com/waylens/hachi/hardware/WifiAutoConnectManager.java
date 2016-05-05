package com.waylens.hachi.hardware;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;


import java.util.List;

/**
 * Created by Xiaofei on 2016/3/14.
 */
public class WifiAutoConnectManager {
    private static final String TAG = WifiAutoConnectManager.class.getSimpleName();

    private WifiManager wifiManager;

    private WifiAutoConnectListener mListener;

    public enum WifiCipherType {
        WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
    }

    public WifiAutoConnectManager(WifiManager wifiManager, WifiAutoConnectListener listener) {
        this.wifiManager = wifiManager;
        this.mListener = listener;
    }

    public void connect(String ssid, String password, WifiCipherType type) {
        Thread thread = new Thread(new ConnectRunnable(ssid, password, type));
        thread.start();
    }

    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    private WifiConfiguration createWifiInfo(String SSID, String Password, WifiCipherType Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        if (Type == WifiCipherType.WIFICIPHER_NOPASS) {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == WifiCipherType.WIFICIPHER_WEP) {
            if (!TextUtils.isEmpty(Password)) {
                if (isHexWepKey(Password)) {
                    config.wepKeys[0] = Password;
                } else {
                    config.wepKeys[0] = "\"" + Password + "\"";
                }
            }
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == WifiCipherType.WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);

            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    private boolean openWifi() {
        boolean bRet = true;
        if (!wifiManager.isWifiEnabled()) {
            bRet = wifiManager.setWifiEnabled(true);
        }
        return bRet;
    }

    class ConnectRunnable implements Runnable {
        private String ssid;

        private String password;

        private WifiCipherType type;

        public ConnectRunnable(String ssid, String password, WifiCipherType type) {
            this.ssid = ssid;
            this.password = password;
            this.type = type;
        }

        @Override
        public void run() {
            boolean ret = openWifi();
            Log.d(TAG, "open wifi: " + ret);

            while (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
            }
            Log.d(TAG, "wifi is openned");

            WifiConfiguration wifiConfig = createWifiInfo(ssid, password, type);
//
            if (wifiConfig == null) {
                Log.d(TAG, "wifiConfig is null!");
                return;
            }

            Log.d(TAG, "WifiInfo is created");

            WifiConfiguration tempConfig = isExsits(ssid);

            int netID;
            if (tempConfig == null) {

                netID = wifiManager.addNetwork(wifiConfig);
            } else {
                netID = tempConfig.networkId;
            }

            Log.d(TAG, "current network info: " + wifiManager.getConnectionInfo().toString());
            wifiManager.disableNetwork(wifiManager.getConnectionInfo().getNetworkId());


            Log.d(TAG, "add network " + wifiConfig.toString());

//            wifiManager.getConfiguredNetworks()

            boolean enabled = wifiManager.enableNetwork(netID, true);

            Log.d(TAG, "enableNetwork status enable=" + enabled);
            boolean connected = wifiManager.reconnect();
            Log.d(TAG, "enableNetwork connected=" + connected);
            Log.d(TAG, "current network info: " + wifiManager.getConnectionInfo().toString());
            if (mListener != null) {
                mListener.onAutoConnectStarted();
            }

        }
    }

    private static boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();


        if (len != 10 && len != 26 && len != 58) {
            return false;
        }

        return isHex(wepKey);
    }

    private static boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }

        return true;
    }

    public interface WifiAutoConnectListener {
        void onAutoConnectStarted();
    }
}
