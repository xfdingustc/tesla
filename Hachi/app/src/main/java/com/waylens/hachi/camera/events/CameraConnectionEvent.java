package com.waylens.hachi.camera.events;


import com.waylens.hachi.camera.VdtCamera;

/**
 * Created by Xiaofei on 2016/4/19.
 */
public class CameraConnectionEvent {
    private final int mWhat;
    private final VdtCamera mVdtCamera;


    public static final int VDT_CAMERA_CONNECTED = 0;
    public static final int VDT_CAMERA_CONNECTING = 1;
    public static final int VDT_CAMERA_DISCONNECTED = 2;
    public static final int VDT_CAMERA_SELECTED_CHANGED = 3;
    public static final int VDT_CAMERA_CONNECTING_FAILED = 4;


    public CameraConnectionEvent(int what, VdtCamera camera) {
        this.mWhat = what;
        this.mVdtCamera = camera;

    }


    public int getWhat() {
        return mWhat;
    }

    public VdtCamera getVdtCamera() {
        return mVdtCamera;
    }



}
