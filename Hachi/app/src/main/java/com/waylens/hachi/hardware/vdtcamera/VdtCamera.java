package com.waylens.hachi.hardware.vdtcamera;

import android.os.Handler;
import android.util.Log;

import com.transee.ccam.BtState;
import com.transee.ccam.CameraState;
import com.transee.ccam.GpsState;
import com.transee.ccam.WifiState;
import com.waylens.hachi.snipe.VdbConnection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

// use SSID + hostString to identify a camera

public class VdtCamera {
    private static final String TAG = VdtCamera.class.getSimpleName();
    private static final boolean DEBUG = false;

    private boolean mIsConnected = false;
    private boolean mIsVdbConnected = false;

    private List<Callback> mCallbacks = new ArrayList<>();
    private final Handler mHandler;
    private final ServiceInfo mServiceInfo;
    private final VdtCameraController mController;

    private InetSocketAddress mPreviewAddress;

    private CameraState mStates = new CameraState();
    private BtState mBtStates = new BtState();
    private GpsState mGpsStates = new GpsState();
    private WifiState mWifiStates = new WifiState();


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

    public interface OnConnectionChangeListener {
        void onConnected(VdtCamera vdtCamera);

        void onVdbConnected(VdtCamera vdtCamera);

        void onDisconnected(VdtCamera vdtCamera);
    }

    public interface OnStateChangeListener {
        void onStateChanged(VdtCamera vdtCamera);

        void onBtStateChanged(VdtCamera vdtCamera);

        void onGpsStateChanged(VdtCamera vdtCamera);

        void onWifiStateChanged(VdtCamera vdtCamera);
    }


    private OnConnectionChangeListener mOnConnectionChangeListener = null;
    private OnStateChangeListener mOnStateChangeListener = null;


    public void setOnConnectionChangeListener(OnConnectionChangeListener listener) {
        mOnConnectionChangeListener = listener;
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        mOnStateChangeListener = listener;
    }

    private VdbConnection mVdbConnection;

    public VdbConnection getVdbConnection() {
        return mVdbConnection;
    }

    public interface Callback {

        void onStartRecordError(VdtCamera vdtCamera, int error);

        void onHostSSIDFetched(VdtCamera vdtCamera, String ssid);

        void onScanBtDone(VdtCamera vdtCamera);

        void onBtDevInfo(VdtCamera vdtCamera, int type, String mac, String name);

        void onStillCaptureStarted(VdtCamera vdtCamera, boolean bOneShot);

        void onStillPictureInfo(VdtCamera vdtCamera, boolean bCapturing, int numPictures, int burstTicks);

        void onStillCaptureDone(VdtCamera vdtCamera);
    }


    public VdtCamera(VdtCamera.ServiceInfo serviceInfo) {
        mServiceInfo = serviceInfo;
        mHandler = new Handler();
        mController = new MyCameraClient(serviceInfo.inetAddr, serviceInfo.port);
    }

    // API
    public boolean isPcServer() {
        return mServiceInfo.bPcServer;
    }


    public boolean isConnected() {
        return mIsConnected && mIsVdbConnected;
    }


    public String getServerName() {
        return mServiceInfo.serverName;
    }

    public InetAddress getAddress() {
        return mServiceInfo.inetAddr;
    }

    public void addCallback(Callback callback) {
        mCallbacks.add(callback);
    }


    public void removeCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    public String getSSID() {
        return mServiceInfo.ssid;
    }

    public String getHostString() {
        return mServiceInfo.inetAddr.getHostAddress();
    }


    public boolean idMatch(String ssid, String hostString) {
        if (ssid == null || hostString == null) {
            return false;
        }
        String myHostString = getHostString();
        if (mServiceInfo.ssid == null || myHostString == null) {
            return false;
        }
        return mServiceInfo.ssid.equals(ssid) && myHostString.equals(hostString);
    }


    // API - camera can be null
    public CameraState getState() {
        return mStates;
    }

    // API - camera can be null
    public BtState getBtStates() {
        return mBtStates;
    }

    // API - camera can be null
    public WifiState getWifiStates() {
        return mWifiStates;
    }

    // API
    public InetSocketAddress getInetSocketAddress() {
        return mController.getInetSocketAddress();
    }

    // API
    public InetSocketAddress getPreviewAddress() {
        return mPreviewAddress;
    }

    private void onCameraConnected() {
        InetSocketAddress addr = mController.getInetSocketAddress();
        if (addr != null) {
            mPreviewAddress = new InetSocketAddress(addr.getAddress(), 8081);
            if (mOnConnectionChangeListener != null) {
                mOnConnectionChangeListener.onConnected(this);
            }
        }
        mVdbConnection = new VdbConnection(getHostString());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mVdbConnection.connect();
                    mIsVdbConnected = true;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mOnConnectionChangeListener.onVdbConnected(VdtCamera.this);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        mIsConnected = true;
    }

    private void onCameraDisconnected() {
        // callback may unregister itself; so use a copy
        ArrayList<Callback> c = createCallbackCopy();
        if (mOnConnectionChangeListener != null) {
            mOnConnectionChangeListener.onDisconnected(this);
        }
        mIsConnected = false;
    }

