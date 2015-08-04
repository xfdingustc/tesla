package com.transee.camera;

import android.hardware.Camera;
import android.util.Log;

public final class GingerbreadOpenCamera implements OpenCameraInterface {

	private static final String TAG = "GingerbreadOpenCamera";

	private int mCameraId;

	@Override
	public int getCameraId() {
		return mCameraId;
	}

	@Override
	public Camera open() {
		int numCameras = Camera.getNumberOfCameras();
		if (numCameras == 0) {
			Log.w(TAG, "No camera!");
			return null;
		}

		for (int index = 0; index < numCameras; index++) {
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo(index, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				Log.i(TAG, "Opening camera #" + index);
				mCameraId = index;
				return Camera.open(index);
			}
		}

		Log.i(TAG, "No camera facing back, returning camera #0");
		mCameraId = 0;
		return Camera.open(0);
	}
}
