package com.waylens.hachi.snipe;

import android.content.Context;

import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.hardware.VdtCameraManager;

/**
 * Created by Xiaofei on 2015/8/17.
 */
public class Snipe {
    private static VdbConnection mVdbConnection;
    public static VdbRequestQueue newRequestQueue(Context context) {
        VdbSocket vdbSocket = new BasicVdbSocket();
        VdbRequestQueue queue = new VdbRequestQueue(vdbSocket);
        queue.start();

        return queue;
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
