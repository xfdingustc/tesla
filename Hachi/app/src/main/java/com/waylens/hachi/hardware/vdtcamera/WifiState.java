package com.waylens.hachi.hardware.vdtcamera;

import android.util.Log;

public class WifiState {

	public static final boolean DEBUG = false;
	public static final String TAG = "WifiState";



	public int mStateSN = 0;
	public boolean mbSchedule = false;



	// default states when not available
	public static final WifiState nullState = new WifiState();

	// sync our states to users
//	synchronized public boolean syncStates(WifiState user) {
//		if (mStateSN == user.mStateSN) {
//			if (DEBUG) {
//				Log.d(TAG, "-- syncStates: no change ---");
//			}
//			return false;
//		} else {
//			if (DEBUG) {
//				Log.d(TAG, "-- syncStates ---");
//			}
//		}
//
//		user.mStateSN = mStateSN;
//
//		user.mWifiMode = mWifiMode;
//		user.mNumWifiAP = mNumWifiAP;
//
//		return true;
//	}

	private final void stateChanged() {
		mStateSN++;
		mbSchedule = true;
	}



}
