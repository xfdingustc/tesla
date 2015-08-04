package com.transee.viditcam.actions;

import android.app.Activity;

import com.transee.ccam.CameraState;
import com.waylens.hachi.R;

abstract public class SelectVideoQuality extends SingleSelect {

	final int mVideoQualityList;
	final int mVideoQualityIndex;

	abstract protected void onSelectVideoQuality(int qualityIndex);

	public SelectVideoQuality(Activity activity, CameraState states) {
		super(activity);
		mVideoQualityList = states.mVideoQualityList;
		mVideoQualityIndex = states.mVideoQualityIndex;
	}

	@Override
	protected void onSelectItem(int id) {
		onSelectVideoQuality(id);
	}

	@Override
	public void show() {
		String[] videoQualityNames = mContext.getResources().getStringArray(R.array.quality);
		for (int i = 0; i < videoQualityNames.length - 1; i++) {
			if ((mVideoQualityList & (1 << i)) != 0) {
				addItem(videoQualityNames[i + 1], i);
			}
		}
		setSelId(mVideoQualityIndex);
		super.show();
	}

}
