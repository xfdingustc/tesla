package com.transee.viditcam.app;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.CameraClient;
import com.transee.viditcam.actions.GetWifiAP;
import com.transee.viditcam.actions.RemoveWifiAP;
import com.transee.viditcam.actions.SelectWifiAp;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.WifiAdmin;
import com.waylens.hachi.hardware.WifiAdminManager;

import java.util.List;

public class CameraWifiSetupActivity extends BaseActivity {

    static final boolean DEBUG = false;
    static final String TAG = "CameraWifiSetupActivity";

    private boolean mbRequested;
    private VdtCamera mVdtCamera;
    private ListView mListView;
    private WifiListAdapter mListAdapter;
    private Button mAddNetworkButton;


    @Override
    protected void requestContentView() {
        setContentView(R.layout.activity_camera_wifi_setup);
    }

    @Override
    protected void onCreateActivity(Bundle savedInstanceState) {
        mListView = (ListView) findViewById(R.id.listView1);
        mListAdapter = new WifiListAdapter(this, mListView) {
            @Override
            public List<ScanResult> getScanResult() {
                WifiAdmin wifiAdmin = WifiAdminManager.getManager().getWifiAdmin();
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
        mAddNetworkButton = (Button) findViewById(R.id.btnAddNetwork);
        mAddNetworkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getNetwork(null);
            }
        });
    }

    @Override
    protected void onStartActivity() {
        mVdtCamera = getCameraFromIntent(null);
        if (mVdtCamera == null) {
            noCamera();
            return;
        }
        mbRequested = isActivityRequested();

        //mVdtCamera.addCallback(mCameraCallback);
        mVdtCamera.getNetworkHostHum();

        mListAdapter.clear();
        WifiAdmin wifiAdmin = WifiAdminManager.getManager().attachWifiAdmin(mWifiCallback);
        onScanWifiDone(wifiAdmin);
    }

    @Override
    protected void onStopActivity() {
        WifiAdminManager.getManager().detachWifiAdmin(mWifiCallback, false);
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
        if (mVdtCamera != null) {
            mListAdapter.removeSSID(ssid);
            mVdtCamera.setNetworkRmvHost(ssid);
        }
    }

    private void removeCamera() {
        if (mVdtCamera != null) {
            //mVdtCamera.removeCallback(mCameraCallback);
            mVdtCamera = null;
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
                if (mVdtCamera != null) {
                    addWifiAP(ssid, password);
                }
            }
        };
        action.show();
    }

    private void addWifiAP(String ssid, String password) {
        if (ssid != null && ssid.length() > 0 && !mListAdapter.exists(ssid)) {
            mListAdapter.addSSID(ssid);
            mVdtCamera.setAddNetworkHost(ssid, password);
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
            b.putString("ssid", mVdtCamera.getSSID());
            b.putString("hostString", mVdtCamera.getHostString());
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

    /*

    private final VdtCamera.Callback mCameraCallback = new VdtCamera.Callback() {

        @Override
        public void onConnected(VdtCamera vdtCamera) {

        }

        @Override
        public void onDisconnected(VdtCamera vdtCamera) {
            if (vdtCamera == mVdtCamera) {
                removeCamera();
                noCamera();
            }
        }

        @Override
        public void onStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onBtStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onGpsStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onWifiStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onStartRecordError(VdtCamera vdtCamera, int error) {

        }

        @Override
        public void onHostSSIDFetched(VdtCamera vdtCamera, String ssid) {
            if (vdtCamera == mVdtCamera) {
                CameraWifiSetupActivity.this.onHostSSIDFetched(ssid);
            }
        }

        @Override
        public void onScanBtDone(VdtCamera vdtCamera) {

        }

        @Override
        public void onBtDevInfo(VdtCamera vdtCamera, int type, String mac, String name) {

        }

        @Override
        public void onStillCaptureStarted(VdtCamera vdtCamera, boolean bOneShot) {

        }

        @Override
        public void onStillPictureInfo(VdtCamera vdtCamera, boolean bCapturing, int numPictures, int burstTicks) {

        }

        @Override
        public void onStillCaptureDone(VdtCamera vdtCamera) {

        }

    }; */

    private final WifiAdminManager.WifiCallback mWifiCallback = new WifiAdminManager.WifiCallback() {

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
