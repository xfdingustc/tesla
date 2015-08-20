package com.transee.viditcam.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;

import com.transee.ccam.Camera;
import com.transee.ccam.CameraManager;
import com.transee.ccam.CameraState;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;

@SuppressLint("Registered")
public class BaseActivity extends Activity {

	protected Hachi thisApp;
	protected int mOrientation;
	protected boolean mbStarted;
	protected boolean mbRotating;
	protected PopupWindow mPopupWindow;

	// static final String TAG = "BaseActivity";

	// inherit
	protected void requestContentView() {
	}

	// inherit
	protected void onCreateActivity(Bundle savedInstanceState) {
	}

	// inherit
	protected void onDestroyActivity() {
	}

	// inherit
	protected void onStartActivity() {
	}

	// inherit
	protected void onStopActivity() {
	}

	// inherit
	protected void onPauseActivity() {
	}

	// inherit
	protected void onResumeActivity() {
	}

	// inherit
	protected void onReleaseUI() {
	}

	// inherit
	protected void onInitUI() {
	}

	// inherit
	protected void onSetupUI() {
	}

	protected String TAG() {
		return getTitle().toString();
	}

	private final int getOrientation() {
		return getResources().getConfiguration().orientation;
	}

	// API
	public final boolean isLandscape() {
		return mOrientation == Configuration.ORIENTATION_LANDSCAPE;
	}

	// API
	public final boolean isPortrait() {
		return mOrientation == Configuration.ORIENTATION_PORTRAIT;
	}

	// API
	public final boolean isRotating() {
		return mbRotating;
	}

	// API
	public final boolean isStarted() {
		return mbStarted;
	}

	// API
	public final void toggleFullScreen() {
		WindowManager.LayoutParams params = getWindow().getAttributes();
		if (isLandscape()) {
			params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
			getWindow().setAttributes(params);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		} else {
			params.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().setAttributes(params);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		}
	}

	// activity:
	// isLocal=false, [isServer=false], ssid, hostString: vidit camera
	// isLocal=true, [isServer=false]: local vdb activity
	// isLocal=false, isServer=true: server vdb activity

	static final private String IS_LOCAL = "isLocal";
	static final private String IS_PC_SERVER = "isPcServer";
	static final private String SSID = "ssid";
	static final private String HOST_STRING = "hostString";

	// API
	protected void startCameraActivity(Camera camera, Class<?> cls, int requestCode) {
		Intent intent = new Intent(this, cls);
		Bundle bundle = new Bundle();
		bundle.putBoolean(IS_LOCAL, false);
		bundle.putBoolean(IS_PC_SERVER, camera.isPcServer());
		bundle.putString(SSID, camera.getSSID());
		bundle.putString(HOST_STRING, camera.getHostString());
		if (requestCode < 0) {
			intent.putExtras(bundle);
			startActivity(intent);
		} else {
			bundle.putBoolean("requested", true);
			intent.putExtras(bundle);
			startActivityForResult(intent, requestCode);
		}
	}

	// API
	protected void startCameraActivity(Camera camera, Class<?> cls) {
		startCameraActivity(camera, cls, -1);
	}

	// API
	protected boolean isActivityRequested() {
		return getIntent().getExtras().getBoolean("requested");
	}

	// API
	protected void startLocalActivity(Class<?> cls) {
		Intent intent = new Intent(this, cls);
		Bundle bundle = new Bundle();
		bundle.putBoolean(IS_LOCAL, true);
		intent.putExtras(bundle);
		startActivity(intent);
	}

	/*
	// API
	protected void startServerActivity(Class<?> cls, String serverAddress) {
		Intent intent = new Intent(this, cls);
		Bundle bundle = new Bundle();
		bundle.putBoolean(IS_LOCAL, false);
		bundle.putBoolean(IS_SERVER, true);
		bundle.putString(HOST_STRING, serverAddress);
		intent.putExtras(bundle);
		startActivity(intent);
	}
	*/

	// API


	// API
	protected boolean isServerActivity(Bundle bundle) {
		return bundle.getBoolean(IS_PC_SERVER, false);
	}

	// API
	protected String getServerAddress(Bundle bundle) {
		return bundle.getString(HOST_STRING);
	}

	// API
	protected Camera getCameraFromIntent(Bundle bundle) {
		if (bundle == null) {
			bundle = getIntent().getExtras();
		}
		String ssid = bundle.getString(SSID);
		String hostString = bundle.getString(HOST_STRING);
		if (ssid == null || hostString == null) {
			return null;
		}
		CameraManager cameraManager = CameraManager.getManager();
		Camera camera = cameraManager.findConnectedCamera(ssid, hostString);
		return camera;
	}

	// API
	protected String getCameraName(CameraState states) {
		if (states.mCameraName.length() == 0) {
			return getResources().getString(R.string.lable_camera_noname);
		} else {
			return states.mCameraName;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		thisApp = (Hachi)getApplication();
		super.onCreate(savedInstanceState);
		mOrientation = getOrientation();
		requestContentView();
		onCreateActivity(savedInstanceState);
		onInitUI();
	}

	@Override
	protected void onDestroy() {
		onDestroyActivity();
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mbStarted = true;
		onStartActivity();
		onSetupUI();
	}

	@Override
	protected void onStop() {
		mbStarted = false;
		onStopActivity();
		super.onStop();
	}

	@Override
	protected void onPause() {
		onPauseActivity();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		onResumeActivity();
	}

	// activity A -> B;
	// rotate B;
	// back to A;

	// A.onConfigurationChanged();
	// A.onStart();

	// inherit
	protected void onRotate() {
		onReleaseUI();
		requestContentView();
		onInitUI();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		int orientation = getOrientation();
		if (orientation != mOrientation) {
			mOrientation = orientation;
			mbRotating = true;
			onRotate();
			if (mbStarted) {
				if (mPopupWindow != null) {
					mPopupWindow.dismiss();
					mPopupWindow = null;
				}
				onSetupUI();
			}
			mbRotating = false;
		}
	}

	// API
	public void setPopupWindow(PopupWindow popupWindow, boolean bHookDismiss) {
		if (mPopupWindow != null) {
			mPopupWindow.dismiss();
		}
		mPopupWindow = popupWindow;
		if (bHookDismiss) {
			mPopupWindow.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss() {
					mPopupWindow = null;
				}
			});
		}
	}

	// API
	public PopupWindow createPopupWindow(View layout) {
		Drawable background = getResources().getDrawable(R.drawable.new_menu_bg);
		Rect prect = new Rect();
		background.getPadding(prect);

		int width = layout.getMeasuredWidth() + prect.left + prect.right;
		int height = layout.getMeasuredHeight() + prect.top + prect.bottom;

		PopupWindow window = new PopupWindow(layout, width, height, true);

		// window.setWindowLayoutMode(LayoutParams.WRAP_CONTENT,
		// LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(background);
		window.setOutsideTouchable(true);
		// window.setFocusable(true);
		window.setAnimationStyle(android.R.style.Animation_Dialog);
		window.update();

		setPopupWindow(window, true);

		return window;
	}

}
