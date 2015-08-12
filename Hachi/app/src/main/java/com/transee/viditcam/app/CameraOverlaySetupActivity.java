package com.transee.viditcam.app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import com.transee.ccam.Camera;
import com.transee.ccam.CameraClient;
import com.transee.ccam.CameraState;
import com.transee.common.Utils;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;

public class CameraOverlaySetupActivity extends BaseActivity {

	static final boolean DEBUG = false;
	static final String TAG = "CameraOverlaySetupAct";

	private Camera mCamera;

	private CheckBox mCBShowName;
	private CheckBox mCBShowTime;
	private CheckBox mCBShowGPS;
	private CheckBox mCBShowSpeed;

	private CameraState getCameraStates() {
		return Camera.getCameraStates(mCamera);
	}

	private CameraClient getCameraClient() {
		return (CameraClient)mCamera.getClient();
	}

	@Override
	protected void requestContentView() {
		setContentView(R.layout.activity_camera_overlay_setup);
	}

	@Override
	protected void onCreateActivity(Bundle savedInstanceState) {
		View layout = findViewById(R.id.layoutShowOverlayName);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleOverlayFlag(CameraState.OVERLAY_FLAG_NAME);
			}
		});
		mCBShowName = (CheckBox)layout.findViewById(R.id.checkBoxShowName);

		layout = findViewById(R.id.layoutShowOverlayTime);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleOverlayFlag(CameraState.OVERLAY_FLAG_TIME);
			}
		});
		mCBShowTime = (CheckBox)layout.findViewById(R.id.checkBoxShowTime);

		layout = findViewById(R.id.layoutShowOverlayGPS);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleOverlayFlag(CameraState.OVERLAY_FLAG_GPS);
			}
		});
		mCBShowGPS = (CheckBox)layout.findViewById(R.id.checkBoxShowGPS);

		layout = findViewById(R.id.layoutShowOverlaySpeed);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleOverlayFlag(CameraState.OVERLAY_FLAG_SPEED);
			}
		});
		mCBShowSpeed = (CheckBox)layout.findViewById(R.id.checkBoxShowSpeed);
	}

	private void toggleOverlayFlag(int flag) {
		if (mCamera != null) {
			CameraState states = getCameraStates();
			int flags = Utils.toggleBit(states.mOverlayFlags, flag);
			getCameraClient().cmd_Rec_setOverlay(flags);
			getCameraClient().cmd_Rec_getOverlayState();
			// force refresh
			states.mOverlayFlags = flags;
			updateOverlayStates();
		}
	}

	private void updateOverlayStates() {
		int flags = getCameraStates().mOverlayFlags;
		boolean checked = flags >= 0 && (flags & CameraState.OVERLAY_FLAG_NAME) != 0;
		mCBShowName.setChecked(checked);
		checked = flags >= 0 && (flags & CameraState.OVERLAY_FLAG_TIME) != 0;
		mCBShowTime.setChecked(checked);
		checked = flags >= 0 && (flags & CameraState.OVERLAY_FLAG_GPS) != 0;
		mCBShowGPS.setChecked(checked);
		checked = flags >= 0 && (flags & CameraState.OVERLAY_FLAG_SPEED) != 0;
		mCBShowSpeed.setChecked(checked);
	}

	@Override
	protected void onStartActivity() {
		mCamera = getCameraFromIntent(null);
		if (mCamera == null) {
			noCamera();
			return;
		}

		mCamera.addCallback(mCameraCallback);
		updateCameraState();
	}

	@Override
	protected void onStopActivity() {
		removeCamera();
	}

	private void updateCameraState() {
		updateOverlayStates();
	}

	private void removeCamera() {
		if (mCamera != null) {
			mCamera.removeCallback(mCameraCallback);
			mCamera = null;
		}
	}

	private void noCamera() {
		if (DEBUG) {
			Log.d(TAG, "camera not found or disconnected");
		}
		performFinish();
	}

	@Override
	public void onBackPressed() {
		performFinish();
	}

	private void performFinish() {
		finish();
	}

	private final Camera.Callback mCameraCallback = new Camera.CallbackImpl() {

		@Override
		public void onStateChanged(Camera camera) {
			if (camera == mCamera) {
				updateCameraState();
			}
		}

		@Override
		public void onDisconnected(Camera camera) {
			if (camera == mCamera) {
				removeCamera();
				noCamera();
			}
		}
	};
}
