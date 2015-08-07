package com.transee.ccam;

import android.content.Context;
import android.net.wifi.ScanResult;

import com.orhanobut.logger.Logger;
import com.transee.common.Utils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class CameraManager {

    static final String TAG = "CameraManager";
    static final String PASSWORD_FILE = "wifipass";

    public interface Callback {
        public void onCameraConnecting(CameraManager manager, Camera camera);

        public void onCameraConnected(CameraManager manager, Camera camera);

        public void onCameraDisconnected(CameraManager manager, Camera camera);

        public void onCameraStateChanged(CameraManager manager, Camera camera);

        public void onWifiListChanged(CameraManager manager);
    }

    static final int TAG_SHOULD_REMOVE = 0;
    static final int TAG_SHOULD_KEEP = 1;
    static final int TAG_ADDED = 2;

    static public class WifiItem {
        public String mSSID;
        public String mPassword; // may be null
        private int mTag;

        public WifiItem(String ssid, String password, int tag) {
            mSSID = ssid;
            mPassword = password;
            mTag = tag;
        }
    }

    private final Context mContext;
    private PasswordList mPasswordList;
    private boolean mPasswordLoaded;

    // note: CameraManager is a global object,
    // we have to track each callback even they are installed by the same activity.
    private ArrayList<Callback> mCallbackList = new ArrayList<Callback>();

    // cameras: connected + connecting + wifi-ap
    private final ArrayList<Camera> mConnectedCameras = new ArrayList<Camera>();
    private final ArrayList<Camera> mConnectingCameras = new ArrayList<Camera>();
    private final ArrayList<WifiItem> mWifiList = new ArrayList<WifiItem>();

    public CameraManager(Context context) {
        mContext = context;
        mPasswordList = new PasswordList();
    }

    // API
    public void addCallback(Callback callback) {
        mCallbackList.add(callback);
        Logger.t(TAG).d("add callback: " + callback);
    }

    // API
    public void removeCallback(Callback callback) {
        Logger.t(TAG).d("remove callback: " + callback);
        if (!mCallbackList.remove(callback)) {
            Logger.t(TAG).d("remove callback failed!");
        }
    }

    // API : list may be null
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
                    Logger.t(TAG).d(ssid + " already in camera list");
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

        Logger.t(TAG).d("wifi list: removed " + nRemoved + ", added " + nAdded);

        if (nRemoved + nAdded > 0) {
            for (Callback callback : mCallbackList) {
                callback.onWifiListChanged(this);
            }
        }
    }

    private boolean isPossibleCamera(String ssid) {
        if (ssid.length() != 8)
            return false;
        if (ssid.charAt(0) != 'C' || ssid.charAt(1) != '9' || ssid.charAt(2) != 'J')
            return false;
        return true;
    }

    // in connected, connecting
    private boolean wifiExistsInCameraLists(String ssid) {
        if (wifiExistsIn(ssid, mConnectedCameras))
            return true;
        if (wifiExistsIn(ssid, mConnectingCameras))
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

    private boolean wifiExistsIn(String ssid, ArrayList<Camera> list) {
        for (Camera c : list) {
            if (ssid.equals(c.getSSID()))
                return true;
        }
        return false;
    }

    // API
    public void connectCamera(Camera.ServiceInfo serviceInfo) {

        if (cameraExistsIn(serviceInfo.inetAddr, serviceInfo.port, mConnectedCameras)) {
            // already connected
            return;
        }

        if (cameraExistsIn(serviceInfo.inetAddr, serviceInfo.port, mConnectingCameras)) {
            // already connecting
            return;
        }

        WifiItem item = findWifi(serviceInfo.ssid);
        if (item != null) {
            Logger.t(TAG).d("connecting wifi " + serviceInfo.ssid);
            removeWifi(serviceInfo.ssid);
        }

        Camera camera = new Camera(serviceInfo);
        camera.addCallback(mCameraCallback);

        if (serviceInfo.bPcServer) {
            mConnectedCameras.add(camera);
        } else {
            mConnectingCameras.add(camera);
            camera.getClient().start();
        }

        for (Callback callback : mCallbackList) {
            if (serviceInfo.bPcServer) {
                callback.onCameraConnected(this, camera);
            } else {
                callback.onCameraConnecting(this, camera);
            }
        }
    }

    private boolean cameraExistsIn(InetAddress inetAddr, int port, ArrayList<Camera> list) {
        for (Camera c : list) {
            InetAddress address = c.getAddress();
            if (address.equals(inetAddr))
                return true;
        }
        return false;
    }

    // API
    public int getTotalItems() {
        return mConnectedCameras.size() + mConnectingCameras.size() + mWifiList.size();
    }

    // API
    public List<Camera> getConnectedCameras() {
        return mConnectedCameras;
    }

    // API
    public boolean removeConnectedCamera(Camera camera) {
        return mConnectedCameras.remove(camera);
    }

    // API
    public List<Camera> getConnectingCameras() {
        return mConnectingCameras;
    }

    // API
    public List<WifiItem> getWifiList() {
        return mWifiList;
    }

    // API
    public Camera findConnectedCamera(String ssid, String hostString) {
        return findCameraInList(ssid, hostString, mConnectedCameras);
    }

    // API
    public Camera findCameraById(String ssid, String hostString) {
        Camera camera;
        camera = findCameraInList(ssid, hostString, mConnectedCameras);
        if (camera != null)
            return camera;
        camera = findCameraInList(ssid, hostString, mConnectingCameras);
        if (camera != null)
            return camera;
        return null;
    }

    private Camera findCameraInList(String ssid, String hostString, ArrayList<Camera> list) {
        for (Camera c : list) {
            if (c.idMatch(ssid, hostString))
                return c;
        }
        return null;
    }

    // API
    public int findCameraIndex(Camera camera) {
        int count = mConnectedCameras.size();
        for (int i = 0; i < count; i++) {
            if (camera == mConnectedCameras.get(i))
                return i;
        }
        count = mConnectingCameras.size();
        for (int i = 0; i < count; i++) {
            if (camera == mConnectingCameras.get(i))
                return i + mConnectedCameras.size();
        }
        return -1;
    }

    // API
    public Object getItem(int index) {

        if (index < 0)
            return null;

        if (index < mConnectedCameras.size())
            return mConnectedCameras.get(index);
        index -= mConnectedCameras.size();

        if (index < mConnectingCameras.size())
            return mConnectingCameras.get(index);
        index -= mConnectingCameras.size();

        if (index < mWifiList.size())
            return mWifiList.get(index);

        return null;
    }

    // API
    public void clearAll() {
        clearCameras(mConnectedCameras);
        clearCameras(mConnectingCameras);
        mWifiList.clear();
    }

    // API
    public void setPassword(String ssid, String password) {
        loadPassword();
        mPasswordList.setPassword(ssid, password);
    }

    private void clearCameras(ArrayList<Camera> list) {
        for (Camera c : list) {
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

    private void onCameraConnected(Camera camera) {
        int index = 0;
        for (Camera c : mConnectingCameras) {
            if (c == camera) {
                mConnectingCameras.remove(index);
                mConnectedCameras.add(camera);
                Logger.t(TAG).d("camera connected: " + camera.getInetSocketAddress());
                for (Callback callback : mCallbackList) {
                    callback.onCameraConnected(this, camera);
                }
                return;
            }
            index++;
        }
        Logger.t(TAG).d("camera connected, but was not connecting, stop it");
        camera.getClient().stop();
    }

    private void onCameraDisconnected(Camera camera) {
        // disconnect msg may be sent from msg thread,
        // need to stop it fully
        camera.removeCallback(mCameraCallback);
        camera.getClient().stop();

        for (int i = 0; i < mConnectedCameras.size(); i++) {
            if (mConnectedCameras.get(i) == camera) {
                mConnectedCameras.remove(i);
                Logger.t(TAG).d("camera disconnected " + camera.getInetSocketAddress());
                break;
            }
        }

        //Connecting cameras might also emit disconnect event
        for (int i = 0; i < mConnectingCameras.size(); i++) {
            if (mConnectingCameras.get(i) == camera) {
                mConnectingCameras.remove(i);
                break;
            }
        }
        for (Callback callback : mCallbackList) {
            callback.onCameraDisconnected(this, camera);
        }


        Logger.t(TAG).d("camera disconnected, but was not connected");
    }

    private void onCameraStateChanged(Camera camera) {
        // TODO: check if the camera exists in our lists
        for (Callback callback : mCallbackList) {
            callback.onCameraStateChanged(this, camera);
        }
    }

    private final Camera.Callback mCameraCallback = new Camera.CallbackImpl() {

        @Override
        public void onConnected(Camera camera) {
            onCameraConnected(camera);
        }

        @Override
        public void onDisconnected(Camera camera) {
            onCameraDisconnected(camera);
        }

        @Override
        public void onStateChanged(Camera camera) {
            onCameraStateChanged(camera);
        }

    };
}
