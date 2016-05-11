package com.waylens.hachi.hardware.vdtcamera;

import com.orhanobut.logger.Logger;

public class BtState {
    public static final String TAG = BtState.class.getSimpleName();
    public static final boolean DEBUG = false;


    // is bt supported
    public static final int BT_SUPPORT_UNKNOWN = -1;
    public static final int BT_SUPPORT_NO = 0;
    public static final int BT_SUPPORT_YES = 1;

    // is bt enabled
    public static final int BT_STATE_UNKNOWN = -1;
    public static final int BT_STATE_DISABLED = 0;
    public static final int BT_STATE_ENABLED = 1;


    public int mStateSN = 0;
    public boolean mbSchedule = false;

    public int mBtSupport = BT_SUPPORT_UNKNOWN;
    public int mBtState = BT_STATE_UNKNOWN;
    public boolean mbBtEnabling = false;
    public boolean mbBtScanning = false;

    public int mNumDevs = 0;

    static public class BtDevState {

    }


    public void setIsBTEnabled(int state) {
        if (mBtState != state) {
            Logger.t(TAG).d("setIsBTEnabled: " + state);
            mBtState = state;
        }
    }


    // default states when not available
//	public static final BtState nullState = new BtState();

    // sync our states to users


//	final static private boolean isBoundState(int state) {
//		return state == BTDEV_STATE_ON || state == BTDEV_STATE_BUSY || state == BTDEV_STATE_WAIT;
//	}
//
//	public boolean isDeviceBound(int type) {
//		if (mHidState.type == type)
//			return isBoundState(mHidState.mState);
//		if (mObdState.type == type)
//			return isBoundState(mObdState.mState);
//		return false;
//	}
//
//	public boolean isDeviceBound(int type, String mac) {
//		if (mHidState.type == type && mHidState.mMac.equals(mac))
//			return true;
//		if (mObdState.type == type && mObdState.mMac.equals(mac))
//			return true;
//		return false;
//	}
}
