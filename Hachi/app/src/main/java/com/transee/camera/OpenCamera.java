package com.transee.camera;

import com.transee.common.PlatformSupportManager;

public final class OpenCamera extends PlatformSupportManager<OpenCameraInterface> {

	public OpenCamera() {
		super(OpenCameraInterface.class, new DefaultOpenCamera());
		addImplementationClass(9, GingerbreadOpenCamera.class.getName());
	}
}
