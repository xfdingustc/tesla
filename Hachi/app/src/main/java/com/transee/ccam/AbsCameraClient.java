package com.transee.ccam;

import java.net.InetSocketAddress;

public interface AbsCameraClient {

	public void start();

	public void stop();

	public boolean syncState(CameraState user);

	public boolean syncBtState(BtState user);

	public boolean syncGpsState(GpsState user);

	public boolean syncWifiState(WifiState user);

	public InetSocketAddress getInetSocketAddress();

	public void cmd_CAM_BT_Enable(boolean bEnable);

	public void cmd_CAM_BT_doScan();

	public void cmd_CAM_BT_getHostNum();

	public void cmd_CAM_BT_doUnBind(int type, String mac);

	public void cmd_CAM_BT_doBind(int type, String mac);

	public void cmd_CAM_WantIdle();

	public void cmd_CAM_WantPreview();

	public void cmd_Rec_get_RecMode();

	public void ack_Cam_get_time();

	public void cmd_audio_getMicState();

	public void cmd_Rec_List_Resolutions();

	public void cmd_Cam_start_rec();

	public void ack_Cam_stop_rec();

	public void cmd_Rec_StartStillCapture(boolean bOneShot);

	public void cmd_Rec_StopStillCapture();

	public void cmd_Rec_SetStillMode(boolean bStillMode);

	public void cmd_Cam_Reboot();

	public void cmd_Cam_PowerOff();

	public void cmd_Network_ConnectHost(int mode, String apName);

	public void userCmd_ExitThread();

	public void cmd_Cam_get_getStorageInfor();
}
