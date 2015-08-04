package com.transee.camera;

import android.hardware.Camera;

public final class DefaultOpenCamera implements OpenCameraInterface {

	@Override
	public int getCameraId() {
		return 0;
	}

	@Override
	public Camera open() {
		return Camera.open();
	}
}
