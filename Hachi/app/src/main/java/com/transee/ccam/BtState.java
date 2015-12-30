package com.transee.ccam;

import android.util.Log;

public class BtState {

	public static final boolean DEBUG = false;
	public static final String TAG = "BtState";

	// is bt supported
	public static final int BT_Support_Unknown = -1;
	public static final int BT_Support_No = 0;
	public static final int BT_Support_Yes = 1;

	// is bt enabled
	public static final int BT_State_Unknown = -1;
	public static final int BT_State_Disabled = 0;
	public static final int BT_State_Enabled = 1;

	// bt types
	public static final int BT_Type_OBD = 0;
	public static final int BT_Type_HID = 1;

	// bt device state
	public static final int BTDEV_State_Unknown = -1;
	public static final int BTDEV_State_Off = 0;
	public static final int BTDEV_State_On = 1;
	public static final int BTDEV_State_Busy = 2;
	public static final int BTDEV_State_Wait = 3;

	public int mStateSN = 0;
	public boolean mbSchedule = false;

	public int mBtSupport = BT_Support_Unknown;
	public int mBtState = BT_State_Unknown;
	public boolean mbBtEnabling = false;
	public boolean mbBtScanning = false;

	public int mNumDevs = 0;

	static public class BtDevState {
		public int mState = BTDEV_State_Unknown;
		public String mMac = "";
		public String mName = "";

		final public int type;
		final public String typeName;

		public BtDevState(int type, String typeName) {
			this.type = type;
			this.typeName = typeName;
		}

		public void assignTo(BtDevState other) {
			other.mState = mState;
			other.mMac = mMac;
			other.mName = mName;
		}
	}

	public BtDevState mObdState = new BtDevState(BT_Type_OBD, "OBD");
	public BtDevState mHidState = new BtDevState(BT_Type_HID, "HID");

	// default states when not available
	public static final BtState nullState = new BtState();

	// sync our states to users
	synchronized public boolean syncStates(BtState user) {
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

		user.mBtSupport = mBtSupport;
		user.mBtState = mBtState;
		user.mbBtEnabling = mbBtEnabling;
		user.mbBtScanning = mbBtScanning;

		user.mNumDevs = mNumDevs;

		mObdState.assignTo(user.mObdState);
		mHidState.assignTo(user.mHidState);

		return true;
	}

	private void stateChanged() {
		mStateSN++;
		mbSchedule = true;
	}

	synchronized public void setIsBTSupported(int btSupport) {
		if (mBtSupport != btSupport) {
			if (DEBUG) {
				Log.d(TAG, "setIsBTSupported: " + btSupport);
			}
			mBtSupport = btSupport;
			stateChanged();
		}
	}

	synchronized public void setIsBTEnabled(int state) {
		if (mBtState != state) {
			if (DEBUG) {
				Log.d(TAG, "setIsBTEnabled: " + state);
			}
			mBtState = state;
			mbBtEnabling = false;
			stateChanged();
		}
	}

	synchronized public void scanBtDone() {
		if (DEBUG) {
			Log.d(TAG, "scanBtDone");
		}
		mbBtScanning = false;
		stateChanged();
	}

	// called from UI thread
	public void enableBt(boolean bEnable) {
		mBtState = bEnable ? BT_State_Enabled : BT_State_Disabled;
		mbBtEnabling = true;
	}

	// called from UI thread
	public boolean canEnableBt() {
		return mBtState != BT_State_Unknown && !mbBtEnabling && !mbBtScanning;
	}

	// called from UI thread
	public void scanBt() {
		mbBtScanning = true;
	}

	// called from UI thread
	public boolean canOperate() {
		return mBtState != BT_State_Unknown && !mbBtEnabling && !mbBtScanning;
	}

	synchronized public void setNumDevs(int numDevs) {
		if (mNumDevs != numDevs) {
			if (DEBUG) {
				Log.d(TAG, "setNumDevs: " + numDevs);
			}
			mNumDevs = numDevs;
			stateChanged();
		}
	}

	synchronized public void setDevState(int devType, int state, String mac, String name) {
		BtDevState devState;

		if (devType == mObdState.type)
			devState = mObdState;
		else if (devType == mHidState.type)
			devState = mHidState;
		else
			return;

		if (devState.mState != state || !devState.mMac.equals(mac) || !devState.mName.equals(name)) {
			if (DEBUG) {
				Log.d(TAG, "setDevState: " + devState.typeName + ", " + state + ", " + mac + ", " + name);
			}
			devState.mState = state;
			devState.mMac = mac;
			devState.mName = name;
			stateChanged();
		}
	}

	final static private boolean isBoundState(int state) {
		return state == BTDEV_State_On || state == BTDEV_State_Busy || state == BTDEV_State_Wait;
	}

	public boolean isDeviceBound(int type) {
		if (mHidState.type == type)
			return isBoundState(mHidState.mState);
		if (mObdState.type == type)
			return isBoundState(mObdState.mState);
		return false;
	}

	public boolean isDeviceBound(int type, String mac) {
		if (mHidState.type == type && mHidState.mMac.equals(mac))
			return true;
		if (mObdState.type == type && mObdState.mMac.equals(mac))
			return true;
		return false;
	}
}
