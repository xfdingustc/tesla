package com.transee.viditcam.actions;

import android.app.Activity;

import com.transee.ccam.CameraState;
import com.waylens.hachi.R;

abstract public class SelectColorMode extends SingleSelect {

	abstract protected void onSelectColorMode(int index);

	private final int mColorModeList;
	private final int mColorModeIndex;

	public SelectColorMode(Activity activity, CameraState states) {
		super(activity);
		mColorModeList = states.mColorModeList;
		mColorModeIndex = states.mColorModeIndex;
	}

	@Override
	protected void onSelectItem(int id) {
		onSelectColorMode(id);
	}

	@Override
	public void show() {
		String[] colorModeNames = mContext.getResources().getStringArray(R.array.colormode);
		for (int i = 0; i < colorModeNames.length - 1; i++) {
			if ((mColorModeList & (1 << i)) != 0) {
				addItem(colorModeNames[i + 1], i);
			}
		}
		setSelId(mColorModeIndex);
		super.show();
	}
}
