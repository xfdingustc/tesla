package com.waylens.hachi.hardware.vdtcamera;

import android.util.Log;
import android.util.Xml;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.eventbus.events.CameraStateChangeEvent;
import com.waylens.hachi.eventbus.events.RawDataItemEvent;
import com.waylens.hachi.hardware.vdtcamera.events.BluetoothEvent;
import com.waylens.hachi.hardware.vdtcamera.events.NetworkEvent;
import com.waylens.hachi.snipe.BasicVdbSocket;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbConnection;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.VdbSocket;
import com.waylens.hachi.snipe.toolbox.ClipInfoMsgHandler;
import com.waylens.hachi.snipe.toolbox.MarkLiveMsgHandler;
import com.waylens.hachi.snipe.toolbox.RawDataMsgHandler;
import com.waylens.hachi.ui.entities.NetworkItemBean;
import com.waylens.hachi.utils.ToStringUtils;
import com.waylens.hachi.vdb.ClipActionInfo;
import com.waylens.hachi.vdb.rawdata.RawDataItem;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


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


    public static final int BT_STATE_UNKNOWN = -1;
    public static final int BT_STATE_DISABLED = 0;
    public static final int BT_STATE_ENABLED = 1;


    private boolean mIsConnected = false;
    private boolean mIsVdbConnected = false;


    private String mCameraName = new String();
    private String mFirmwareVersion = new String();
    private String mBspVersion = new String();
    private String mHardwareName;
    private int mApiVersion = 0;
    private String mApiVersionStr = new String();
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


    private int mBtState = BT_STATE_UNKNOWN;
    private BtDevice mObdDevice = new BtDevice(BtDevice.BT_DEVICE_TYPE_OBD);
    private BtDevice mRemoteCtrlDevice = new BtDevice(BtDevice.BT_DEVICE_TYPE_REMOTE_CTR);


    private final ServiceInfo mServiceInfo;
    private final VdtCameraController mController;

    private InetSocketAddress mPreviewAddress;


    private OnScanHostListener mOnScanHostListener;

    private List<BtDevice> mScannedBtDeviceList = new ArrayList<>();
    private int mScannedBtDeviceNumber = 0;

    private VdbRequestQueue mVdbRequestQueue;

    private EventBus mEventBus = EventBus.getDefault();


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

        @Override
        public String toString() {
            return ToStringUtils.getString(this);
        }
    }


    public interface OnConnectionChangeListener {
        void onConnected(VdtCamera vdtCamera);

        void onVdbConnected(VdtCamera vdtCamera);

        void onDisconnected(VdtCamera vdtCamera);
    }

    public interface OnNewFwVersionListern {
        void onNewVersion(int response);
    }


    private OnConnectionChangeListener mOnConnectionChangeListener = null;

    private OnNewFwVersionListern mOnNewFwVersionListerner = null;


    public void setOnConnectionChangeListener(OnConnectionChangeListener listener) {
        mOnConnectionChangeListener = listener;
    }


    private VdbConnection mVdbConnection;

    public VdbConnection getVdbConnection() {
        return mVdbConnection;
    }


    public VdtCamera(VdtCamera.ServiceInfo serviceInfo) {
        mServiceInfo = serviceInfo;
        mController = new VdtCameraController(serviceInfo.inetAddr, serviceInfo.port);
        startClient();
    }

    public void setCameraName(String name) {
        Logger.t(TAG).d("set Camera Name: " + name);
        if (name == null || name.isEmpty()) {
            // use empty string for unnamed camera
            name = "No name";
        }
        if (!mCameraName.equals(name)) {
            Logger.t(TAG).d("setCameraName: " + name);
            mCameraName = name;
            mEventBus.post(new CameraStateChangeEvent(CameraStateChangeEvent.CAMERA_STATE_INFO, this, null));
        }
    }

    public String versionString() {
        int main = (mApiVersion >> 16) & 0xff;
        int sub = mApiVersion & 0xffff;
        return String.format(Locale.US, "%d.%d.%s", main, sub, mBuild);
    }

    public void setFirmwareVersion(String hardwareName, String bspVersion) {
        mHardwareName = hardwareName.substring(hardwareName.indexOf("@") + 1);
        if (!mBspVersion.equals(bspVersion)) {
//            Logger.t(TAG).d("setFirmwareVersion: " + version);
            mBspVersion = bspVersion;
        }
    }

    public String getApiVersion() {
        return mApiVersionStr;
    }

    public String getHardwareName() {
        return mHardwareName;
    }


    private int makeVersion(int main, int sub) {
        return (main << 16) | sub;
    }

    public void setApiVersion(int main, int sub, String build) {
        int version = makeVersion(main, sub);
        if (mApiVersion != version || !mBuild.equals(build)) {
//            Logger.t(TAG).d("setApiVersion: " + version);
            mApiVersion = version;
            mBuild = build;

        }
    }

    // API
    public boolean isPcServer() {
        return mServiceInfo.bPcServer;
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

    public ServiceInfo getServerInfo() {
        return mServiceInfo;
    }

    public int getBtState() {
        return mBtState;
    }

    public BtDevice getObdDevice() {
        mController.cmd_CAM_BT_isEnabled();
        return mObdDevice;
    }

    public BtDevice getRemoteCtrlDevice() {
        mController.cmd_CAM_BT_isEnabled();
        return mRemoteCtrlDevice;
    }


    public boolean isMicOn() {
        return mMicState == STATE_MIC_ON;
    }

    public void setMicOn(boolean on) {
        int micState = on ? STATE_MIC_ON : STATE_MIC_OFF;
        mController.cmd_audio_setMic(micState, 0);
    }

    public void setWifiMode(int wifiMode) {
        mController.cmd_NetWork_SetWLandMode(wifiMode);
    }

    public int getWifiMode() {
        mController.cmd_Network_GetWLanMode();
        return mWifiMode;
    }

    public String getBspFirmware() {
        mController.cmd_fw_getVersion();
        return mBspVersion;
    }

    public void sendNewFirmware(String md5, OnNewFwVersionListern listener) {
        mOnNewFwVersionListerner = listener;
        mController.sendCmdFwNewVersion(md5);
    }

    public void upgradeFirmware() {
        mController.sendUpgradeFirmware();
    }

    public VdbRequestQueue getRequestQueue() {
        return mVdbRequestQueue;
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

    public int getVideoResolution() {
        mController.cmd_Rec_get_Resolution();
        Logger.t(TAG).d("get video quality index: " + mVideoResolutionIndex);
        switch (mVideoResolutionIndex) {
            case VIDEO_RESOLUTION_1080P30:
            case VIDEO_RESOLUTION_1080P60:
                return VIDEO_RESOLUTION_1080P;
            default:
                return VIDEO_RESOLUTION_720P;
        }
    }

    public int getVideoFramerate() {
        mController.cmd_Rec_get_Resolution();
        Logger.t(TAG).d("get video quality index: " + mVideoResolutionIndex);
        switch (mVideoResolutionIndex) {
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


    public InetSocketAddress getInetSocketAddress() {
        return mController.getInetSocketAddress();
    }

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

        try {
            mVdbConnection.connect();
            mIsVdbConnected = true;
            VdbSocket vdbSocket = new BasicVdbSocket(getVdbConnection());
            mVdbRequestQueue = new VdbRequestQueue(vdbSocket);
            mVdbRequestQueue.start();
            mOnConnectionChangeListener.onVdbConnected(VdtCamera.this);
            registerMessageHandler();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mIsConnected = true;
    }

    private void registerMessageHandler() {
        RawDataMsgHandler rawDataMsgHandler = new RawDataMsgHandler(new VdbResponse.Listener<RawDataItem>() {
            @Override
            public void onResponse(RawDataItem response) {
//                mGaugeView.updateRawDateItem(response);
                mEventBus.post(new RawDataItemEvent(VdtCamera.this, response));
            }

        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e(TAG, "RawDataMsgHandler ERROR", error);
            }
        });
        mVdbRequestQueue.registerMessageHandler(rawDataMsgHandler);

        ClipInfoMsgHandler clipInfoMsgHandler = new ClipInfoMsgHandler(
            new VdbResponse.Listener<ClipActionInfo>() {
                @Override
                public void onResponse(ClipActionInfo response) {
//                    Logger.t(TAG).e(response.toString());
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("ClipInfoMsgHandler ERROR", error);
                }
            });
        mVdbRequestQueue.registerMessageHandler(clipInfoMsgHandler);

        MarkLiveMsgHandler markLiveMsgHandler = new MarkLiveMsgHandler(
            new VdbResponse.Listener<ClipActionInfo>() {
                @Override
                public void onResponse(ClipActionInfo response) {
                    Logger.t(TAG).d(response.toString());
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("MarkLiveMsgHandler ERROR", error);
                }
            });
        mVdbRequestQueue.registerMessageHandler(markLiveMsgHandler);
    }

    private void onCameraDisconnected() {

//        mVdbRequestQueue.stop();
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.unregisterMessageHandler(VdbCommand.Factory.MSG_RawData);
            mVdbRequestQueue.unregisterMessageHandler(VdbCommand.Factory.MSG_ClipInfo);
            mVdbRequestQueue.unregisterMessageHandler(VdbCommand.Factory.VDB_MSG_MarkLiveClipInfo);
        }

        if (mOnConnectionChangeListener != null) {
            mOnConnectionChangeListener.onDisconnected(this);
        }
        mIsConnected = false;
    }


    // called on camera thread
    private void initCameraState() {
        mController.cmd_Cam_getApiVersion();
        mController.cmd_fw_getVersion();
//        mController.getName();
        mController.getNameAsync();
        mController.cmd_Rec_get_RecMode();
        mController.cmd_Rec_List_Resolutions(); // see if still capture is supported
        mController.cmd_Cam_get_getAllInfor();
        mController.cmd_Cam_get_State();
        mController.cmd_Network_GetWLanMode();
        mController.cmd_Network_GetHostNum();
        mController.cmd_Rec_GetMarkTime();
        long timeMillis = System.currentTimeMillis();
        int timeZone = TimeZone.getDefault().getRawOffset();

        mController.cmd_Network_Synctime(timeMillis / 1000, timeZone / (3600 * 1000));

        mController.cmd_CAM_BT_isEnabled();
        mController.cmd_CAM_BT_getDEVStatus(BtDevice.BT_DEVICE_TYPE_REMOTE_CTR);
        mController.cmd_CAM_BT_getDEVStatus(BtDevice.BT_DEVICE_TYPE_OBD);

    }


    // Control APIs
    public void setBtEnable(boolean enable) {
        mController.cmd_CAM_BT_Enable(enable);
    }

    public void scanBluetoothDevices() {
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


    public void setRecordMarkTime(int markBeforeTime, int newMarkTime) {
        mController.cmd_Rec_SetMarkTime(markBeforeTime, newMarkTime);
    }

    public void setName(String name) {
        mController.cmd_Cam_set_Name(name);
    }

    public String getName() {
        mController.getNameAsync();
        return mCameraName;
    }

    public void setVideoResolution(int resolution, int frameRate) {
        int videoResulution = VIDEO_RESOLUTION_1080P30;
        if (resolution == VIDEO_RESOLUTION_1080P) {
            switch (frameRate) {
                case VIDEO_FRAMERATE_30FPS:
                    videoResulution = VIDEO_RESOLUTION_1080P30;
                    break;
                case VIDEO_FRAMERATE_60FPS:
                    videoResulution = VIDEO_RESOLUTION_1080P60;
                    break;
            }
        } else if (resolution == VIDEO_RESOLUTION_720P) {
            switch (frameRate) {
                case VIDEO_FRAMERATE_30FPS:
                    videoResulution = VIDEO_RESOLUTION_720P30;
                    break;
                case VIDEO_FRAMERATE_60FPS:
                    videoResulution = VIDEO_RESOLUTION_720P60;
                    break;

            }
        }
        Logger.t(TAG).d("set video resolution: " + videoResulution);
        setVideoResolution(videoResulution);
    }

    public void setVideoResolution(int resolutionIndex) {
        mController.cmd_Rec_Set_Resolution(resolutionIndex);
    }


    public void setVideoQuality(int qualityIndex) {
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

    public void setAudioMic(boolean isOn, int vol) {
        int state = isOn ? STATE_MIC_ON : STATE_MIC_OFF;
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

    public void getSetup() {
        mController.userCmd_GetSetup();
    }

    private void startClient() {
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

    public int getMarkBeforeTime() {
        return mMarkBeforeTime;
    }

    public int getMarkAfterTime() {
        return mMarkAfterTime;
    }

    public void setMarkTime(int before, int after) {
        mController.cmd_Rec_SetMarkTime(before, after);
    }

    public void stopRecording() {
        mController.ack_Cam_stop_rec();
    }

    public void startRecording() {
        mController.cmd_Cam_start_rec();
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


    public void connectNetworkHost(String ssid) {
        mController.cmd_Network_ConnectHost(ssid);
    }

    public boolean isMicEnabled() {
        return mMicState == STATE_MIC_ON;
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
        void OnScanHostResult(List<NetworkItemBean> addedNetworkList, List<NetworkItemBean> networkList);
    }


    class VdtCameraController implements VdtCameraCmdConsts {

        private InetSocketAddress mAddress;
        private final BlockingQueue<VdtCameraCommand> mCameraRequestQueue;

        private Socket mSocket;

        private boolean mRunCmdAndMsgLoop = true;

        public VdtCameraController(InetAddress host, int port) {
            mAddress = new InetSocketAddress(host, port);

            mCameraRequestQueue = new LinkedBlockingQueue<>();
            createCmdObserver(mAddress);


        }

        private void createCmdObserver(final InetSocketAddress address) {
            Logger.t(TAG).d("create cmd observer");
            Observable.create(new Observable.OnSubscribe<Integer>() {
                @Override
                public void call(Subscriber<? super Integer> subscriber) {
                    mSocket = new Socket();
                    try {
                        mSocket.setReceiveBufferSize(8192);
                        mSocket.connect(address);

                        subscriber.onNext(0);


                        while (mRunCmdAndMsgLoop) {
                            VdtCameraCommand command = mCameraRequestQueue.take();


                            if (command.mDomain == CMD_DOMAIN_USER) {
                                if (!createUserCmd(command)) {
                                    break;
                                }
                                continue;
                            }

                            SocketUtils.writeCommand(mSocket, command);
                        }

                        subscriber.onCompleted();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        initCameraState();
                        onCameraConnected();
                        createMsgObserver();
                    }
                });
        }

        private void createMsgObserver() {
            Logger.t(TAG).d("create msg observer");
            Observable.create(new Observable.OnSubscribe<Integer>() {
                @Override
                public void call(Subscriber<? super Integer> subscriber) {
                    try {
                        while (mRunCmdAndMsgLoop) {
                            SimpleInputStream sis = new SimpleInputStream(8192);
                            XmlPullParser xpp = Xml.newPullParser();
                            int length = 0;
                            int appended = 0;


                            sis.clear();

                            SocketUtils.readFully(mSocket, sis.getBuffer(), 0, HEAD_SIZE);
                            length = sis.readi32(0);
                            appended = sis.readi32(4);
                            if (appended > 0) {
                                sis.expand(HEAD_SIZE + appended);
                                SocketUtils.readFully(mSocket, sis.getBuffer(), HEAD_SIZE, appended);
                            }
                            sis.setRange(8, length);

                            xpp.setInput(sis, "UTF-8");


                            int eventType = xpp.getEventType();


                            while (true) {
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


                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (XmlPullParserException e) {
                        Logger.t(TAG).d("XmlPullParserException: length=");
                        e.printStackTrace();

                    }

                }
            })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer integer) {

                    }
                });
        }


        public InetSocketAddress getInetSocketAddress() {
            return mAddress;
        }

        public void cmd_NetWork_SetWLandMode(int wifiMode) {
            postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_CONNECT_HOST, wifiMode);
        }









        private void postRequest(int domain, int cmd) {
            postRequest(domain, cmd, "", "");
        }

        private void postRequest(int domain, int cmd, int p1) {
            postRequest(domain, cmd, Integer.toString(p1), "");
        }

        private void postRequest(int domain, int cmd, String p1) {
            postRequest(domain, cmd, p1, "");
        }

        private void postRequest(int domain, int cmd, String p1, String p2) {
            VdtCameraCommand request = new VdtCameraCommand(domain, cmd, p1, p2);
            postRequest(request);
        }

        private void postRequest(VdtCameraCommand request) {
            mCameraRequestQueue.offer(request);

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
            mApiVersionStr = p1;
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


        public void getNameAsync() {
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
            if (mRecordState != state) {
                mEventBus.post(new CameraStateChangeEvent(CameraStateChangeEvent.CAMERA_STATE_REC, VdtCamera.this, null));
                mRecordState = state;
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

            if (mRecordTime != duration) {
                mEventBus.post(new CameraStateChangeEvent(CameraStateChangeEvent.CAMERA_STATE_REC_DURATION, VdtCamera.this, duration));
                mRecordTime = duration;
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

        public void cmd_Network_ConnectHost(/*int mode,*/ String apName) {
            if (apName == null) {
                apName = "";
            }
            postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_CONNECTHOTSPOT, /*Integer.toString(mode),*/ apName, "");
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
            mEventBus.post(new CameraStateChangeEvent(CameraStateChangeEvent.CAMERA_STATE_REC_ERROR, VdtCamera.this, error));
        }

        public void cmd_audio_setMic(int state, int gain) {
            if (state == STATE_MIC_ON && gain == 0) {
                gain = 5;
            }
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
            Logger.t(TAG).d("ack get firmware p1: " + p1 + " P2: " + p2);
            setFirmwareVersion(p1, p2);
        }

        public void sendCmdFwNewVersion(String md5) {
            postRequest(CMD_DOMAIN_CAM, CMD_FW_NEW_VERSION, md5);
        }

        public void sendUpgradeFirmware() {
            postRequest(CMD_DOMAIN_CAM, CMD_FW_DO_UPGRADE);
        }

        private static final String XML_CCEV = "ccev";
        private static final String XML_CMD = "cmd";
        private static final String XML_ACT = "act";
        private static final String XML_P1 = "p1";
        private static final String XML_P2 = "p2";
        private static final int HEAD_SIZE = 128;

        public void cmd_CAM_BT_isEnabled() {

            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_IS_ENABLED);

        }

        private void ack_CAM_BT_isEnabled(String p1) {
            int enabled = Integer.parseInt(p1);
            mBtState = enabled;
            if (enabled == BT_STATE_ENABLED) {
                cmd_CAM_BT_getDEVStatus(BtDevice.BT_DEVICE_TYPE_REMOTE_CTR);
                cmd_CAM_BT_getDEVStatus(BtDevice.BT_DEVICE_TYPE_OBD);
            }
        }

        public void cmd_CAM_BT_Enable(boolean bEnable) {

            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_ENABLE, bEnable ? 1 : 0);

        }

        public void cmd_CAM_BT_getDEVStatus(int type) {

            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_GET_DEV_STATUS, type);

        }

        private void ack_CAM_BT_getDEVStatus(String p1, String p2) {
            int i_p1 = Integer.parseInt(p1);
            int devType = (i_p1 >> 8) & 0xff;
            int devState = i_p1 & 0xff;
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
                devState = BtDevice.BT_DEVICE_STATE_OFF;
                mac = "";
                name = "";
            }

//            Logger.t(TAG).d("bt devide type: " + devType + " dev_state " + devState + " mac: " + mac + " name " + name);
            if (BtDevice.BT_DEVICE_TYPE_OBD == devType) {
                mObdDevice.setDevState(devState, mac, name);
            } else if (BtDevice.BT_DEVICE_TYPE_REMOTE_CTR == devType) {
                mRemoteCtrlDevice.setDevState(devState, mac, name);
            }

        }

        public void cmd_CAM_BT_getHostNum() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_GET_HOST_NUM);
        }

        private void ack_CAM_BT_getHostNum(String p1) {
            int numDevs = Integer.parseInt(p1);
            if (numDevs < 0) {
                numDevs = 0;
            }
            mScannedBtDeviceNumber = numDevs;
            Logger.t(TAG).d("find devices: " + mScannedBtDeviceNumber);
            mScannedBtDeviceList.clear();
            for (int i = 0; i < numDevs; i++) {
                cmd_CAM_BT_getHostInfor(i);
            }
        }

        public void cmd_CAM_BT_getHostInfor(int index) {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_GET_HOST_INFOR, index);
        }


        private void ack_CAM_BT_getHostInfor(String name, String mac) {
            int type;
            if (name.indexOf("OBD") >= 0) {
                type = BtDevice.BT_DEVICE_TYPE_OBD;
            } else if (name.indexOf("RC") >= 0) {
                type = BtDevice.BT_DEVICE_TYPE_REMOTE_CTR;
            } else {
                type = BtDevice.BT_DEVICE_TYPE_OTHER;
            }

            Logger.t(TAG).d("type: " + type + " mac: " + mac + " name: " + name);
            BtDevice device = new BtDevice(type);
            device.setDevState(BtDevice.BT_DEVICE_STATE_UNKNOWN, mac, name);

            mScannedBtDeviceList.add(device);
            if (mScannedBtDeviceList.size() == mScannedBtDeviceNumber) {
                mEventBus.post(new BluetoothEvent(BluetoothEvent.BT_SCAN_COMPLETE, mScannedBtDeviceList));
            }

        }


        public void cmd_CAM_BT_doScan() {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_DO_SCAN);

        }

        private void ack_CAM_BT_doScan(String p1) {
            int ret = Integer.parseInt(p1);
            Logger.t(TAG).d("ret: " + ret);
            if (ret == 0) {
                cmd_CAM_BT_getHostNum();
            }
        }

        public void cmd_CAM_BT_doBind(int type, String mac) {
            Log.d(TAG, "cmd_CAM_BT_doBind, type=" + type + ", mac=" + mac);
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_DO_BIND, Integer.toString(type), mac);
        }

        private void ack_CAM_BT_doBind(String p1, String p2) {
            int type = Integer.parseInt(p1);
            int result = Integer.parseInt(p2);
            if (result == 0) {
                if (type == BtDevice.BT_DEVICE_TYPE_REMOTE_CTR || type == BtDevice.BT_DEVICE_TYPE_OBD) {
                    postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_GET_DEV_STATUS, type);
                }
            }
            mEventBus.post(new BluetoothEvent(BluetoothEvent.BT_DEVICE_BIND_FINISHED));
        }

        public void cmd_CAM_BT_doUnBind(int type, String mac) {
            Log.d(TAG, "cmd_CAM_BT_doUnBind, type=" + type + ", mac=" + mac);
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_DO_UNBIND, Integer.toString(type), mac);
        }

        private void ack_CAM_BT_doUnBind(String p1, String p2) {
            int type = Integer.parseInt(p1);
            if (type == BtDevice.BT_DEVICE_TYPE_REMOTE_CTR || type == BtDevice.BT_DEVICE_TYPE_OBD) {
                postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_GET_DEV_STATUS, type);
            }

            mEventBus.post(new BluetoothEvent(BluetoothEvent.BT_DEVICE_UNBIND_FINISHED));
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
//            Logger.t(TAG).d("set video resolution index: " + index);
            mVideoResolutionIndex = index;
        }


        public void cmd_Rec_List_Qualities() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_LIST_QUALITIES);
        }

        private void ack_Rec_List_Qualities(String p1, String p2) {
            int list = Integer.parseInt(p1);
            mVideoQualityList = list;
        }

        public void cmd_Rec_Set_Quality(int index) {
            postRequest(CMD_DOMAIN_REC, CMD_REC_SET_QUALITY, index);
        }


        public void cmd_Rec_get_Quality() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_GET_QUALITY);
        }

        private void ack_Rec_get_Quality(String p1, String p2) {
            int index = Integer.parseInt(p1);

            mVideoQualityIndex = index;
        }


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
            if (mRecordModeIndex != index) {
                mEventBus.post(new CameraStateChangeEvent(CameraStateChangeEvent.CAMERA_STATE_REC, VdtCamera.this, null));
                mRecordModeIndex = index;
            }
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

        public void cmd_Rec_getOverlayState() {
            postRequest(CMD_DOMAIN_REC, CMD_REC_GET_OVERLAY_STATE);
        }

        private void ack_Rec_getOverlayState(String p1, String p2) {
            int flags = Integer.parseInt(p1);
            mOverlayFlags = flags;
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
//            Logger.t(TAG).d(String.format("cmd_Rec_GetMarkTime: p1: %s, p2: %s", p1, p2));
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
            postRequest(CMD_DOMAIN_REC, CMD_REC_SET_MARK_TIME, String.valueOf(before), String.valueOf(after));
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
//            mConnection.start(null);
        }

        // API
        public void stop() {
            mRunCmdAndMsgLoop = false;
        }


        private boolean createUserCmd(VdtCameraCommand command) {
            switch (command.mCmd) {
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
//                    this.cmd_CAM_BT_isSupported();
                    this.cmd_CAM_BT_isEnabled();
                    this.cmd_Rec_GetMarkTime();
                    break;

                case USER_CMD_EXIT_THREAD:
                    return false;

                default:
                    Logger.t(TAG).d("unknown user cmd " + command.mCmd);
                    break;
            }

            return true;
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
//                            Logger.t(TAG).d("Domain = " + domain + " cmd = " + cmd);
                            camDomainMsg(cmd, p1, p2);
                            break;
                        case CMD_DOMAIN_REC:
//                            Logger.t(TAG).d("Domain = " + domain + " cmd =" + cmd);
                            recDomainMsg(cmd, p1, p2);
                            break;
                        default:
                            break;
                    }
                }
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
//                    ackNotHandled("CMD_NETWORK_ADD_HOST", p1, p2);
                    handleOnNetworkAddHost(p1, p2);
                    break;
                case CMD_NETWORK_RMV_HOST:
                    ackNotHandled("CMD_NETWORK_RMV_HOST", p1, p2);
                    break;
                case CMD_NETWORK_CONNECT_HOST:
                    handleOnNetworkConnectHost(p1, p2);
                    break;
                case CMD_NETWORK_SCANHOST:
                    handleNetWorkScanHostResult(p1, p2);
                    break;
                case CMD_NETWORK_CONNECTHOTSPOT:
                    handleNetworkConnectHost(p1, p2);
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
                    handleNewFwVersion(p1, p2);
                    break;
                case CMD_FW_DO_UPGRADE:
                    ackNotHandled("CMD_FW_DO_UPGRADE", p1, p2);
                    break;
                case CMD_CAM_BT_IS_SUPPORTED:
