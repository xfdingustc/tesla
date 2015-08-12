package com.transee.viditcam.app;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.transee.ccam.BtState;
import com.transee.ccam.Camera;
import com.transee.ccam.CameraClient;
import com.transee.ccam.CameraState;
import com.transee.ccam.WifiState;
import com.transee.common.Utils;
import com.waylens.hachi.R;
import com.transee.viditcam.actions.GetCameraName;
import com.transee.viditcam.actions.SelectColorMode;
import com.transee.viditcam.actions.SelectVideoQuality;
import com.transee.viditcam.actions.SelectVideoResolution;
import com.transee.viditcam.actions.SyncDateTime;
import com.waylens.hachi.app.Hachi;

public class CameraSetupActivity extends BaseActivity {

	static final boolean DEBUG = false;
	static final String TAG = "CameraSetupActivity";

	@Nullable
	private Camera mCamera;

	private TextView mTextCameraName;

	private ImageView mBatteryIcon;
	private TextView mTextBatteryInfo;

	private ProgressBar mStorageUsage;
	private TextView mTextStorageInfo;

	private TextView mTextVideoResolution;
	private TextView mTextVideoQuality;
	private TextView mTextColorMode;

	private CheckBox mCBAutoRecord;
	private CheckBox mCBAutoDelete;
	private CheckBox mCBEnableMic;

	private TextView mTextWifiMode;
	private TextView mTextBtSupport;
	private TextView mTextFirmwareVersion;

	private CameraState getCameraStates() {
		return Camera.getCameraStates(mCamera);
	}

	private BtState getBtStates() {
		return Camera.getBtStates(mCamera);
	}

	private CameraClient getCameraClient() {
		return (CameraClient)mCamera.getClient();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera_setup);