    private ArrayList<Callback> createCallbackCopy() {
        ArrayList<Callback> c = new ArrayList<Callback>();
        for (Callback callback : mCallbacks) {
            c.add(callback);
        }
        return c;
    }


    // called on camera thread
    private void initCameraState() {
        VdtCameraController client = mController;
        client.cmd_Cam_getApiVersion();
        client.cmd_fw_getVersion();
        client.cmd_fw_getVersion();
        client.cmd_Cam_get_Name();
        client.cmd_Rec_List_Resolutions(); // see if still capture is supported
        client.cmd_Cam_get_getAllInfor();
        client.cmd_Cam_get_State();
        client.cmd_Network_GetWLanMode();
        client.cmd_Network_GetHostNum();
        client.cmd_Rec_GetMarkTime();

    }

    // client told us camera state has changed,
    // so synchronize our state with it (on main thread)
    private void syncCameraState() {
        if (mController.syncState(mStates)) {
            if (mOnStateChangeListener != null) {
                mOnStateChangeListener.onStateChanged(this);
            }
        }
    }

    private void syncBtState() {
        if (mController.syncBtState(mBtStates)) {
            if (mOnStateChangeListener != null) {
                mOnStateChangeListener.onBtStateChanged(this);
            }
        }
    }

    private void syncGpsState() {
        if (mController.syncGpsState(mGpsStates)) {
            if (mOnStateChangeListener != null) {
                mOnStateChangeListener.onGpsStateChanged(this);
            }
        }
    }

    private void syncWifiState() {
        if (mController.syncWifiState(mWifiStates)) {
            if (mOnStateChangeListener != null) {
                mOnStateChangeListener.onWifiStateChanged(this);
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

    class MyCameraClient extends VdtCameraController {

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


    // Control APIs
    public void setBtEnable(boolean enable) {
        mController.cmd_CAM_BT_Enable(enable);
    }

    public void doBtScan() {
        mController.cmd_CAM_BT_doScan();
    }

    public void getBtHostNumber() {
        mController.cmd_CAM_BT_getHostNum();
    }

    public void doBtUnbind(int type, String mac) {
        mController.cmd_CAM_BT_doUnBind(type, mac);
    }

    public void setRecordOverlay(int flags) {
        mController.cmd_Rec_setOverlay(flags);
    }

    public void getRecordOverlayState() {
        mController.cmd_Rec_getOverlayState();
    }

    public void setRecordMarkTime(int markBeforeTime, int newMarkTime) {
        mController.cmd_Rec_SetMarkTime(markBeforeTime, newMarkTime);
    }

    public void setCameraName(String name) {
        mController.cmd_Cam_set_Name(name);
    }

    public void getCameraName() {
        mController.cmd_Cam_get_Name();
    }

    public void setRecordResolution(int resolutionIndex) {
        mController.cmd_Rec_Set_Resolution(resolutionIndex);
    }

    public void getRecordResolution() {
        mController.cmd_Rec_get_Resolution();
    }

    public void setRecordQuality(int qualityIndex) {
        mController.cmd_Rec_Set_Quality(qualityIndex);
    }

    public void getRecordQuality() {
        mController.cmd_Rec_get_Quality();
    }

    public void setRecordColorMode(int index) {
        mController.cmd_Rec_Set_ColorMode(index);
    }

    public void getRecordColorMode() {
        mController.cmd_Rec_get_ColorMode();
    }

    public void setRecordRecMode(int flags) {
        mController.cmd_Rec_Set_RecMode(flags);
    }

    public void getRecordRecMode() {
        mController.cmd_Rec_get_RecMode();
    }

    public void setAudioMic(int state, int vol) {
        mController.cmd_audio_setMic(state, vol);
    }

    public void getAudioMicState() {
        mController.cmd_audio_getMicState();
    }

    public void setNetworkSynctime(long time, int timezone) {
        mController.cmd_Network_Synctime(time, timezone);
    }

    public void GetSetup() {
        mController.userCmd_GetSetup();
    }

    public void startClient() {
        mController.start();
    }

    public void stopClient() {
        mController.stop();
    }

    public void setCameraWantIdle() {
        mController.cmd_CAM_WantIdle();
    }

    public void startPreview() {
        mController.cmd_CAM_WantPreview();
    }

    public void getCameraTime() {
        mController.ack_Cam_get_time();
    }

    public void getRecordResolutionList() {
        mController.cmd_Rec_List_Resolutions();
    }

    public void markLiveVideo() {
        mController.cmd_Rec_MarkLiveVideo();
    }

    public void stopRecording() {
        mController.ack_Cam_stop_rec();
    }

    public void startRecording() {
        mController.cmd_Cam_start_rec();
    }

    public void startStillCapture(boolean oneShot) {
        mController.cmd_Rec_StartStillCapture(oneShot);
    }

    public void stopStillCapture() {
        mController.cmd_Rec_StopStillCapture();
    }

    public void setRecordStillMode(boolean stillMode) {
        mController.cmd_Rec_SetStillMode(stillMode);
    }

    public void getNetworkHostHum() {
        mController.cmd_Network_GetHostNum();
    }

    public void setNetworkRmvHost(String ssid) {
        mController.cmd_Network_RmvHost(ssid);
    }

    public void setAddNetworkHost(String ssid, String password) {
        mController.cmd_Network_AddHost(ssid, password);
    }
}
