package com.waylens.hachi.camera.events;



import com.waylens.hachi.camera.VdtCamera;
import com.xfdingustc.snipe.vdb.ClipActionInfo;


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