		View layout = findViewById(R.id.layoutCameraName);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCamera != null) {
					onChangeCameraName();
				}
			}
		});
		mTextCameraName = (TextView)layout.findViewById(R.id.textCameraName);

		layout = findViewById(R.id.layoutBattery);
		mBatteryIcon = (ImageView)layout.findViewById(R.id.imageView7);
		mTextBatteryInfo = (TextView)layout.findViewById(R.id.textBatteryInfo);

		layout = findViewById(R.id.layoutStorage);
		mStorageUsage = (ProgressBar)layout.findViewById(R.id.progressBar1);
		mTextStorageInfo = (TextView)layout.findViewById(R.id.textStorageInfo);

		layout = findViewById(R.id.layoutVideoResolution);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCamera != null) {
					changeVideoResolution();
				}
			}
		});
		mTextVideoResolution = (TextView)layout.findViewById(R.id.textVideoResolution);

		layout = findViewById(R.id.layoutVideoQuality);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCamera != null) {
					changeVideoQuality();
				}
			}
		});
		mTextVideoQuality = (TextView)layout.findViewById(R.id.textVideoQuality);

		layout = findViewById(R.id.layoutColorMode);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCamera != null) {
					changeColorMode();
				}
			}
		});
		mTextColorMode = (TextView)layout.findViewById(R.id.textColorMode);

		layout = findViewById(R.id.layoutAutoRecord);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleRecordMode(CameraState.FLAG_AUTO_RECORD);
			}
		});
		mCBAutoRecord = (CheckBox)layout.findViewById(R.id.checkBoxAutoRecord);

		layout = findViewById(R.id.layoutAutoDelete);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleRecordMode(CameraState.FLAG_LOOP_RECORD);
			}
		});
		mCBAutoDelete = (CheckBox)layout.findViewById(R.id.checkBoxAutoDelete);

		layout = findViewById(R.id.layoutMuteAudio);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onMuteAudio();
			}
		});
		mCBEnableMic = (CheckBox)layout.findViewById(R.id.checkBoxEnableMic);

		layout = findViewById(R.id.layoutVideoOverlay);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickVideoOverlay();
			}
		});

		layout = findViewById(R.id.layoutSyncDateTime);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onSyncDateTime();
			}
		});

		layout = findViewById(R.id.layoutWIFIMode);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onChangeWifiMode();
			}
		});
		mTextWifiMode = (TextView)layout.findViewById(R.id.wifiModeText);

		layout = findViewById(R.id.layoutBluetooth);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickBluetooth();
			}
		});
		mTextBtSupport = (TextView)layout.findViewById(R.id.textBtSupport);

		mTextFirmwareVersion = (TextView)findViewById(R.id.textVersion);
	}

	private void onChangeCameraName() {
		GetCameraName action = new GetCameraName(this, getCameraStates().mCameraName) {
			@Override
			public void onCameraNameChanged(String value) {
				if (mCamera != null) {
					getCameraClient().cmd_Cam_set_Name(value);
					getCameraClient().cmd_Cam_get_Name(); // TODO
					// TODO - update UI now?
				}
			}
		};
		action.show();
	}

	private void changeVideoResolution() {
		SelectVideoResolution action = new SelectVideoResolution(this, getCameraStates()) {
			@Override
			protected void onSelectVideoResolution(int resolutionIndex) {
				if (mCamera != null) {
					getCameraClient().cmd_Rec_Set_Resolution(resolutionIndex);
					getCameraClient().cmd_Rec_get_Resolution(); // TODO
					Camera.getCameraStates(mCamera).mVideoResolutionIndex = resolutionIndex;
					updateVideoResolution(null);
				}
			}
		};
		action.show();
	}

	private void changeVideoQuality() {
		SelectVideoQuality action = new SelectVideoQuality(this, getCameraStates()) {
			@Override
			protected void onSelectVideoQuality(int qualityIndex) {
				if (mCamera != null) {
					getCameraClient().cmd_Rec_Set_Quality(qualityIndex);
					getCameraClient().cmd_Rec_get_Quality(); // TODO
					Camera.getCameraStates(mCamera).mVideoQualityIndex = qualityIndex;
					updateVideoQuality(null);
				}
			}
		};
		action.show();
	}

	private void changeColorMode() {
		SelectColorMode action = new SelectColorMode(this, getCameraStates()) {
			@Override
			protected void onSelectColorMode(int index) {
				if (mCamera != null) {
					getCameraClient().cmd_Rec_Set_ColorMode(index);
					getCameraClient().cmd_Rec_get_ColorMode(); // TODO
					Camera.getCameraStates(mCamera).mColorModeIndex = index;
					updateColorMode(null);
				}
			}
		};
		action.show();
	}

	private void toggleRecordMode(int flag) {
		if (mCamera != null) {
			CameraState states = getCameraStates();
			int flags = Utils.toggleBit(states.mRecordModeIndex, flag);
			getCameraClient().cmd_Rec_Set_RecMode(flags);
			getCameraClient().cmd_Rec_get_RecMode(); // TODO
			states.mRecordModeIndex = flags;
			updateRecordMode();
		}
	}

	private void onMuteAudio() {
		CameraState states = getCameraStates();
		if (states.mMicState != CameraState.State_Mic_Unknown) { // &&
																	// states.mMicVol
																	// >= 0) {
			int state = states.mMicState;
			int vol = 5;
			if (state == CameraState.State_Mic_ON) {
				state = CameraState.State_Mic_MUTE;
			} else if (state == CameraState.State_Mic_MUTE) {
				state = CameraState.State_Mic_ON;
				if (states.mMicVol > 0) {
					vol = states.mMicVol;
				}
			} else {
				return;
			}
			getCameraClient().cmd_audio_setMic(state, vol);
			getCameraClient().cmd_audio_getMicState();
			// force refresh
			states.mMicState = state;
			states.mMicVol = vol;
			updateMicState();
		}
	}

	private void onSyncDateTime() {
		SyncDateTime action = new SyncDateTime(this) {
			@Override
			public void onSyncDateTime(long timeMillis, int timezone) {
				if (mCamera != null) {
					getCameraClient().cmd_Network_Synctime(timeMillis / 1000, timezone / (3600 * 1000));
				}
			}
		};
		action.show();
	}

	private void onChangeWifiMode() {
		if (mCamera != null) {
			super.startCameraActivity(mCamera, CameraWifiSetupActivity.class);
		}
	}

	private void onClickVideoOverlay() {
		if (mCamera != null) {
			super.startCameraActivity(mCamera, CameraOverlaySetupActivity.class);
		}
	}

	private void onClickBluetooth() {
		if (getBtStates().mBtSupport == BtState.BT_Support_Yes) {
			super.startCameraActivity(mCamera, CameraBtSetupActivity.class);
		}
	}

	private void updateStorageInfo(Resources res, CameraState states) {
		if (res == null)
			res = getResources();
		int total_kb = (int)(states.mStorageTotalSpace / 1000);
		int free_kb = (int)(states.mStorageFreeSpace / 1000);
		String total = Utils.formatSpace(total_kb);
		String free = Utils.formatSpace(free_kb);
		String fmt = res.getString(R.string.lable_storage_info_fmt);
		String text = String.format(fmt, free, total);
		mTextStorageInfo.setText(text);
		mStorageUsage.setMax(total_kb);
		mStorageUsage.setProgress(total_kb - free_kb);
		mStorageUsage.setVisibility(View.VISIBLE);
	}

	private void updateVideoResolution(Resources res) {
		if (res == null)
			res = getResources();
		int index = getCameraStates().mVideoResolutionIndex;
		String[] resolutionNames = res.getStringArray(R.array.resolution);
		mTextVideoResolution.setText(Utils.getStringFromArray(index, resolutionNames));
	}

	private void updateVideoQuality(Resources res) {
		if (res == null)
			res = getResources();
		int index = getCameraStates().mVideoQualityIndex;
		String[] qualityNames = res.getStringArray(R.array.quality);
		mTextVideoQuality.setText(Utils.getStringFromArray(index, qualityNames));
	}

	private void updateColorMode(Resources res) {
		if (res == null)
			res = getResources();
		int index = getCameraStates().mColorModeIndex;
		String[] colorModeNames = res.getStringArray(R.array.colormode);
		mTextColorMode.setText(Utils.getStringFromArray(index, colorModeNames));
	}

	private void updateRecordMode() {
		int index = getCameraStates().mRecordModeIndex;
		boolean checked = index >= 0 && (index & CameraState.FLAG_AUTO_RECORD) != 0;
		mCBAutoRecord.setChecked(checked);
		checked = index >= 0 && (index & CameraState.FLAG_LOOP_RECORD) != 0;
		mCBAutoDelete.setChecked(checked);
	}

	private void updateMicState() {
		boolean checked = getCameraStates().mMicState == CameraState.State_Mic_ON;
		mCBEnableMic.setChecked(checked);
	}

	private void updateCameraState() {
		Resources res = getResources();

		// camera name
		CameraState states = getCameraStates();
		mTextCameraName.setText(getCameraName(states));

		// battery vol
		if (states.mBatteryVol < 0) {
			mBatteryIcon.setVisibility(View.GONE);
		} else {
			if (states.mBatteryVol <= 0) {
				mBatteryIcon.setVisibility(View.GONE);
			} else {
				if (states.mBatteryVol < 25) {
					mBatteryIcon.setImageResource(R.drawable.setup_battery_low);
				} else if (states.mBatteryVol < 50) {
					mBatteryIcon.setImageResource(R.drawable.setup_battery_25);
				} else if (states.mBatteryVol < 75) {
					mBatteryIcon.setImageResource(R.drawable.setup_battery_50);
				} else if (states.mBatteryVol < 100) {
					mBatteryIcon.setImageResource(R.drawable.setup_battery_75);
				} else {
					mBatteryIcon.setImageResource(R.drawable.setup_battery_full);
				}
				mBatteryIcon.setVisibility(View.VISIBLE);
			}
		}

		// batter state
		switch (states.mBatteryState) {
		default:
		case CameraState.State_Battery_Unknown:
			mTextBatteryInfo.setText(R.string.unkown_text);
			break;
		case CameraState.State_Battery_Full:
			mTextBatteryInfo.setText(R.string.lable_battery_full);
			break;
		case CameraState.State_Battery_NotCharging:
			mTextBatteryInfo.setText(R.string.lable_battery_not_charging);
			break;
		case CameraState.State_Battery_Discharging:
			mTextBatteryInfo.setText(R.string.lable_battery_discharging);
			break;
		case CameraState.State_Battery_Charging:
			mTextBatteryInfo.setText(R.string.lable_battery_charging);
			break;
		}

		// storage state
		switch (states.mStorageState) {
		default:
		case CameraState.State_Storage_Unknown:
			mStorageUsage.setVisibility(View.INVISIBLE);
			mTextStorageInfo.setText(R.string.unkown_text);
			break;
		case CameraState.State_storage_noStorage:
			mStorageUsage.setVisibility(View.INVISIBLE);
			mTextStorageInfo.setText(R.string.lable_storage_notf);
			break;
		case CameraState.State_storage_loading:
			mStorageUsage.setVisibility(View.INVISIBLE);
			mTextStorageInfo.setText(R.string.lable_storage_loading);
			break;
		case CameraState.State_storage_ready:
			updateStorageInfo(res, states);
			break;
		case CameraState.State_storage_error:
			mStorageUsage.setVisibility(View.INVISIBLE);
			mTextStorageInfo.setText(R.string.lable_storage_error);
			break;
		case CameraState.State_storage_usbdisc:
			mStorageUsage.setVisibility(View.INVISIBLE);
			mTextStorageInfo.setText(R.string.lable_storage_usbdisc);
			break;
		}

		// resolution
		updateVideoResolution(res);

		// quality
		updateVideoQuality(res);

		// color mode
		updateColorMode(res);

		// auto record & loop record
		updateRecordMode();

		// mic mute
		updateMicState();

		// firmware version
		//mTextFirmwareVersion.setText(states.mFirmwareVersion);
		mTextFirmwareVersion.setText(states.versionString());
	}

	private void updateWifiStates() {
		WifiState states = Camera.getWifiStates(mCamera);
		switch (states.mWifiMode) {
		default:
		case WifiState.WIFI_Mode_Unknown:
			mTextWifiMode.setVisibility(View.GONE);
			break;
		case WifiState.WIFI_Mode_AP:
			mTextWifiMode.setText(R.string.lable_wifi_ap_mode);
			mTextWifiMode.setVisibility(View.VISIBLE);
			break;
		case WifiState.WIFI_Mode_Client:
			mTextWifiMode.setText(R.string.lable_wifi_client_mode);
			mTextWifiMode.setVisibility(View.VISIBLE);
		}
	}

	private void updateBtStates() {
		boolean bVisible = false;
		boolean bHighlight = false;
		int textId = 0;

		BtState btStates = getBtStates();
		switch (btStates.mBtSupport) {
		default:
		case BtState.BT_Support_Unknown:
			break;
		case BtState.BT_Support_No:
			bVisible = true;
			textId = R.string.lable_bt_support_no;
			break;
		case BtState.BT_Support_Yes:
			switch (btStates.mBtState) {
			default:
			case BtState.BT_State_Unknown:
				break;
			case BtState.BT_State_Disabled:
				bVisible = true;
				textId = R.string.lable_bt_disabled;
				break;
			case BtState.BT_State_Enabled:
				bVisible = true;
				bHighlight = true;
				textId = R.string.lable_bt_enabled;
				break;
			}
			break;
		}
		if (textId != 0) {
			mTextBtSupport.setText(textId);
		} else {
			mTextBtSupport.setText("");
		}
		if (bHighlight) {
			mTextBtSupport.setTextColor(getResources().getColor(R.color.pref_value_text));
		} else {
			mTextBtSupport.setTextColor(getResources().getColor(R.color.pref_hint_text));
		}
		mTextBtSupport.setVisibility(bVisible ? View.VISIBLE : View.GONE);
	}

	@Override
	protected void onStartActivity() {
		mCamera = getCameraFromIntent(null);
		if (mCamera == null) {
			noCamera();
			return;
		}
		mCamera.addCallback(mCameraCallback);
		getCameraClient().userCmd_GetSetup();
		updateCameraState();
		updateBtStates();
		updateWifiStates();
	}

	@Override
	protected void onStopActivity() {
		removeCamera();
	}

	private void removeCamera() {
		if (mCamera != null) {
			mCamera.removeCallback(mCameraCallback);
			mCamera = null;
		}
	}

	private void noCamera() {
		if (DEBUG) {
			Log.d(TAG, "camera not found or disconnected");
		}
		// TODO - if dialog/popupmenu exists
		finish();
	}

	@Override
	public void onBackPressed() {
		finish();
		Hachi.slideOutToRight(this, false);
	}

	private final Camera.Callback mCameraCallback = new Camera.CallbackImpl() {

		@Override
		public void onStateChanged(Camera camera) {
			if (camera == mCamera) {
				updateCameraState();
			}
		}

		@Override
		public void onBtStateChanged(Camera camera) {
			if (camera == mCamera) {
				updateBtStates();
			}
		}

		@Override
		public void onWifiStateChanged(Camera camera) {
			if (camera == mCamera) {
				updateWifiStates();
			}
		}

		@Override
		public void onDisconnected(Camera camera) {
			if (camera == mCamera) {
				removeCamera();
				noCamera();
			}
		}

	};

}
