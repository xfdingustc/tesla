package com.transee.ccam;

import java.net.InetSocketAddress;

public class NullCameraClient implements AbsCameraClient {

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public InetSocketAddress getInetSocketAddress() {
		return null;
	}

	@Override
	public void cmd_CAM_BT_Enable(boolean bEnable) {
	}

	@Override
	public void cmd_CAM_BT_doScan() {
	}

	@Override
	public void cmd_CAM_BT_getHostNum() {
	}

	@Override
	public void cmd_CAM_BT_doUnBind(int type, String mac) {
	}

	@Override
	public void cmd_CAM_BT_doBind(int type, String mac) {
	}

	@Override
	public void cmd_CAM_WantIdle() {
	}

	@Override
	public void cmd_CAM_WantPreview() {
	}

	@Override
	public void cmd_Rec_get_RecMode() {
	}

	@Override
	public void ack_Cam_get_time() {
	}

	@Override
	public void cmd_audio_getMicState() {
	}

	@Override
	public void cmd_Rec_List_Resolutions() {
	}

	@Override
	public void cmd_Cam_start_rec() {
	}

	@Override
	public void ack_Cam_stop_rec() {
	}

	@Override
	public void cmd_Rec_StartStillCapture(boolean bOneShot) {
	}

	@Override
	public void cmd_Rec_StopStillCapture() {
	}

	@Override
	public void cmd_Rec_SetStillMode(boolean bStillMode) {
	}

	@Override
	public void cmd_Cam_Reboot() {
	}

	@Override
	public void cmd_Cam_PowerOff() {
	}

	@Override
	public void cmd_Network_ConnectHost(int mode, String apName) {
	}

	@Override
	public void userCmd_ExitThread() {
	}

	@Override
	public void cmd_Cam_get_getStorageInfor() {
	}

	@Override
	public boolean syncState(CameraState user) {
		return false;
	}

	@Override
	public boolean syncBtState(BtState user) {
		return false;
	}

	@Override
	public boolean syncGpsState(GpsState user) {
		return false;
	}

	@Override
	public boolean syncWifiState(WifiState user) {
		return false;
	}

}