//                    ack_CAM_BT_isSupported(p1);
                    ackNotHandled("CMD_CAM_BT_IS_SUPPORTED", p1, p2);
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
                    ack_CAM_BT_doScan(p1);
                    break;
                case CMD_CAM_BT_DO_BIND:
                    ack_CAM_BT_doBind(p1, p2);
                    break;
                case CMD_CAM_BT_DO_UNBIND:
                    ack_CAM_BT_doUnBind(p1, p2);
                    break;

                default:
                    Logger.t(TAG).d("ack " + cmd + " not handled, p1=" + p1 + ", p2=" + p2);
                    break;
            }
        }

        private void handleNewFwVersion(String p1, String p2) {
            Logger.t(TAG).d("p1: " + p1 + " p2: " + p2);
            if (mOnNewFwVersionListerner != null) {
                mOnNewFwVersionListerner.onNewVersion(Integer.valueOf(p1));
            }
        }

        private void handleOnNetworkConnectHost(String p1, String p2) {
            Logger.t(TAG).d("p1: " + p1 + " p2: " + p2);
        }

        private void handleNetworkConnectHost(String p1, String p2) {
            Logger.t(TAG).d("p1: " + p1 + " p2: " + p2);

            mEventBus.post(new NetworkEvent(NetworkEvent.NETWORK_EVENT_WHAT_CONNECTED));
        }

        private void handleOnNetworkAddHost(String p1, String p2) {
            Logger.t(TAG).d("p1: " + p1 + " p2: " + p2);

            mEventBus.post(new NetworkEvent(NetworkEvent.NETWORK_EVENT_WHAT_ADDED));
        }

        private void handleNetWorkScanHostResult(String p1, String p2) {
            if (p1 == null || p1.isEmpty()) {
                return;
            }
            Logger.t(TAG).json(p1);
            List<NetworkItemBean> networkItemBeanList = new ArrayList<>();
            List<NetworkItemBean> addedNetworkItemBeanList = new ArrayList<>();
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
                    networkItem.added = networkObject.optBoolean("added");
                    if (networkItem.added) {
                        addedNetworkItemBeanList.add(networkItem);
                    } else {
                        networkItemBeanList.add(networkItem);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (mOnScanHostListener != null) {
                mOnScanHostListener.OnScanHostResult(addedNetworkItemBeanList, networkItemBeanList);
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
                case CMD_REC_STOP_STILL_CAPTURE:
                    ackNotHandled("CMD_REC_STOP_STILL_CAPTURE", p1, p2);
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


    }


}
