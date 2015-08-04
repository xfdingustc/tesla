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

abstract public class GetCameraPassword extends DialogBuilder {

	abstract public void onGetPasswordOK(GetCameraPassword action, String ssid, String password);

	abstract public void onClickScanCode(GetCameraPassword action, String ssid);

	private String mSSID;
	public String mOldPassword; // may be null
	public boolean mbChangePassword;
	private String mPassword;
	private EditText mPasswordEdit;
	private Button mOKButton;

	public GetCameraPassword(Activity activity, String ssid, String password, boolean bChangePassword) {
		super(activity);
		mSSID = ssid;
		mOldPassword = password;
		mbChangePassword = bChangePassword;
		setContent(R.layout.dialog_get_password);
		setButtons(DialogBuilder.DLG_OK_CANCEL);
		setNeutralButton(R.string.btn_scan_qrcode);
	}

	private void enablePositiveButton() {
		mOKButton.setEnabled(isValidPassword());
	}

	private boolean isValidPassword() {
		return mPassword != null && mPassword.length() >= 8;
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
		mPasswordEdit = (EditText)layout.findViewById(R.id.editText1);
		mPasswordEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				mPassword = s.toString();
				enablePositiveButton();
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		mOKButton = dialog.getButton(DialogBuilder.BUTTON_POSITIVE);
		CheckBox cb = (CheckBox)layout.findViewById(R.id.checkBox1);
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				showPassword(isChecked);
			}
		});

		enablePositiveButton();
		cb.setChecked(true);

		if (mOldPassword != null) {
			mPasswordEdit.setText(mOldPassword);
			mPasswordEdit.selectAll();
		}
	}

	@Override
	protected void onClickPositiveButton() {
		onGetPasswordOK(GetCameraPassword.this, mSSID, mPassword);
	}

	@Override
	protected void onClickNeutralButton() {
		onClickScanCode(GetCameraPassword.this, mSSID);
	}

}
