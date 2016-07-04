package com.waylens.hachi.eventbus.events;

import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.vdb.ClipActionInfo;
import com.waylens.hachi.vdb.rawdata.RawDataItem;

/**
 * Created by laina on 16/7/4.
 */
public class MarkLiveMsgEvent {
    private final VdtCamera mVdtCamera;
    private final ClipActionInfo mClipActionInfo;

    public MarkLiveMsgEvent(VdtCamera camera, ClipActionInfo mClipActionInfo) {
        this.mVdtCamera = camera;
        this.mClipActionInfo = mClipActionInfo;
    }

    public VdtCamera getCamera() {
        return mVdtCamera;
    }

    public ClipActionInfo getClipActionInfo() {
        return mClipActionInfo;
    }
}
