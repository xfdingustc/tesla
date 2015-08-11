package com.transee.viditcam.actions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.Button;
import android.widget.PopupWindow;

import com.transee.ccam.Camera;
import com.transee.ccam.WifiState;
import com.transee.common.Utils;
import com.transee.viditcam.app.BaseActivity;
import com.waylens.hachi.R;

abstract public class CameraOperations {

	abstract protected void onClickBrowseVideo(CameraOperations action);

	abstract protected void onClickSetup(CameraOperations action);

	abstract protected void onClickChangePassword(CameraOperations action);

	abstract protected void onClickPowerOff(CameraOperations action);

	abstract protected void onClickReboot(CameraOperations action);

	abstract protected void onClickWifiMode(CameraOperations action);

	private final Activity mActivity;
	private View mLayout;
	private PopupWindow mWindow;

	public String mSSID;
	public String mHostString; // null: camera not connected

	@SuppressLint("InflateParams")
	public CameraOperations(Activity activity, Camera camera, String titleString, String ssid, String hostString) {
		mActivity = activity;
		mSSID = ssid;
		mHostString = hostString;

		mLayout = LayoutInflater.from(activity).inflate(R.layout.menu_camera_operations, null);

		Button button;
		button = (Button)mLayout.findViewById(R.id.btnBrowseVideo);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mWindow.dismiss();
				onClickBrowseVideo(CameraOperations.this);
			}
		});
		if (mHostString == null) {
			button.setEnabled(false);
		}

		button = (Button)mLayout.findViewById(R.id.btnSetup);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mWindow.dismiss();
				onClickSetup(CameraOperations.this);
			}
		});
		if (mHostString == null) {
			button.setEnabled(false);
		}

		button = (Button)mLayout.findViewById(R.id.btnChangePassword);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mWindow.dismiss();
				onClickChangePassword(CameraOperations.this);
			}
		});

		button = (Button)mLayout.findViewById(R.id.btnPowerOff);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mWindow.dismiss();
				onClickPowerOff(CameraOperations.this);
			}
		});
		if (mHostString == null) {
			button.setEnabled(false);
		}

		button = (Button)mLayout.findViewById(R.id.btnReboot);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mWindow.dismiss();
				onClickReboot(CameraOperations.this);
			}
		});
		if (mHostString == null) {
			button.setEnabled(false);
		}

		button = (Button)mLayout.findViewById(R.id.btnWifiMode);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mWindow.dismiss();
				onClickWifiMode(CameraOperations.this);
			}
		});

		if (camera == null || Camera.getWifiStates(camera).mWifiMode == WifiState.WIFI_Mode_Unknown) {
			button.setVisibility(View.GONE);
			mLayout.findViewById(R.id.separator2).setVisibility(View.GONE);
		}

		mLayout.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		//mWindow = activity.createPopupWindow(mLayout);
	}

	public void show(View anchor) {
		int[] location = new int[2];
		anchor.getLocationInWindow(location);
		int screenWidth = Utils.getScreenWidth(mActivity);
		int screenHeight = Utils.getScreenHeight(mActivity);

		int margin = (int)Utils.dp2px(mActivity, 8);

		// calc x pos
		int x = location[0];
		if (x + mWindow.getWidth() + margin > screenWidth) {
			x = screenWidth - (mWindow.getWidth() + margin);
		}

		// calc y pos
		int y = location[1] + anchor.getHeight();
		if (y + mWindow.getHeight() > screenHeight) {
			y = location[1] - mWindow.getHeight();
		}
		mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
	}
}
