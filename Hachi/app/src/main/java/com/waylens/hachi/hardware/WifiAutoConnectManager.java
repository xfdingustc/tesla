package com.waylens.hachi.hardware;

import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.camera.connectivity.VdtCameraConnectivityManager;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/3/14.
 */
public class WifiAutoConnectManager {
    private static final String TAG = WifiAutoConnectManager.class.getSimpleName();

    private static final int STATUS_OPEN_WIFI = 0x100;
    private static final int STATUS_SCAN_WIFI = 0x101;
    private static final int STATUS_WIFI_OPENNED = 0x102;
    private static final int STATUS_DISABLE_NETWORK = 0x103;
    private static final int STATUS_RECONNECT_TO_NETWORK = 0x104;


    private WifiManager mWifiManager;

    private WifiAutoConnectListener mListener;

    public enum WifiCipherType {
        WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
    }

    public WifiAutoConnectManager(WifiManager wifiManager, WifiAutoConnectListener listener) {
        this.mWifiManager = wifiManager;
        this.mListener = listener;
    }

    public void connect(final String ssid, final String password, final WifiCipherType type) {
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                boolean ret = openWifi();
                VdtCameraConnectivityManager.getManager().stopSearchCamera();
                subscriber.onNext(STATUS_OPEN_WIFI);

                while (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                    }
                }
                subscriber.onNext(STATUS_WIFI_OPENNED);

                mWifiManager.startScan();
                subscriber.onNext(STATUS_SCAN_WIFI);

                WifiConfiguration wifiConfig = createWifiInfo(ssid, password, type);
//
                if (wifiConfig == null) {
                    subscriber.onError(new IllegalStateException("Wifi config is null!"));

                } else {
                    Logger.t(TAG).d("WifiInfo is created");


                    Logger.t(TAG).d("current network info: " + mWifiManager.getConnectionInfo().toString());
                    mWifiManager.disableNetwork(mWifiManager.getConnectionInfo().getNetworkId());
                    mWifiManager.removeNetwork(mWifiManager.getConnectionInfo().getNetworkId());
                    subscriber.onNext(STATUS_DISABLE_NETWORK);

                    mWifiManager.disconnect();

                    WifiConfiguration tempConfig = isExsits(ssid);

                    int netID;
                    if (tempConfig == null) {
                        netID = mWifiManager.addNetwork(wifiConfig);
                    } else {
                        netID = tempConfig.networkId;
                    }


                    Logger.t(TAG).d("add network " + wifiConfig.toString());

                    boolean enabled = mWifiManager.enableNetwork(netID, true);
                    subscriber.onNext(STATUS_RECONNECT_TO_NETWORK);

                    Logger.t(TAG).d("enableNetwork status enable=" + enabled);

                    mWifiManager.saveConfiguration();
//                    mWifiManager.disconnect();
                    boolean connected = mWifiManager.reconnect();
                    Logger.t(TAG).d("enableNetwork connected=" + connected);
                    Logger.t(TAG).d("current network info: " + mWifiManager.getConnectionInfo().toString());
                    subscriber.onCompleted();
                }

            }
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Integer>() {
                @Override
                public void onCompleted() {
                    if (mListener != null) {
                        mListener.onAutoConnectStarted();
                    }
                    VdtCameraConnectivityManager.getManager().startSearchCamera();
                }

                @Override
                public void onError(Throwable e) {
                    Logger.t(TAG).e(e.toString());
                    if (mListener != null) {
                        mListener.onAutoConnectError(e.getMessage());
                    }
                    VdtCameraConnectivityManager.getManager().startSearchCamera();
                }

                @Override
                public void onNext(Integer integer) {
                    sendConnectStatus2Listener(integer);
                }
            });
    }

    private void sendConnectStatus2Listener(Integer status) {
        if (mListener == null) {
            return;
        }
        String statusMsg = null;
        Resources resources = Hachi.getContext().getResources();
        switch (status) {
            case STATUS_OPEN_WIFI:
                statusMsg = resources.getString(R.string.wifi_status_open_wifi);
                break;
            case STATUS_SCAN_WIFI:
                statusMsg = resources.getString(R.string.wifi_status_scan_wifi);
                break;
            case STATUS_WIFI_OPENNED:
                statusMsg = resources.getString(R.string.wifi_status_wifi_openned);
                break;
            case STATUS_DISABLE_NETWORK:
                statusMsg = resources.getString(R.string.wifi_status_disable_network);
                break;
            case STATUS_RECONNECT_TO_NETWORK:
                statusMsg = resources.getString(R.string.wifi_status_reconnect_to_network);
                break;

        }

        mListener.onAutoConnectStatus(statusMsg);
    }

    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
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
        if (!mWifiManager.isWifiEnabled()) {
            bRet = mWifiManager.setWifiEnabled(true);
        }
        return bRet;
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

        void onAutoConnectError(String errorMsg);

        void onAutoConnectStatus(String status);
    }
}
