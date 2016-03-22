package com.waylens.hachi.hardware.vdtcamera;

import com.waylens.hachi.snipe.VdbConnection;
import com.waylens.hachi.ui.entities.NetworkItemBean;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;


public class VdtCamera {
    private static final String TAG = VdtCamera.class.getSimpleName();

    public static final int STATE_RECORD_UNKNOWN = -1;
    public static final int STATE_RECORD_STOPPED = 0;
    public static final int STATE_RECORD_STOPPING = 1;
    public static final int STATE_RECORD_STARTING = 2;
    public static final int STATE_RECORD_RECORDING = 3;
    public static final int STATE_RECORD_SWITCHING = 4;

    private boolean mIsConnected = false;
    private boolean mIsVdbConnected = false;

    private int mRecordState = STATE_RECORD_UNKNOWN;


    private final ServiceInfo mServiceInfo;
    private final VdtCameraController mController;

    private InetSocketAddress mPreviewAddress;

    private CameraState mState = new CameraState();
    private BtState mBtStates = new BtState();
    private GpsState mGpsStates = new GpsState();
    private WifiState mWifiStates = new WifiState();

    private OnScanHostListener mOnScanHostListener;


    public static class ServiceInfo {
        public String ssid;
        public final InetAddress inetAddr;
        public final int port;
        public final String serverName;
        public final String serviceName;
        public final boolean bPcServer;
        public int sessionCounter;


        public ServiceInfo(InetAddress inetAddr, int port, String serverName, String serviceName, boolean bPcServer) {
            this.ssid = "";
            this.inetAddr = inetAddr;
            this.port = port;
            this.serverName = serverName;
            this.serviceName = serviceName;
            this.bPcServer = bPcServer;
        }
    }

    public interface OnRecStateChangeListener {
        void onRecStateChanged(int newState, boolean isStill);
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
    private OnRecStateChangeListener mOnRecStateChangeListener = null;


    public void setOnConnectionChangeListener(OnConnectionChangeListener listener) {
        mOnConnectionChangeListener = listener;
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        mOnStateChangeListener = listener;
    }

    public void setOnRecStateChangeListener(OnRecStateChangeListener listener) {
        mOnRecStateChangeListener = listener;
    }

    private VdbConnection mVdbConnection;

    public VdbConnection getVdbConnection() {
        return mVdbConnection;
    }




    public VdtCamera(VdtCamera.ServiceInfo serviceInfo) {
        mServiceInfo = serviceInfo;
        mState.setOnStateChangeListener(new CameraState.OnStateChangeListener() {
            @Override
            public void onStateChange() {
                if (mOnStateChangeListener != null) {
                    mOnStateChangeListener.onStateChanged(VdtCamera.this);
                }
            }
        });
        mController = new VdtCameraController(serviceInfo.inetAddr, serviceInfo.port, mState);
        mController.setListener(new VdtCameraController.Listener() {
            @Override
            public void onConnected() {
                initCameraState();
                onCameraConnected();

            }

            @Override
            public void onDisconnected() {
                onCameraDisconnected();
            }

            @Override
            public void onRecStateChanged(int state, boolean isStill) {
                mRecordState = state;
                if (mOnRecStateChangeListener != null) {
                    mOnRecStateChangeListener.onRecStateChanged(state, isStill);
                }
            }

            @Override
            public void onRecDurationChanged(int duration) {

            }

            @Override
            public void onBtStateChanged() {
                syncBtState();

            }

            @Override
            public void onGpsStateChanged() {
                syncGpsState();

            }

            @Override
            public void onWifiStateChanged() {
                syncWifiState();

            }

            @Override
            public void onStartRecordError(final int error) {
                VdtCamera.this.onStartRecordError(error);
            }

            @Override
            public void onHostSSIDFetched(final String ssid) {
                VdtCamera.this.onHostSSIDFetched(ssid);
            }

            @Override
            public void onScanBtDone() {
                VdtCamera.this.onScanBtDone();
            }

            @Override
            public void onBtDevInfo(final int type, final String mac, final String name) {
                VdtCamera.this.onBtDevInfo(type, mac, name);
            }

            @Override
            public void onStillCaptureStarted(final boolean bOneShot) {
                VdtCamera.this.onStillCaptureStarted(bOneShot);
            }

            @Override
            public void onStillPictureInfo(final boolean bCapturing, final int numPictures,
                                           final int burstTicks) {
                VdtCamera.this.onStillPictureInfo(bCapturing, numPictures, burstTicks);
            }

            @Override
            public void onStillCaptureDone() {
                VdtCamera.this.onStillCaptureDone();
            }

            @Override
            public void onScanHostResult(List<NetworkItemBean> networkList) {
                if (mOnScanHostListener != null) {
                    mOnScanHostListener.OnScanHostResult(networkList);
                }
            }
        });
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

    public int getBatteryState() {
        return mState.getBatteryState();
    }

    public int getBatterVolume() {
        return mState.getBatteryVolume();
    }

    public InetAddress getAddress() {
        return mServiceInfo.inetAddr;
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



    public CameraState getState() {
        return mState;
    }

    public int getRecordState() {
        return mRecordState;
    }

    public BtState getBtStates() {
        return mBtStates;
    }

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
                    mOnConnectionChangeListener.onVdbConnected(VdtCamera.this);

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

        if (mOnConnectionChangeListener != null) {
            mOnConnectionChangeListener.onDisconnected(this);
        }
        mIsConnected = false;
    }



    // called on camera thread
    private void initCameraState() {
        mController.cmd_Cam_getApiVersion();
        mController.cmd_fw_getVersion();
        mController.cmd_fw_getVersion();
        mController.cmd_Cam_get_Name();
        mController.cmd_Rec_List_Resolutions(); // see if still capture is supported
        mController.cmd_Cam_get_getAllInfor();
        mController.cmd_Cam_get_State();
        mController.cmd_Network_GetWLanMode();
        mController.cmd_Network_GetHostNum();
        mController.cmd_Rec_GetMarkTime();

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

    }

    private void onHostSSIDFetched(String ssid) {

    }

    private void onScanBtDone() {
        mController.syncBtState(mBtStates);

    }

    private void onBtDevInfo(int type, String mac, String name) {

    }

    private void onStillCaptureStarted(boolean bOneShot) {

    }

    private void onStillPictureInfo(boolean bCapturing, int numPictures, int burst_ticks) {

    }

    private void onStillCaptureDone() {

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

    public void doBind(int type, String mac) {
        mController.cmd_CAM_BT_doBind(type, mac);
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

    public void setName(String name) {
        mController.cmd_Cam_set_Name(name);
    }

    public String getName() {
        mController.cmd_Cam_get_Name();
        return mState.getName();
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

    public void scanHost(OnScanHostListener listener) {
        mOnScanHostListener = listener;
        mController.cmd_Network_ScanHost();
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

    public void addNetworkHost(String ssid, String password) {
        mController.cmd_Network_AddHost(ssid, password);
    }

    public boolean isMicEnabled() {
        int micState = mState.getMicState();
        return micState == CameraState.STATE_MIC_ON;
    }

    public static class StorageInfo {
        public int totalSpace;
        public int freeSpace;
    }

    public StorageInfo getStorageInfo() {
        StorageInfo storageInfo = new StorageInfo();
        storageInfo.totalSpace = (int)(mState.getStorageTotalSpace() / 1024);
        storageInfo.freeSpace = (int)(mState.getStorageFreeSpace() / 1024);
        return storageInfo;
    }


    public interface OnScanHostListener {
        void OnScanHostResult(List<NetworkItemBean> networkList);
    }

}
