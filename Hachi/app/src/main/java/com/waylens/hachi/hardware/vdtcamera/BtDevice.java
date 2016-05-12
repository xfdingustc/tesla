package com.waylens.hachi.hardware.vdtcamera;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/5/11.
 */
public class BtDevice {
    private static final String TAG = BtDevice.class.getSimpleName();
    public static final int BT_DEVICE_STATE_UNKNOWN = -1;
    public static final int BT_DEVICE_STATE_OFF = 0;
    public static final int BT_DEVICE_STATE_ON = 1;
    public static final int BT_DEVICE_STATE_BUSY = 2;
    public static final int BT_DEVICE_STATE_WAIT = 3;

    public static final int BT_DEVICE_TYPE_OBD = 0;
    public static final int BT_DEVICE_TYPE_REMOTE_CTR = 1;

    public enum BtDeviceType {
        BT_DEVICE_TYPE_OBD,
        BT_DEVICE_TYPE_REMOTE_CTR
    }

    private int mState = BT_DEVICE_STATE_UNKNOWN;
    private String mMac = "";
    private String mName = "";

    private final BtDeviceType mType;
    private final String mTypeName;

    public BtDevice(BtDeviceType type) {
        this.mType = type;
        this.mTypeName = "";
    }

    public BtDevice(BtDeviceType type, String typeName) {
        this.mType = type;
        this.mTypeName = typeName;
    }


    public void setDevState(int state, String mac, String name) {
        if (mState != state || !mMac.equals(mac) || !mName.equals(name)) {
            Logger.t(TAG).d(TAG, "setDevState: " + mTypeName + ", " + state + ", " + mac + ", " + name);
            mState = state;
            mMac = mac;
            mName = name;
        }
    }

    public BtDeviceType getType() {
        return mType;
    }

    public int getState() {
        return mState;
    }

    public String getName() {
        return mName;
    }

    public String getMac() {
        return mMac;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
