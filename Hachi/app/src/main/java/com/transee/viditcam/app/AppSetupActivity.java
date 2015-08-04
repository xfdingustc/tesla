package com.transee.viditcam.app;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.transee.viditcam.app.comp.MapProvider;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.views.PrefsUtil;

public class AppSetupActivity extends BaseActivity {

	private final CameraVideoEditPref mVideoEditPref = new CameraVideoEditPref();

	private boolean mbAutoPreview;
	private CheckBox mAutoPreviewCB;

	private CheckBox mShowAccCB;
	private CheckBox mAutoFastBrowseCB;
	private CheckBox mShowButtonHintCB;

	private RadioButton rbReal;
	private RadioButton rbSimulation;
	private RadioButton gpsDevice;
	private RadioButton gpsCamera;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_app_setup);

		mVideoEditPref.load(this);

		setupMapButton(R.id.radio1, MapProvider.MAP_GOOGLE, mVideoEditPref.mapProvider);
		setupMapButton(R.id.radio2, MapProvider.MAP_AMAP, mVideoEditPref.mapProvider);

		setupPlayStream(R.id.radioHighBitrate, false, mVideoEditPref.playLowBitrateStream);
		setupPlayStream(R.id.radioLowBitrate, true, mVideoEditPref.playLowBitrateStream);

		mAutoPreviewCB = (CheckBox)findViewById(R.id.checkBoxAutoPreview);
		mbAutoPreview = CameraListActivity.getAutoPreview(this);
		mAutoPreviewCB.setChecked(mbAutoPreview);

		View layoutAutoPreview = findViewById(R.id.layoutAutoPreview);

		layoutAutoPreview.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mbAutoPreview = !mbAutoPreview;
				mAutoPreviewCB.setChecked(mbAutoPreview);
				CameraListActivity.setAutoPreview(AppSetupActivity.this, mbAutoPreview);
			}
		});

		PackageInfo info = getPackageInfo();

		if (info != null) {
			TextView versionText = (TextView)findViewById(R.id.textVersion);
			versionText.setText(info.versionName);
			TextView releaseDateText = (TextView)findViewById(R.id.textReleaseDate);
			releaseDateText.setText(getReleaseDate());
		}

		View viewShowAcc = findViewById(R.id.layoutShowAcc);

		mShowAccCB = (CheckBox)viewShowAcc.findViewById(R.id.checkBoxShowAcc);
		mShowAccCB.setChecked(mVideoEditPref.showAcc);

		viewShowAcc.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mVideoEditPref.showAcc = !mVideoEditPref.showAcc;
				mShowAccCB.setChecked(mVideoEditPref.showAcc);
				mVideoEditPref.save(AppSetupActivity.this);
			}
		});

		View autoFastBrowse = findViewById(R.id.layoutAutoFastBrowse);

		mAutoFastBrowseCB = (CheckBox)autoFastBrowse.findViewById(R.id.checkBoxAutoFastBrowse);
		mAutoFastBrowseCB.setChecked(mVideoEditPref.autoFastBrowse);

		autoFastBrowse.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mVideoEditPref.autoFastBrowse = !mVideoEditPref.autoFastBrowse;
				mAutoFastBrowseCB.setChecked(mVideoEditPref.autoFastBrowse);
				mVideoEditPref.save(AppSetupActivity.this);
			}
		});

		View showButtonHint = findViewById(R.id.layoutShowButtonHint);

		mShowButtonHintCB = (CheckBox)showButtonHint.findViewById(R.id.checkBoxShowButtonHint);
		mShowButtonHintCB.setChecked(mVideoEditPref.showButtonHint);

		showButtonHint.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mVideoEditPref.showButtonHint = !mVideoEditPref.showButtonHint;
				mShowButtonHintCB.setChecked(mVideoEditPref.showButtonHint);
				mVideoEditPref.save(AppSetupActivity.this);
			}
		});

		setUpDatMode();
	}

	private void setUpDatMode() {
		rbReal = (RadioButton) findViewById(R.id.radio_real);
		rbSimulation = (RadioButton) findViewById(R.id.radio_simulation);
		int mode = PrefsUtil.getDataMode();
		if (mode == PrefsUtil.MODE_REAL) {
			rbReal.setChecked(true);
		} else {
			rbSimulation.setChecked(true);
		}

		gpsDevice = (RadioButton) findViewById(R.id.radio_gps_device);
		gpsCamera = (RadioButton) findViewById(R.id.radio_gps_camera);
		if (PrefsUtil.getGPSource() == PrefsUtil.GPS_DEVICE) {
			gpsDevice.setChecked(true);
		} else {
			gpsCamera.setChecked(true);
		}
	}

	public void onModeRadioButtonClicked(View view) {
		boolean checked = ((RadioButton) view).isChecked();
		switch (view.getId()) {
			case R.id.radio_real:
				if (checked) {
					PrefsUtil.setDataMode(PrefsUtil.MODE_REAL);
				}
				break;
			case R.id.radio_simulation:
				if (checked) {
					PrefsUtil.setDataMode(PrefsUtil.MODE_SIMULATION);
				}
				break;
		}
	}

	private PackageInfo getPackageInfo() {
		PackageManager packageManager = getPackageManager();
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
			return packageInfo;
		} catch (Exception ex) {
			return null;
		}
	}

	private String getReleaseDate() {
		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
			Bundle bundle = ai.metaData;
			String date = bundle.getString("date.release");
			return date;
		} catch (NameNotFoundException e) {
			return "";
		}
	}

	private void setupMapButton(int id, int mapProvider, int mCurrMapProvider) {
		RadioButton button = (RadioButton)findViewById(id);
		button.setTag(mapProvider);
		if (mapProvider == mCurrMapProvider) {
			button.setChecked(true);
		}
		button.setOnClickListener(mOnClickMapProvider);
	}

	private final View.OnClickListener mOnClickMapProvider = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			mVideoEditPref.mapProvider = (Integer)v.getTag();
			mVideoEditPref.save(AppSetupActivity.this);
		}
	};

	private void setupPlayStream(int id, boolean bPlayLowBitrate, boolean bCurrValue) {
		RadioButton button = (RadioButton)findViewById(id);
		button.setTag(bPlayLowBitrate);
		if (bPlayLowBitrate == bCurrValue) {
			button.setChecked(true);
		}
		button.setOnClickListener(mOnClickBitrate);
	}

	private final View.OnClickListener mOnClickBitrate = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			mVideoEditPref.playLowBitrateStream = (Boolean)v.getTag();
			mVideoEditPref.save(AppSetupActivity.this);
		}
	};

	@Override
	public void onBackPressed() {
		finish();
		Hachi.slideOutToRight(this, false);
	}

	public void onGPSRadioButtonClicked(View view) {
		boolean checked = ((RadioButton) view).isChecked();
		switch (view.getId()) {
			case R.id.radio_gps_device:
				if (checked) {
					PrefsUtil.setGPSSource(PrefsUtil.GPS_DEVICE);
				}
				break;
			case R.id.radio_gps_camera:
				if (checked) {
					PrefsUtil.setGPSSource(PrefsUtil.GPS_CAMERA);
				}
				break;
		}
	}
}
