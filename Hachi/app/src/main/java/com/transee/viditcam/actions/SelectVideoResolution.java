package com.transee.viditcam.actions;

import android.app.Activity;

import com.transee.ccam.CameraState;
import com.waylens.hachi.R;

abstract public class SelectVideoResolution extends SingleSelect {

	abstract protected void onSelectVideoResolution(int resolutionIndex);

	final int mVideoResolutionList;
	final int mVideoResolutionIndex;

	// index -> resolution
	static final int[] mVideoResIndex = new int[] {
			5, // 4kp60
			4, // 4kp30
			1, // 1080p60
			0, // 1080p30
			8, // 720p120
			3, // 720p60
			2, // 720p30
			7, // 480p60
			6, // 480p30
	};

	public SelectVideoResolution(Activity activity, CameraState states) {
		super(activity);
		mVideoResolutionList = states.mVideoResolutionList;
		mVideoResolutionIndex = states.mVideoResolutionIndex;
	}

	@Override
	protected void onSelectItem(int id) {
		onSelectVideoResolution(id);
	}

	@Override
	public void show() {
		String[] resolutionNames = mContext.getResources().getStringArray(R.array.resolution);
		for (int i = 0; i < mVideoResIndex.length; i++) {
			int res = mVideoResIndex[i];
			if ((mVideoResolutionList & (1 << res)) != 0) {
				addItem(resolutionNames[res + 1], res);
			}
		}
		setSelId(mVideoResolutionIndex);
		super.show();
	}

}
