package com.waylens.hachi.hardware.vdtcamera;

import android.util.JsonReader;
import android.util.Log;
import android.util.Xml;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.ui.entities.NetworkItemBean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class VdtCameraController {
    private static final String TAG = VdtCameraController.class.getSimpleName();

    private static final int CMD_DOMAIN_USER = -1;
    private static final int CMD_DOMAIN_CAM = 0;
    private static final int CMD_DOMAIN_P2P = 1;
    private static final int CMD_DOMAIN_REC = 2;
    private static final int CMD_DOMAIN_DECODE = 3;
    private static final int CMD_DOMAIN_NETWORK = 4;
    private static final int CMD_DOMAIN_POWER = 5;
    private static final int CMD_DOMAIN_STORAGE = 6;
    private static final int CMD_DOMAIN_STREAM = 7;
    private static final int CMD_DOMAIN_MOTOR_CONTROL = 8;

    // For debug
    private static final Map<Integer, String> DOMAIN_TO_STRING = new HashMap<Integer, String>() {
        {
            put(CMD_DOMAIN_USER, "CMD_DOMAIN_USER");
            put(CMD_DOMAIN_CAM, "CMD_DOMAIN_CAM");
            put(CMD_DOMAIN_P2P, "CMD_DOMAIN_P2P");
            put(CMD_DOMAIN_REC, "CMD_DOMAIN_REC");
            put(CMD_DOMAIN_DECODE, "CMD_DOMAIN_DECODE");
            put(CMD_DOMAIN_NETWORK, "CMD_DOMAIN_NETWORK");
            put(CMD_DOMAIN_POWER, "CMD_DOMAIN_POWER");
            put(CMD_DOMAIN_STORAGE, "CMD_DOMAIN_STORAGE");
            put(CMD_DOMAIN_STREAM, "CMD_DOMAIN_STREAM");
            put(CMD_DOMAIN_MOTOR_CONTROL, "CMD_DOMAIN_MOTOR_CONTROL");
        }
    };

    // CMD_DOMAIN_CAM
    private static final int CMD_CAM_GET_MODE = 0;
    private static final int CMD_CAM_GET_MODE_RESULT = 1;
    private static final int CMD_CAM_GET_API_VERSION = 2;
    private static final int CMD_CAM_IS_API_SUPPORTED = 3;
    private static final int CMD_CAM_GET_NAME = 4;
    private static final int CMD_CAM_GET_NAME_RESULT = 5;
    private static final int CMD_CAM_SET_NAME = 6;
    private static final int CMD_CAM_SET_NAME_RESULT = 7;
    private static final int CMD_CAM_GET_STATE = 8;
    private static final int CMD_CAM_GET_STATE_RESULT = 9;
    private static final int CMD_CAM_START_REC = 10;
    private static final int CMD_CAM_STOP_REC = 11;
    private static final int CMD_CAM_GET_TIME = 12;
    private static final int CMD_CAM_GET_TIME_RESULT = 13;

    private static final int CMD_CAM_GET_GET_ALL_INFOR = 14;
    private static final int CMD_CAM_GET_GET_STORAGE_INFOR = 15;
    private static final int CMD_CAM_MSG_STORAGE_INFOR = 16;
    private static final int CMD_CAM_MSG_STORAGE_SPACE_INFOR = 17;
    private static final int CMD_CAM_MSG_BATTERY_INFOR = 18;
    private static final int CMD_CAM_MSG_POWER_INFOR = 19;
    private static final int CMD_CAM_MSG_BT_INFOR = 20;
    private static final int CMD_CAM_MSG_GPS_INFOR = 21;
    private static final int CMD_CAM_MSG_INTERNET_INFOR = 22;
    private static final int CMD_CAM_MSG_MIC_INFOR = 23;
    private static final int CMD_CAM_SET_STREAM_SIZE = 24;

    private static final int CMD_CAM_POWER_OFF = 25;
    private static final int CMD_CAM_REBOOT = 26;

    private static final int CMD_NETWORK_GET_WLAN_MODE = 27;
    private static final int CMD_NETWORK_GET_HOST_NUM = 28;
    private static final int CMD_NETWORK_GET_HOST_INFOR = 29;
    private static final int CMD_NETWORK_ADD_HOST = 30;
    private static final int CMD_NETWORK_RMV_HOST = 31;
    private static final int CMD_NETWORK_CONNECT_HOST = 32;

    private static final int CMD_NETWORK_SCANHOST = 74;

    private static final int CMD_NETWORK_SYNCTIME = 33;
    private static final int CMD_NETWORK_GET_DEVICETIME = 34;

    private static final int CMD_REC_ERROR = 35;

    private static final int CMD_AUDIO_SET_MIC = 36;
    private static final int CMD_AUDIO_GET_MIC_STATE = 37;

    private static final int CMD_FW_GET_VERSION = 38;
    private static final int CMD_FW_NEW_VERSION = 39;
    private static final int CMD_FW_DO_UPGRADE = 40;

    // 1.2
    private static final int CMD_CAM_BT_IS_SUPPORTED = 41;
    private static final int CMD_CAM_BT_IS_ENABLED = 42;
    private static final int CMD_CAM_BT_ENABLE = 43;
    private static final int CMD_CAM_BT_GET_DEV_STATUS = 44;
    private static final int CMD_CAM_BT_GET_HOST_NUM = 45;
    private static final int CMD_CAM_BT_GET_HOST_INFOR = 46;
    private static final int CMD_CAM_BT_DO_SCAN = 47;
    private static final int CMD_CAM_BT_DO_BIND = 48;
    private static final int CMD_CAM_BT_DO_UNBIND = 49;
    private static final int CMD_CAM_BT_SET_OBD_TYPES = 50;
    private static final int CMD_CAM_BT_RESERVED = 51;

    // oliver
    private static final int CMD_CAM_WANT_IDLE = 100;
    private static final int CMD_CAM_WANT_PREVIEW = 101;

    // CMD_DOMAIN_REC
    private static final int CMD_REC_START = 0;
    private static final int CMD_REC_STOP = 1;
    private static final int CMD_REC_LIST_RESOLUTIONS = 2;
    private static final int CMD_REC_SET_RESOLUTION = 3;
    private static final int CMD_REC_GET_RESOLUTION = 4;
    private static final int CMD_REC_LIST_QUALITIES = 5;
    private static final int CMD_REC_SET_QUALITY = 6;
    private static final int CMD_REC_GET_QUALITY = 7;
    private static final int CMD_REC_LIST_REC_MODES = 8;
    private static final int CMD_REC_SET_REC_MODE = 9;
    private static final int CMD_REC_GET_REC_MODE = 10;
    private static final int CMD_REC_LIST_COLOR_MODES = 11;
    private static final int CMD_REC_SET_COLOR_MODE = 12;
    private static final int CMD_REC_GET_COLOR_MODE = 13;
    private static final int CMD_REC_LIST_SEG_LENS = 14;
    private static final int CMD_REC_SET_SEG_LEN = 15;
    private static final int CMD_REC_GET_SEG_LEN = 16;
    private static final int CMD_REC_GET_STATE = 17;
    private static final int EVT_REC_STATE_CHANGE = 18;
    private static final int CMD_REC_GET_TIME = 19;
    private static final int CMD_REC_GET_TIME_RESULT = 20;

    private static final int CMD_REC_SET_DUAL_STREAM = 21;
    private static final int CMD_REC_GET_DUAL_STREAM_STATE = 22;

    private static final int CMD_REC_SET_OVERLAY = 23;
    private static final int CMD_REC_GET_OVERLAY_STATE = 24;

    private static final int CMD_REC_MARK_LIVE_VIDEO = 27;
    private static final int CMD_REC_SET_MARK_TIME = 28;
    private static final int CMD_REC_GET_MARK_TIME = 29;



    // oliver
    private static final int CMD_REC_SET_STILL_MODE = 100;
    private static final int CMD_REC_START_STILL_CAPTURE = 101;
    private static final int CMD_REC_STOP_STILL_CAPTURE = 102;

    private static final int MSG_REC_STILL_PICTURE_INFO = 103;
    private static final int MSG_REC_STILL_CAPTURE_DONE = 104;

    private static final int USER_CMD_GET_SETUP = 1;
    private static final int USER_CMD_EXIT_THREAD = 2;


    private static final Map<Integer, String> CMD_CAM_TO_STRING = new HashMap<Integer, String>() {
        {

            put(CMD_CAM_GET_MODE, "CMD_CAM_GET_MODE");
            put(CMD_CAM_GET_MODE_RESULT, "CMD_CAM_GET_MODE_RESULT");
            put(CMD_CAM_GET_API_VERSION, "CMD_CAM_GET_API_VERSION");
            put(CMD_CAM_IS_API_SUPPORTED, "CMD_CAM_IS_API_SUPPORTED");
            put(CMD_CAM_GET_NAME, "CMD_CAM_GET_NAME");
            put(CMD_CAM_GET_NAME_RESULT, "CMD_CAM_GET_NAME_RESULT");
            put(CMD_CAM_SET_NAME, "CMD_CAM_SET_NAME");
            put(CMD_CAM_SET_NAME_RESULT, "CMD_CAM_SET_NAME_RESULT");
            put(CMD_CAM_GET_STATE, "CMD_CAM_GET_STATE");


            put(CMD_CAM_GET_STATE_RESULT, "CMD_CAM_GET_STATE_RESULT");
            put(CMD_CAM_START_REC, "CMD_CAM_START_REC");
            put(CMD_CAM_STOP_REC, "CMD_CAM_STOP_REC");
            put(CMD_CAM_GET_TIME, "CMD_CAM_GET_TIME");
            put(CMD_CAM_GET_TIME_RESULT, "CMD_CAM_GET_TIME_RESULT");


            put(CMD_CAM_GET_GET_ALL_INFOR, "CMD_CAM_GET_GET_ALL_INFOR");
            put(CMD_CAM_GET_GET_STORAGE_INFOR, "CMD_CAM_GET_GET_STORAGE_INFOR");
            put(CMD_CAM_MSG_STORAGE_INFOR, "CMD_CAM_MSG_STORAGE_INFOR");
            put(CMD_CAM_MSG_STORAGE_SPACE_INFOR, "CMD_CAM_MSG_STORAGE_SPACE_INFOR");
            put(CMD_CAM_MSG_BATTERY_INFOR, "CMD_CAM_MSG_BATTERY_INFOR");
            put(CMD_CAM_MSG_POWER_INFOR, "CMD_CAM_MSG_POWER_INFOR");
            put(CMD_CAM_MSG_BT_INFOR, "CMD_CAM_MSG_BT_INFOR");
            put(CMD_CAM_MSG_GPS_INFOR, "CMD_CAM_MSG_GPS_INFOR");
            put(CMD_CAM_MSG_INTERNET_INFOR, "CMD_CAM_MSG_INTERNET_INFOR");
            put(CMD_CAM_MSG_MIC_INFOR, "CMD_CAM_MSG_MIC_INFOR");
            put(CMD_CAM_SET_STREAM_SIZE, "CMD_CAM_SET_STREAM_SIZE");


            put(CMD_CAM_POWER_OFF, "CMD_CAM_POWER_OFF");
            put(CMD_CAM_REBOOT, "CMD_CAM_REBOOT");
            put(CMD_NETWORK_GET_WLAN_MODE, "CMD_NETWORK_GET_WLAN_MODE");
            put(CMD_NETWORK_GET_HOST_NUM, "CMD_NETWORK_GET_HOST_NUM");
            put(CMD_NETWORK_GET_HOST_INFOR, "CMD_NETWORK_GET_HOST_INFOR");
            put(CMD_NETWORK_ADD_HOST, "CMD_NETWORK_ADD_HOST");
            put(CMD_NETWORK_RMV_HOST, "CMD_NETWORK_RMV_HOST");
            put(CMD_NETWORK_CONNECT_HOST, "CMD_NETWORK_CONNECT_HOST");
            put(CMD_NETWORK_SYNCTIME, "CMD_NETWORK_SYNCTIME");
            put(CMD_NETWORK_GET_DEVICETIME, "CMD_NETWORK_GET_DEVICETIME");
            put(CMD_REC_ERROR, "CMD_REC_ERROR");
            put(CMD_AUDIO_SET_MIC, "CMD_AUDIO_SET_MIC");
            put(CMD_AUDIO_GET_MIC_STATE, "CMD_AUDIO_GET_MIC_STATE");
            put(CMD_FW_GET_VERSION, "CMD_FW_GET_VERSION");
            put(CMD_FW_NEW_VERSION, "CMD_FW_NEW_VERSION");


            put(CMD_CAM_BT_IS_SUPPORTED, "CMD_CAM_BT_IS_SUPPORTED");
            put(CMD_CAM_BT_IS_ENABLED, "CMD_CAM_BT_IS_ENABLED");
            put(CMD_CAM_BT_ENABLE, "CMD_CAM_BT_ENABLE");
            put(CMD_CAM_BT_GET_DEV_STATUS, "CMD_CAM_BT_GET_DEV_STATUS");
            put(CMD_CAM_BT_GET_HOST_NUM, "CMD_CAM_BT_GET_HOST_NUM");
            put(CMD_CAM_BT_GET_HOST_INFOR, "CMD_CAM_BT_GET_HOST_INFOR");
            put(CMD_CAM_BT_DO_SCAN, "CMD_CAM_BT_DO_SCAN");
            put(CMD_CAM_BT_DO_BIND, "CMD_CAM_BT_DO_BIND");
            put(CMD_CAM_BT_DO_UNBIND, "CMD_CAM_BT_DO_UNBIND");
            put(CMD_CAM_BT_SET_OBD_TYPES, "CMD_CAM_BT_SET_OBD_TYPES");
            put(CMD_CAM_BT_RESERVED, "CMD_CAM_BT_RESERVED");
            put(CMD_CAM_WANT_IDLE, "CMD_CAM_WANT_IDLE");

            put(CMD_CAM_WANT_PREVIEW, "CMD_CAM_WANT_PREVIEW");


        }
    };


    private static Map<Integer, String> CMD_REC_TO_STRING = new HashMap<Integer, String>() {
        {

            put(CMD_REC_START, "CMD_REC_START");
            put(CMD_REC_STOP, "CMD_REC_STOP");
            put(CMD_REC_LIST_RESOLUTIONS, "CMD_REC_LIST_RESOLUTIONS");
            put(CMD_REC_SET_RESOLUTION, "CMD_REC_SET_RESOLUTION");
            put(CMD_REC_GET_RESOLUTION, "CMD_REC_GET_RESOLUTION");
            put(CMD_REC_LIST_QUALITIES, "CMD_REC_LIST_QUALITIES");
            put(CMD_REC_SET_QUALITY, "CMD_REC_SET_QUALITY");
            put(CMD_REC_GET_QUALITY, "CMD_REC_GET_QUALITY");
            put(CMD_REC_LIST_REC_MODES, "CMD_REC_LIST_REC_MODES");
            put(CMD_REC_SET_REC_MODE, "CMD_REC_SET_REC_MODE");
            put(CMD_REC_GET_REC_MODE, "CMD_REC_GET_REC_MODE");
            put(CMD_REC_LIST_COLOR_MODES, "CMD_REC_LIST_COLOR_MODES");
            put(CMD_REC_SET_COLOR_MODE, "CMD_REC_SET_COLOR_MODE");
            put(CMD_REC_GET_COLOR_MODE, "CMD_REC_GET_COLOR_MODE");
            put(CMD_REC_LIST_SEG_LENS, "CMD_REC_LIST_SEG_LENS");
            put(CMD_REC_SET_SEG_LEN, "CMD_REC_SET_SEG_LEN");
            put(CMD_REC_GET_SEG_LEN, "CMD_REC_GET_SEG_LEN");
            put(EVT_REC_STATE_CHANGE, "EVT_REC_STATE_CHANGE");
            put(CMD_REC_GET_TIME, "CMD_REC_GET_TIME");
            put(CMD_REC_GET_TIME_RESULT, "CMD_REC_GET_TIME_RESULT");
            put(CMD_REC_SET_DUAL_STREAM, "CMD_REC_SET_DUAL_STREAM");
            put(CMD_REC_GET_DUAL_STREAM_STATE, "CMD_REC_GET_DUAL_STREAM_STATE");
            put(CMD_REC_SET_OVERLAY, "CMD_REC_SET_OVERLAY");
            put(CMD_REC_GET_OVERLAY_STATE, "CMD_REC_GET_OVERLAY_STATE");
            put(CMD_REC_MARK_LIVE_VIDEO, "CMD_REC_MARK_LIVE_VIDEO");
            put(CMD_REC_SET_MARK_TIME, "CMD_REC_SET_MARK_TIME");
            put(CMD_REC_GET_MARK_TIME, "CMD_REC_GET_MARK_TIME");
            put(CMD_REC_SET_STILL_MODE, "CMD_REC_SET_STILL_MODE");
            put(CMD_REC_START_STILL_CAPTURE, "CMD_REC_START_STILL_CAPTURE");
            put(CMD_REC_STOP_STILL_CAPTURE, "CMD_REC_STOP_STILL_CAPTURE");
            put(MSG_REC_STILL_PICTURE_INFO, "MSG_REC_STILL_PICTURE_INFO");
            put(MSG_REC_STILL_CAPTURE_DONE, "MSG_REC_STILL_CAPTURE_DONE");


        }
    };

    public interface Listener {
        void onConnected();

        void onDisconnected();

        void onBtStateChanged();

        void onGpsStateChanged();

        void onWifiStateChanged();

        void onStartRecordError(int error);

        void onHostSSIDFetched(String ssid);

        void onScanBtDone();

        void onBtDevInfo(int type, String mac, String name);

        void onStillCaptureStarted(boolean bOneShot);

        void onStillPictureInfo(boolean bCapturing, int numPictures, int burstTicks);

        void onStillCaptureDone();

        void onScanHostResult(List<NetworkItemBean> networkList);
    }


    private Listener mListener;

    public void setListener(Listener listener) {
        mListener = listener;
    }


    private final CameraState mStates;
    private final BtState mBtStates = new BtState();
    private final GpsState mGpsStates = new GpsState();
    private final WifiState mWifiStates = new WifiState();


    public VdtCameraController(InetAddress host, int port, CameraState state) {
        InetSocketAddress address = new InetSocketAddress(host, port);
        Logger.t("testconnect").d("create connection " + address.getAddress());
        mConnection = new MyTcpConnection("ccam", address);
        this.mStates = state;
    }


    public boolean syncBtState(BtState user) {
        return mBtStates.syncStates(user);
    }

    public boolean syncGpsState(GpsState user) {
        return mGpsStates.syncStates(user);
    }

    public boolean syncWifiState(WifiState user) {
        return mWifiStates.syncStates(user);
    }

    public InetSocketAddress getInetSocketAddress() {
        return getConnection().getInetSocketAddress();
    }

    // domains


    static final class Request {
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
        mStates.setApiVersion(main, sub, build);
    }

    // ========================================================
    // CMD_CAM_IS_API_SUPPORTED
    // ========================================================

    // ========================================================
    // CMD_CAM_GET_NAME
    // ========================================================
    public void cmd_Cam_get_Name() {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_GET_NAME);
    }

    // ========================================================
    // CMD_CAM_GET_NAME_RESULT
    // ========================================================
    private void ack_Cam_get_Name_result(String p1, String p2) {
        mStates.setCameraName(p1);
    }

    // ========================================================
    // CMD_CAM_SET_NAME
    // ========================================================
    public void cmd_Cam_set_Name(String name) {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_SET_NAME, name, "");
    }

    // ========================================================
    // CMD_CAM_SET_NAME_RESULT - not used
    // ========================================================

    // ========================================================
    // CMD_CAM_GET_STATE
    // ========================================================
    public void cmd_Cam_get_State() {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_GET_STATE);
    }

    // ========================================================
    // CMD_CAM_GET_STATE_RESULT
    // ========================================================
    private void ack_Cam_get_State_result(String p1, String p2) {
        int state = Integer.parseInt(p1);
        boolean is_still = p2.length() > 0 ? Integer.parseInt(p2) != 0 : false;
        mStates.setRecordState(state, is_still);

    }

    // ========================================================
    // CMD_CAM_START_REC
    // ========================================================
    public void cmd_Cam_start_rec() {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_START_REC);
    }

    // ========================================================
    // CMD_CAM_STOP_REC
    // ========================================================
    public void ack_Cam_stop_rec() {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_STOP_REC);
    }

    // ========================================================
    // CMD_CAM_GET_TIME
    // ========================================================
    public void ack_Cam_get_time() {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_GET_TIME);
    }

    // ========================================================
    // CMD_CAM_GET_TIME_RESULT
    // ========================================================
    private void ack_Cam_get_time_result(String p1, String p2) {
        int duration = Integer.parseInt(p1);
        mStates.setRecordDuration(duration);
    }

    // ========================================================
    // CMD_CAM_GET_GET_ALL_INFOR
    // audio, power, storage, gps
    // ========================================================
    public void cmd_Cam_get_getAllInfor() {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_GET_GET_ALL_INFOR);
    }

    // ========================================================
    // CMD_CAM_GET_GET_STORAGE_INFOR
    // ========================================================
    public void cmd_Cam_get_getStorageInfor() {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_GET_GET_STORAGE_INFOR);
    }

    // ========================================================
    // CMD_CAM_MSG_STORAGE_INFOR
    // ========================================================
    private void ack_Cam_msg_Storage_infor(String p1, String p2) {
        int state = Integer.parseInt(p1);
        mStates.setStorageState(state);
    }

    // ========================================================
    // CMD_CAM_MSG_STORAGE_SPACE_INFOR
    // ========================================================
    private void ack_Cam_msg_StorageSpace_infor(String p1, String p2) {
        long totalSpace = p1.length() > 0 ? Long.parseLong(p1) : 0;
        long freeSpace = p2.length() > 0 ? Long.parseLong(p2) : 0;
        mStates.setStorageSpace(totalSpace, freeSpace);
    }

    // ========================================================
    // CMD_CAM_MSG_BATTERY_INFOR
    // ========================================================
    private void ack_Cam_msg_Battery_infor(String p1, String p2) {
        int vol = Integer.parseInt(p2);
        mStates.setBatteryVol(vol);
    }

    // ========================================================
    // CMD_CAM_MSG_POWER_INFOR
    // ========================================================
    private void ack_Cam_msg_power_infor(String p1, String p2) {
        if (p1.length() == 0 || p2.length() == 0) {
            // workaround: request again after 1 s
            Logger.t(TAG).d("bad power info, schedule update");
//            mQueue.scheduleGetAllInfo();
        } else {
            int batteryState = CameraState.STATE_BATTERY_UNKNOWN;
            if (p1.equals("Full")) {
                batteryState = CameraState.STATE_BATTERY_FULL;
            } else if (p1.equals("Not charging")) {
                batteryState = CameraState.STATE_BATTERY_NOT_CHARGING;
            } else if (p1.equals("Discharging")) {
                batteryState = CameraState.STATE_BATTERY_DISCHARGING;
            } else if (p1.equals("Charging")) {
                batteryState = CameraState.STATE_BATTERY_CHARGING;
            }
            int powerState = Integer.parseInt(p2);
            mStates.setPowerState(batteryState, powerState);
        }
    }

    // ========================================================
    // CMD_CAM_MSG_BT_INFOR
    // ========================================================

    // ========================================================
    // CMD_CAM_MSG_GPS_INFOR
    // ========================================================
    private void ack_Cam_msg_GPS_infor(String p1, String p2) {
        int state = Integer.parseInt(p1);
        mGpsStates.setGpsState(state);
    }

    // ========================================================
    // CMD_CAM_MSG_INTERNET_INFOR
    // ========================================================

    // ========================================================
    // CMD_CAM_MSG_MIC_INFOR
    // ========================================================
    private void ack_Cam_msg_Mic_infor(String p1, String p2) {
        int state = Integer.parseInt(p1);
        int vol = Integer.parseInt(p2);
        mStates.setMicState(state, vol);
    }

    // ========================================================
    // CMD_CAM_SET_STREAM_SIZE
    // ========================================================

    // ========================================================
    // CMD_CAM_POWER_OFF
    // ========================================================
    public void cmd_Cam_PowerOff() {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_POWER_OFF);
    }

    // ========================================================
    // CMD_CAM_REBOOT
    // ========================================================
    public void cmd_Cam_Reboot() {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_REBOOT);
    }

    // ========================================================
    // CMD_NETWORK_GET_W_LAN_MODE
    // ========================================================
    public void cmd_Network_GetWLanMode() {
        postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_GET_WLAN_MODE);
    }

    private void ack_Network_GetWLanMode(String p1, String p2) {
        int mode = Integer.parseInt(p1);
        mWifiStates.setWifiMode(mode);
    }

    // ========================================================
    // CMD_NETWORK_GET_HOST_NUM
    // ========================================================
    public void cmd_Network_GetHostNum() {
        postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_GET_HOST_NUM);
    }

    private void ack_Network_GetHostNum(String p1, String p2) {
        int num = Integer.parseInt(p1);
        mWifiStates.setNumWifiAP(num);
        for (int i = 0; i < num; i++) {
            cmd_Network_GetHostInfor(i);
        }
    }

    // ========================================================
    // CMD_NETWORK_GET_HOST_INFOR
    // ========================================================
    public void cmd_Network_GetHostInfor(int index) {
        postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_GET_HOST_INFOR, index);
    }

    private void ack_Network_GetHostInfor(String p1, String p2) {
        if (mListener != null) {
            mListener.onHostSSIDFetched(p1);
        }
    }

    // ========================================================
    // CMD_NETWORK_ADD_HOST
    // ========================================================
    public void cmd_Network_AddHost(String hostName, String password) {
        postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_ADD_HOST, hostName, password);
        cmd_Network_GetHostNum();
    }

    // ========================================================
    // CMD_NETWORK_RMV_HOST
    // ========================================================
    public void cmd_Network_RmvHost(String hostName) {
        postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_RMV_HOST, hostName, "");
        cmd_Network_GetHostNum();
    }

    // ========================================================
    // CMD_NETWORK_CONNECT_HOST
    // ========================================================
    public void cmd_Network_ConnectHost(int mode, String apName) {
        if (apName == null)
            apName = "";
        postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_CONNECT_HOST, Integer.toString(mode), apName);
    }

    // ========================================================
    // CMD_NETWORK_SYNCTIME
    // ========================================================
    public void cmd_Network_Synctime(long time, int timezone) {
        String p1 = Long.toString(time);
        String p2 = Integer.toString(timezone);
        postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_SYNCTIME, p1, p2);
    }

    public void cmd_Network_ScanHost() {
        postRequest(CMD_DOMAIN_CAM, CMD_NETWORK_SCANHOST);
    }

    // ========================================================
    // CMD_NETWORK_GET_DEVICETIME
    // ========================================================

    // ========================================================
    // CMD_REC_ERROR
    // ========================================================
    private void ack_Rec_error(String p1, String p2) {
        int error = Integer.parseInt(p1);
        if (mListener != null) {
            mListener.onStartRecordError(error);
        }
    }

    // ========================================================
    // CMD_AUDIO_SET_MIC
    // ========================================================
    public void cmd_audio_setMic(int state, int gain) {
        if (state == CameraState.STATE_MIC_ON && gain == 0)
            gain = 5;
        String p1 = Integer.toString(state);
        String p2 = Integer.toString(gain);
        postRequest(CMD_DOMAIN_CAM, CMD_AUDIO_SET_MIC, p1, p2);
    }

    // ========================================================
    // CMD_AUDIO_GET_MIC_STATE
    // ========================================================
    public void cmd_audio_getMicState() {
        postRequest(CMD_DOMAIN_CAM, CMD_AUDIO_GET_MIC_STATE);
    }

    // ========================================================
    // CMD_FW_GET_VERSION
    // ========================================================
    public void cmd_fw_getVersion() {
        postRequest(CMD_DOMAIN_CAM, CMD_FW_GET_VERSION);
    }

    private void ack_fw_getVersion(String p1, String p2) {
        mStates.setFirmwareVersion(p2);
    }

    // ========================================================
    // CMD_CAM_BT_IS_SUPPORTED
    // ========================================================
    public void cmd_CAM_BT_isSupported() {
        if (mStates.version12() && mBtStates.mBtSupport == BtState.BT_SUPPORT_UNKNOWN) {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_IS_SUPPORTED);
        }
    }

    private void ack_CAM_BT_isSupported(String p1) {
        mBtStates.setIsBTSupported(Integer.parseInt(p1));
    }

    // ========================================================
    // CMD_CAM_BT_IS_ENABLED
    // ========================================================
    public void cmd_CAM_BT_isEnabled() {
        if (mStates.version12()) {
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

    // ========================================================
    // CMD_CAM_BT_ENABLE
    // ========================================================
    public void cmd_CAM_BT_Enable(boolean bEnable) {
        if (mStates.version12()) {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_ENABLE, bEnable ? 1 : 0);
        }
    }

    // ========================================================
    // CMD_CAM_BT_GET_DEV_STATUS
    // ========================================================
    public void cmd_CAM_BT_getDEVStatus(int type) {
        if (mStates.version12()) {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_GET_DEV_STATUS, type);
        }
    }

    // 259, 11:D6:00:BB:71:58#Smart Shutter
    // 256, NA#Unknown
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

    // ========================================================
    // CMD_CAM_BT_GET_HOST_NUM
    // ========================================================
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

    // ========================================================
    // CMD_CAM_BT_GET_HOST_INFOR
    // ========================================================
    public void cmd_CAM_BT_getHostInfor(int index) {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_GET_HOST_INFOR, index);
    }

    // p1: name; p2: mac
    private void ack_CAM_BT_getHostInfor(String p1, String p2) {
        int type = p1.indexOf("OBD") >= 0 ? BtState.BT_TYPE_OBD : BtState.BT_TYPE_HID;
        if (mListener != null) {
            mListener.onBtDevInfo(type, p2, p1);
        }

    }

    // ========================================================
    // CMD_CAM_BT_DO_SCAN
    // ========================================================
    public void cmd_CAM_BT_doScan() {
        if (mStates.version12()) {
            postRequest(CMD_DOMAIN_CAM, CMD_CAM_BT_DO_SCAN);
        }
    }

    private void ack_CAM_BT_doScan() {
        mBtStates.scanBtDone();
        if (mListener != null) {
            mListener.onScanBtDone();
        }
    }

    // ========================================================
    // CMD_CAM_BT_DO_BIND
    // ========================================================
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

    // ========================================================
    // CMD_CAM_BT_DO_UN_BIND
    // ========================================================
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

    // ========================================================
    // CMD_CAM_BT_SET_OBD_TYPES
    // ========================================================
    public void cmd_CAM_BT_setOBDTypes() {
    }

    // ========================================================
    // CMD_CAM_WANT_IDLE
    // ========================================================
    public void cmd_CAM_WantIdle() {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_WANT_IDLE);
    }

    // ========================================================
    // CMD_CAM_WANT_PREVIEW
    // ========================================================
    public void cmd_CAM_WantPreview() {
        postRequest(CMD_DOMAIN_CAM, CMD_CAM_WANT_PREVIEW);
    }

    // /////////////////////////////////////////////////////////

    // ========================================================
    // CMD_REC_START - not used
    // ========================================================
    // ========================================================
    // CMD_REC_STOP - not used
    // ========================================================

    // ========================================================
    // CMD_REC_LIST_RESOLUTIONS
    // ========================================================
    public void cmd_Rec_List_Resolutions() {
        postRequest(CMD_DOMAIN_REC, CMD_REC_LIST_RESOLUTIONS);
    }

    private void ack_Rec_List_Resolutions(String p1, String p2) {
        int list = Integer.parseInt(p1);
        mStates.setVideoResolutionList(list);
    }

    // ========================================================
    // CMD_REC_SET_RESOLUTION
    // ========================================================
    public void cmd_Rec_Set_Resolution(int index) {
        postRequest(CMD_DOMAIN_REC, CMD_REC_SET_RESOLUTION, index);
    }

    // ========================================================
    // CMD_REC_GET_RESOLUTION
    // ========================================================
    public void cmd_Rec_get_Resolution() {
        postRequest(CMD_DOMAIN_REC, CMD_REC_GET_RESOLUTION);
    }

    private void ack_Rec_get_Resolution(String p1, String p2) {
        int index = Integer.parseInt(p1);
        mStates.setVideoResolution(index);
    }

    // ========================================================
    // CMD_REC_LIST_QUALITIES
    // ========================================================
    public void cmd_Rec_List_Qualities() {
        postRequest(CMD_DOMAIN_REC, CMD_REC_LIST_QUALITIES);
    }

    private void ack_Rec_List_Qualities(String p1, String p2) {
        int list = Integer.parseInt(p1);
        mStates.setVideoQualityList(list);
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
        mStates.setVideoQuality(index);
    }

    // ========================================================
    // CMD_REC_LIST_REC_MODES
    // ========================================================
    public void cmd_Rec_List_RecModes() {
        postRequest(CMD_DOMAIN_REC, CMD_REC_LIST_REC_MODES);
    }

    private void ack_Rec_List_RecModes(String p1, String p2) {
        int list = Integer.parseInt(p1);
        mStates.setRecordModeList(list);
    }

    // ========================================================
    // CMD_REC_SET_REC_MODE
    // ========================================================
    public void cmd_Rec_Set_RecMode(int index) {
        postRequest(CMD_DOMAIN_REC, CMD_REC_SET_REC_MODE, index);
    }

    // ========================================================
    // CMD_REC_GET_REC_MODE
    // ========================================================
    public void cmd_Rec_get_RecMode() {
        postRequest(CMD_DOMAIN_REC, CMD_REC_GET_REC_MODE);
    }

    private void ack_Rec_get_RecMode(String p1, String p2) {
        int index = Integer.parseInt(p1);
        mStates.setRecordMode(index);
    }

    // ========================================================
    // CMD_REC_LIST_COLOR_MODES
    // ========================================================
    public void cmd_Rec_List_ColorModes() {
        postRequest(CMD_DOMAIN_REC, CMD_REC_LIST_COLOR_MODES);
    }

    private void ack_Rec_List_ColorModes(String p1, String p2) {
        int list = Integer.parseInt(p1);
        mStates.setColorModeList(list);
    }

    // ========================================================
    // CMD_REC_SET_COLOR_MODE
    // ========================================================
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
        mStates.setColorMode(index);
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
        mStates.setOverlayFlags(flags);
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
        if (mListener != null) {
            mListener.onStillCaptureStarted(bOneShot);
        }
    }

    // ========================================================
    // CMD_REC_STOP_STILL_CAPTURE
    // ========================================================
    public void cmd_Rec_StopStillCapture() {
        postRequest(CMD_DOMAIN_REC, CMD_REC_STOP_STILL_CAPTURE);
    }

    // ========================================================
    // MSG_REC_STILL_PICTURE_INFO
    // ========================================================
    private void ack_Rec_StillPictureInfo(String p1, String p2) {
        int value_p1 = p1.length() > 0 ? Integer.parseInt(p1) : 0;
        boolean bCapturing = (value_p1 & 1) != 0;
        int numPictures = p2.length() > 0 ? Integer.parseInt(p2) : 0;
        int burstTicks = value_p1 >>> 1;
        if (mListener != null) {
            mListener.onStillPictureInfo(bCapturing, numPictures, burstTicks);
        }
    }

    // ========================================================
    // MSG_REC_STILL_CAPTURE_DONE
    // ========================================================
    private void ack_Rec_StillCaptureDone() {
        if (mListener != null) {
            mListener.onStillCaptureDone();
        }
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
        Log.e("test", String.format("cmd_Rec_GetMarkTime: p1: %s, p2: %s", p1, p2));
        try {
            mStates.setMarkTime(Integer.parseInt(p1), Integer.parseInt(p2));
        } catch (Exception e) {
            Log.e("test", String.format("cmd_Rec_GetMarkTime: p1: %s, p2: %s", p1, p2), e);
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
        Log.e("test", String.format("ack_Rec_SetMarkTime: p1: %s, p2: %s", p1, p2));
        try {
            mStates.setMarkTime(Integer.parseInt(p1), Integer.parseInt(p2));
        } catch (Exception e) {
            Log.e("test", String.format("ack_Rec_SetMarkTime: p1: %s, p2: %s", p1, p2), e);
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

            String cmd;
            if (request.mDomain == CMD_DOMAIN_CAM) {
                cmd = CMD_CAM_TO_STRING.get(request.mCmd);
            } else {
                cmd = CMD_REC_TO_STRING.get(request.mCmd);
            }
            //Logger.t(TAG).d("Send domain: " + DOMAIN_TO_STRING.get(request.mDomain) + " cmd: " + cmd);

            writeRequest(request);

        }
    }

    static private final Pattern mPattern = Pattern.compile("ECMD(\\d+).(\\d+)", Pattern.CASE_INSENSITIVE
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

        SimpleInputStream sis = new SimpleInputStream(1024);
        XmlPullParser xpp = Xml.newPullParser();
        int length = 0;
        int appended = 0;

        try {

            while (!thread.isInterrupted()) {

                mGpsStates.mbSchedule = false;
                mWifiStates.mbSchedule = false;
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

                if (mWifiStates.mbSchedule) {
                    mWifiStates.mbSchedule = false;
//                    mQueue.scheduleUpdate(Queue.SCHEDULE_WIFI_UPDATE);
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
        Logger.t(TAG).d("p1: " + p1 + " P2: " + p2);
        List<NetworkItemBean> networkItemBeanList = new ArrayList<>();
        try {
            JSONObject object = new JSONObject(p1);
            JSONArray networks = object.getJSONArray("networks");
            for (int i = 0; i < networks.length(); i++) {
                NetworkItemBean networkItem = new NetworkItemBean();
                networkItem.ssid = object.optString("ssid");
                networkItem.bssid = object.optString("bssid");
                networkItem.flags = object.optString("flags");
                networkItem.frequency = object.optInt("frequency");
                networkItem.singalLevel = object.optInt("signal_level");
                networkItemBeanList.add(networkItem);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (mListener != null) {
            mListener.onScanHostResult(networkItemBeanList);
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
            mListener.onConnected();
        }

        @Override
        public void onConnectErrorAsync() {
            mListener.onDisconnected();
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
