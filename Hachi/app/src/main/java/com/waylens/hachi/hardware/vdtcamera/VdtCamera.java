package com.waylens.hachi.hardware.vdtcamera;

import android.util.Log;
import android.util.Xml;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbConnection;
import com.waylens.hachi.ui.entities.NetworkItemBean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class VdtCamera {
    private static final String TAG = VdtCamera.class.getSimpleName();

    public static final int STATE_MIC_UNKNOWN = -1;
    public static final int STATE_MIC_ON = 0;
    public static final int STATE_MIC_OFF = 1;

    public static final int STATE_BATTERY_UNKNOWN = -1;
    public static final int STATE_BATTERY_FULL = 0;
    public static final int STATE_BATTERY_NOT_CHARGING = 1;
    public static final int STATE_BATTERY_DISCHARGING = 2;
    public static final int STATE_BATTERY_CHARGING = 3;

    public static final int STATE_POWER_UNKNOWN = -1;
    public static final int STATE_POWER_NO = 0;
    public static final int STATE_POWER_YES = 1;

    public static final int STATE_RECORD_UNKNOWN = -1;
    public static final int STATE_RECORD_STOPPED = 0;
    public static final int STATE_RECORD_STOPPING = 1;
    public static final int STATE_RECORD_STARTING = 2;
    public static final int STATE_RECORD_RECORDING = 3;
    public static final int STATE_RECORD_SWITCHING = 4;

    public static final int STATE_STORAGE_UNKNOWN = -1;
    public static final int STATE_STORAGE_NO_STORAGE = 0;
    public static final int STATE_STORAGE_LOADING = 1;
    public static final int STATE_STORAGE_READY = 2;
    public static final int STATE_STORAGE_ERROR = 3;
    public static final int STATE_STORAGE_USBDISC = 4;

    public static final int OVERLAY_FLAG_NAME = 0x01;
    public static final int OVERLAY_FLAG_TIME = 0x02;
    public static final int OVERLAY_FLAG_GPS = 0x04;
    public static final int OVERLAY_FLAG_SPEED = 0x08;

    public static final int VIDEO_RESOLUTION_UNKNOWN = -1;
    public static final int VIDEO_RESOLUTION_1080P30 = 0;
    public static final int VIDEO_RESOLUTION_1080P60 = 1;
    public static final int VIDEO_RESOLUTION_720P30 = 2;
    public static final int VIDEO_RESOLUTION_720P60 = 3;
    public static final int VIDEO_RESOLUTION_4KP30 = 4;
    public static final int VIDEO_RESOLUTION_4KP60 = 5;
    public static final int VIDEO_RESOLUTION_480P30 = 6;
    public static final int VIDEO_RESOLUTION_480P60 = 7;
    public static final int VIDEO_RESOLUTION_720P120 = 8;
    public static final int VIDEO_RESOLUTION_STILL = 9;
    public static final int VIDEO_RESOLUTION_NUM = 10;

    public static final int VIDEO_RESOLUTION_720P = 0;
    public static final int VIDEO_RESOLUTION_1080P = 1;

    public static final int VIDEO_FRAMERATE_30FPS = 0;
    public static final int VIDEO_FRAMERATE_60FPS = 1;
    public static final int VIDEO_FRAMERATE_120FPS = 2;

    public static final int VIDEO_QUALITY_UNKNOWN = -1;
    public static final int VIDEO_QUALITY_SUPPER = 0;
    public static final int VIDEO_QUALITY_HI = 1;
    public static final int VIDEO_QUALITY_MID = 2;
    public static final int VIDEO_QUALITY_LOW = 3;
    public static final int VIDEO_QUALITY_NUM = 4;

    public static final int REC_MODE_UNKNOWN = -1;
    public static final int FLAG_AUTO_RECORD = 1 << 0;
    public static final int FLAG_LOOP_RECORD = 1 << 1;
    public static final int REC_MODE_MANUAL = 0;
    public static final int REC_MODE_AUTOSTART = FLAG_AUTO_RECORD;
    public static final int REC_MODE_MANUAL_LOOP = FLAG_LOOP_RECORD;
    public static final int REC_MODE_AUTOSTART_LOOP = (FLAG_AUTO_RECORD | FLAG_LOOP_RECORD);

    public static final int COLOR_MODE_UNKNOWN = -1;
    public static final int COLOR_MODE_NORMAL = 0;
    public static final int COLOR_MODE_SPORT = 1;
    public static final int COLOR_MODE_CARDV = 2;
    public static final int COLOR_MODE_SCENE = 3;
    public static final int COLOR_MODE_NUM = 4;

    public static final int ERROR_START_RECORD_OK = 0;
    public static final int ERROR_START_RECORD_NO_CARD = 1;
    public static final int ERROR_START_RECORD_CARD_FULL = 2;
    public static final int ERROR_START_RECORD_CARD_ERROR = 3;


    public static final int WIFI_MODE_UNKNOWN = -1;
    public static final int WIFI_MODE_AP = 0;
    public static final int WIFI_MODE_CLIENT = 1;
    public static final int WIFI_MODE_OFF = 2; //

    private boolean mIsConnected = false;
    private boolean mIsVdbConnected = false;

    private String mCameraName = new String();
    private String mFirmwareVersion = new String();
    private int mApiVersion = 0;
    private String mBuild = new String();

    private int mMicState = STATE_MIC_UNKNOWN;
    private int mMicVol = -1;

    private int mBatteryState = STATE_BATTERY_UNKNOWN;
    private int mPowerState = STATE_POWER_UNKNOWN;
    private int mBatteryVol = -1;

    private int mStorageState = STATE_STORAGE_UNKNOWN;
    private long mStorageTotalSpace = 0;
    private long mStorageFreeSpace = 0;

    private int mRecordState = STATE_RECORD_UNKNOWN;
    private int mRecordTime = 0;

    private int mOverlayFlags = -1;

    private int mVideoResolutionList = 0;
    private int mVideoResolutionIndex = VIDEO_RESOLUTION_UNKNOWN;

    private int mVideoQualityList = 0;
    private int mVideoQualityIndex = VIDEO_QUALITY_UNKNOWN;

    private int mRecordModeList = 0;
    private int mRecordModeIndex = REC_MODE_UNKNOWN;

    private int mColorModeList = 0;
    private int mColorModeIndex = COLOR_MODE_UNKNOWN;

    private int mMarkBeforeTime = -1;
    private int mMarkAfterTime = -1;

    private int mWifiMode = WIFI_MODE_UNKNOWN;
    public int mNumWifiAP = 0;


    private final ServiceInfo mServiceInfo;
    private final VdtCameraController mController;

    private InetSocketAddress mPreviewAddress;


    private BtState mBtStates = new BtState();
    private GpsState mGpsStates = new GpsState();

    private OnScanHostListener mOnScanHostListener;



    public static class ServiceInfo {
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

    public interface OnRecStateChangeListener {
        void onRecStateChanged(int newState, boolean isStill);

        void onRecDurationChanged(int duration);

        void onRecError(int error);
    }

    public interface OnConnectionChangeListener {
        void onConnected(VdtCamera vdtCamera);

        void onVdbConnected(VdtCamera vdtCamera);

        void onDisconnected(VdtCamera vdtCamera);
    }

    public interface OnStateChangeListener {

        void onBtStateChanged(VdtCamera vdtCamera);

        void onGpsStateChanged(VdtCamera vdtCamera);

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

        mController = new VdtCameraController(serviceInfo.inetAddr, serviceInfo.port);

    }

    public void setCameraName(String name) {
        if (name.equals("No Named")) {
            // use empty string for unnamed camera
            name = "";
        }
        if (!mCameraName.equals(name)) {
            Logger.t(TAG).d("setCameraName: " + name);
            mCameraName = name;

        }
    }

    public String versionString() {
        int main = (mApiVersion >> 16) & 0xff;
        int sub = mApiVersion & 0xffff;
        return String.format(Locale.US, "%d.%d.%s", main, sub, mBuild);
    }

    public void setFirmwareVersion(String version) {
        if (!mFirmwareVersion.equals(version)) {
            Logger.t(TAG).d("setFirmwareVersion: " + version);

            mFirmwareVersion = version;

        }
    }
    public boolean version12() {
        return mApiVersion >= makeVersion(1, 2);
    }

    private int makeVersion(int main, int sub) {
        return (main << 16) | sub;
    }

    public void setApiVersion(int main, int sub, String build) {
        int version = makeVersion(main, sub);
        if (mApiVersion != version || !mBuild.equals(build)) {
            Logger.t(TAG).d("setApiVersion: " + version);
            mApiVersion = version;
            mBuild = build;

        }
    }

    // API
    public boolean isPcServer() {
        return mServiceInfo.bPcServer;
    }


    public boolean isConnected() {
        return mIsConnected && mIsVdbConnected;
    }


    public int getBatteryState() {
        return mBatteryState;
    }

    public int getBatteryVolume() {
        return mBatteryVol;
    }

    public int getStorageState() {
        return mStorageState;
    }

    public long getStorageTotalSpace() {
        return mStorageTotalSpace;
    }

    public long getStorageFreeSpace() {
        return mStorageFreeSpace;
    }

    public String getServerName() {
        return mServiceInfo.serverName;
    }

    public int getMicState() {
        return mMicState;
    }

    public int getWifiMode() {
        return mWifiMode;
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




    public int getRecordState() {
        return mRecordState;
    }

    public BtState getBtStates() {
        return mBtStates;
    }


    public int getVideoResolution() {
        switch (mVideoQualityIndex) {
            case VIDEO_RESOLUTION_1080P30:
            case VIDEO_RESOLUTION_1080P60:

                return VIDEO_RESOLUTION_1080P;
            default:
                return VIDEO_RESOLUTION_720P;
        }
    }

    public int getVideoFramerate() {
        switch (mVideoQualityIndex) {
            case VIDEO_RESOLUTION_1080P30:
            case VIDEO_RESOLUTION_4KP30:
            case VIDEO_RESOLUTION_480P30:
            case VIDEO_RESOLUTION_720P30:
                return VIDEO_FRAMERATE_30FPS;

            case VIDEO_RESOLUTION_1080P60:
            case VIDEO_RESOLUTION_720P60:
            case VIDEO_RESOLUTION_4KP60:
            case VIDEO_RESOLUTION_480P60:
                return VIDEO_FRAMERATE_60FPS;


            case VIDEO_RESOLUTION_720P120:
                return VIDEO_FRAMERATE_120FPS;
            default:
                return VIDEO_FRAMERATE_30FPS;

        }
    }







    public int getRecordMode() {
        return mRecordModeIndex;
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


    private void onScanBtDone() {
        mController.syncBtState(mBtStates);

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
        return mCameraName;
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

    public int getRecordTime() {
        mController.ack_Cam_get_time();
        return mRecordTime;
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
        int micState = getMicState();
        return micState == STATE_MIC_ON;
    }

    public static class StorageInfo {
        public int totalSpace;
        public int freeSpace;
    }

    public StorageInfo getStorageInfo() {
        StorageInfo storageInfo = new StorageInfo();
        storageInfo.totalSpace = (int) (getStorageTotalSpace() / 1024);
        storageInfo.freeSpace = (int) (getStorageFreeSpace() / 1024);
        return storageInfo;
    }


    public interface OnScanHostListener {
        void OnScanHostResult(List<NetworkItemBean> networkList);
    }


    class VdtCameraController implements VdtCameraCmdConsts {

        private final BtState mBtStates = new BtState();
        private final GpsState mGpsStates = new GpsState();



        public VdtCameraController(InetAddress host, int port) {
            InetSocketAddress address = new InetSocketAddress(host, port);
            mConnection = new MyTcpConnection("ccam", address);
        }


        public boolean syncBtState(BtState user) {
            return mBtStates.syncStates(user);
        }

        public boolean syncGpsState(GpsState user) {
            return mGpsStates.syncStates(user);
        }



        public InetSocketAddress getInetSocketAddress() {
            return getConnection().getInetSocketAddress();
        }


        private class Request {
            final int mDomain;
            final int mCmd;
            final String mP1;
            final String mP2;

            Request(int domain, int cmd, String p1, String p2) {
                mDomain = domain;
                mCmd = cmd;
                mP1 = p1;
                mP2 = p2;
            }


        }

        static final int HEAD_SIZE = 128;

        static final String XML_CCEV = "ccev";
        static final String XML_CMD = "cmd";
        static final String XML_ACT = "act";
        static final String XML_P1 = "p1";
        static final String XML_P2 = "p2";

        private final TcpConnection mConnection;

        private final BlockingQueue<Request> mCameraRequestQueue = new LinkedBlockingQueue<>();

        private void postRequest(int domain, int cmd) {
            postRequest(domain, cmd, "", "");
        }

        private void postRequest(int domain, int cmd, int p1) {
            postRequest(domain, cmd, Integer.toString(p1), "");
        }


        private void postRequest(int domain, int cmd, String p1, String p2) {
            Request request = new Request(domain, cmd, p1, p2);
            postRequest(request);
        }

        private void postRequest(Request request) {
            mCameraRequestQueue.add(request);
        }

        // all info for setup
        public void userCmd_GetSetup() {
            postRequest(CMD_DOMAIN_USER, USER_CMD_GET_SETUP);
        }

        public void userCmd_ExitThread() {
            postRequest(CMD_DOMAIN_USER, USER_CMD_EXIT_THREAD);
        }

        public void cmd_Cam_getMode() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_GET_MODE);
        }

        public void cmd_Cam_getApiVersion() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_GET_API_VERSION);
        }

        private void ack_Cam_getApiVersion(String p1) {
            int main = 0, sub = 0;
            String build = "";
            int i_main = p1.indexOf('.', 0);
            if (i_main >= 0) {
                String t = p1.substring(0, i_main);
                main = Integer.parseInt(t);
                i_main++;
                int i_sub = p1.indexOf('.', i_main);
                if (i_sub >= 0) {
                    t = p1.substring(i_main, i_sub);
                    sub = Integer.parseInt(t);
                    i_sub++;
                    build = p1.substring(i_sub);
                }
            }
            setApiVersion(main, sub, build);
        }


        public void cmd_Cam_get_Name() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_GET_NAME);
        }

        private void ack_Cam_get_Name_result(String p1, String p2) {
            setCameraName(p1);
        }

        public void cmd_Cam_set_Name(String name) {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_SET_NAME, name, "");
        }


        public void cmd_Cam_get_State() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_GET_STATE);
        }


        private void ack_Cam_get_State_result(String p1, String p2) {
            int state = Integer.parseInt(p1);
            boolean is_still = p2.length() > 0 ? Integer.parseInt(p2) != 0 : false;
            mRecordState = state;
            if (mOnRecStateChangeListener != null) {
                mOnRecStateChangeListener.onRecStateChanged(state, is_still);
            }

        }


        public void cmd_Cam_start_rec() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_START_REC);
        }

        public void ack_Cam_stop_rec() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_STOP_REC);
        }

        public void ack_Cam_get_time() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_GET_TIME);
        }


        private void ack_Cam_get_time_result(String p1, String p2) {
            int duration = Integer.parseInt(p1);
            mRecordTime = duration;
            if (mOnRecStateChangeListener != null) {
                mOnRecStateChangeListener.onRecDurationChanged(duration);
            }

        }

        public void cmd_Cam_get_getAllInfor() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_GET_GET_ALL_INFOR);
        }

        public void cmd_Cam_get_getStorageInfor() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_GET_GET_STORAGE_INFOR);
        }


        private void ack_Cam_msg_Storage_infor(String p1, String p2) {
            mStorageState = Integer.parseInt(p1);
        }

        private void ack_Cam_msg_StorageSpace_infor(String p1, String p2) {
            long totalSpace = p1.length() > 0 ? Long.parseLong(p1) : 0;
            long freeSpace = p2.length() > 0 ? Long.parseLong(p2) : 0;

            mStorageTotalSpace = totalSpace;
            mStorageFreeSpace = freeSpace;
        }


        private void ack_Cam_msg_Battery_infor(String p1, String p2) {
            int vol = Integer.parseInt(p2);
            mBatteryVol = vol;
        }

        private void ack_Cam_msg_power_infor(String p1, String p2) {
            if (p1.length() == 0 || p2.length() == 0) {
                Logger.t(TAG).d("bad power info, schedule update");

            } else {
                int batteryState = STATE_BATTERY_UNKNOWN;
                if (p1.equals("Full")) {
                    batteryState = STATE_BATTERY_FULL;
                } else if (p1.equals("Not charging")) {
                    batteryState = STATE_BATTERY_NOT_CHARGING;
                } else if (p1.equals("Discharging")) {
                    batteryState = STATE_BATTERY_DISCHARGING;
                } else if (p1.equals("Charging")) {
                    batteryState = STATE_BATTERY_CHARGING;
                }
                int powerState = Integer.parseInt(p2);
                mBatteryState = batteryState;
                mPowerState = powerState;
            }
        }


        private void ack_Cam_msg_GPS_infor(String p1, String p2) {
            int state = Integer.parseInt(p1);
            mGpsStates.setGpsState(state);
        }

        private void ack_Cam_msg_Mic_infor(String p1, String p2) {
            int state = Integer.parseInt(p1);
            int vol = Integer.parseInt(p2);
            mMicState = state;
            mMicVol = vol;

        }


        public void cmd_Cam_PowerOff() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_POWER_OFF);
        }

        public void cmd_Cam_Reboot() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_REBOOT);
        }


        public void cmd_Network_GetWLanMode() {
            postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_GET_WLAN_MODE);
        }

        private void ack_Network_GetWLanMode(String p1, String p2) {
            int mode = Integer.parseInt(p1);
            mWifiMode = mode;
        }


        public void cmd_Network_GetHostNum() {
            postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_GET_HOST_NUM);
        }

        private void ack_Network_GetHostNum(String p1, String p2) {
            int num = Integer.parseInt(p1);
            mNumWifiAP = num;
            for (int i = 0; i < num; i++) {
                cmd_Network_GetHostInfor(i);
            }
        }


        public void cmd_Network_GetHostInfor(int index) {
            postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_GET_HOST_INFOR, index);
        }

        private void ack_Network_GetHostInfor(String p1, String p2) {
            // TODO:
//            if (mListener != null) {
//                mListener.onHostSSIDFetched(p1);
//            }
        }

        public void cmd_Network_AddHost(String hostName, String password) {
            postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_ADD_HOST, hostName, password);
            cmd_Network_GetHostNum();
        }


        public void cmd_Network_RmvHost(String hostName) {
            postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_RMV_HOST, hostName, "");
            cmd_Network_GetHostNum();
        }

        public void cmd_Network_ConnectHost(int mode, String apName) {
            if (apName == null)
                apName = "";
            postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_CONNECT_HOST, Integer.toString(mode), apName);
        }

        public void cmd_Network_Synctime(long time, int timezone) {
            String p1 = Long.toString(time);
            String p2 = Integer.toString(timezone);
            postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_SYNCTIME, p1, p2);
        }

        public void cmd_Network_ScanHost() {
            postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_SCANHOST);
        }


        private void ack_Rec_error(String p1, String p2) {
            int error = Integer.parseInt(p1);
            if (mOnRecStateChangeListener != null) {
                mOnRecStateChangeListener.onRecError(error);
            }
        }

        public void cmd_audio_setMic(int state, int gain) {
            if (state == STATE_MIC_ON && gain == 0)
                gain = 5;
            String p1 = Integer.toString(state);
            String p2 = Integer.toString(gain);
            postRequest(CMD_DOMAIN_CAM, CMD_AUDIO_SET_MIC, p1, p2);
        }

        public void cmd_audio_getMicState() {
            postRequest(CMD_DOMAIN_CAM, CMD_AUDIO_GET_MIC_STATE);
        }

        public void cmd_fw_getVersion() {
            postRequest(CMD_DOMAIN_CAM, CMD_FW_GET_VERSION);
        }

        private void ack_fw_getVersion(String p1, String p2) {
            setFirmwareVersion(p2);
        }

        public void cmd_CAM_BT_isSupported() {
            if (version12() && mBtStates.mBtSupport == BtState.BT_SUPPORT_UNKNOWN) {
                postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_IS_SUPPORTED);
            }
        }

        private void ack_CAM_BT_isSupported(String p1) {
            mBtStates.setIsBTSupported(Integer.parseInt(p1));
        }

        public void cmd_CAM_BT_isEnabled() {
            if (version12()) {
                postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_IS_ENABLED);
            }
        }

        private void ack_CAM_BT_isEnabled(String p1) {
            int enabled = Integer.parseInt(p1);
            mBtStates.setIsBTEnabled(enabled);
            if (enabled == BtState.BT_STATE_ENABLED) {
                cmd_CAM_BT_getDEVStatus(BtState.BT_TYPE_HID);
                cmd_CAM_BT_getDEVStatus(BtState.BT_TYPE_OBD);
            }
        }

        public void cmd_CAM_BT_Enable(boolean bEnable) {
            if (version12()) {
                postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_ENABLE, bEnable ? 1 : 0);
            }
        }

        public void cmd_CAM_BT_getDEVStatus(int type) {
            if (version12()) {
                postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_GET_DEV_STATUS, type);
            }
        }

        private void ack_CAM_BT_getDEVStatus(String p1, String p2) {
            int i_p1 = Integer.parseInt(p1);
            int dev_type = (i_p1 >> 8) & 0xff;
            int dev_state = i_p1 & 0xff;
            String mac = "";
            String name = "";
            int index = p2.indexOf('#');
            if (index >= 0) {
                mac = p2.substring(0, index);
                name = p2.substring(index + 1);
            }
            if (mac.equals("NA")) {
                //cmd_CAM_BT_getDEVStatus(dev_type);
                //return;
                dev_state = BtState.BTDEV_STATE_OFF;
                mac = "";
                name = "";
            }
            mBtStates.setDevState(dev_type, dev_state, mac, name);
        }

        public void cmd_CAM_BT_getHostNum() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_GET_HOST_NUM);
        }

        private void ack_CAM_BT_getHostNum(String p1) {
            int numDevs = Integer.parseInt(p1);
            if (numDevs < 0) {
                numDevs = 0;
            }
            mBtStates.setNumDevs(numDevs);
            //onBtDevInfo(BtState.BT_TYPE_HID, "11:D6:00:BB:71:58", "Smart Shutter");
            //onBtDevInfo(BtState.BT_TYPE_OBD, "11:D6:00:BB:71:59", "OBD device");
            //onBtDevInfo(-1, "11:D6:00:BB:71:60", "Smart Shutter 3");
            for (int i = 0; i < numDevs; i++) {
                cmd_CAM_BT_getHostInfor(i);
            }
        }

        public void cmd_CAM_BT_getHostInfor(int index) {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_GET_HOST_INFOR, index);
        }

        // p1: name; p2: mac
        private void ack_CAM_BT_getHostInfor(String p1, String p2) {
            int type = p1.indexOf("OBD") >= 0 ? BtState.BT_TYPE_OBD : BtState.BT_TYPE_HID;
//            if (mListener != null) {
//                mListener.onBtDevInfo(type, p2, p1);
//            }

        }


        public void cmd_CAM_BT_doScan() {
            if (version12()) {
                postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_DO_SCAN);
            }
        }

        private void ack_CAM_BT_doScan() {
            mBtStates.scanBtDone();
//            if (mListener != null) {
//                mListener.onScanBtDone();
//            }
        }

        public void cmd_CAM_BT_doBind(int type, String mac) {
            Log.d(TAG, "cmd_CAM_BT_doBind, type=" + type + ", mac=" + mac);
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_DO_BIND, Integer.toString(type), mac);
        }

        private void ack_CAM_BT_doBind(String p1, String p2) {
            int type = Integer.parseInt(p1);
            int result = Integer.parseInt(p2);
            if (result == 0) {
                if (type == BtState.BT_TYPE_HID || type == BtState.BT_TYPE_OBD) {
                    postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_GET_DEV_STATUS, type);
                }
            }
        }

        public void cmd_CAM_BT_doUnBind(int type, String mac) {
            Log.d(TAG, "cmd_CAM_BT_doUnBind, type=" + type + ", mac=" + mac);
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_DO_UNBIND, Integer.toString(type), mac);
        }

        private void ack_CAM_BT_doUnBind(String p1, String p2) {
            int type = Integer.parseInt(p1);
            if (type == BtState.BT_TYPE_HID || type == BtState.BT_TYPE_OBD) {
                postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_GET_DEV_STATUS, type);
            }
        }

        public void cmd_CAM_BT_setOBDTypes() {
        }

        public void cmd_CAM_WantIdle() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_WANT_IDLE);
        }


        public void cmd_CAM_WantPreview() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_WANT_PREVIEW);
        }


        public void cmd_Rec_List_Resolutions() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_LIST_RESOLUTIONS);
        }

        private void ack_Rec_List_Resolutions(String p1, String p2) {
            int list = Integer.parseInt(p1);
            mVideoResolutionList = list;
        }

        public void cmd_Rec_Set_Resolution(int index) {
            postRequest(CMD_DOMAIN_REC, CMD_REC_SET_RESOLUTION, index);
        }


        public void cmd_Rec_get_Resolution() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_GET_RESOLUTION);
        }

        private void ack_Rec_get_Resolution(String p1, String p2) {
            int index = Integer.parseInt(p1);
            mVideoResolutionIndex = index;
        }


        public void cmd_Rec_List_Qualities() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_LIST_QUALITIES);
        }

        private void ack_Rec_List_Qualities(String p1, String p2) {
            int list = Integer.parseInt(p1);
            mVideoQualityList = list;
        }

        // ========================================================
        // CMD_REC_SET_QUALITY
        // ========================================================
        public void cmd_Rec_Set_Quality(int index) {
            postRequest(CMD_DOMAIN_REC, CMD_REC_SET_QUALITY, index);
        }

        // ========================================================
        // CMD_REC_GET_QUALITY
        // ========================================================
        public void cmd_Rec_get_Quality() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_GET_QUALITY);
        }

        private void ack_Rec_get_Quality(String p1, String p2) {
            int index = Integer.parseInt(p1);
            mVideoQualityIndex = index;
        }

        // ========================================================
        // CMD_REC_LIST_REC_MODES
        // ========================================================
        public void cmd_Rec_List_RecModes() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_LIST_REC_MODES);
        }

        private void ack_Rec_List_RecModes(String p1, String p2) {
            int list = Integer.parseInt(p1);
            mRecordModeList = list;
        }


        public void cmd_Rec_Set_RecMode(int index) {
            postRequest(CMD_DOMAIN_REC, CMD_REC_SET_REC_MODE, index);
        }


        public void cmd_Rec_get_RecMode() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_GET_REC_MODE);
        }

        private void ack_Rec_get_RecMode(String p1, String p2) {
            int index = Integer.parseInt(p1);
            mRecordModeIndex = index;
        }


        public void cmd_Rec_List_ColorModes() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_LIST_COLOR_MODES);
        }

        private void ack_Rec_List_ColorModes(String p1, String p2) {
            int list = Integer.parseInt(p1);
            mColorModeList = list;
        }

        public void cmd_Rec_Set_ColorMode(int index) {
            postRequest(CMD_DOMAIN_REC, CMD_REC_SET_COLOR_MODE, index);
        }

        // ========================================================
        // CMD_REC_GET_COLOR_MODE
        // ========================================================
        public void cmd_Rec_get_ColorMode() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_GET_COLOR_MODE);
        }

        private void ack_Rec_get_ColorMode(String p1, String p2) {
            int index = Integer.parseInt(p1);
            mColorModeIndex = index;
        }


        public void cmd_Rec_setOverlay(int flags) {
            postRequest(CMD_DOMAIN_REC, CMD_REC_SET_OVERLAY, flags);
        }

        // ========================================================
        // CMD_REC_GET_OVERLAY_STATE
        // ========================================================
        public void cmd_Rec_getOverlayState() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_GET_OVERLAY_STATE);
        }

        private void ack_Rec_getOverlayState(String p1, String p2) {
            int flags = Integer.parseInt(p1);
            mOverlayFlags = flags;
        }

        // ========================================================
        // CMD_REC_SET_STILL_MODE
        // ========================================================
        public void cmd_Rec_SetStillMode(boolean bStillMode) {
            postRequest(CMD_DOMAIN_REC, CMD_REC_SET_STILL_MODE, bStillMode ? 1 : 0);
        }

        // ========================================================
        // CMD_REC_START_STILL_CAPTURE
        // ========================================================
        public void cmd_Rec_StartStillCapture(boolean bOneShot) {
            postRequest(CMD_DOMAIN_REC, CMD_REC_START_STILL_CAPTURE, bOneShot ? 1 : 0);
        }

        private void ack_Rec_StartStillCapture(String p1, String p2) {
            boolean bOneShot = p1.length() > 0 ? Integer.parseInt(p1) != 0 : false;
//            if (mListener != null) {
//                mListener.onStillCaptureStarted(bOneShot);
//            }
        }


        public void cmd_Rec_StopStillCapture() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_STOP_STILL_CAPTURE);
        }


        private void ack_Rec_StillPictureInfo(String p1, String p2) {
            int value_p1 = p1.length() > 0 ? Integer.parseInt(p1) : 0;
            boolean bCapturing = (value_p1 & 1) != 0;
            int numPictures = p2.length() > 0 ? Integer.parseInt(p2) : 0;
            int burstTicks = value_p1 >>> 1;
//            if (mListener != null) {
//                mListener.onStillPictureInfo(bCapturing, numPictures, burstTicks);
//            }
        }


        private void ack_Rec_StillCaptureDone() {
//            if (mListener != null) {
//                mListener.onStillCaptureDone();
//            }
        }

        /**
         * This cmd does not have response.
         */
        public void cmd_Rec_MarkLiveVideo() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_MARK_LIVE_VIDEO);
        }

        public void cmd_Rec_GetMarkTime() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_GET_MARK_TIME);
        }

        private void ack_Rec_GetMarkTime(String p1, String p2) {
            Logger.t(TAG).d(String.format("cmd_Rec_GetMarkTime: p1: %s, p2: %s", p1, p2));
            try {
                mMarkBeforeTime = Integer.parseInt(p1);
                mMarkAfterTime = Integer.parseInt(p2);
            } catch (Exception e) {
                Logger.t(TAG).d(String.format("cmd_Rec_GetMarkTime: p1: %s, p2: %s", p1, p2), e);
            }
        }

        public void cmd_Rec_SetMarkTime(int before, int after) {
            if (before < 0 || after < 0) {
                return;
            }
            postRequest(CMD_DOMAIN_REC, CMD_REC_SET_MARK_TIME,
                String.valueOf(before), String.valueOf(after));
        }

        private void ack_Rec_SetMarkTime(String p1, String p2) {
            Logger.t(TAG).d(String.format("ack_Rec_SetMarkTime: p1: %s, p2: %s", p1, p2));
            try {
                mMarkBeforeTime = Integer.parseInt(p1);
                mMarkAfterTime = Integer.parseInt(p2);
            } catch (Exception e) {
                Logger.t(TAG).d(String.format("ack_Rec_SetMarkTime: p1: %s, p2: %s", p1, p2), e);
            }
        }


        // ========================================================


        public void start() {
            mConnection.start(null);
        }

        // API
        public void stop() {
            mConnection.stop();
        }

        // API
        public TcpConnection getConnection() {
            return mConnection;
        }

        private boolean createUserCmd(Request request) {
            switch (request.mCmd) {
                case USER_CMD_GET_SETUP:

                    this.cmd_Cam_get_getAllInfor();

                    this.cmd_Rec_List_Resolutions();
                    this.cmd_Rec_get_Resolution();

                    this.cmd_Rec_List_Qualities();
                    this.cmd_Rec_get_Quality();

                    this.cmd_Rec_List_ColorModes();
                    this.cmd_Rec_get_ColorMode();

                    this.cmd_Rec_List_RecModes();
                    this.cmd_Rec_get_RecMode();

                    this.cmd_audio_getMicState();
                    this.cmd_Rec_getOverlayState();

                    this.cmd_Network_GetWLanMode();
                    this.cmd_CAM_BT_isSupported();
                    this.cmd_CAM_BT_isEnabled();
                    this.cmd_Rec_GetMarkTime();
                    break;

                case USER_CMD_EXIT_THREAD:
                    return false;

                default:
                    Logger.t(TAG).d("unknown user cmd " + request.mCmd);
                    break;
            }

            return true;
        }


        private void writeRequest(Request request) throws IOException, InterruptedException {
            SimpleOutputStream sos = new SimpleOutputStream(1024);
            XmlSerializer xml = Xml.newSerializer();

            sos.reset();
            sos.writeZero(8);

            xml.setOutput(sos, "UTF-8");
            xml.startDocument("UTF-8", true);
            xml.startTag(null, XML_CCEV);

            xml.startTag(null, XML_CMD);
            String act = String.format(Locale.US, "ECMD%1$d.%2$d", request.mDomain, request.mCmd); // TODO : why US

            xml.attribute(null, XML_ACT, act);
            xml.attribute(null, XML_P1, request.mP1);
            xml.attribute(null, XML_P2, request.mP2);
            xml.endTag(null, XML_CMD);

            xml.endTag(null, XML_CCEV);
            xml.endDocument();


            int size = sos.getSize();
            if (size >= HEAD_SIZE) {
                sos.writei32(0, size);
                sos.writei32(4, size - HEAD_SIZE);
            } else {
                sos.writei32(0, HEAD_SIZE);
                // append is 0
                sos.clear(size, HEAD_SIZE - size);
                size = HEAD_SIZE;
            }

            mConnection.sendByteArray(sos.getBuffer(), 0, size);
        }

        private void cmdLoop(Thread thread) throws IOException, InterruptedException {
            while (!thread.isInterrupted()) {
                Request request = mCameraRequestQueue.take();
                if (request.mDomain == CMD_DOMAIN_USER) {
                    if (!createUserCmd(request)) {
                        break;
                    }
                    continue;
                }


                writeRequest(request);

            }
        }

        private final Pattern mPattern = Pattern.compile("ECMD(\\d+).(\\d+)", Pattern.CASE_INSENSITIVE
            | Pattern.MULTILINE);

        private void parseCmdTag(XmlPullParser xpp) {
            int count = xpp.getAttributeCount();
            if (count >= 1) {
                String act = "";
                String p1 = "";
                String p2 = "";
                if (xpp.getAttributeName(0).equals(XML_ACT)) {
                    act = xpp.getAttributeValue(0);
                }
                if (count >= 2) {
                    if (xpp.getAttributeName(1).equals(XML_P1)) {
                        p1 = xpp.getAttributeValue(1);
                    }
                    if (count >= 3) {
                        if (xpp.getAttributeName(2).equals(XML_P2)) {
                            p2 = xpp.getAttributeValue(2);
                        }
                    }
                }


                // ECMD0.5
                Matcher matcher = mPattern.matcher(act);
                if (matcher.find() && matcher.groupCount() == 2) {
                    int domain = Integer.parseInt(matcher.group(1));
                    int cmd = Integer.parseInt(matcher.group(2));

                    switch (domain) {
                        case CMD_DOMAIN_CAM:
//                        Logger.t(TAG).d("Domain = " + DOMAIN_TO_STRING.get(domain)
//                            + " cmd = " + CMD_CAM_TO_STRING.get(cmd));
                            camDomainMsg(cmd, p1, p2);
                            break;
                        case CMD_DOMAIN_REC:
//                        Logger.t(TAG).d("Domain = " + DOMAIN_TO_STRING.get(domain)
//                            + " cmd =" + CMD_REC_TO_STRING.get(cmd));
                            recDomainMsg(cmd, p1, p2);
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        private final void msgLoop(Thread thread) throws IOException, InterruptedException {

            SimpleInputStream sis = new SimpleInputStream(8192);
            XmlPullParser xpp = Xml.newPullParser();
            int length = 0;
            int appended = 0;

            try {

                while (!thread.isInterrupted()) {

                    mGpsStates.mbSchedule = false;

                    mBtStates.mbSchedule = false;

                    sis.clear();

                    mConnection.readFully(sis.getBuffer(), 0, HEAD_SIZE);
                    length = sis.readi32(0);
                    appended = sis.readi32(4);
                    if (appended > 0) {
                        sis.expand(HEAD_SIZE + appended);
                        mConnection.readFully(sis.getBuffer(), HEAD_SIZE, appended);
                    }
                    sis.setRange(8, length);

                    xpp.setInput(sis, "UTF-8");


                    int eventType = xpp.getEventType();
                    while (!thread.isInterrupted()) {
                        switch (eventType) {
                            case XmlPullParser.START_TAG:
                                if (xpp.getName().equals(XML_CMD)) {
                                    parseCmdTag(xpp);
                                }
                                break;
                            default:
                                break;
                        }
                        if (eventType == XmlPullParser.END_DOCUMENT) {
                            break;
                        }
                        eventType = xpp.next();
                    }

                    if (mGpsStates.mbSchedule) {
                        mGpsStates.mbSchedule = false;
//                    mQueue.scheduleUpdate(Queue.SCHEDULE_GPS_UPDATE);
                    }

                    if (mBtStates.mbSchedule) {
                        mBtStates.mbSchedule = false;
//                    mQueue.scheduleUpdate(Queue.SCHEDULE_BT_UPDATE);
                    }
                }

            } catch (XmlPullParserException e) {

                Logger.t(TAG).d("XmlPullParserException: length=" + length + ", appended=" +
                    appended);
                e.printStackTrace();
                throw new IOException("XmlPullParserException");
            }
        }

        private void ackNotHandled(String name, String p1, String p2) {

            //Logger.t(TAG).d("not handled: " + name + ", p1=" + p1 + ",p2=" + p2);

        }

        private void camDomainMsg(int cmd, String p1, String p2) {
            switch (cmd) {
                case CMD_CAM_GET_MODE:
                    ackNotHandled("CMD_CAM_GET_MODE", p1, p2);
                    break;
                case CMD_CAM_GET_MODE_RESULT:
                    ackNotHandled("CMD_CAM_GET_MODE_RESULT", p1, p2);
                    break;
                case CMD_CAM_GET_API_VERSION:
                    ack_Cam_getApiVersion(p1);
                    break;
                case CMD_CAM_IS_API_SUPPORTED:
                    ackNotHandled("CMD_CAM_IS_API_SUPPORTED", p1, p2);
                    break;
                case CMD_CAM_GET_NAME:
                    ackNotHandled("CMD_CAM_GET_NAME", p1, p2);
                    break;
                case CMD_CAM_GET_NAME_RESULT:
                    ack_Cam_get_Name_result(p1, p2);
                    break;
                case CMD_CAM_SET_NAME:
                    ackNotHandled("CMD_CAM_SET_NAME", p1, p2);
                    break;
                case CMD_CAM_SET_NAME_RESULT:
                    ackNotHandled("CMD_CAM_SET_NAME_RESULT", p1, p2);
                    break;
                case CMD_CAM_GET_STATE:
                    ackNotHandled("CMD_CAM_GET_STATE", p1, p2);
                    break;
                case CMD_CAM_GET_STATE_RESULT:
                    ack_Cam_get_State_result(p1, p2);
                    break;
                case CMD_CAM_START_REC:
                    ackNotHandled("CMD_CAM_START_REC", p1, p2);
                    break;
                case CMD_CAM_STOP_REC:
                    ackNotHandled("CMD_CAM_STOP_REC", p1, p2);
                    break;
                case CMD_CAM_GET_TIME:
                    ackNotHandled("CMD_CAM_GET_TIME", p1, p2);
                    break;
                case CMD_CAM_GET_TIME_RESULT:
                    ack_Cam_get_time_result(p1, p2);
                    break;
                case CMD_CAM_GET_GET_ALL_INFOR:
                    ackNotHandled("CMD_CAM_GET_GET_ALL_INFOR", p1, p2);
                    break;
                case CMD_CAM_GET_GET_STORAGE_INFOR:
                    ackNotHandled("CMD_CAM_GET_GET_STORAGE_INFOR", p1, p2);
                    break;
                case CMD_CAM_MSG_STORAGE_INFOR:
                    ack_Cam_msg_Storage_infor(p1, p2);
                    break;
                case CMD_CAM_MSG_STORAGE_SPACE_INFOR:
                    ack_Cam_msg_StorageSpace_infor(p1, p2);
                    break;
                case CMD_CAM_MSG_BATTERY_INFOR:
                    ack_Cam_msg_Battery_infor(p1, p2);
                    break;
                case CMD_CAM_MSG_POWER_INFOR:
                    ack_Cam_msg_power_infor(p1, p2);
                    break;
                case CMD_CAM_MSG_BT_INFOR:
                    ackNotHandled("CMD_CAM_MSG_BT_INFOR", p1, p2);
                    break;
                case CMD_CAM_MSG_GPS_INFOR:
                    ack_Cam_msg_GPS_infor(p1, p2);
                    break;
                case CMD_CAM_MSG_INTERNET_INFOR:
                    ackNotHandled("CMD_CAM_MSG_INTERNET_INFOR", p1, p2);
                    break;
                case CMD_CAM_MSG_MIC_INFOR:
                    ack_Cam_msg_Mic_infor(p1, p2);
                    break;
                case CMD_CAM_SET_STREAM_SIZE:
                    ackNotHandled("CMD_CAM_SET_STREAM_SIZE", p1, p2);
                    break;
                case CMD_CAM_POWER_OFF:
                    ackNotHandled("CMD_CAM_POWER_OFF", p1, p2);
                    break;
                case CMD_CAM_REBOOT:
                    ackNotHandled("CMD_CAM_REBOOT", p1, p2);
                    break;
                case CMD_NETWORK_GET_WLAN_MODE:
                    ack_Network_GetWLanMode(p1, p2);
                    break;
                case CMD_NETWORK_GET_HOST_NUM:
                    ack_Network_GetHostNum(p1, p2);
                    break;
                case CMD_NETWORK_GET_HOST_INFOR:
                    ack_Network_GetHostInfor(p1, p2);
                    break;
                case CMD_NETWORK_ADD_HOST:
                    ackNotHandled("CMD_NETWORK_ADD_HOST", p1, p2);
                    break;
                case CMD_NETWORK_RMV_HOST:
                    ackNotHandled("CMD_NETWORK_RMV_HOST", p1, p2);
                    break;
                case CMD_NETWORK_CONNECT_HOST:
                    ackNotHandled("CMD_NETWORK_CONNECT_HOST", p1, p2);
                    break;
                case CMD_NETWORK_SCANHOST:
                    handleNetWorkScanHostResult(p1, p2);
                    break;
                case CMD_NETWORK_SYNCTIME:
                    ackNotHandled("CMD_NETWORK_SYNCTIME", p1, p2);
                    break;
                case CMD_NETWORK_GET_DEVICETIME:
                    ackNotHandled("CMD_NETWORK_GET_DEVICETIME", p1, p2);
                    break;
                case CMD_REC_ERROR:
                    ack_Rec_error(p1, p2);
                    break;
                case CMD_AUDIO_SET_MIC:
                    ackNotHandled("CMD_AUDIO_SET_MIC", p1, p2);
                    break;
                case CMD_AUDIO_GET_MIC_STATE:
                    ackNotHandled("CMD_AUDIO_GET_MIC_STATE", p1, p2);
                    break;
                case CMD_FW_GET_VERSION:
                    ack_fw_getVersion(p1, p2);
                    break;
                case CMD_FW_NEW_VERSION:
                    ackNotHandled("CMD_FW_NEW_VERSION", p1, p2);
                    break;
                case CMD_FW_DO_UPGRADE:
                    ackNotHandled("CMD_FW_DO_UPGRADE", p1, p2);
                    break;
                case CMD_CAM_BT_IS_SUPPORTED:
                    ack_CAM_BT_isSupported(p1);
                    break;
                case CMD_CAM_BT_IS_ENABLED:
                    ack_CAM_BT_isEnabled(p1);
                    break;
                case CMD_CAM_BT_ENABLE:
                    ackNotHandled("CMD_CAM_BT_ENABLE", p1, p2);
                    break;
                case CMD_CAM_BT_GET_DEV_STATUS:
                    ack_CAM_BT_getDEVStatus(p1, p2);
                    break;
                case CMD_CAM_BT_GET_HOST_NUM:
                    ack_CAM_BT_getHostNum(p1);
                    break;
                case CMD_CAM_BT_GET_HOST_INFOR:
                    ack_CAM_BT_getHostInfor(p1, p2);
                    break;
                case CMD_CAM_BT_DO_SCAN:
                    ack_CAM_BT_doScan();
                    break;
                case CMD_CAM_BT_DO_BIND:
                    ack_CAM_BT_doBind(p1, p2);
                    break;
                case CMD_CAM_BT_DO_UNBIND:
                    ack_CAM_BT_doUnBind(p1, p2);
                    break;
                case CMD_CAM_BT_SET_OBD_TYPES:
                    ackNotHandled("CMD_CAM_BT_SET_OBD_TYPES", p1, p2);
                    break;
                case CMD_CAM_WANT_IDLE:
                    // not used
                    break;
                case CMD_CAM_WANT_PREVIEW:
                    // not used
                    break;
                default:
                    Logger.t(TAG).d("ack " + cmd + " not handled, p1=" + p1 + ", p2=" + p2);
                    break;
            }
        }

        private void handleNetWorkScanHostResult(String p1, String p2) {
            List<NetworkItemBean> networkItemBeanList = new ArrayList<>();
            try {
                JSONObject object = new JSONObject(p1);
                JSONArray networks = object.getJSONArray("networks");
                for (int i = 0; i < networks.length(); i++) {
                    JSONObject networkObject = networks.getJSONObject(i);
                    NetworkItemBean networkItem = new NetworkItemBean();
                    networkItem.ssid = networkObject.optString("ssid");
                    networkItem.bssid = networkObject.optString("bssid");
                    networkItem.flags = networkObject.optString("flags");
                    networkItem.frequency = networkObject.optInt("frequency");
                    networkItem.singalLevel = networkObject.optInt("signal_level");
                    networkItemBeanList.add(networkItem);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (mOnScanHostListener != null) {
                mOnScanHostListener.OnScanHostResult(networkItemBeanList);
            }

        }

        private void recDomainMsg(int cmd, String p1, String p2) {
            switch (cmd) {
                case CMD_REC_START:
                    ackNotHandled("CMD_REC_START", p1, p2);
                    break;
                case CMD_REC_STOP:
                    ackNotHandled("CMD_REC_STOP", p1, p2);
                    break;
                case CMD_REC_LIST_RESOLUTIONS:
                    ack_Rec_List_Resolutions(p1, p2);
                    break;
                case CMD_REC_SET_RESOLUTION:
                    ackNotHandled("CMD_REC_SET_RESOLUTION", p1, p2);
                    break;
                case CMD_REC_GET_RESOLUTION:
                    ack_Rec_get_Resolution(p1, p2);
                    break;
                case CMD_REC_LIST_QUALITIES:
                    ack_Rec_List_Qualities(p1, p2);
                    break;
                case CMD_REC_SET_QUALITY:
                    ackNotHandled("CMD_REC_SET_QUALITY", p1, p2);
                    break;
                case CMD_REC_GET_QUALITY:
                    ack_Rec_get_Quality(p1, p2);
                    break;
                case CMD_REC_LIST_REC_MODES:
                    ack_Rec_List_RecModes(p1, p2);
                    break;
                case CMD_REC_SET_REC_MODE:
                    ackNotHandled("CMD_REC_SET_REC_MODE", p1, p2);
                    break;
                case CMD_REC_GET_REC_MODE:
                    ack_Rec_get_RecMode(p1, p2);
                    break;
                case CMD_REC_LIST_COLOR_MODES:
                    ack_Rec_List_ColorModes(p1, p2);
                    break;
                case CMD_REC_SET_COLOR_MODE:
                    ackNotHandled("CMD_REC_SET_COLOR_MODE", p1, p2);
                    break;
                case CMD_REC_GET_COLOR_MODE:
                    ack_Rec_get_ColorMode(p1, p2);
                    break;
                case CMD_REC_LIST_SEG_LENS:
                    ackNotHandled("CMD_REC_LIST_SEG_LENS", p1, p2);
                    break;
                case CMD_REC_SET_SEG_LEN:
                    ackNotHandled("CMD_REC_SET_SEG_LEN", p1, p2);
                    break;
                case CMD_REC_GET_SEG_LEN:
                    ackNotHandled("CMD_REC_GET_SEG_LEN", p1, p2);
                    break;
                case CMD_REC_GET_STATE:
                    ackNotHandled("CMD_REC_GET_STATE", p1, p2);
                    break;
                case EVT_REC_STATE_CHANGE:
                    ackNotHandled("EVT_REC_STATE_CHANGE", p1, p2);
                    break;
                case CMD_REC_GET_TIME:
                    ackNotHandled("CMD_REC_GET_TIME", p1, p2);
                    break;
                case CMD_REC_GET_TIME_RESULT:
                    ackNotHandled("CMD_REC_GET_TIME_RESULT", p1, p2);
                    break;
                case CMD_REC_SET_DUAL_STREAM:
                    ackNotHandled("CMD_REC_SET_DUAL_STREAM", p1, p2);
                    break;
                case CMD_REC_GET_DUAL_STREAM_STATE:
                    ackNotHandled("CMD_REC_GET_DUAL_STREAM_STATE", p1, p2);
                    break;
                case CMD_REC_SET_OVERLAY:
                    ackNotHandled("CMD_REC_SET_OVERLAY", p1, p2);
                    break;
                case CMD_REC_GET_OVERLAY_STATE:
                    ack_Rec_getOverlayState(p1, p2);
                    break;
                case CMD_REC_SET_STILL_MODE:
                    ackNotHandled("CMD_REC_SET_STILL_MODE", p1, p2);
                    break;
                case CMD_REC_START_STILL_CAPTURE:
                    ack_Rec_StartStillCapture(p1, p2);
                    break;
                case CMD_REC_STOP_STILL_CAPTURE:
                    ackNotHandled("CMD_REC_STOP_STILL_CAPTURE", p1, p2);
                    break;
                case MSG_REC_STILL_PICTURE_INFO:
                    ack_Rec_StillPictureInfo(p1, p2);
                    break;
                case MSG_REC_STILL_CAPTURE_DONE:
                    ack_Rec_StillCaptureDone();
                    break;
                case CMD_REC_GET_MARK_TIME:
                    ack_Rec_GetMarkTime(p1, p2);
                    break;
                case CMD_REC_SET_MARK_TIME:
                    ack_Rec_SetMarkTime(p1, p2);
                    break;

                default:
                    Logger.t(TAG).d("ack " + cmd + " not handled, p1=" + p1 + ", p2=" + p2);
                    break;
            }
        }

        class MyTcpConnection extends TcpConnection {

            public MyTcpConnection(String name, InetSocketAddress address) {
                super(name, address);
            }

            @Override
            public void onConnectedAsync() {
                initCameraState();
                onCameraConnected();
            }

            @Override
            public void onConnectErrorAsync() {
                onCameraDisconnected();
            }

            @Override
            public void cmdLoop(Thread thread) throws IOException, InterruptedException {
                VdtCameraController.this.cmdLoop(thread);
            }

            @Override
            public void msgLoop(Thread thread) throws IOException, InterruptedException {
                VdtCameraController.this.msgLoop(thread);
            }
        }


    }


}
