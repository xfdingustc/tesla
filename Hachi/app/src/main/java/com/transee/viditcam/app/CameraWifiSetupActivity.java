package com.transee.viditcam.app;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.transee.ccam.Camera;
import com.transee.ccam.CameraClient;
import com.waylens.hachi.hardware.WifiAdmin;
import com.waylens.hachi.R;
import com.transee.viditcam.actions.GetWifiAP;
import com.transee.viditcam.actions.RemoveWifiAP;
import com.transee.viditcam.actions.SelectWifiAp;
import com.waylens.hachi.app.Hachi;

import java.util.List;

public class CameraWifiSetupActivity extends BaseActivity {

	static final boolean DEBUG = false;
	static final String TAG = "CameraWifiSetupActivity";

	private boolean mbRequested;
	private Camera mCamera;
	private ListView mListView;
	private WifiListAdapter mListAdapter;
	private Button mAddNetworkButton;

	private CameraClient getCameraClient() {
		return (CameraClient)mCamera.getClient();
	}

	@Override
	protected void requestContentView() {
		setContentView(R.layout.activity_camera_wifi_setup);
	}

	@Override
	protected void onCreateActivity(Bundle savedInstanceState) {
		mListView = (ListView)findViewById(R.id.listView1);
		mListAdapter = new WifiListAdapter(this, mListView) {
			@Override
			public List<ScanResult> getScanResult() {
				WifiAdmin wifiAdmin = thisApp.getWifiAdmin();
				return wifiAdmin == null ? null : wifiAdmin.getScanResult();
			}

			@Override
			public void onClickDelete(int position) {
				onClickDeleteWifiItem(position);
			}
		};
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mListAdapter.toggleItem(view, position);
			}
		});
		mAddNetworkButton = (Button)findViewById(R.id.btnAddNetwork);
		mAddNetworkButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getNetwork(null);
			}
		});
	}

	@Override
	protected void onStartActivity() {
		mCamera = getCameraFromIntent(null);
		if (mCamera == null) {
			noCamera();
			return;
		}
		mbRequested = isActivityRequested();

		mCamera.addCallback(mCameraCallback);
		getCameraClient().cmd_Network_GetHostNum();

		mListAdapter.clear();
		WifiAdmin wifiAdmin = thisApp.attachWifiAdmin(mWifiCallback);
		onScanWifiDone(wifiAdmin);
	}

	@Override
	protected void onStopActivity() {
		thisApp.detachWifiAdmin(mWifiCallback, false);
		removeCamera();
	}

	private void onClickDeleteWifiItem(int position) {
		String ssid = mListAdapter.getSSID(position);
		if (ssid != null) {
			RemoveWifiAP action = new RemoveWifiAP(this, ssid) {
				@Override
				public void onRemoveWifiAPConfirmed(String ssid) {
					removeWifiAP(ssid);
				}
			};
			action.show();
		}
	}

	private void removeWifiAP(String ssid) {
		if (mCamera != null) {
			mListAdapter.removeSSID(ssid);
			getCameraClient().cmd_Network_RmvHost(ssid);
		}
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
		performFinish();
	}

	private void getNetwork(String ssid) {
		GetWifiAP action = new GetWifiAP(this, ssid) {
			@Override
			public void onShowWifiList() {
				CameraWifiSetupActivity.this.onShowWifiList();
			}

			@Override
			public void onGetWifiAP(String ssid, String password) {
				if (mCamera != null) {
					addWifiAP(ssid, password);
				}
			}
		};
		action.show();
	}

	private void addWifiAP(String ssid, String password) {
		if (ssid != null && ssid.length() > 0 && !mListAdapter.exists(ssid)) {
			mListAdapter.addSSID(ssid);
			getCameraClient().cmd_Network_AddHost(ssid, password);
		}
	}

	private void onShowWifiList() {
		SelectWifiAp action = new SelectWifiAp(this, thisApp) {
			@Override
			protected void onSelectWifi(String ssid) {
				getNetwork(ssid);
			}
		};
		action.show();
	}

	@Override
	public void onBackPressed() {
		performFinish();
	}

	private void performFinish() {
		if (mbRequested) {
			Bundle b = new Bundle();
			b.putString("ssid", mCamera.getSSID());
			b.putString("hostString", mCamera.getHostString());
			Intent intent = new Intent();
			intent.putExtras(b);
			setResult(RESULT_OK, intent);
		}
		finish();
	}

	private void onScanWifiDone(WifiAdmin wifiAdmin) {
		mListAdapter.filterScanList(wifiAdmin.getScanResult());
	}

	private void onHostSSIDFetched(String ssid) {
		mListAdapter.addSSID(ssid);
	}

	private final Camera.Callback mCameraCallback = new Camera.CallbackImpl() {

		@Override
		public void onDisconnected(Camera camera) {
			if (camera == mCamera) {
				removeCamera();
				noCamera();
			}
		}

		@Override
		public void onHostSSIDFetched(Camera camera, String ssid) {
			if (camera == mCamera) {
				CameraWifiSetupActivity.this.onHostSSIDFetched(ssid);
			}
		}

	};

	private final Hachi.WifiCallback mWifiCallback = new Hachi.WifiCallback() {

		@Override
		public void networkStateChanged(WifiAdmin wifiAdmin) {
		}

		@Override
		public void wifiScanResult(WifiAdmin wifiAdmin) {
			onScanWifiDone(wifiAdmin);
		}

		@Override
		public void onConnectError(WifiAdmin wifiAdmin) {
		}

		@Override
		public void onConnectDone(WifiAdmin wifiAdmin) {
		}

	};
}
