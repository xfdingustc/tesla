package com.waylens.hachi.hardware.vdtcamera;

import android.content.Context;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;


import org.greenrobot.eventbus.EventBus;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class VdtCameraManager {
    private static final String TAG = VdtCameraManager.class.getSimpleName();
    private static final String PASSWORD_FILE = "wifipass";

    private static VdtCameraManager mSharedManager = new VdtCameraManager();

    private static Context mContext;


    private EventBus mEventBus = EventBus.getDefault();

    private static final int TAG_SHOULD_REMOVE = 0;
    private static final int TAG_SHOULD_KEEP = 1;
    private static final int TAG_ADDED = 2;

    public static VdtCameraManager getManager() {
        return mSharedManager;
    }

    public static void initialize(Context context) {
        mContext = context;
    }

    // note: CameraManager is a global data,
    // we have to track each callback even they are installed by the same activity.


    // cameras: connected + connecting + wifi-ap
    private final List<VdtCamera> mConnectedVdtCameras = new ArrayList<>();
    private final List<VdtCamera> mConnectingVdtCameras = new ArrayList<>();

    private VdtCamera mCurrentCamera;


    // API
    synchronized public void connectCamera(VdtCamera.ServiceInfo serviceInfo) {
//        Logger.t(TAG).d("connect Camera");
        if (cameraExistsIn(serviceInfo.inetAddr, serviceInfo.port, mConnectedVdtCameras)) {
            // already connected
            return;
        }

        if (cameraExistsIn(serviceInfo.inetAddr, serviceInfo.port, mConnectingVdtCameras)) {
            // already connecting
            return;
        }


        VdtCamera vdtCamera = new VdtCamera(serviceInfo);

        Logger.t(TAG).d("create new VdtCamera current connected camera size: " + mConnectedVdtCameras.size());

        //vdtCamera.addCallback(mCameraCallback);
        vdtCamera.setOnConnectionChangeListener(new VdtCamera.OnConnectionChangeListener() {
            @Override
            public void onConnected(VdtCamera vdtCamera) {
                onCameraConnected(vdtCamera);
            }

            @Override
            public void onVdbConnected(VdtCamera vdtCamera) {
                if (mCurrentCamera == null) {
                    mCurrentCamera = vdtCamera;
                }
                mEventBus.post(new CameraConnectionEvent(CameraConnectionEvent.VDT_CAMERA_CONNECTED, vdtCamera));
            }

            @Override
            public void onDisconnected(VdtCamera vdtCamera) {
                onCameraDisconnected(vdtCamera);
            }
        });


        mConnectingVdtCameras.add(vdtCamera);
//        vdtCamera.startClient();


        mEventBus.post(new CameraConnectionEvent(CameraConnectionEvent.VDT_CAMERA_CONNECTING, vdtCamera));
    }

    private boolean cameraExistsIn(InetAddress inetAddr, int port, List<VdtCamera> list) {
        for (VdtCamera c : list) {
            InetAddress address = c.getAddress();
            if (address.equals(inetAddr))
                return true;
        }
        return false;
    }


    public List<VdtCamera> getConnectedCameras() {
        return mConnectedVdtCameras;
    }


    public VdtCamera findConnectedCamera(String ssid, String hostString) {
        return findCameraInList(ssid, hostString, mConnectedVdtCameras);
    }


    private VdtCamera findCameraInList(String ssid, String hostString, List<VdtCamera> list) {
        for (VdtCamera c : list) {
            if (c.idMatch(ssid, hostString))
                return c;
        }
        return null;
    }


    public boolean isConnected() {
        if (getConnectedCameras().size() > 0) {
            return true;
        }
        return false;
    }


    public void setCurrentCamera(int position) {
        this.mCurrentCamera = mConnectedVdtCameras.get(position);
    }


    public VdtCamera getCurrentCamera() {
        return mCurrentCamera;
    }

    private void onCameraConnected(VdtCamera vdtCamera) {
        for (int i = 0; i < mConnectingVdtCameras.size(); i++) {
            VdtCamera oneCamera = mConnectingVdtCameras.get(i);
            if (oneCamera == vdtCamera) {
                mConnectingVdtCameras.remove(i);
                mConnectedVdtCameras.add(vdtCamera);
                Logger.t(TAG).d("camera connected: " + vdtCamera.getInetSocketAddress());
                return;
            }
        }

        Logger.t(TAG).d("camera connected, but was not connecting, stop it");
        vdtCamera.stopClient();

    }




    private void onCameraDisconnected(VdtCamera vdtCamera) {
        // disconnect msg may be sent from msg thread,
        // need to stop it fully
        //vdtCamera.removeCallback(mCameraCallback);
        vdtCamera.stopClient();

        for (int i = 0; i < mConnectedVdtCameras.size(); i++) {
            if (mConnectedVdtCameras.get(i) == vdtCamera) {
                mConnectedVdtCameras.remove(i);
                Logger.t(TAG).d("camera disconnected " + vdtCamera.getInetSocketAddress());
                break;
            }
        }

        if (vdtCamera == mCurrentCamera) {
            if (mConnectedVdtCameras.size() == 0) {
                mCurrentCamera = null;
            } else {
                mCurrentCamera = mConnectedVdtCameras.get(0);
            }
        }

        //Connecting cameras might also emit disconnect event
        for (int i = 0; i < mConnectingVdtCameras.size(); i++) {
            if (mConnectingVdtCameras.get(i) == vdtCamera) {
                mConnectingVdtCameras.remove(i);
                break;
            }
        }
        mEventBus.post(new CameraConnectionEvent(CameraConnectionEvent.VDT_CAMERA_DISCONNECTED, vdtCamera));


    }


}
