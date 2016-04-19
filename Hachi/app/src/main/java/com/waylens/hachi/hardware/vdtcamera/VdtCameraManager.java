package com.waylens.hachi.hardware.vdtcamera;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.hardware.NetworkStateReceiver;
import com.waylens.hachi.hardware.smartconfig.NetworkUtil;
import com.waylens.hachi.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class VdtCameraManager {
    private static final String TAG = VdtCameraManager.class.getSimpleName();
    private static final String PASSWORD_FILE = "wifipass";

    private static VdtCameraManager mSharedManager = new VdtCameraManager();

    private static Context mContext;
    private PasswordList mPasswordList;
    private boolean mPasswordLoaded;

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
    private final List<WifiItem> mWifiList = new ArrayList<>();

    private VdtCamera mCurrentCamera;




    public static class WifiItem {
        public String mSSID;
        public String mPassword; // may be null
        private int mTag;

        public WifiItem(String ssid, String password, int tag) {
            mSSID = ssid;
            mPassword = password;
            mTag = tag;
        }
    }




    private VdtCameraManager() {
        mPasswordList = new PasswordList();
    }


    //
    private boolean removeWifi(String ssid) {
        int index = 0;
        for (WifiItem item : mWifiList) {
            if (ssid.equals(item.mSSID)) {
                mWifiList.remove(index);
                return true;
            }
            index++;
        }
        return false;
    }

    //
    private WifiItem findWifi(String ssid) {
        for (WifiItem item : mWifiList) {
            if (ssid.equals(item.mSSID))
                return item;
        }
        return null;
    }


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

        WifiItem item = findWifi(serviceInfo.ssid);
        if (item != null) {
            Logger.t(TAG).d("connecting wifi " + serviceInfo.ssid);
            removeWifi(serviceInfo.ssid);
        }

        VdtCamera vdtCamera = new VdtCamera(serviceInfo);
        Logger.t(TAG).d("camera is created");
        //vdtCamera.addCallback(mCameraCallback);
        vdtCamera.setOnConnectionChangeListener(new VdtCamera.OnConnectionChangeListener() {
            @Override
            public void onConnected(VdtCamera vdtCamera) {
                onCameraConnected(vdtCamera);
            }

            @Override
            public void onVdbConnected(VdtCamera vdtCamera) {

                mEventBus.post(new CameraConnectionEvent(CameraConnectionEvent.VDT_CAMERA_CONNECTED, vdtCamera));
            }

            @Override
            public void onDisconnected(VdtCamera vdtCamera) {
                onCameraDisconnected(vdtCamera);
            }
        });



        vdtCamera.setOnStateChangeListener(new VdtCamera.OnStateChangeListener() {

            @Override
            public void onBtStateChanged(VdtCamera vdtCamera) {

            }

            @Override
            public void onGpsStateChanged(VdtCamera vdtCamera) {

            }

        });


        if (serviceInfo.bPcServer) {
            mConnectedVdtCameras.add(vdtCamera);
        } else {
            mConnectingVdtCameras.add(vdtCamera);
            vdtCamera.startClient();
        }


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



    private final void loadPassword() {
        if (!mPasswordLoaded) {
            mPasswordList.load(mContext, PASSWORD_FILE);
            mPasswordLoaded = true;
        }
    }

    // API
    public String getPassword(String ssid) {
        loadPassword();
        return mPasswordList.getPassword(ssid);
    }

    private void onCameraConnected(VdtCamera vdtCamera) {
        int index = 0;
        for (VdtCamera c : mConnectingVdtCameras) {
            if (c == vdtCamera) {
                mConnectingVdtCameras.remove(index);
                mConnectedVdtCameras.add(vdtCamera);
                Logger.t(TAG).d("camera connected: " + vdtCamera.getInetSocketAddress());

                return;
            }
            index++;
        }
        Logger.t(TAG).d("camera connected, but was not connecting, stop it");
        vdtCamera.stopClient();
        enableNetworkStateReceiver(false);
    }

    void enableNetworkStateReceiver(boolean isEnabled) {
        int newState = isEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        ComponentName receiver = new ComponentName(mContext, NetworkStateReceiver.class);
        PackageManager pm = mContext.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                newState,
                PackageManager.DONT_KILL_APP);
        Logger.t(TAG).e("NetworkStateReceiver status: " + newState);
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

        //Connecting cameras might also emit disconnect event
        for (int i = 0; i < mConnectingVdtCameras.size(); i++) {
            if (mConnectingVdtCameras.get(i) == vdtCamera) {
                mConnectingVdtCameras.remove(i);
                break;
            }
        }
        mEventBus.post(new CameraConnectionEvent(CameraConnectionEvent.VDT_CAMERA_DISCONNECTED, vdtCamera));

        if (!NetworkUtil.isWifiConnected(mContext)) {
            enableNetworkStateReceiver(true);
        }
    }


}
