package com.transee.viditcam.actions;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.waylens.hachi.R;

abstract public class GetWifiAP extends DialogBuilder {

	abstract public void onShowWifiList();

	abstract public void onGetWifiAP(String ssid, String password);

	private String mSSID;
	private EditText mSSIDEdit;
	private EditText mPasswordEdit;
	private Button mOKButton;

	public GetWifiAP(Activity activity, String ssid) {
		super(activity);
		mSSID = ssid;
		setContent(R.layout.dialog_get_wifi_ap);
		setButtons(DialogBuilder.DLG_OK_CANCEL);
		setNeutralButton(R.string.btn_wifi_list);
	}

	private void showPassword(boolean isChecked) {
		if (isChecked) {
			mPasswordEdit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
		} else {
			mPasswordEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
		}
	}

	@Override
	protected void onDialogCreated(BaseDialog dialog, View layout) {
		mOKButton = dialog.getButton(DialogBuilder.BUTTON_POSITIVE);

		mSSIDEdit = (EditText)layout.findViewById(R.id.editText1);
		mSSIDEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				mOKButton.setEnabled(s.length() > 0);
			}
		});
		if (mSSID != null) {
			mSSIDEdit.setText(mSSID);
			mOKButton.setEnabled(true);
		} else {
			mOKButton.setEnabled(false);
		}

		mPasswordEdit = (EditText)layout.findViewById(R.id.editText2);
		mPasswordEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		if (mSSID != null) {
			mPasswordEdit.requestFocus();
		}

		CheckBox cb = (CheckBox)layout.findViewById(R.id.checkBox1);
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				showPassword(isChecked);
			}
		});

		cb.setChecked(true);
	}

	@Override
	protected void onClickNeutralButton() {
		GetWifiAP.this.onShowWifiList();
	}

	@Override
	protected void onClickPositiveButton() {
		String ssid = mSSIDEdit.getText().toString();
		String password = mPasswordEdit.getText().toString();
		onGetWifiAP(ssid, password);
	}

}
