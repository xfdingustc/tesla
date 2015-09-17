package com.waylens.hachi.hardware;

import android.content.Context;
import android.net.wifi.ScanResult;

import com.orhanobut.logger.Logger;
import com.transee.ccam.PasswordList;
import com.transee.common.Utils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class VdtCameraManager {
    private static final String TAG = VdtCameraManager.class.getSimpleName();
    private static final String PASSWORD_FILE = "wifipass";

    private static VdtCameraManager mSharedManager = null;

    private static Context mContext;
    private PasswordList mPasswordList;
    private boolean mPasswordLoaded;

    private static final int TAG_SHOULD_REMOVE = 0;
    private static final int TAG_SHOULD_KEEP = 1;
    private static final int TAG_ADDED = 2;

    // note: CameraManager is a global object,
    // we have to track each callback even they are installed by the same activity.
    private List<Callback> mCallbackList = new ArrayList<>();

    // cameras: connected + connecting + wifi-ap
    private final List<VdtCamera> mConnectedVdtCameras = new ArrayList<>();
    private final List<VdtCamera> mConnectingVdtCameras = new ArrayList<>();
    private final List<WifiItem> mWifiList = new ArrayList<>();


    public interface Callback {
        void onCameraConnecting(VdtCamera vdtCamera);

        void onCameraConnected(VdtCamera vdtCamera);

        void onCameraVdbConnected(VdtCamera vdtCamera);

        void onCameraDisconnected(VdtCamera vdtCamera);

        void onCameraStateChanged(VdtCamera vdtCamera);

        void onWifiListChanged();
    }


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


    public static VdtCameraManager getManager() {
        if (mSharedManager == null) {
            mSharedManager = new VdtCameraManager();
        }

        return mSharedManager;
    }

    public static void initialize(Context context) {
        mContext = context;
    }

    private VdtCameraManager() {
        mPasswordList = new PasswordList();
    }

    public VdtCamera getCamera(int position) {

        int index = position;
        if (index < 0)
            return null;

        if (index < mConnectedVdtCameras.size()) {
            return mConnectedVdtCameras.get(index);
        }

        index -= mConnectedVdtCameras.size();
        if (index < mConnectingVdtCameras.size()) {
            return mConnectingVdtCameras.get(index);
        }

        return null;
    }


    public void addCallback(Callback callback) {
        mCallbackList.add(callback);
    }


    public void removeCallback(Callback callback) {
        mCallbackList.remove(callback);
    }

    public void filterScanResult(List<ScanResult> list) {

        // mark all wifi as to remove
        for (WifiItem item : mWifiList) {
            item.mTag = TAG_SHOULD_REMOVE;
        }

        if (list != null) {
            for (ScanResult result : list) {

                String ssid = Utils.normalizeNetworkName(result.SSID);

                if (ssid == null || !isPossibleCamera(ssid)) {
                    continue;
                }

                if (wifiExistsInCameraLists(ssid)) {
                    //Logger.t(TAG).d(ssid + " already in camera list");
                    continue;
                }

                WifiItem item = findWifi(ssid);
                if (item != null) {
                    item.mTag = TAG_SHOULD_KEEP; // keep
                    continue;
                }

                String password = getPassword(ssid);
                item = new WifiItem(ssid, password, TAG_ADDED);
                mWifiList.add(item);
                Logger.t(TAG).d("add wifi cam " + item.mSSID);
            }
        }

        // manage the list
        int nRemoved = 0;
        int nAdded = 0;
        int index = 0;
        while (index < mWifiList.size()) {
            WifiItem item = mWifiList.get(index);
            if (item.mTag == TAG_SHOULD_REMOVE) {
                mWifiList.remove(index);
                nRemoved++;
                continue;
            }
            if (item.mTag == TAG_ADDED) {
                nAdded++;
            }
            index++;
        }

        //Logger.t(TAG).d("wifi list: removed " + nRemoved + ", added " + nAdded);

        if (nRemoved + nAdded > 0) {
            for (Callback callback : mCallbackList) {
                callback.onWifiListChanged();
            }
        }
    }

    private boolean isPossibleCamera(String ssid) {
        if (ssid.length() != 8) {
            return false;
        }
        if (ssid.charAt(0) != 'C' || ssid.charAt(1) != '9' || ssid.charAt(2) != 'J') {
            return false;
        }
        return true;
    }

    // in connected, connecting
    private boolean wifiExistsInCameraLists(String ssid) {
        if (wifiExistsIn(ssid, mConnectedVdtCameras))
            return true;
        if (wifiExistsIn(ssid, mConnectingVdtCameras))
            return true;
        return false;
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

    private boolean wifiExistsIn(String ssid, List<VdtCamera> list) {
        for (VdtCamera c : list) {
            if (ssid.equals(c.getSSID()))
                return true;
        }
        return false;
    }

    // API
    public void connectCamera(VdtCamera.ServiceInfo serviceInfo) {

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
        //vdtCamera.addCallback(mCameraCallback);
        vdtCamera.setOnConnectionChangeListener(new VdtCamera.OnConnectionChangeListener() {
            @Override
            public void onConnected(VdtCamera vdtCamera) {
                onCameraConnected(vdtCamera);
            }

            @Override
            public void onVdbConnected(VdtCamera vdtCamera) {
                onCameraVdbConnected(vdtCamera);
            }

            @Override
            public void onDisconnected(VdtCamera vdtCamera) {
                onCameraDisconnected(vdtCamera);
            }
        });

        vdtCamera.setOnStateChangeListener(new VdtCamera.OnStateChangeListener() {
            @Override
            public void onStateChanged(VdtCamera vdtCamera) {
                for (Callback callback : mCallbackList) {
                    callback.onCameraStateChanged(vdtCamera);
                }
            }

            @Override
            public void onBtStateChanged(VdtCamera vdtCamera) {

            }

            @Override
            public void onGpsStateChanged(VdtCamera vdtCamera) {

            }

            @Override
            public void onWifiStateChanged(VdtCamera vdtCamera) {

            }
        });


        if (serviceInfo.bPcServer) {
            mConnectedVdtCameras.add(vdtCamera);
        } else {
            mConnectingVdtCameras.add(vdtCamera);
            vdtCamera.getClient().start();
        }

        for (Callback callback : mCallbackList) {
            if (serviceInfo.bPcServer) {
                callback.onCameraConnected(vdtCamera);
            } else {
                callback.onCameraConnecting(vdtCamera);
            }
        }
    }

    private boolean cameraExistsIn(InetAddress inetAddr, int port, List<VdtCamera> list) {
        for (VdtCamera c : list) {
            InetAddress address = c.getAddress();
            if (address.equals(inetAddr))
                return true;
        }
        return false;
    }


    public int getTotalItems() {
        return mConnectedVdtCameras.size() + mConnectingVdtCameras.size() + mWifiList.size();
    }

    public List<VdtCamera> getConnectedCameras() {
        return mConnectedVdtCameras;
    }

    public boolean removeConnectedCamera(VdtCamera vdtCamera) {
        return mConnectedVdtCameras.remove(vdtCamera);
    }

    public List<VdtCamera> getConnectingCameras() {
        return mConnectingVdtCameras;
    }

    public List<WifiItem> getWifiList() {
        return mWifiList;
    }

    public VdtCamera findConnectedCamera(String ssid, String hostString) {
        return findCameraInList(ssid, hostString, mConnectedVdtCameras);
    }

    // API
    public VdtCamera findCameraById(String ssid, String hostString) {
        VdtCamera vdtCamera;
        vdtCamera = findCameraInList(ssid, hostString, mConnectedVdtCameras);
        if (vdtCamera != null)
            return vdtCamera;
        vdtCamera = findCameraInList(ssid, hostString, mConnectingVdtCameras);
        if (vdtCamera != null)
            return vdtCamera;
        return null;
    }

    private VdtCamera findCameraInList(String ssid, String hostString, List<VdtCamera> list) {
        for (VdtCamera c : list) {
            if (c.idMatch(ssid, hostString))
                return c;
        }
        return null;
    }

    // API
    public int findCameraIndex(VdtCamera vdtCamera) {
        int count = mConnectedVdtCameras.size();
        for (int i = 0; i < count; i++) {
            if (vdtCamera == mConnectedVdtCameras.get(i)) {
                return i;
            }
        }
        count = mConnectingVdtCameras.size();
        for (int i = 0; i < count; i++) {
            if (vdtCamera == mConnectingVdtCameras.get(i)) {
                return i + mConnectedVdtCameras.size();
            }
        }
        return -1;
    }


    // API
    public void clearAll() {
        clearCameras(mConnectedVdtCameras);
        clearCameras(mConnectingVdtCameras);
        mWifiList.clear();
    }

    // API
    public void setPassword(String ssid, String password) {
        loadPassword();
        mPasswordList.setPassword(ssid, password);
    }

    private void clearCameras(List<VdtCamera> list) {
        for (VdtCamera c : list) {
            c.getClient().stop();
        }
        list.clear();
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
                for (Callback callback : mCallbackList) {
                    callback.onCameraConnected(vdtCamera);
                }
                return;
            }
            index++;
        }
        Logger.t(TAG).d("camera connected, but was not connecting, stop it");
        vdtCamera.getClient().stop();
    }

    private void onCameraVdbConnected(VdtCamera vdtCamera) {
        for (Callback callback : mCallbackList) {
            callback.onCameraVdbConnected(vdtCamera);
        }
    }

    private void onCameraDisconnected(VdtCamera vdtCamera) {
        // disconnect msg may be sent from msg thread,
        // need to stop it fully
        //vdtCamera.removeCallback(mCameraCallback);
        vdtCamera.getClient().stop();

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
        for (Callback callback : mCallbackList) {
            callback.onCameraDisconnected(vdtCamera);
        }


        Logger.t(TAG).d("camera disconnected, but was not connected");
    }


}
