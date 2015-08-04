package com.transee.common;

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
import android.util.Log;

import java.util.List;

// wifi broadcast:
//	WIFI_STATE_CHANGED_ACTION
//	WIFI_AP_STATE_CHANGED_ACTION
//	SUPPLICANT_CONNECTION_CHANGE_ACTION
//	NETWORK_STATE_CHANGED_ACTION
//	SUPPLICANT_STATE_CHANGED_ACTION
//	CONFIGURED_NETWORKS_CHANGED_ACTION
//	SCAN_RESULTS_AVAILABLE_ACTION
//	BATCHED_SCAN_RESULTS_AVAILABLE_ACTION
//	RSSI_CHANGED_ACTION
//	LINK_CONFIGURATION_CHANGED_ACTION
//	NETWORK_IDS_CHANGED_ACTION

// this class provides 3 services:
//	track and keep wifi state
//	connect to specified AP
//	maintain wifi list

abstract public class WifiAdmin {

	static final boolean DEBUG = false;
	static final String TAG = "WifiAdmin";

	static final int STATE_IDLE = 0;
	static final int STATE_CONNECTING = 1;

	private final Context mContext;
	private final WifiManager mWifiManager;
	private final ConnectivityManager mConnectivityManager;
	private MyBroadcastReceiver mReceiver;
	private String mCurrSSID = "";

	// wifi state
	private NetworkInfo mNetworkInfo;

	// wifi-list
	private List<ScanResult> mScanResult;

	// connection
	private String mTargetSSID;
	private String mTargetPassword;
	private int mState = STATE_IDLE;
	private WifiConfiguration mWifiConfig; // connecting if != null

	abstract public void networkStateChanged(WifiAdmin wifiAdmin);

	abstract public void wifiScanResult(WifiAdmin wifiAdmin);

	abstract public void onConnectError(WifiAdmin wifiAdmin);

	abstract public void onConnectDone(WifiAdmin wifiAdmin);

	public WifiAdmin(Context context) {
		mContext = context;
		mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		mConnectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	// API
	public void init() {
		mNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		mScanResult = mWifiManager.getScanResults();
		updateWifiSSID(); // init wifi SSID

		if (DEBUG) {
			Log.d(TAG, "startTrackWifiState");
		}

		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		// filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

		mReceiver = new MyBroadcastReceiver();
		mContext.registerReceiver(mReceiver, filter);
	}

	// API
	public void release() {
		if (mReceiver != null) {
			if (DEBUG) {
				Log.d(TAG, "stopTrackWifiState");
			}
			mContext.unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}

	// API
	public WifiManager getWifiManager() {
		return mWifiManager;
	}

	// API
	public ConnectivityManager getConnectivityManager() {
		return mConnectivityManager;
	}

	// API
	public List<ScanResult> getScanResult() {
		return mScanResult;
	}

	// API
	public NetworkInfo getNetworkInfo() {
		return mNetworkInfo;
	}

	// API
	public String getCurrSSID() {
		return mCurrSSID;
	}

	// API
	public String getTargetSSID() {
		return mTargetSSID;
	}

	// API
	public void scan() {
		if (DEBUG) {
			Log.d(TAG, "call scan()");
		}
		mWifiManager.startScan();
	}

	// API
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

		if (DEBUG) {
			Log.d(TAG, "=== connecTo(" + ssid + ") ===");
		}

		mState = STATE_CONNECTING;
		startTargetWifi();
	}

	// API
	public void cancelConnect() {
		mState = STATE_IDLE;
		mWifiConfig = null;
	}

	// API
	public boolean isConnecting() {
		return mState == STATE_CONNECTING;
	}

	// check to call addNetwork()/enableNetwork()
	private void startTargetWifi() {
		if (!mNetworkInfo.isAvailable()) {
			if (DEBUG) {
				Log.d(TAG, "call setWifiEnabled");
			}
			if (!mWifiManager.setWifiEnabled(true)) {
				Log.w(TAG, "setWifiEnabled failed");
			}
		} else {
			NetworkInfo.State state = mNetworkInfo.getState();
			switch (state) {
			case CONNECTING:
			case CONNECTED:
				if (!mCurrSSID.equals(mTargetSSID)) {
					if (DEBUG) {
						Log.d(TAG, "connecting/connected to " + mCurrSSID + ", not target " + mTargetSSID);
						Log.d(TAG, "call disconnect()");
					}
					mWifiManager.disconnect();
				} else {
					if (DEBUG) {
						Log.d(TAG, "connecting/connected to target " + mTargetSSID);
					}
					mWifiConfig = null; // no further action
					if (state == NetworkInfo.State.CONNECTED) {
						mState = STATE_IDLE;
						onConnectDone(this);
					}
				}
				break;
			case DISCONNECTED:
				if (mWifiConfig != null) {
					int netId = addTargetNetwork(mWifiConfig);
					if (DEBUG) {
						Log.d(TAG, "Wifi is enabled and disconnected");
						Log.d(TAG, "addNetwork, netID: " + netId);
						Log.d(TAG, "enableNetwork " + mWifiConfig.SSID);
					}
					if (netId >= 0 && mWifiManager.enableNetwork(netId, true)) {
						if (DEBUG) {
							Log.d(TAG, "enableNetwork OK");
						}
						mWifiConfig = null; // no further action
					} else {
						// TODO
					}
				} else {
					if (DEBUG) {
						Log.d(TAG, "disconnected state, currSSID: " + mCurrSSID);
					}
					if (mCurrSSID.equals(mTargetSSID)) {
						mState = STATE_IDLE;
						onConnectError(this);
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
					if (DEBUG) {
						Log.d(TAG, "updateNetwork: " + mTargetSSID);
					}
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
			if (DEBUG) {
				Log.d(TAG, "SSID: " + mCurrSSID);
			}
		}
	}

	private void networkStateChanged(NetworkInfo networkInfo) {
		mNetworkInfo = networkInfo;
		if (mState == STATE_CONNECTING) {
			startTargetWifi();
		}
		networkStateChanged(this);
	}

	private void onWifiScanResult(Intent intent) {
		mScanResult = mWifiManager.getScanResults();
		wifiScanResult(this);
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

	class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (DEBUG) {
				Log.d(TAG, "=== receive " + action);
			}

			if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
				int prevState = intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE,
						WifiManager.WIFI_STATE_UNKNOWN);
				if (DEBUG) {
					Log.d(TAG, "WIFI_STATE_CHANGED_ACTION: " + getWifiStateName(state) + ", prev: "
							+ getWifiStateName(prevState));
				}
				// TODO
				return;
			}

			if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
				android.net.wifi.SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
				int supplicantError = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0);
				if (DEBUG) {
					Log.d(TAG, "SUPPLICANT_STATE_CHANGED_ACTION: newState=" + state + ", supplicantError="
							+ supplicantError);
				}
				return;
			}

			if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				if (DEBUG) {
					Log.d(TAG, "SCAN_RESULTS_AVAILABLE_ACTION");
				}
				onWifiScanResult(intent);
				return;
			}

			if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				NetworkInfo networkInfo = (NetworkInfo)intent.getExtras().get(WifiManager.EXTRA_NETWORK_INFO);
				if (DEBUG) {
					Log.d(TAG, "NETWORK_STATE_CHANGED_ACTION: " + networkInfo.toString());
				}
				if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
					updateWifiSSID();
					networkStateChanged(networkInfo);
				}
				return;
			}
		}
	}
}
