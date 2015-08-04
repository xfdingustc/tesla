package com.transee.viditcam.actions;

import android.app.Activity;
import android.view.View;
import android.widget.EditText;

import com.waylens.hachi.R;


abstract public class GetCameraName extends DialogBuilder {

	abstract public void onCameraNameChanged(String value);

	protected final String mOldName;
	protected EditText mEditCameraName;

	public GetCameraName(Activity activity, String oldName) {
		super(activity);
		mOldName = oldName;
		setContent(R.layout.dialog_get_camera_name);
		setButtons(DialogBuilder.DLG_OK_CANCEL);
	}

	@Override
	protected void onDialogCreated(BaseDialog dialog, View layout) {
		mEditCameraName = (EditText)layout.findViewById(R.id.editText1);
		mEditCameraName.setHint(R.string.lable_camera_noname);
		if (mOldName.length() > 0) {
			mEditCameraName.setText(mOldName);
			mEditCameraName.selectAll();
		}
	}

	@Override
	protected void onClickPositiveButton() {
		String value = mEditCameraName.getText().toString();
		if (!value.equals(mOldName)) {
			onCameraNameChanged(value);
		}
	}

}
