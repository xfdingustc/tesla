package com.waylens.hachi.snipe;

import android.content.Context;

import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;

/**
 * Created by Xiaofei on 2015/8/17.
 */
public class Snipe {
    private static VdbConnection mVdbConnection;

    private static volatile VdbRequestQueue _REQUEST_QUEUE_SINGLETON;

    public static VdbRequestQueue newRequestQueue() {
        if (_REQUEST_QUEUE_SINGLETON == null) {
            synchronized (VdbRequestQueue.class) {
                if (_REQUEST_QUEUE_SINGLETON == null) {
                    VdbSocket vdbSocket = new BasicVdbSocket();
                    _REQUEST_QUEUE_SINGLETON = new VdbRequestQueue(vdbSocket);
                    _REQUEST_QUEUE_SINGLETON.start();
                }
            }
        }
        return _REQUEST_QUEUE_SINGLETON;
    }

    public static VdbRequestQueue newRequestQueue(Context context) {
        if (_REQUEST_QUEUE_SINGLETON == null) {
            synchronized (VdbRequestQueue.class) {
                if (_REQUEST_QUEUE_SINGLETON == null) {
                    VdbSocket vdbSocket = new BasicVdbSocket();
                    _REQUEST_QUEUE_SINGLETON = new VdbRequestQueue(vdbSocket);
                    _REQUEST_QUEUE_SINGLETON.start();
                }
            }
        }
        return _REQUEST_QUEUE_SINGLETON;
    }

    // TODO: We will add multi camera support later, we need implement different request queue for
    //       different vdt camera;
    public static VdbRequestQueue newRequestQueue(Context context, VdtCamera camera) {
        return newRequestQueue();
    }

    public static void init() {
        VdtCameraManager manager = VdtCameraManager.getManager();
        manager.addCallback(new VdtCameraManager.Callback() {
            @Override
            public void onCameraConnecting(VdtCamera vdtCamera) {

            }

            @Override
            public void onCameraConnected(VdtCamera vdtCamera) {

            }

            @Override
            public void onCameraVdbConnected(VdtCamera vdtCamera) {
                mVdbConnection = vdtCamera.getVdbConnection();
            }

            @Override
            public void onCameraDisconnected(VdtCamera vdtCamera) {

            }

            @Override
            public void onCameraStateChanged(VdtCamera vdtCamera) {

            }

            @Override
            public void onWifiListChanged() {

            }
        });
    }

    public static VdbConnection getVdbConnect() {
        return mVdbConnection;
    }

}
