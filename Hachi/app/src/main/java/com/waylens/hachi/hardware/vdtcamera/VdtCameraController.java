package com.waylens.hachi.hardware.vdtcamera;

import android.util.Log;
import android.util.Xml;

import com.transee.ccam.BtState;
import com.transee.ccam.GpsState;
import com.transee.ccam.SimpleInputStream;
import com.transee.ccam.SimpleOutputStream;
import com.transee.ccam.WifiState;
import com.transee.common.CmdQueue;
import com.transee.common.TcpConnection;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class VdtCameraController {
    public static final String TAG = VdtCameraController.class.getSimpleName();
    static final boolean DEBUG = true;


    abstract public void onConnected();

    abstract public void onDisconnected();

    abstract public void onCameraStateChanged();

    abstract public void onBtStateChanged();

    abstract public void onGpsStateChanged();

    abstract public void onWifiStateChanged();

    abstract public void onStartRecordError(int error);

    abstract public void onHostSSIDFetched(String ssid);

    abstract public void onScanBtDone();

    abstract public void onBtDevInfo(int type, String mac, String name);

    abstract public void onStillCaptureStarted(boolean bOneShot);

    abstract public void onStillPictureInfo(boolean bCapturing, int numPictures, int burstTicks);

    abstract public void onStillCaptureDone();

    private final CameraState mStates = new CameraState();
    private final BtState mBtStates = new BtState();
    private final GpsState mGpsStates = new GpsState();
    private final WifiState mWifiStates = new WifiState();


    public boolean syncState(CameraState user) {
        return mStates.syncStates(user);
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
    public static final int CMD_Domain_user = -1;
    public static final int CMD_Domain_cam = 0;
    public static final int CMD_Domain_p2p = 1;
    public static final int CMD_Domain_rec = 2;
    public static final int CMD_Domain_decode = 3;
    public static final int CMD_Domain_network = 4;
    public static final int CMD_Domain_power = 5;
    public static final int CMD_Domain_storage = 6;
    public static final int CMD_Domain_stream = 7;
    public static final int CMD_Domain_MotorControl = 8;

    // CMD_Domain_cam
    public static final int CMD_Cam_getMode = 0;
    public static final int CMD_Cam_getMode_result = 1;
    public static final int CMD_Cam_getApiVersion = 2;
    public static final int CMD_Cam_isApiSupported = 3;
    public static final int CMD_Cam_get_Name = 4;
    public static final int CMD_Cam_get_Name_result = 5;
    public static final int CMD_Cam_set_Name = 6;
    public static final int CMD_Cam_set_Name_result = 7;
    public static final int CMD_Cam_get_State = 8;
    public static final int CMD_Cam_get_State_result = 9;
    public static final int CMD_Cam_start_rec = 10;
    public static final int CMD_Cam_stop_rec = 11;
    public static final int CMD_Cam_get_time = 12;
    public static final int CMD_Cam_get_time_result = 13;

    public static final int CMD_Cam_get_getAllInfor = 14;
    public static final int CMD_Cam_get_getStorageInfor = 15;
    public static final int CMD_Cam_msg_Storage_infor = 16;
    public static final int CMD_Cam_msg_StorageSpace_infor = 17;
    public static final int CMD_Cam_msg_Battery_infor = 18;
    public static final int CMD_Cam_msg_power_infor = 19;
    public static final int CMD_Cam_msg_BT_infor = 20;
    public static final int CMD_Cam_msg_GPS_infor = 21;
    public static final int CMD_Cam_msg_Internet_infor = 22;
    public static final int CMD_Cam_msg_Mic_infor = 23;
    public static final int CMD_Cam_set_StreamSize = 24;

    public static final int CMD_Cam_PowerOff = 25;
    public static final int CMD_Cam_Reboot = 26;

    public static final int CMD_Network_GetWLanMode = 27;
    public static final int CMD_Network_GetHostNum = 28;
    public static final int CMD_Network_GetHostInfor = 29;
    public static final int CMD_Network_AddHost = 30;
    public static final int CMD_Network_RmvHost = 31;
    public static final int CMD_Network_ConnectHost = 32;

    public static final int CMD_Network_Synctime = 33;
    public static final int CMD_Network_GetDevicetime = 34;

    public static final int CMD_Rec_error = 35;

    public static final int CMD_audio_setMic = 36;
    public static final int CMD_audio_getMicState = 37;

    public static final int CMD_fw_getVersion = 38;
    public static final int CMD_fw_newVersion = 39;
    public static final int CMD_fw_doUpgrade = 40;

    // 1.2
    public static final int CMD_CAM_BT_isSupported = 41;
    public static final int CMD_CAM_BT_isEnabled = 42;
    public static final int CMD_CAM_BT_Enable = 43;
    public static final int CMD_CAM_BT_getDEVStatus = 44;
    public static final int CMD_CAM_BT_getHostNum = 45;
    public static final int CMD_CAM_BT_getHostInfor = 46;
    public static final int CMD_CAM_BT_doScan = 47;
    public static final int CMD_CAM_BT_doBind = 48;
    public static final int CMD_CAM_BT_doUnBind = 49;
    public static final int CMD_CAM_BT_setOBDTypes = 50;
    public static final int CMD_CAM_BT_RESERVED = 51;

    // oliver
    public static final int CMD_CAM_WantIdle = 100;
    public static final int CMD_CAM_WantPreview = 101;

    // CMD_Domain_rec
    public static final int CMD_Rec_Start = 0;
    public static final int CMD_Rec_Stop = 1;
    public static final int CMD_Rec_List_Resolutions = 2;
    public static final int CMD_Rec_Set_Resolution = 3;
    public static final int CMD_Rec_get_Resolution = 4;
    public static final int CMD_Rec_List_Qualities = 5;
    public static final int CMD_Rec_Set_Quality = 6;
    public static final int CMD_Rec_get_Quality = 7;
    public static final int CMD_Rec_List_RecModes = 8;
    public static final int CMD_Rec_Set_RecMode = 9;
    public static final int CMD_Rec_get_RecMode = 10;
    public static final int CMD_Rec_List_ColorModes = 11;
    public static final int CMD_Rec_Set_ColorMode = 12;
    public static final int CMD_Rec_get_ColorMode = 13;
    public static final int CMD_Rec_List_SegLens = 14;
    public static final int CMD_Rec_Set_SegLen = 15;
    public static final int CMD_Rec_get_SegLen = 16;
    public static final int CMD_Rec_get_State = 17;
    public static final int EVT_Rec_state_change = 18;
    public static final int CMD_Rec_getTime = 19;
    public static final int CMD_Rec_getTime_result = 20;

    public static final int CMD_Rec_setDualStream = 21;
    public static final int CMD_Rec_getDualStreamState = 22;

    public static final int CMD_Rec_setOverlay = 23;
    public static final int CMD_Rec_getOverlayState = 24;

    public static final int CMD_Rec_Mark_Live_Video = 27;
    public static final int CMD_Rec_Set_Mark_Time = 28;
    public static final int CMD_Rec_Get_Mark_Time = 29;

    // oliver
    public static final int CMD_Rec_SetStillMode = 100;
    public static final int CMD_Rec_StartStillCapture = 101;
    public static final int CMD_Rec_StopStillCapture = 102;

    public static final int MSG_Rec_StillPictureInfo = 103;
    public static final int MSG_Rec_StillCaptureDone = 104;

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
    private final Queue mQueue = new Queue();

    static final int UserCmd_GetSetup = 1;
    static final int UserCmd_ExitThread = 2;

    // all info for setup
    public void userCmd_GetSetup() {
        Request request = new Request(CMD_Domain_user, UserCmd_GetSetup, "", "");
        mQueue.postRequest(request);
    }

    public void userCmd_ExitThread() {
        Request request = new Request(CMD_Domain_user, UserCmd_ExitThread, "", "");
        mQueue.postRequest(request);
    }

    private final void postRequest(int domain, int cmd) {
        Request request = new Request(domain, cmd, "", "");
        mQueue.postRequest(request);
    }

    private final void postRequest(int domain, int cmd, String p1, String p2) {
        Request request = new Request(domain, cmd, p1, p2);
        mQueue.postRequest(request);
    }

    private final void postRequest(int domain, int cmd, int p1) {
        Request request = new Request(domain, cmd, Integer.toString(p1), "");
        mQueue.postRequest(request);
    }

    // ========================================================
    // CMD_Cam_getMode
    // ========================================================
    public void cmd_Cam_getMode() {
        postRequest(CMD_Domain_cam, CMD_Cam_getMode);
    }

    // ========================================================
    // CMD_Cam_getMode_result
    // ========================================================

    // ========================================================
    // CMD_Cam_getApiVersion
    // ========================================================
    public void cmd_Cam_getApiVersion() {
        postRequest(CMD_Domain_cam, CMD_Cam_getApiVersion);
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
    // CMD_Cam_isApiSupported
    // ========================================================

    // ========================================================
    // CMD_Cam_get_Name
    // ========================================================
    public void cmd_Cam_get_Name() {
        postRequest(CMD_Domain_cam, CMD_Cam_get_Name);
    }

    // ========================================================
    // CMD_Cam_get_Name_result
    // ========================================================
    private void ack_Cam_get_Name_result(String p1, String p2) {
        mStates.setCameraName(p1);
    }

    // ========================================================
    // CMD_Cam_set_Name
    // ========================================================
    public void cmd_Cam_set_Name(String name) {
        postRequest(CMD_Domain_cam, CMD_Cam_set_Name, name, "");
    }

    // ========================================================
    // CMD_Cam_set_Name_result - not used
    // ========================================================

    // ========================================================
    // CMD_Cam_get_State
    // ========================================================
    public void cmd_Cam_get_State() {
        postRequest(CMD_Domain_cam, CMD_Cam_get_State);
    }

    // ========================================================
    // CMD_Cam_get_State_result
    // ========================================================
    private void ack_Cam_get_State_result(String p1, String p2) {
        int state = Integer.parseInt(p1);
        boolean is_still = p2.length() > 0 ? Integer.parseInt(p2) != 0 : false;
        mStates.setRecordState(state, is_still);
    }

    // ========================================================
    // CMD_Cam_start_rec
    // ========================================================
    public void cmd_Cam_start_rec() {
        postRequest(CMD_Domain_cam, CMD_Cam_start_rec);
    }

    // ========================================================
    // CMD_Cam_stop_rec
    // ========================================================
    public void ack_Cam_stop_rec() {
        postRequest(CMD_Domain_cam, CMD_Cam_stop_rec);
    }

    // ========================================================
    // CMD_Cam_get_time
    // ========================================================
    public void ack_Cam_get_time() {
        postRequest(CMD_Domain_cam, CMD_Cam_get_time);
    }

    // ========================================================
    // CMD_Cam_get_time_result
    // ========================================================
    private void ack_Cam_get_time_result(String p1, String p2) {
        int duration = Integer.parseInt(p1);
        mStates.setRecordDuration(duration);
    }

    // ========================================================
    // CMD_Cam_get_getAllInfor
    // audio, power, storage, gps
    // ========================================================
    public void cmd_Cam_get_getAllInfor() {
        postRequest(CMD_Domain_cam, CMD_Cam_get_getAllInfor);
    }

    // ========================================================
    // CMD_Cam_get_getStorageInfor
    // ========================================================
    public void cmd_Cam_get_getStorageInfor() {
        postRequest(CMD_Domain_cam, CMD_Cam_get_getStorageInfor);
    }

    // ========================================================
    // CMD_Cam_msg_Storage_infor
    // ========================================================
    private void ack_Cam_msg_Storage_infor(String p1, String p2) {
        int state = Integer.parseInt(p1);
        mStates.setStorageState(state);
    }

    // ========================================================
    // CMD_Cam_msg_StorageSpace_infor
    // ========================================================
    private void ack_Cam_msg_StorageSpace_infor(String p1, String p2) {
        long totalSpace = p1.length() > 0 ? Long.parseLong(p1) : 0;
        long freeSpace = p2.length() > 0 ? Long.parseLong(p2) : 0;
        mStates.setStorageSpace(totalSpace, freeSpace);
    }

    // ========================================================
    // CMD_Cam_msg_Battery_infor
    // ========================================================
    private void ack_Cam_msg_Battery_infor(String p1, String p2) {
        int vol = Integer.parseInt(p2);
        mStates.setBatteryVol(vol);
    }

    // ========================================================
    // CMD_Cam_msg_power_infor
    // ========================================================
    private void ack_Cam_msg_power_infor(String p1, String p2) {
        if (p1.length() == 0 || p2.length() == 0) {
            // workaround: request again after 1 s
            if (DEBUG) {
                Log.d(TAG, "bad power info, schedule update");
            }
            mQueue.scheduleGetAllInfo();
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
    // CMD_Cam_msg_BT_infor
    // ========================================================

    // ========================================================
    // CMD_Cam_msg_GPS_infor
    // ========================================================
    private void ack_Cam_msg_GPS_infor(String p1, String p2) {
        int state = Integer.parseInt(p1);
        mGpsStates.setGpsState(state);
    }

    // ========================================================
    // CMD_Cam_msg_Internet_infor
    // ========================================================

    // ========================================================
    // CMD_Cam_msg_Mic_infor
    // ========================================================
    private void ack_Cam_msg_Mic_infor(String p1, String p2) {
        int state = Integer.parseInt(p1);
        int vol = Integer.parseInt(p2);
        mStates.setMicState(state, vol);
    }

    // ========================================================
    // CMD_Cam_set_StreamSize
    // ========================================================

    // ========================================================
    // CMD_Cam_PowerOff
    // ========================================================
    public void cmd_Cam_PowerOff() {
        postRequest(CMD_Domain_cam, CMD_Cam_PowerOff);
    }

    // ========================================================
    // CMD_Cam_Reboot
    // ========================================================
    public void cmd_Cam_Reboot() {
        postRequest(CMD_Domain_cam, CMD_Cam_Reboot);
    }

    // ========================================================
    // CMD_Network_GetWLanMode
    // ========================================================
    public void cmd_Network_GetWLanMode() {
        postRequest(CMD_Domain_cam, CMD_Network_GetWLanMode);
    }

    private void ack_Network_GetWLanMode(String p1, String p2) {
        int mode = Integer.parseInt(p1);
        mWifiStates.setWifiMode(mode);
    }

    // ========================================================
    // CMD_Network_GetHostNum
    // ========================================================
    public void cmd_Network_GetHostNum() {
        postRequest(CMD_Domain_cam, CMD_Network_GetHostNum);
    }

    private void ack_Network_GetHostNum(String p1, String p2) {
        int num = Integer.parseInt(p1);
        mWifiStates.setNumWifiAP(num);
        for (int i = 0; i < num; i++) {
            cmd_Network_GetHostInfor(i);
        }
    }

    // ========================================================
    // CMD_Network_GetHostInfor
    // ========================================================
    public void cmd_Network_GetHostInfor(int index) {
        postRequest(CMD_Domain_cam, CMD_Network_GetHostInfor, index);
    }

    private void ack_Network_GetHostInfor(String p1, String p2) {
        onHostSSIDFetched(p1);
    }

    // ========================================================
    // CMD_Network_AddHost
    // ========================================================
    public void cmd_Network_AddHost(String hostName, String password) {
        postRequest(CMD_Domain_cam, CMD_Network_AddHost, hostName, password);
        cmd_Network_GetHostNum();
    }

    // ========================================================
    // CMD_Network_RmvHost
    // ========================================================
    public void cmd_Network_RmvHost(String hostName) {
        postRequest(CMD_Domain_cam, CMD_Network_RmvHost, hostName, "");
        cmd_Network_GetHostNum();
    }

    // ========================================================
    // CMD_Network_ConnectHost
    // ========================================================
    public void cmd_Network_ConnectHost(int mode, String apName) {
        if (apName == null)
            apName = "";
        postRequest(CMD_Domain_cam, CMD_Network_ConnectHost, Integer.toString(mode), apName);
    }

    // ========================================================
    // CMD_Network_Synctime
    // ========================================================
    public void cmd_Network_Synctime(long time, int timezone) {
        String p1 = Long.toString(time);
        String p2 = Integer.toString(timezone);
        postRequest(CMD_Domain_cam, CMD_Network_Synctime, p1, p2);
    }

    // ========================================================
    // CMD_Network_GetDevicetime
    // ========================================================

    // ========================================================
    // CMD_Rec_error
    // ========================================================
    private void ack_Rec_error(String p1, String p2) {
        int error = Integer.parseInt(p1);
        onStartRecordError(error);
    }

    // ========================================================
    // CMD_audio_setMic
    // ========================================================
    public void cmd_audio_setMic(int state, int gain) {
        if (state == CameraState.STATE_MIC_ON && gain == 0)
            gain = 5;
        String p1 = Integer.toString(state);
        String p2 = Integer.toString(gain);
        postRequest(CMD_Domain_cam, CMD_audio_setMic, p1, p2);
    }

    // ========================================================
    // CMD_audio_getMicState
    // ========================================================
    public void cmd_audio_getMicState() {
        postRequest(CMD_Domain_cam, CMD_audio_getMicState);
    }

    // ========================================================
    // CMD_fw_getVersion
    // ========================================================
    public void cmd_fw_getVersion() {
        postRequest(CMD_Domain_cam, CMD_fw_getVersion);
    }

    private void ack_fw_getVersion(String p1, String p2) {
        mStates.setFirmwareVersion(p2);
    }

    // ========================================================
    // CMD_CAM_BT_isSupported
    // ========================================================
    public void cmd_CAM_BT_isSupported() {
        if (mStates.version12() && mBtStates.mBtSupport == BtState.BT_Support_Unknown) {
            postRequest(CMD_Domain_cam, CMD_CAM_BT_isSupported);
        }
    }

    private void ack_CAM_BT_isSupported(String p1) {
        mBtStates.setIsBTSupported(Integer.parseInt(p1));
    }

    // ========================================================
    // CMD_CAM_BT_isEnabled
    // ========================================================
    public void cmd_CAM_BT_isEnabled() {
        if (mStates.version12()) {
            postRequest(CMD_Domain_cam, CMD_CAM_BT_isEnabled);
        }
    }

    private void ack_CAM_BT_isEnabled(String p1) {
        int enabled = Integer.parseInt(p1);
        mBtStates.setIsBTEnabled(enabled);
        if (enabled == BtState.BT_State_Enabled) {
            cmd_CAM_BT_getDEVStatus(BtState.BT_Type_HID);
            cmd_CAM_BT_getDEVStatus(BtState.BT_Type_OBD);
        }
    }

    // ========================================================
    // CMD_CAM_BT_Enable
    // ========================================================
    public void cmd_CAM_BT_Enable(boolean bEnable) {
        if (mStates.version12()) {
            postRequest(CMD_Domain_cam, CMD_CAM_BT_Enable, bEnable ? 1 : 0);
        }
    }

    // ========================================================
    // CMD_CAM_BT_getDEVStatus
    // ========================================================
    public void cmd_CAM_BT_getDEVStatus(int type) {
        if (mStates.version12()) {
            postRequest(CMD_Domain_cam, CMD_CAM_BT_getDEVStatus, type);
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
            dev_state = BtState.BTDEV_State_Off;
            mac = "";
            name = "";
        }
        mBtStates.setDevState(dev_type, dev_state, mac, name);
    }

    // ========================================================
    // CMD_CAM_BT_getHostNum
    // ========================================================
    public void cmd_CAM_BT_getHostNum() {
        postRequest(CMD_Domain_cam, CMD_CAM_BT_getHostNum);
    }

    private void ack_CAM_BT_getHostNum(String p1) {
        int numDevs = Integer.parseInt(p1);
        if (numDevs < 0) {
            numDevs = 0;
        }
        mBtStates.setNumDevs(numDevs);
        //onBtDevInfo(BtState.BT_Type_HID, "11:D6:00:BB:71:58", "Smart Shutter");
        //onBtDevInfo(BtState.BT_Type_OBD, "11:D6:00:BB:71:59", "OBD device");
        //onBtDevInfo(-1, "11:D6:00:BB:71:60", "Smart Shutter 3");
        for (int i = 0; i < numDevs; i++) {
            cmd_CAM_BT_getHostInfor(i);
        }
    }

    // ========================================================
    // CMD_CAM_BT_getHostInfor
    // ========================================================
    public void cmd_CAM_BT_getHostInfor(int index) {
        postRequest(CMD_Domain_cam, CMD_CAM_BT_getHostInfor, index);
    }

    // p1: name; p2: mac
    private void ack_CAM_BT_getHostInfor(String p1, String p2) {
        int type = p1.indexOf("OBD") >= 0 ? BtState.BT_Type_OBD : BtState.BT_Type_HID;
        onBtDevInfo(type, p2, p1);
    }

    // ========================================================
    // CMD_CAM_BT_doScan
    // ========================================================
    public void cmd_CAM_BT_doScan() {
        if (mStates.version12()) {
            postRequest(CMD_Domain_cam, CMD_CAM_BT_doScan);
        }
    }

    private void ack_CAM_BT_doScan() {
        mBtStates.scanBtDone();
        onScanBtDone();
    }

    // ========================================================
    // CMD_CAM_BT_doBind
    // ========================================================
    public void cmd_CAM_BT_doBind(int type, String mac) {
        Log.d(TAG, "cmd_CAM_BT_doBind, type=" + type + ", mac=" + mac);
        postRequest(CMD_Domain_cam, CMD_CAM_BT_doBind, Integer.toString(type), mac);
    }

    private void ack_CAM_BT_doBind(String p1, String p2) {
        int type = Integer.parseInt(p1);
        int result = Integer.parseInt(p2);
        if (result == 0) {
            if (type == BtState.BT_Type_HID || type == BtState.BT_Type_OBD) {
                postRequest(CMD_Domain_cam, CMD_CAM_BT_getDEVStatus, type);
            }
        }
    }

    // ========================================================
    // CMD_CAM_BT_doUnBind
    // ========================================================
    public void cmd_CAM_BT_doUnBind(int type, String mac) {
        Log.d(TAG, "cmd_CAM_BT_doUnBind, type=" + type + ", mac=" + mac);
        postRequest(CMD_Domain_cam, CMD_CAM_BT_doUnBind, Integer.toString(type), mac);
    }

    private void ack_CAM_BT_doUnBind(String p1, String p2) {
        int type = Integer.parseInt(p1);
        if (type == BtState.BT_Type_HID || type == BtState.BT_Type_OBD) {
            postRequest(CMD_Domain_cam, CMD_CAM_BT_getDEVStatus, type);
        }
    }

    // ========================================================
    // CMD_CAM_BT_setOBDTypes
    // ========================================================
    public void cmd_CAM_BT_setOBDTypes() {
    }

    // ========================================================
    // CMD_CAM_WantIdle
    // ========================================================
    public void cmd_CAM_WantIdle() {
        postRequest(CMD_Domain_cam, CMD_CAM_WantIdle);
    }

    // ========================================================
    // CMD_CAM_WantPreview
    // ========================================================
    public void cmd_CAM_WantPreview() {
        postRequest(CMD_Domain_cam, CMD_CAM_WantPreview);
    }

    // /////////////////////////////////////////////////////////

    // ========================================================
    // CMD_Rec_Start - not used
    // ========================================================
    // ========================================================
    // CMD_Rec_Stop - not used
    // ========================================================

    // ========================================================
    // CMD_Rec_List_Resolutions
    // ========================================================
    public void cmd_Rec_List_Resolutions() {
        postRequest(CMD_Domain_rec, CMD_Rec_List_Resolutions);
    }

    private void ack_Rec_List_Resolutions(String p1, String p2) {
        int list = Integer.parseInt(p1);
        mStates.setVideoResolutionList(list);
    }

    // ========================================================
    // CMD_Rec_Set_Resolution
    // ========================================================
    public void cmd_Rec_Set_Resolution(int index) {
        postRequest(CMD_Domain_rec, CMD_Rec_Set_Resolution, index);
    }

    // ========================================================
    // CMD_Rec_get_Resolution
    // ========================================================
    public void cmd_Rec_get_Resolution() {
        postRequest(CMD_Domain_rec, CMD_Rec_get_Resolution);
    }

    private void ack_Rec_get_Resolution(String p1, String p2) {
        int index = Integer.parseInt(p1);
        mStates.setVideoResolution(index);
    }

    // ========================================================
    // CMD_Rec_List_Qualities
    // ========================================================
    public void cmd_Rec_List_Qualities() {
        postRequest(CMD_Domain_rec, CMD_Rec_List_Qualities);
    }

    private void ack_Rec_List_Qualities(String p1, String p2) {
        int list = Integer.parseInt(p1);
        mStates.setVideoQualityList(list);
    }

    // ========================================================
    // CMD_Rec_Set_Quality
    // ========================================================
    public void cmd_Rec_Set_Quality(int index) {
        postRequest(CMD_Domain_rec, CMD_Rec_Set_Quality, index);
    }

    // ========================================================
    // CMD_Rec_get_Quality
    // ========================================================
    public void cmd_Rec_get_Quality() {
        postRequest(CMD_Domain_rec, CMD_Rec_get_Quality);
    }

    private void ack_Rec_get_Quality(String p1, String p2) {
        int index = Integer.parseInt(p1);
        mStates.setVideoQuality(index);
    }

    // ========================================================
    // CMD_Rec_List_RecModes
    // ========================================================
    public void cmd_Rec_List_RecModes() {
        postRequest(CMD_Domain_rec, CMD_Rec_List_RecModes);
    }

    private void ack_Rec_List_RecModes(String p1, String p2) {
        int list = Integer.parseInt(p1);
        mStates.setRecordModeList(list);
    }

    // ========================================================
    // CMD_Rec_Set_RecMode
    // ========================================================
    public void cmd_Rec_Set_RecMode(int index) {
        postRequest(CMD_Domain_rec, CMD_Rec_Set_RecMode, index);
    }

    // ========================================================
    // CMD_Rec_get_RecMode
    // ========================================================
    public void cmd_Rec_get_RecMode() {
        postRequest(CMD_Domain_rec, CMD_Rec_get_RecMode);
    }

    private void ack_Rec_get_RecMode(String p1, String p2) {
        int index = Integer.parseInt(p1);
        mStates.setRecordMode(index);
    }

    // ========================================================
    // CMD_Rec_List_ColorModes
    // ========================================================
    public void cmd_Rec_List_ColorModes() {
        postRequest(CMD_Domain_rec, CMD_Rec_List_ColorModes);
    }

    private void ack_Rec_List_ColorModes(String p1, String p2) {
        int list = Integer.parseInt(p1);
        mStates.setColorModeList(list);
    }

    // ========================================================
    // CMD_Rec_Set_ColorMode
    // ========================================================
    public void cmd_Rec_Set_ColorMode(int index) {
        postRequest(CMD_Domain_rec, CMD_Rec_Set_ColorMode, index);
    }

    // ========================================================
    // CMD_Rec_get_ColorMode
    // ========================================================
    public void cmd_Rec_get_ColorMode() {
        postRequest(CMD_Domain_rec, CMD_Rec_get_ColorMode);
    }

    private void ack_Rec_get_ColorMode(String p1, String p2) {
        int index = Integer.parseInt(p1);
        mStates.setColorMode(index);
    }

    // ========================================================
    // CMD_Rec_List_SegLens
    // ========================================================
    // ========================================================
    // CMD_Rec_Set_SegLen
    // ========================================================
    // ========================================================
    // CMD_Rec_get_SegLen
    // ========================================================

    // ========================================================
    // CMD_Rec_get_State
    // ========================================================
    // ========================================================
    // EVT_Rec_state_change
    // ========================================================

    // ========================================================
    // CMD_Rec_getTime
    // ========================================================
    // ========================================================
    // CMD_Rec_getTime_result
    // ========================================================

    // ========================================================
    // CMD_Rec_setDualStream
    // ========================================================
    // ========================================================
    // CMD_Rec_getDualStreamState
    // ========================================================

    // ========================================================
    // CMD_Rec_setOverlay
    // ========================================================
    public void cmd_Rec_setOverlay(int flags) {
        postRequest(CMD_Domain_rec, CMD_Rec_setOverlay, flags);
    }

    // ========================================================
    // CMD_Rec_getOverlayState
    // ========================================================
    public void cmd_Rec_getOverlayState() {
        postRequest(CMD_Domain_rec, CMD_Rec_getOverlayState);
    }

    private void ack_Rec_getOverlayState(String p1, String p2) {
        int flags = Integer.parseInt(p1);
        mStates.setOverlayFlags(flags);
    }

    // ========================================================
    // CMD_Rec_SetStillMode
    // ========================================================
    public void cmd_Rec_SetStillMode(boolean bStillMode) {
        postRequest(CMD_Domain_rec, CMD_Rec_SetStillMode, bStillMode ? 1 : 0);
    }

    // ========================================================
    // CMD_Rec_StartStillCapture
    // ========================================================
    public void cmd_Rec_StartStillCapture(boolean bOneShot) {
        postRequest(CMD_Domain_rec, CMD_Rec_StartStillCapture, bOneShot ? 1 : 0);
    }

    private void ack_Rec_StartStillCapture(String p1, String p2) {
        boolean bOneShot = p1.length() > 0 ? Integer.parseInt(p1) != 0 : false;
        onStillCaptureStarted(bOneShot);
    }

    // ========================================================
    // CMD_Rec_StopStillCapture
    // ========================================================
    public void cmd_Rec_StopStillCapture() {
        postRequest(CMD_Domain_rec, CMD_Rec_StopStillCapture);
    }

    // ========================================================
    // MSG_Rec_StillPictureInfo
    // ========================================================
    private void ack_Rec_StillPictureInfo(String p1, String p2) {
        int value_p1 = p1.length() > 0 ? Integer.parseInt(p1) : 0;
        boolean bCapturing = (value_p1 & 1) != 0;
        int numPictures = p2.length() > 0 ? Integer.parseInt(p2) : 0;
        int burstTicks = value_p1 >>> 1;
        onStillPictureInfo(bCapturing, numPictures, burstTicks);
    }

    // ========================================================
    // MSG_Rec_StillCaptureDone
    // ========================================================
    private void ack_Rec_StillCaptureDone() {
        onStillCaptureDone();
    }

    /**
     * This cmd does not have response.
     */
    public void cmd_Rec_MarkLiveVideo() {
        postRequest(CMD_Domain_rec, CMD_Rec_Mark_Live_Video);
    }

    public void cmd_Rec_GetMarkTime() {
        postRequest(CMD_Domain_rec, CMD_Rec_Get_Mark_Time);
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
        postRequest(CMD_Domain_rec, CMD_Rec_Set_Mark_Time,
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

    public VdtCameraController(InetAddress host, int port) {
        InetSocketAddress address = new InetSocketAddress(host, port);
        mConnection = new MyTcpConnection("ccam", address);
    }

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
            case UserCmd_GetSetup:

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

            case UserCmd_ExitThread:
                return false;

            default:
                if (DEBUG) {
                    Log.d(TAG, "unknown user cmd " + request.mCmd);
                    break;
                }
        }

        return true;
    }

    private final void cmdLoop(Thread thread) throws IOException, InterruptedException {

        SimpleOutputStream sos = new SimpleOutputStream(1024);
        XmlSerializer xml = Xml.newSerializer();
        Queue.CmdResult cmdResult = new Queue.CmdResult();

        while (!thread.isInterrupted()) {
            mQueue.getRequest(cmdResult);

            if (cmdResult.request == null) {
                switch (cmdResult.scheduleType) {
                    case Queue.SCHEDULE_UPDATE:
                        onCameraStateChanged();
                        break;
                    case Queue.SCHEDULE_BT_UPDATE:
                        onBtStateChanged();
                        break;
                    case Queue.SCHEDULE_GPS_UPDATE:
                        onGpsStateChanged();
                        break;
                    case Queue.SCHEDULE_WIFI_UPDATE:
                        onWifiStateChanged();
                        break;
                    case Queue.SCHEDULE_GET_ALL_INFO:
                        cmd_Cam_get_getAllInfor();
                        break;
                    default:
                        break;
                }
                continue;
            }

            Request request = (Request) cmdResult.request;

            if (request.mDomain == CMD_Domain_user) {
                if (!createUserCmd(request)) {
                    break;
                }
                continue;
            }

            // init header
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

            //if (DEBUG) {
            //	Log.d(TAG, "cmd: " + sos.toString(8));
            //}

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

            if (DEBUG) {
                Log.d(TAG, "ack: act=" + act + ", p1=" + p1 + ", p2=" + p2);
            }

            // ECMD0.5
            Matcher matcher = mPattern.matcher(act);
            if (matcher.find() && matcher.groupCount() == 2) {
                int domain = Integer.parseInt(matcher.group(1));
                int cmd = Integer.parseInt(matcher.group(2));
                switch (domain) {
                    case CMD_Domain_cam:
                        camDomainMsg(cmd, p1, p2);
                        break;
                    case CMD_Domain_rec:
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

                mStates.mbSchedule = false;
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

                if (mStates.mbSchedule) {
                    mStates.mbSchedule = false;
                    mQueue.scheduleUpdate(Queue.SCHEDULE_UPDATE);
                }

                if (mGpsStates.mbSchedule) {
                    mGpsStates.mbSchedule = false;
                    mQueue.scheduleUpdate(Queue.SCHEDULE_GPS_UPDATE);
                }

                if (mWifiStates.mbSchedule) {
                    mWifiStates.mbSchedule = false;
                    mQueue.scheduleUpdate(Queue.SCHEDULE_WIFI_UPDATE);
                }

                if (mBtStates.mbSchedule) {
                    mBtStates.mbSchedule = false;
                    mQueue.scheduleUpdate(Queue.SCHEDULE_BT_UPDATE);
                }
            }

        } catch (XmlPullParserException e) {
            if (DEBUG) {
                Log.d(TAG, "XmlPullParserException: length=" + length + ", appended=" + appended);
                e.printStackTrace();
            }
            throw new IOException("XmlPullParserException");
        }
    }

    private void ackNotHandled(String name, String p1, String p2) {
        if (DEBUG) {
            Log.d(TAG, "not handled: " + name + ", p1=" + p1 + ",p2=" + p2);
        }
    }

    private void camDomainMsg(int cmd, String p1, String p2) {
        switch (cmd) {
            case CMD_Cam_getMode:
                ackNotHandled("CMD_Cam_getMode", p1, p2);
                break;
            case CMD_Cam_getMode_result:
                ackNotHandled("CMD_Cam_getMode_result", p1, p2);
                break;
            case CMD_Cam_getApiVersion:
                ack_Cam_getApiVersion(p1);
                break;
            case CMD_Cam_isApiSupported:
                ackNotHandled("CMD_Cam_isApiSupported", p1, p2);
                break;
            case CMD_Cam_get_Name:
                ackNotHandled("CMD_Cam_get_Name", p1, p2);
                break;
            case CMD_Cam_get_Name_result:
                ack_Cam_get_Name_result(p1, p2);
                break;
            case CMD_Cam_set_Name:
                ackNotHandled("CMD_Cam_set_Name", p1, p2);
                break;
            case CMD_Cam_set_Name_result:
                ackNotHandled("CMD_Cam_set_Name_result", p1, p2);
                break;
            case CMD_Cam_get_State:
                ackNotHandled("CMD_Cam_get_State", p1, p2);
                break;
            case CMD_Cam_get_State_result:
                ack_Cam_get_State_result(p1, p2);
                break;
            case CMD_Cam_start_rec:
                ackNotHandled("CMD_Cam_start_rec", p1, p2);
                break;
            case CMD_Cam_stop_rec:
                ackNotHandled("CMD_Cam_stop_rec", p1, p2);
                break;
            case CMD_Cam_get_time:
                ackNotHandled("CMD_Cam_get_time", p1, p2);
                break;
            case CMD_Cam_get_time_result:
                ack_Cam_get_time_result(p1, p2);
                break;
            case CMD_Cam_get_getAllInfor:
                ackNotHandled("CMD_Cam_get_getAllInfor", p1, p2);
                break;
            case CMD_Cam_get_getStorageInfor:
                ackNotHandled("CMD_Cam_get_getStorageInfor", p1, p2);
                break;
            case CMD_Cam_msg_Storage_infor:
                ack_Cam_msg_Storage_infor(p1, p2);
                break;
            case CMD_Cam_msg_StorageSpace_infor:
                ack_Cam_msg_StorageSpace_infor(p1, p2);
                break;
            case CMD_Cam_msg_Battery_infor:
                ack_Cam_msg_Battery_infor(p1, p2);
                break;
            case CMD_Cam_msg_power_infor:
                ack_Cam_msg_power_infor(p1, p2);
                break;
            case CMD_Cam_msg_BT_infor:
                ackNotHandled("CMD_Cam_msg_BT_infor", p1, p2);
                break;
            case CMD_Cam_msg_GPS_infor:
                ack_Cam_msg_GPS_infor(p1, p2);
                break;
            case CMD_Cam_msg_Internet_infor:
                ackNotHandled("CMD_Cam_msg_Internet_infor", p1, p2);
                break;
            case CMD_Cam_msg_Mic_infor:
                ack_Cam_msg_Mic_infor(p1, p2);
                break;
            case CMD_Cam_set_StreamSize:
                ackNotHandled("CMD_Cam_set_StreamSize", p1, p2);
                break;
            case CMD_Cam_PowerOff:
                ackNotHandled("CMD_Cam_PowerOff", p1, p2);
                break;
            case CMD_Cam_Reboot:
                ackNotHandled("CMD_Cam_Reboot", p1, p2);
                break;
            case CMD_Network_GetWLanMode:
                ack_Network_GetWLanMode(p1, p2);
                break;
            case CMD_Network_GetHostNum:
                ack_Network_GetHostNum(p1, p2);
                break;
            case CMD_Network_GetHostInfor:
                ack_Network_GetHostInfor(p1, p2);
                break;
            case CMD_Network_AddHost:
                ackNotHandled("CMD_Network_AddHost", p1, p2);
                break;
            case CMD_Network_RmvHost:
                ackNotHandled("CMD_Network_RmvHost", p1, p2);
                break;
            case CMD_Network_ConnectHost:
                ackNotHandled("CMD_Network_ConnectHost", p1, p2);
                break;
            case CMD_Network_Synctime:
                ackNotHandled("CMD_Network_Synctime", p1, p2);
                break;
            case CMD_Network_GetDevicetime:
                ackNotHandled("CMD_Network_GetDevicetime", p1, p2);
                break;
            case CMD_Rec_error:
                ack_Rec_error(p1, p2);
                break;
            case CMD_audio_setMic:
                ackNotHandled("CMD_audio_setMic", p1, p2);
                break;
            case CMD_audio_getMicState:
                ackNotHandled("CMD_audio_getMicState", p1, p2);
                break;
            case CMD_fw_getVersion:
                ack_fw_getVersion(p1, p2);
                break;
            case CMD_fw_newVersion:
                ackNotHandled("CMD_fw_newVersion", p1, p2);
                break;
            case CMD_fw_doUpgrade:
                ackNotHandled("CMD_fw_doUpgrade", p1, p2);
                break;
            case CMD_CAM_BT_isSupported:
                ack_CAM_BT_isSupported(p1);
                break;
            case CMD_CAM_BT_isEnabled:
                ack_CAM_BT_isEnabled(p1);
                break;
            case CMD_CAM_BT_Enable:
                ackNotHandled("CMD_CAM_BT_Enable", p1, p2);
                break;
            case CMD_CAM_BT_getDEVStatus:
                ack_CAM_BT_getDEVStatus(p1, p2);
                break;
            case CMD_CAM_BT_getHostNum:
                ack_CAM_BT_getHostNum(p1);
                break;
            case CMD_CAM_BT_getHostInfor:
                ack_CAM_BT_getHostInfor(p1, p2);
                break;
            case CMD_CAM_BT_doScan:
                ack_CAM_BT_doScan();
                break;
            case CMD_CAM_BT_doBind:
                ack_CAM_BT_doBind(p1, p2);
                break;
            case CMD_CAM_BT_doUnBind:
                ack_CAM_BT_doUnBind(p1, p2);
                break;
            case CMD_CAM_BT_setOBDTypes:
                ackNotHandled("CMD_CAM_BT_setOBDTypes", p1, p2);
                break;
            case CMD_CAM_WantIdle:
                // not used
                break;
            case CMD_CAM_WantPreview:
                // not used
                break;
            default:
                if (DEBUG) {
                    Log.d(TAG, "ack " + cmd + " not handled, p1=" + p1 + ", p2=" + p2);
                }
                break;
        }
    }

    private void recDomainMsg(int cmd, String p1, String p2) {
        switch (cmd) {
            case CMD_Rec_Start:
                ackNotHandled("CMD_Rec_Start", p1, p2);
                break;
            case CMD_Rec_Stop:
                ackNotHandled("CMD_Rec_Stop", p1, p2);
                break;
            case CMD_Rec_List_Resolutions:
                ack_Rec_List_Resolutions(p1, p2);
                break;
            case CMD_Rec_Set_Resolution:
                ackNotHandled("CMD_Rec_Set_Resolution", p1, p2);
                break;
            case CMD_Rec_get_Resolution:
                ack_Rec_get_Resolution(p1, p2);
                break;
            case CMD_Rec_List_Qualities:
                ack_Rec_List_Qualities(p1, p2);
                break;
            case CMD_Rec_Set_Quality:
                ackNotHandled("CMD_Rec_Set_Quality", p1, p2);
                break;
            case CMD_Rec_get_Quality:
                ack_Rec_get_Quality(p1, p2);
                break;
            case CMD_Rec_List_RecModes:
                ack_Rec_List_RecModes(p1, p2);
                break;
            case CMD_Rec_Set_RecMode:
                ackNotHandled("CMD_Rec_Set_RecMode", p1, p2);
                break;
            case CMD_Rec_get_RecMode:
                ack_Rec_get_RecMode(p1, p2);
                break;
            case CMD_Rec_List_ColorModes:
                ack_Rec_List_ColorModes(p1, p2);
                break;
            case CMD_Rec_Set_ColorMode:
                ackNotHandled("CMD_Rec_Set_ColorMode", p1, p2);
                break;
            case CMD_Rec_get_ColorMode:
                ack_Rec_get_ColorMode(p1, p2);
                break;
            case CMD_Rec_List_SegLens:
                ackNotHandled("CMD_Rec_List_SegLens", p1, p2);
                break;
            case CMD_Rec_Set_SegLen:
                ackNotHandled("CMD_Rec_Set_SegLen", p1, p2);
                break;
            case CMD_Rec_get_SegLen:
                ackNotHandled("CMD_Rec_get_SegLen", p1, p2);
                break;
            case CMD_Rec_get_State:
                ackNotHandled("CMD_Rec_get_State", p1, p2);
                break;
            case EVT_Rec_state_change:
                ackNotHandled("EVT_Rec_state_change", p1, p2);
                break;
            case CMD_Rec_getTime:
                ackNotHandled("CMD_Rec_getTime", p1, p2);
                break;
            case CMD_Rec_getTime_result:
                ackNotHandled("CMD_Rec_getTime_result", p1, p2);
                break;
            case CMD_Rec_setDualStream:
                ackNotHandled("CMD_Rec_setDualStream", p1, p2);
                break;
            case CMD_Rec_getDualStreamState:
                ackNotHandled("CMD_Rec_getDualStreamState", p1, p2);
                break;
            case CMD_Rec_setOverlay:
                ackNotHandled("CMD_Rec_setOverlay", p1, p2);
                break;
            case CMD_Rec_getOverlayState:
                ack_Rec_getOverlayState(p1, p2);
                break;
            case CMD_Rec_SetStillMode:
                ackNotHandled("CMD_Rec_SetStillMode", p1, p2);
                break;
            case CMD_Rec_StartStillCapture:
                ack_Rec_StartStillCapture(p1, p2);
                break;
            case CMD_Rec_StopStillCapture:
                ackNotHandled("CMD_Rec_StopStillCapture", p1, p2);
                break;
            case MSG_Rec_StillPictureInfo:
                ack_Rec_StillPictureInfo(p1, p2);
                break;
            case MSG_Rec_StillCaptureDone:
                ack_Rec_StillCaptureDone();
                break;
            case CMD_Rec_Get_Mark_Time:
                ack_Rec_GetMarkTime(p1, p2);
                break;
            case CMD_Rec_Set_Mark_Time:
                ack_Rec_SetMarkTime(p1, p2);
                break;

            default:
                if (DEBUG) {
                    Log.d(TAG, "ack " + cmd + " not handled, p1=" + p1 + ", p2=" + p2);
                }
                break;
        }
    }

    class MyTcpConnection extends TcpConnection {

        public MyTcpConnection(String name, InetSocketAddress address) {
            super(name, address);
        }

        @Override
        public void onConnectedAsync() {
            VdtCameraController.this.onConnected();
        }

        @Override
        public void onConnectErrorAsync() {
            VdtCameraController.this.onDisconnected();
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

    static class Queue extends CmdQueue<Request> {

        static final int SCHEDULE_NULL = -1;
        static final int SCHEDULE_UPDATE = 0;
        static final int SCHEDULE_BT_UPDATE = 1;
        static final int SCHEDULE_GPS_UPDATE = 2;
        static final int SCHEDULE_WIFI_UPDATE = 3;
        static final int SCHEDULE_GET_ALL_INFO = 4;
        static final int SCHEDULE_NUM = 5;

        static final int SCHEDULE_UPDATE_DELAY = 100;
        static final int SCHEDULE_GET_ALL_INFO_DELAY = 1000;

        public Queue() {
            super(SCHEDULE_NUM);
        }

        public void scheduleUpdate(int type) {
            schedule(type, SCHEDULE_UPDATE_DELAY);
        }

        public void scheduleGetAllInfo() {
            schedule(SCHEDULE_GET_ALL_INFO, SCHEDULE_GET_ALL_INFO_DELAY);
        }
    }

}
