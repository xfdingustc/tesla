package com.transee.viditcam.actions;

import android.app.Activity;
import android.view.View;
import android.widget.Button;

import com.waylens.hachi.R;

abstract public class ShowCameraMenu extends DialogBuilder {

	abstract protected void onClickBrowseVideo(ShowCameraMenu action);

	abstract protected void onClickSetup(ShowCameraMenu action);

	abstract protected void onClickChangePassword(ShowCameraMenu action);

	abstract protected void onClickPowerOff(ShowCameraMenu action);

	public String mSSID;
	public String mHostString; // null: camera not connected

	public ShowCameraMenu(Activity activity, String titleString, String ssid, String hostString) {
		super(activity);
		mSSID = ssid;
		mHostString = hostString;
		setTitle(titleString);
		setContent(R.layout.dialog_camera_menu);
		setButtons(DialogBuilder.DLG_CANCEL);
	}

	private View.OnClickListener mOnClickButton = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// Log.d("ddd", "id : " + Integer.toHexString(v.getId()) + ", " + ((Button)v).getText().toString());
			dismiss();
			switch (v.getId()) {
			case R.id.btnBrowseVideo:
				onClickBrowseVideo(ShowCameraMenu.this);
				break;
			case R.id.btnSetup:
				onClickSetup(ShowCameraMenu.this);
				break;
			case R.id.btnChangePassword:
				onClickChangePassword(ShowCameraMenu.this);
				break;
			case R.id.btnPowerOff:
				onClickPowerOff(ShowCameraMenu.this);
				break;
			}
		}
	};

	private void setupButton(View layout, int resId, boolean bCheck) {
		Button button = (Button)layout.findViewById(resId);
		if (bCheck && mHostString == null) {
			button.setEnabled(false);
		} else {
			button.setOnClickListener(mOnClickButton);
		}
	}

	@Override
	protected void onDialogCreated(BaseDialog dialog, View layout) {
		dialog.requestNoPadding();
		setupButton(layout, R.id.btnBrowseVideo, true);
		setupButton(layout, R.id.btnSetup, true);
		setupButton(layout, R.id.btnChangePassword, false);
		setupButton(layout, R.id.btnPowerOff, true);
	}

}
