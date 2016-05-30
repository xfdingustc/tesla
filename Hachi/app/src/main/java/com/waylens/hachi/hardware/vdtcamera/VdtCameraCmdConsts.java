package com.waylens.hachi.hardware.vdtcamera;

/**
 * Created by Xiaofei on 2016/3/22.
 */
public interface VdtCameraCmdConsts {
    int CMD_DOMAIN_USER = -1;
    int CMD_DOMAIN_CAM = 0;
    int CMD_DOMAIN_P2P = 1;
    int CMD_DOMAIN_REC = 2;
    int CMD_DOMAIN_DECODE = 3;
    int CMD_DOMAIN_NETWORK = 4;
    int CMD_DOMAIN_POWER = 5;
    int CMD_DOMAIN_STORAGE = 6;
    int CMD_DOMAIN_STREAM = 7;
    int CMD_DOMAIN_MOTOR_CONTROL = 8;


    int CMD_DOMAIN_CAM_START = 0;
    int CMD_CAM_GET_MODE = 0;
    int CMD_CAM_GET_MODE_RESULT = 1;
    int CMD_CAM_GET_API_VERSION = 2;
    int CMD_CAM_IS_API_SUPPORTED = 3;
    int CMD_CAM_GET_NAME = 4;
    int CMD_CAM_GET_NAME_RESULT = 5;
    int CMD_CAM_SET_NAME = 6;
    int CMD_CAM_SET_NAME_RESULT = 7;
    int CMD_CAM_GET_STATE = 8;
    int CMD_CAM_GET_STATE_RESULT = 9;
    int CMD_CAM_START_REC = 10;
    int CMD_CAM_STOP_REC = 11;
    int CMD_CAM_GET_TIME = 12;
    int CMD_CAM_GET_TIME_RESULT = 13;

    int CMD_CAM_GET_GET_ALL_INFOR = 14;
    int CMD_CAM_GET_GET_STORAGE_INFOR = 15;
    int CMD_CAM_MSG_STORAGE_INFOR = 16;
    int CMD_CAM_MSG_STORAGE_SPACE_INFOR = 17;
    int CMD_CAM_MSG_BATTERY_INFOR = 18;
    int CMD_CAM_MSG_POWER_INFOR = 19;
    int CMD_CAM_MSG_BT_INFOR = 20;
    int CMD_CAM_MSG_GPS_INFOR = 21;
    int CMD_CAM_MSG_INTERNET_INFOR = 22;
    int CMD_CAM_MSG_MIC_INFOR = 23;
    int CMD_CAM_SET_STREAM_SIZE = 24;

    int CMD_CAM_POWER_OFF = 25;
    int CMD_CAM_REBOOT = 26;

    int CMD_NETWORK_GET_WLAN_MODE = 27;
    int CMD_NETWORK_GET_HOST_NUM = 28;
    int CMD_NETWORK_GET_HOST_INFOR = 29;
    int CMD_NETWORK_ADD_HOST = 30;
    int CMD_NETWORK_RMV_HOST = 31;
    int CMD_NETWORK_CONNECT_HOST = 32;



    int CMD_NETWORK_SCANHOST = 74;
    int CMD_NETWORK_CONNECTHOTSPOT  = 75;

    int CMD_NETWORK_SYNCTIME = 33;
    int CMD_NETWORK_GET_DEVICETIME = 34;

    int CMD_REC_ERROR = 35;

    int CMD_AUDIO_SET_MIC = 36;
    int CMD_AUDIO_GET_MIC_STATE = 37;

    int CMD_FW_GET_VERSION = 38;
    int CMD_FW_NEW_VERSION = 39;
    int CMD_FW_DO_UPGRADE = 40;

    // 1.2
    int CMD_CAM_BT_IS_SUPPORTED = 41;
    int CMD_CAM_BT_IS_ENABLED = 42;
    int CMD_CAM_BT_ENABLE = 43;
    int CMD_CAM_BT_GET_DEV_STATUS = 44;
    int CMD_CAM_BT_GET_HOST_NUM = 45;
    int CMD_CAM_BT_GET_HOST_INFOR = 46;
    int CMD_CAM_BT_DO_SCAN = 47;
    int CMD_CAM_BT_DO_BIND = 48;
    int CMD_CAM_BT_DO_UNBIND = 49;
    int CMD_CAM_BT_SET_OBD_TYPES = 50;
    int CMD_CAM_BT_RESERVED = 51;

    // oliver
    int CMD_CAM_WANT_IDLE = 100;
    int CMD_CAM_WANT_PREVIEW = 101;

    // CMD_DOMAIN_REC
    int CMD_DOMAIN_REC_START = 1000;
    int CMD_REC_START = 1000;
    int CMD_REC_STOP = 1001;
    int CMD_REC_LIST_RESOLUTIONS = 1002;
    int CMD_REC_SET_RESOLUTION = 1003;
    int CMD_REC_GET_RESOLUTION = 1004;
    int CMD_REC_LIST_QUALITIES = 1005;
    int CMD_REC_SET_QUALITY = 1006;
    int CMD_REC_GET_QUALITY = 1007;
    int CMD_REC_LIST_REC_MODES = 1008;
    int CMD_REC_SET_REC_MODE = 1009;
    int CMD_REC_GET_REC_MODE = 1010;
    int CMD_REC_LIST_COLOR_MODES = 1011;
    int CMD_REC_SET_COLOR_MODE = 1012;
    int CMD_REC_GET_COLOR_MODE = 1013;
    int CMD_REC_LIST_SEG_LENS = 1014;
    int CMD_REC_SET_SEG_LEN = 1015;
    int CMD_REC_GET_SEG_LEN = 1016;
    int CMD_REC_GET_STATE = 1017;
    int EVT_REC_STATE_CHANGE = 1018;
    int CMD_REC_GET_TIME = 1019;
    int CMD_REC_GET_TIME_RESULT = 1020;

    int CMD_REC_SET_DUAL_STREAM = 1021;
    int CMD_REC_GET_DUAL_STREAM_STATE = 1022;

    int CMD_REC_SET_OVERLAY = 1023;
    int CMD_REC_GET_OVERLAY_STATE = 1024;

    int CMD_REC_MARK_LIVE_VIDEO = 1027;
    int CMD_REC_SET_MARK_TIME = 1028;
    int CMD_REC_GET_MARK_TIME = 1029;


    // oliver
    int CMD_REC_SET_STILL_MODE = 100;
    int CMD_REC_START_STILL_CAPTURE = 101;
    int CMD_REC_STOP_STILL_CAPTURE = 102;

    int MSG_REC_STILL_PICTURE_INFO = 103;
    int MSG_REC_STILL_CAPTURE_DONE = 104;

    int USER_CMD_GET_SETUP = 1;
    int USER_CMD_EXIT_THREAD = 2;
}
