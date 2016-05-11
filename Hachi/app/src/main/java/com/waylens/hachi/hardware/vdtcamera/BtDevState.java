package com.waylens.hachi.hardware.vdtcamera;

import android.util.Log;

import com.orhanobut.logger.Logger;

/**
 * Created by Xiaofei on 2016/5/11.
 */
public class BtDevState {
    private static final String TAG = BtDevState.class.getSimpleName();
    public static final int BT_DEVICE_STATE_UNKNOWN = -1;
    public static final int BT_DEVICE_STATE_OFF = 0;
    public static final int BT_DEVICE_STATE_ON = 1;
    public static final int BT_DEVICE_STATE_BUSY = 2;
    public static final int BT_DEVICE_STATE_WAIT = 3;

    public static final int BT_DEVICE_TYPE_OBD = 0;
    public static final int BT_DEVICE_TYPE_REMOTE_CTR = 1;

    private int mState = BT_DEVICE_STATE_UNKNOWN;
    private String mMac = "";
    private String mName = "";

    final public int type;
    final public String typeName;

    public BtDevState(int type, String typeName) {
        this.type = type;
        this.typeName = typeName;
    }


    public void setDevState(int state, String mac, String name) {
        if (mState != state || !mMac.equals(mac) || !mName.equals(name)) {
            Logger.t(TAG).d(TAG, "setDevState: " + typeName + ", " + state + ", " + mac + ", " + name);
            mState = state;
            mMac = mac;
            mName = name;
        }
    }

    public int getState() {
        return mState;
    }
}
