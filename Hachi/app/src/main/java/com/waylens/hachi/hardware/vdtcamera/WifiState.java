package com.waylens.hachi.hardware.vdtcamera;

import android.util.Log;

public class WifiState {

	public static final boolean DEBUG = false;
	public static final String TAG = "WifiState";

	public static final int WIFI_MODE_UNKNOWN = -1;
	public static final int WIFI_MODE_AP = 0;
	public static final int WIFI_MODE_CLIENT = 1;
	public static final int WIFI_MODE_OFF = 2; //

	public int mStateSN = 0;
	public boolean mbSchedule = false;

	public int mWifiMode = WIFI_MODE_UNKNOWN;
	public int mNumWifiAP = 0;

	// default states when not available
	public static final WifiState nullState = new WifiState();

	// sync our states to users
	synchronized public boolean syncStates(WifiState user) {
		if (mStateSN == user.mStateSN) {
			if (DEBUG) {
				Log.d(TAG, "-- syncStates: no change ---");
			}
			return false;
		} else {
			if (DEBUG) {
				Log.d(TAG, "-- syncStates ---");
			}
		}

		user.mStateSN = mStateSN;

		user.mWifiMode = mWifiMode;
		user.mNumWifiAP = mNumWifiAP;

		return true;
	}

	private final void stateChanged() {
		mStateSN++;
		mbSchedule = true;
	}

	synchronized public void setWifiMode(int mode) {
		if (mWifiMode != mode) {
			if (DEBUG) {
				Log.d(TAG, "setWifiMode: " + mode);
			}
			mWifiMode = mode;
			stateChanged();
		}
	}

	synchronized public void setNumWifiAP(int num) {
		if (mNumWifiAP != num) {
			if (DEBUG) {
				Log.d(TAG, "setNumWifiAP: " + num);
			}
			mNumWifiAP = num;
			stateChanged();
		}
	}

}
