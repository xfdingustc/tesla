package com.transee.camera;

import android.hardware.Camera;

public interface OpenCameraInterface {
	int getCameraId(); // valid after open() is called
	Camera open();
}
