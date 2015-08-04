package com.transee.viditcam.actions;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.waylens.hachi.R;

abstract public class ShowScanResult extends DialogBuilder {

	private final String mWifiName;
	private final String mWifiPassword;
	private final String mScanResult;
	private final boolean mbDecoded;
	private boolean mbYesClicked;

	abstract public void onClickYes();

	abstract public void onDismiss(ShowScanResult action);

	public ShowScanResult(Activity activity, String wifiName, String wifiPassword, String result, boolean bDecoded) {
		super(activity);
		mWifiName = wifiName;
		mWifiPassword = wifiPassword;
		mScanResult = result;
		mbDecoded = bDecoded;
		mbYesClicked = false;
		setTitle(R.string.title_scan_result);
		if (bDecoded) {
			setContent(R.layout.dialog_barcode_scan_ok);
			setButtons(DialogBuilder.DLG_YES_NO);
		} else {
			setContent(R.layout.dialog_unknown_device);
			setButtons(DialogBuilder.DLG_OK);
		}
	}

	public boolean isYesClicked() {
		return mbYesClicked;
	}

	@Override
	protected void onDialogCreated(BaseDialog dialog, View layout) {
		if (mbDecoded) {
			TextView nameView = (TextView)layout.findViewById(R.id.textCameraName);
			nameView.setText(mWifiName);
			TextView passwordView = (TextView)layout.findViewById(R.id.textView6);
			passwordView.setText(mWifiPassword);
		} else {
			TextView code = (TextView)layout.findViewById(R.id.textCameraName);
			code.setText(mScanResult);
		}
	}

	@Override
	protected void onClickPositiveButton() {
		mbYesClicked = true;
		onClickYes();
	}

	@Override
	protected void onDismiss() {
		onDismiss(ShowScanResult.this);
	}

}
