package com.transee.viditcam.actions;

import android.app.Activity;
import android.view.View;
import android.widget.RadioButton;

import com.waylens.hachi.hardware.VdtCamera;
import com.transee.ccam.WifiState;
import com.waylens.hachi.R;

abstract public class SelectWifiMode extends DialogBuilder {

	abstract protected void onChangeWifiMode(VdtCamera vdtCamera, int newMode);

	abstract protected void onSetupWifiAP(VdtCamera vdtCamera);

	private final VdtCamera mVdtCamera;
	private int mWifiMode = WifiState.WIFI_Mode_Unknown;

	public SelectWifiMode(Activity activity, VdtCamera vdtCamera) {
		super(activity);
		mVdtCamera = vdtCamera;
		setTitle(R.string.title_switch_wifi_mode);
		setContent(R.layout.dialog_switch_wifi_mode);
		setButtons(DLG_OK_CANCEL);
		setNeutralButton(R.string.btn_setup_wifi_ap);
	}

	private final View.OnClickListener mOnClickWifiMode = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			mWifiMode = (Integer)v.getTag();
		}
	};

	private RadioButton setupRadioButton(View layout, int id, int mode) {
		RadioButton radio = (RadioButton)layout.findViewById(id);
		radio.setTag(mode);
		radio.setOnClickListener(mOnClickWifiMode);
		if (VdtCamera.getWifiStates(mVdtCamera).mWifiMode == mode) {
			radio.setChecked(true);
		}
		return radio;
	}

	@Override
	protected void onDialogCreated(BaseDialog dialog, View layout) {
		// AP mode
		setupRadioButton(layout, R.id.radio0, WifiState.WIFI_Mode_AP);
		// Client mode
		RadioButton radio = setupRadioButton(layout, R.id.radio1, WifiState.WIFI_Mode_Client);
		if (VdtCamera.getWifiStates(mVdtCamera).mNumWifiAP == 0) {
			radio.setEnabled(false);
		}
	}

	@Override
	protected void onClickPositiveButton() {
		if (mWifiMode != WifiState.WIFI_Mode_Unknown && mWifiMode != VdtCamera.getWifiStates(mVdtCamera).mWifiMode) {
			onChangeWifiMode(mVdtCamera, mWifiMode);
		}
	}

	@Override
	protected void onClickNegativeButton() {
	}

	@Override
	protected void onClickNeutralButton() {
		onSetupWifiAP(mVdtCamera);
	}

	@Override
	protected void onDismiss() {
	}

}
