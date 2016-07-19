package com.waylens.hachi.eventbus.events;

import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.library.vdb.rawdata.RawDataItem;


import java.util.List;

/**
 * Created by Xiaofei on 2016/4/20.
 */
public class RawDataItemEvent {
    private final VdtCamera mVdtCamera;
    private final List<RawDataItem> mItemList;

    public RawDataItemEvent(VdtCamera camera, List<RawDataItem> item) {
        this.mVdtCamera = camera;
        this.mItemList = item;
    }

    public VdtCamera getCamera() {
        return mVdtCamera;
    }

    public List<RawDataItem> getRawDataItemList() {
        return mItemList;
    }
}
