package com.transee.viditcam.app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import com.transee.ccam.CameraState;
import com.transee.common.Utils;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;

public class CameraOverlaySetupActivity extends BaseActivity {

    static final boolean DEBUG = false;
    static final String TAG = "CameraOverlaySetupAct";

    private VdtCamera mVdtCamera;

    private CheckBox mCBShowName;
    private CheckBox mCBShowTime;
    private CheckBox mCBShowGPS;
    private CheckBox mCBShowSpeed;


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
        mCBShowName = (CheckBox) layout.findViewById(R.id.checkBoxShowName);

        layout = findViewById(R.id.layoutShowOverlayTime);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleOverlayFlag(CameraState.OVERLAY_FLAG_TIME);
            }
        });
        mCBShowTime = (CheckBox) layout.findViewById(R.id.checkBoxShowTime);

        layout = findViewById(R.id.layoutShowOverlayGPS);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleOverlayFlag(CameraState.OVERLAY_FLAG_GPS);
            }
        });
        mCBShowGPS = (CheckBox) layout.findViewById(R.id.checkBoxShowGPS);

        layout = findViewById(R.id.layoutShowOverlaySpeed);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleOverlayFlag(CameraState.OVERLAY_FLAG_SPEED);
            }
        });
        mCBShowSpeed = (CheckBox) layout.findViewById(R.id.checkBoxShowSpeed);
    }

    private void toggleOverlayFlag(int flag) {
        if (mVdtCamera != null) {
            CameraState states = mVdtCamera.getState();
            int flags = Utils.toggleBit(states.mOverlayFlags, flag);
			mVdtCamera.setRecordOverlay(flags);
            mVdtCamera.getRecordOverlayState();
            // force refresh
            states.mOverlayFlags = flags;
            updateOverlayStates();
        }
    }

    private void updateOverlayStates() {
        int flags = mVdtCamera.getState().mOverlayFlags;
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
        mVdtCamera = getCameraFromIntent(null);
        if (mVdtCamera == null) {
            noCamera();
            return;
        }

        //mVdtCamera.addCallback(mCameraCallback);
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
        if (mVdtCamera != null) {
            //mVdtCamera.removeCallback(mCameraCallback);
            mVdtCamera = null;
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


	/*
    private final VdtCamera.Callback mCameraCallback = new VdtCamera.Callback() {

		@Override
		public void onStateChanged(VdtCamera vdtCamera) {
			if (vdtCamera == mVdtCamera) {
				updateCameraState();
			}
		}

		@Override
		public void onBtStateChanged(VdtCamera vdtCamera) {

		}

		@Override
		public void onGpsStateChanged(VdtCamera vdtCamera) {

		}

		@Override
		public void onWifiStateChanged(VdtCamera vdtCamera) {

		}

		@Override
		public void onStartRecordError(VdtCamera vdtCamera, int error) {

		}

		@Override
		public void onHostSSIDFetched(VdtCamera vdtCamera, String ssid) {

		}

		@Override
		public void onScanBtDone(VdtCamera vdtCamera) {

		}

		@Override
		public void onBtDevInfo(VdtCamera vdtCamera, int type, String mac, String name) {

		}

		@Override
		public void onStillCaptureStarted(VdtCamera vdtCamera, boolean bOneShot) {

		}

		@Override
		public void onStillPictureInfo(VdtCamera vdtCamera, boolean bCapturing, int numPictures, int burstTicks) {

		}

		@Override
		public void onStillCaptureDone(VdtCamera vdtCamera) {

		}

		@Override
		public void onConnected(VdtCamera vdtCamera) {

		}

		@Override
		public void onDisconnected(VdtCamera vdtCamera) {
			if (vdtCamera == mVdtCamera) {
				removeCamera();
				noCamera();
			}
		}
	}; */
}
