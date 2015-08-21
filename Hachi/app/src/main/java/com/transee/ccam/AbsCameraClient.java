package com.transee.ccam;

import java.net.InetSocketAddress;

public interface AbsCameraClient {

    void start();

    void stop();

    boolean syncState(CameraState user);

    boolean syncBtState(BtState user);

    boolean syncGpsState(GpsState user);

    boolean syncWifiState(WifiState user);

    InetSocketAddress getInetSocketAddress();

    void cmd_CAM_BT_Enable(boolean bEnable);

    void cmd_CAM_BT_doScan();

    void cmd_CAM_BT_getHostNum();

    void cmd_CAM_BT_doUnBind(int type, String mac);

    void cmd_CAM_BT_doBind(int type, String mac);

    void cmd_CAM_WantIdle();

    void cmd_CAM_WantPreview();

    void cmd_Rec_get_RecMode();

    void ack_Cam_get_time();

    void cmd_audio_getMicState();

    void cmd_Rec_List_Resolutions();

    void cmd_Cam_start_rec();

    void ack_Cam_stop_rec();

    void cmd_Rec_StartStillCapture(boolean bOneShot);

    void cmd_Rec_StopStillCapture();

    void cmd_Rec_SetStillMode(boolean bStillMode);

    void cmd_Cam_Reboot();

    void cmd_Cam_PowerOff();

    void cmd_Network_ConnectHost(int mode, String apName);

    void userCmd_ExitThread();

    void cmd_Cam_get_getStorageInfor();
}
