package com.waylens.hachi.hardware.vdtcamera;

import android.util.Log;

public class BtState {

	public static final boolean DEBUG = false;
	public static final String TAG = "BtState";

	// is bt supported
	public static final int BT_SUPPORT_UNKNOWN = -1;
	public static final int BT_SUPPORT_NO = 0;
	public static final int BT_SUPPORT_YES = 1;

	// is bt enabled
	public static final int BT_STATE_UNKNOWN = -1;
	public static final int BT_STATE_DISABLED = 0;
	public static final int BT_STATE_ENABLED = 1;

	// bt types
	public static final int BT_TYPE_OBD = 0;
	public static final int BT_TYPE_HID = 1;

	// bt device state
	public static final int BTDEV_STATE_UNKNOWN = -1;
	public static final int BTDEV_STATE_OFF = 0;
	public static final int BTDEV_STATE_ON = 1;
	public static final int BTDEV_STATE_BUSY = 2;
	public static final int BTDEV_STATE_WAIT = 3;

	public int mStateSN = 0;
	public boolean mbSchedule = false;

	public int mBtSupport = BT_SUPPORT_UNKNOWN;
	public int mBtState = BT_STATE_UNKNOWN;
	public boolean mbBtEnabling = false;
	public boolean mbBtScanning = false;

	public int mNumDevs = 0;

	static public class BtDevState {
		public int mState = BTDEV_STATE_UNKNOWN;
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

	public BtDevState mObdState = new BtDevState(BT_TYPE_OBD, "OBD");
	public BtDevState mHidState = new BtDevState(BT_TYPE_HID, "HID");

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
		mBtState = bEnable ? BT_STATE_ENABLED : BT_STATE_DISABLED;
		mbBtEnabling = true;
	}

	// called from UI thread
	public boolean canEnableBt() {
		return mBtState != BT_STATE_UNKNOWN && !mbBtEnabling && !mbBtScanning;
	}

	// called from UI thread
	public void scanBt() {
		mbBtScanning = true;
	}

	// called from UI thread
	public boolean canOperate() {
		return mBtState != BT_STATE_UNKNOWN && !mbBtEnabling && !mbBtScanning;
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
		return state == BTDEV_STATE_ON || state == BTDEV_STATE_BUSY || state == BTDEV_STATE_WAIT;
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
