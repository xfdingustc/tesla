package com.transee.viditcam.actions;

import android.app.Activity;
import android.view.View;
import android.widget.EditText;

import com.waylens.hachi.R;

abstract public class GetServerAddress extends DialogBuilder {

	abstract public void onGetServerAddress(String address);

	private String mDefaultAddress;
	private EditText mAddressEdit;

	public GetServerAddress(Activity activity, String defaultAddress) {
		super(activity);
		mDefaultAddress = defaultAddress;
		setContent(R.layout.dialog_get_server_address);
		setButtons(DialogBuilder.DLG_OK_CANCEL);
	}

	@Override
	protected void onDialogCreated(BaseDialog dialog, View layout) {
		mAddressEdit = (EditText)layout.findViewById(R.id.editText1);
		mAddressEdit.setText(mDefaultAddress);
	}

	@Override
	protected void onClickPositiveButton() {
		mDefaultAddress = mAddressEdit.getText().toString();
		onGetServerAddress(mDefaultAddress);
	}

}
