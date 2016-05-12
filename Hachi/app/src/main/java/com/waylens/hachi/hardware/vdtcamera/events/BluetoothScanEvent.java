package com.waylens.hachi.hardware.vdtcamera.events;

import com.waylens.hachi.hardware.vdtcamera.BtDevice;

import java.util.List;

/**
 * Created by Xiaofei on 2016/5/12.
 */
public class BluetoothScanEvent {
    private final List<BtDevice> mBtDevices;

    public BluetoothScanEvent(List<BtDevice> btDevices) {
        this.mBtDevices = btDevices;
    }

    public List<BtDevice> getDevices() {
        return mBtDevices;
    }


}
