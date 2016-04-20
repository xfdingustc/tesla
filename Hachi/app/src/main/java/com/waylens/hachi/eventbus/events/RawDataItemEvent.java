package com.waylens.hachi.eventbus.events;

import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.vdb.rawdata.RawDataItem;

/**
 * Created by Xiaofei on 2016/4/20.
 */
public class RawDataItemEvent {
    private final VdtCamera mVdtCamera;
    private final RawDataItem mItem;

    public RawDataItemEvent(VdtCamera camera, RawDataItem item) {
        this.mVdtCamera = camera;
        this.mItem = item;
    }

    public VdtCamera getCamera() {
        return mVdtCamera;
    }

    public RawDataItem getRawDataItem() {
        return mItem;
    }
}
