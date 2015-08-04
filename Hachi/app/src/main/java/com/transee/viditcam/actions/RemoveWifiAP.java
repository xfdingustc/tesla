package com.transee.viditcam.actions;

import android.app.Activity;

import com.waylens.hachi.R;

import java.util.Locale;

abstract public class RemoveWifiAP extends DialogBuilder {

	abstract public void onRemoveWifiAPConfirmed(String ssid);

	private String mSSID;

	public RemoveWifiAP(Activity activity, String ssid) {
		super(activity);
		mSSID = ssid;
		String fmt = mContext.getResources().getString(R.string.msg_remove_wifi_ap);
		String msg = String.format(Locale.US, fmt, mSSID);
		setMsg(msg);
		setButtons(DialogBuilder.DLG_OK_CANCEL);
	}

	@Override
	protected void onClickPositiveButton() {
		onRemoveWifiAPConfirmed(mSSID);
	}

}
