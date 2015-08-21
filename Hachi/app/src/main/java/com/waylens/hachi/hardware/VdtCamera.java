package com.waylens.hachi.hardware;

import android.os.Handler;
import android.util.Log;

import com.transee.ccam.AbsCameraClient;
import com.transee.ccam.BtState;
import com.transee.ccam.CameraClient;
import com.transee.ccam.CameraState;
import com.transee.ccam.GpsState;
import com.transee.ccam.NullCameraClient;
import com.transee.ccam.WifiState;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

// use SSID + hostString to identify a camera

public class VdtCamera {
    private static final String TAG = VdtCamera.class.getSimpleName();
    private static final boolean DEBUG = false;

    private boolean mIsConnected  = false;

    static public class ServiceInfo {
        public String ssid;
        public final InetAddress inetAddr;
        public final int port;
        public final String serverName;
        public final String serviceName;
        public final boolean bPcServer;


        public ServiceInfo(InetAddress inetAddr, int port, String serverName, String serviceName, boolean bPcServer) {
            this.ssid = "";
            this.inetAddr = inetAddr;
            this.port = port;
            this.serverName = serverName;
            this.serviceName = serviceName;
            this.bPcServer = bPcServer;
        }
    }

    public interface Callback {
        void onConnected(VdtCamera vdtCamera);

        void onDisconnected(VdtCamera vdtCamera);

        void onStateChanged(VdtCamera vdtCamera);

        void onBtStateChanged(VdtCamera vdtCamera);

        void onGpsStateChanged(VdtCamera vdtCamera);

        void onWifiStateChanged(VdtCamera vdtCamera);

        void onStartRecordError(VdtCamera vdtCamera, int error);

        void onHostSSIDFetched(VdtCamera vdtCamera, String ssid);

        void onScanBtDone(VdtCamera vdtCamera);

        void onBtDevInfo(VdtCamera vdtCamera, int type, String mac, String name);

        void onStillCaptureStarted(VdtCamera vdtCamera, boolean bOneShot);

        void onStillPictureInfo(VdtCamera vdtCamera, boolean bCapturing, int numPictures, int burstTicks);

        void onStillCaptureDone(VdtCamera vdtCamera);
    }

    static public class CallbackImpl implements Callback {
        @Override
        public void onConnected(VdtCamera vdtCamera) {
        }

        @Override
        public void onDisconnected(VdtCamera vdtCamera) {
        }

        @Override
        public void onStateChanged(VdtCamera vdtCamera) {
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

        @Override
        public void onStartRecordError(VdtCamera vdtCamera, int error) {
        }

        @Override
        public void onHostSSIDFetched(VdtCamera vdtCamera, String ssid) {
        }

        @Override
        public void onScanBtDone(VdtCamera vdtCamera) {
        }

        @Override
        public void onBtDevInfo(VdtCamera vdtCamera, int type, String mac, String name) {
        }

        @Override
        public void onStillCaptureStarted(VdtCamera vdtCamera, boolean bOneShot) {
        }

        @Override
        public void onStillPictureInfo(VdtCamera vdtCamera, boolean bCapturing, int numPictures, int burstTicks) {
        }

        @Override
        public void onStillCaptureDone(VdtCamera vdtCamera) {
        }
    }

    private ArrayList<Callback> mCallbacks = new ArrayList<Callback>();
    private final Handler mHandler;
    private final ServiceInfo mServiceInfo;
    private final AbsCameraClient mClient;

    private InetSocketAddress mPreviewAddress;

    private CameraState mStates = new CameraState();
    private BtState mBtStates = new BtState();
    private GpsState mGpsStates = new GpsState();
    private WifiState mWifiStates = new WifiState();

    public VdtCamera(VdtCamera.ServiceInfo serviceInfo) {
        mServiceInfo = serviceInfo;
        mHandler = new Handler();
        mClient = serviceInfo.bPcServer ? new NullCameraClient() : new MyCameraClient(serviceInfo.inetAddr,
            serviceInfo.port);
    }

    // API
    public boolean isPcServer() {
        return mServiceInfo.bPcServer;
    }


    public void markConnected(boolean connected) {
        mIsConnected = connected;
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    // API
    public String getServerName() {
        return mServiceInfo.serverName;
    }

    // API
    public InetAddress getAddress() {
        return mServiceInfo.inetAddr;
    }

    // API
    public void addCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    // API
    public void removeCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    // API
    public String getSSID() {
        return mServiceInfo.ssid;
    }

    // API
    public String getHostString() {
        return mServiceInfo.inetAddr.getHostAddress();
    }

    // API
    public boolean idMatch(String ssid, String hostString) {
        if (ssid == null || hostString == null)
            return false;
        String myHostString = getHostString();
        if (mServiceInfo.ssid == null || myHostString == null)
            return false;
        return mServiceInfo.ssid.equals(ssid) && myHostString.equals(hostString);
    }

    // API
    public AbsCameraClient getClient() {
        return mClient;
    }

    // API - camera can be null
    static public CameraState getCameraStates(VdtCamera vdtCamera) {
        return vdtCamera == null ? CameraState.nullState : vdtCamera.mStates;
    }

    // API - camera can be null
    static public BtState getBtStates(VdtCamera vdtCamera) {
        return vdtCamera == null ? BtState.nullState : vdtCamera.mBtStates;
    }

    // API - camera can be null
    static public WifiState getWifiStates(VdtCamera vdtCamera) {
        return vdtCamera == null ? WifiState.nullState : vdtCamera.mWifiStates;
    }

    // API
    public InetSocketAddress getInetSocketAddress() {
        return mClient.getInetSocketAddress();
    }

    // API
    public InetSocketAddress getPreviewAddress() {
        return mPreviewAddress;
    }

    private void onCameraConnected() {
        InetSocketAddress addr = mClient.getInetSocketAddress();
        if (addr != null) {
            mPreviewAddress = new InetSocketAddress(addr.getAddress(), 8081);
            for (Callback callback : mCallbacks) {
                callback.onConnected(this);
            }
        }
    }

    private ArrayList<Callback> createCallbackCopy() {
        ArrayList<Callback> c = new ArrayList<Callback>();
        for (Callback callback : mCallbacks) {
            c.add(callback);
        }
        return c;
    }

    private void onCameraDisconnected() {
        // callback may unregister itself; so use a copy
        ArrayList<Callback> c = createCallbackCopy();
        for (Callback callback : c) {
            callback.onDisconnected(this);
        }
    }

    // called on camera thread
    private void initCameraState() {
        if (mClient instanceof CameraClient) {
            CameraClient client = (CameraClient) mClient;
            client.cmd_Cam_getApiVersion();
            client.cmd_fw_getVersion();
            client.cmd_fw_getVersion();
            client.cmd_Cam_get_Name();
            client.cmd_Rec_List_Resolutions(); // see if still capture is supported
            client.cmd_Cam_get_getAllInfor();
            client.cmd_Cam_get_State();
            client.cmd_Network_GetWLanMode();
            client.cmd_Network_GetHostNum();
        }
    }

    // client told us camera state has changed,
    // so synchronize our state with it (on main thread)
    private void syncCameraState() {
        if (mClient.syncState(mStates)) {
            for (Callback callback : mCallbacks) {
                callback.onStateChanged(this);
            }
        }
    }

    private void syncBtState() {
        if (mClient.syncBtState(mBtStates)) {
            for (Callback callback : mCallbacks) {
                callback.onBtStateChanged(this);
            }
        }
    }

    private void syncGpsState() {
        if (mClient.syncGpsState(mGpsStates)) {
            for (Callback callback : mCallbacks) {
                callback.onGpsStateChanged(this);
            }
        }
    }

    private void syncWifiState() {
        if (mClient.syncWifiState(mWifiStates)) {
            for (Callback callback : mCallbacks) {
                callback.onWifiStateChanged(this);
            }
        }
    }

    private void onStartRecordError(int error) {
        for (Callback callback : mCallbacks) {
            callback.onStartRecordError(this, error);
        }
    }

    private void onHostSSIDFetched(String ssid) {
        for (Callback callback : mCallbacks) {
            callback.onHostSSIDFetched(this, ssid);
        }
    }

    private void onScanBtDone() {
        for (Callback callback : mCallbacks) {
            callback.onScanBtDone(this);
        }
    }

    private void onBtDevInfo(int type, String mac, String name) {
        for (Callback callback : mCallbacks) {
            callback.onBtDevInfo(this, type, mac, name);
        }
    }

    private void onStillCaptureStarted(boolean bOneShot) {
        for (Callback callback : mCallbacks) {
            callback.onStillCaptureStarted(this, bOneShot);
        }
    }

    private void onStillPictureInfo(boolean bCapturing, int numPictures, int burst_ticks) {
        for (Callback callback : mCallbacks) {
            callback.onStillPictureInfo(this, bCapturing, numPictures, burst_ticks);
        }
    }

    private void onStillCaptureDone() {
        for (Callback callback : mCallbacks) {
            callback.onStillCaptureDone(this);
        }
    }

    private Runnable mSyncStateAction = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) {
                Log.d(TAG, "onCameraStateChanged");
            }
            syncCameraState();
        }
    };

    private Runnable mSyncBtStateAction = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) {
                Log.d(TAG, "onBtStateChanged");
            }
            syncBtState();
        }
    };

    private Runnable mSyncGpsStateAction = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) {
                Log.d(TAG, "onGpsStateChanged");
            }
            syncGpsState();
        }
    };

    private Runnable mSyncWifiStateAction = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) {
                Log.d(TAG, "onWifiStateChanged");
            }
            syncWifiState();
        }
    };

    class MyCameraClient extends CameraClient {

        public MyCameraClient(InetAddress host, int port) {
            super(host, port);
        }

        @Override
        public void onConnected() {
            initCameraState();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onCameraConnected();
                }
            });
        }

        @Override
        public void onDisconnected() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onCameraDisconnected();
                }
            });
        }

        @Override
        public void onCameraStateChanged() {
            mHandler.post(mSyncStateAction);
        }

        @Override
        public void onBtStateChanged() {
            mHandler.post(mSyncBtStateAction);
        }

        @Override
        public void onGpsStateChanged() {
            mHandler.post(mSyncGpsStateAction);
        }

        @Override
        public void onWifiStateChanged() {
            mHandler.post(mSyncWifiStateAction);
        }

        @Override
        public void onStartRecordError(final int error) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    VdtCamera.this.onStartRecordError(error);
                }
            });
        }

        @Override
        public void onHostSSIDFetched(final String ssid) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    VdtCamera.this.onHostSSIDFetched(ssid);
                }
            });
        }

        @Override
        public void onScanBtDone() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    VdtCamera.this.onScanBtDone();
                }
            });
        }

        @Override
        public void onBtDevInfo(final int type, final String mac, final String name) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    VdtCamera.this.onBtDevInfo(type, mac, name);
                }
            });
        }

        @Override
        public void onStillCaptureStarted(final boolean bOneShot) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    VdtCamera.this.onStillCaptureStarted(bOneShot);
                }
            });
        }

        @Override
        public void onStillPictureInfo(final boolean bCapturing, final int numPictures, final int burst_ticks) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    VdtCamera.this.onStillPictureInfo(bCapturing, numPictures, burst_ticks);
                }
            });
        }

        @Override
        public void onStillCaptureDone() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    VdtCamera.this.onStillCaptureDone();
                }
            });
        }

    }
}
