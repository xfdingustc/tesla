package com.transee.viditcam.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.AnimationDrawable;
import android.net.NetworkInfo;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.transee.ccam.Camera;
import com.transee.ccam.CameraManager;
import com.transee.common.ViewAnimation;
import com.transee.common.ViewAnimation.Animation;
import com.transee.common.WifiAdmin;
import com.transee.viditcam.actions.CameraOperations;
import com.transee.viditcam.actions.DialogBuilder;
import com.transee.viditcam.actions.GetCameraPassword;
import com.transee.viditcam.actions.GetServerAddress;
import com.transee.viditcam.actions.SelectWifiMode;
import com.waylens.camera.CameraDiscovery;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;

public class CameraListActivity extends BaseActivity {

    static final boolean DEBUG = false;
    static final String TAG = "CameraListActivity";

    static public final String PREF_CAMERA_LIST = "cameraList";
    static public final String PREF_AUTO_PREVIEW = "autoPreview";
    static public final String PREF_SERVER_IP = "serverIP";

    // intent to scan camera bar code
    private static final int REQUEST_SCAN_CAMERA = 0;
    private static final int REQUEST_SETUP_WIFI_AP = 1;
    private ListView mListView;
    private CameraListAdapter mCameraListAdapter;

    private View mViewSearching;
    private ViewAnimation mSearchingAnim;

    private TextView mTextWifi;
    private ImageView mWifiIcon;

    private Handler mHandler;
    private Runnable mStartPreviewAction;
    private boolean mbAutoPreview;
    private boolean mbStartPreviewScheduled;
    private AnimationDrawable mWifiAnimation;

    private String mNewCameraSSID;
    private String mNewCameraPassword;

    private String mSetupWifi_SSID;
    private String mSetupWifi_hostString;

    @Override
    protected void requestContentView() {
        setContentView(R.layout.activity_camera_list);
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onCreateActivity(Bundle savedInstanceState) {

        mHandler = new Handler();

        mListView = (ListView) findViewById(R.id.listView1);
        View emptyView = findViewById(R.id.emptyCameraList);
        mViewSearching = emptyView; // emptyView.findViewById(R.id.textView1);
        mListView.setEmptyView(emptyView);

        mCameraListAdapter = new MyCameraListAdapter(mListView);
        mListView.setAdapter(mCameraListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onClickListItem(position, true);
            }
        });
    }

    @Override
    protected void onDestroyActivity() {
        mCameraListAdapter.stopAndClear();
    }

    @Override
    protected void onStartActivity() {
        mbAutoPreview = getAutoPreview(this);
        mbStartPreviewScheduled = false;
        WifiAdmin wifiAdmin = thisApp.attachWifiAdmin(mWifiCallback);
        onScanWifiDone(wifiAdmin);
        tryConnectCamera();
        // try resume the action - switch wifi mode
        if (mSetupWifi_SSID != null) {
            Camera camera = mCameraListAdapter.findConnectedCamera(mSetupWifi_SSID, mSetupWifi_hostString);
            mSetupWifi_SSID = null;
            if (camera != null) {
                onClickWifiMode(camera);
            }
        }
    }

    @Override
    protected void onStopActivity() {
        setWifiIcon(0);
        stopDiscovery();
        thisApp.detachWifiAdmin(mWifiCallback, true);
    }

    @Override
    protected void onInitUI() {
        Button button = (Button) findViewById(R.id.btnDownloadedVideos);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickDownloadedVideos();
            }
        });

        button = (Button) findViewById(R.id.btnTestServer);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickTestServer();
                }
            });
        }

        mTextWifi = (TextView) findViewById(R.id.btnWifi);
        mWifiIcon = (ImageView) findViewById(R.id.imageWifi);

        View tmp = findViewById(R.id.linearLayout2);
        tmp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickWifiButton();
            }
        });
    }

    @Override
    protected void onSetupUI() {
        WifiAdmin wifiAdmin = thisApp.getWifiAdmin();
        if (wifiAdmin != null) {
            updateWifiState(wifiAdmin);
            updateNetwork(wifiAdmin);
        }
    }

    static public boolean getAutoPreview(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_CAMERA_LIST, Context.MODE_PRIVATE);
        return pref.getBoolean(PREF_AUTO_PREVIEW, false);
    }

    static public void setAutoPreview(Context context, boolean bAutoPreview) {
        SharedPreferences pref = context.getSharedPreferences(PREF_CAMERA_LIST, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putBoolean(PREF_AUTO_PREVIEW, bAutoPreview);
        editor.commit();
    }

    private void onScanWifiDone(WifiAdmin wifiAdmin) {
        mCameraListAdapter.filterScanResult(wifiAdmin.getScanResult());
    }

    private void startDiscovery() {
        if (CameraDiscovery.isStarted()) {
            return;
        }
        CameraDiscovery.discoverCameras(this, new CameraDiscovery.Callback() {
            @Override
            public void onCameraFound(NsdServiceInfo cameraService) {
                String serviceName = cameraService.getServiceName();
                boolean bIsPcServer = serviceName.equals("Vidit Studio");
                final Camera.ServiceInfo serviceInfo = new Camera.ServiceInfo(
                        cameraService.getHost(),
                        cameraService.getPort(),
                        "", serviceName, bIsPcServer);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onServiceResolved(serviceInfo);
                    }
                });

            }

            @Override
            public void onError(int errorCode) {

            }
        });
    }

    private void stopDiscovery() {
        if (mSearchingAnim != null) {
            mSearchingAnim.stopAnimation();
            mSearchingAnim = null;
        }
        CameraDiscovery.stopDiscovery();
    }

    private void updateWifiState(WifiAdmin wifiAdmin) {
        if (wifiAdmin.isConnecting()) {
            startWifiAnimation();
            String fmt = getResources().getString(R.string.lable_connecting_to);
            String title = String.format(fmt, wifiAdmin.getTargetSSID());
            mTextWifi.setText(title);
        } else {
            NetworkInfo info = wifiAdmin.getNetworkInfo();
            switch (info.getState()) {
                default:
                case DISCONNECTED:
                    setWifiIcon(R.drawable.btn_wifi_off);
                    mTextWifi.setText(R.string.btn_wlan_off);
                    break;
                case CONNECTING:
                    startWifiAnimation();
                    setWifiInfoText(wifiAdmin, R.string.lable_connecting_to);
                    break;
                case DISCONNECTING:
                    startWifiAnimation();
                    setWifiInfoText(wifiAdmin, R.string.lable_disconnecting);
                    break;
                case CONNECTED:
                    setWifiIcon(R.drawable.btn_wifi_on);
                    String wifiName = wifiAdmin.getCurrSSID();
                    mTextWifi.setText(wifiName);
                    break;
            }
        }
    }

    private void setWifiInfoText(WifiAdmin wifiAdmin, int resId) {
        String fmt = getResources().getString(resId);
        String title = String.format(fmt, wifiAdmin.getCurrSSID());
        mTextWifi.setText(title);
    }

    private void updateNetwork(WifiAdmin wifiAdmin) {
        startDiscovery();
    }

    private void setWifiIcon(int resId) {
        if (mWifiAnimation != null) {
            if (DEBUG) {
                Log.d(TAG, "=== stop wifi animation ===");
            }
            mWifiAnimation.stop();
            mWifiAnimation = null;
        }
        if (resId != 0) {
            mWifiIcon.setImageResource(resId);
        }
    }

    private void startWifiAnimation() {
        if (mWifiAnimation == null) {
            if (DEBUG) {
                Log.d(TAG, "--- start wifi animation ---");
            }
            mWifiAnimation = (AnimationDrawable) getResources().getDrawable(R.drawable.wifi);
            mWifiAnimation.setBounds(0, 0, mWifiAnimation.getIntrinsicWidth(), mWifiAnimation.getIntrinsicHeight());
            mWifiIcon.setImageDrawable(mWifiAnimation);
            mWifiAnimation.start();
        }
    }

    private void beginScanCameraCode() {
        Intent intent = new Intent(this, AddCameraActivity.class);
        startActivityForResult(intent, REQUEST_SCAN_CAMERA);
    }

    private void onClipAppSetupButton() {
        Intent intent = new Intent(this, AppSetupActivity.class);
        startActivity(intent);
        Hachi.slideInFromRight(this, false);
    }

    private void onClickWifiButton() {
        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
    }

    private void onClickDownloadedVideos() {
        startLocalActivity(CameraVideoActivity.class);
        Hachi.slideInFromRight(this, false);
    }

    private String getSavedServerAddress() {
        SharedPreferences pref = getSharedPreferences(PREF_CAMERA_LIST, Context.MODE_PRIVATE);
        return pref.getString(PREF_SERVER_IP, "");
    }

    private void saveServerAddress(String serverAddress) {
        SharedPreferences pref = getSharedPreferences(PREF_CAMERA_LIST, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putString(PREF_SERVER_IP, serverAddress);
        editor.commit();
    }

    private void onClickTestServer() {
        GetServerAddress action = new GetServerAddress(this, getSavedServerAddress()) {
            @Override
            public void onGetServerAddress(String address) {
                saveServerAddress(address);
                //startServerActivity(CameraVideoActivity.class, address);
                //Hachi.slideInFromRight(CameraListActivity.this, false);
            }
        };
        action.show();
    }

    private void tryStartPreview() {
        if (DEBUG) {
            Log.d(TAG, "tryStartPreview");
        }
        if (!isFinishing()) {
            if (mCameraListAdapter.getConnectedCameras() > 0) {
                onClickListItem(0, false);
            }
        }
    }

    private void scheduleStartPreview() {
        if (DEBUG) {
            Log.d(TAG, "scheduleStartPreview, scheduled: " + mbStartPreviewScheduled);
        }
        if (mStartPreviewAction != null && !mbStartPreviewScheduled) {
            mbStartPreviewScheduled = true;
            mHandler.postDelayed(mStartPreviewAction, 1000);
        }
    }

    private void prepareStartPreview() {
        if (mStartPreviewAction == null && mbAutoPreview) {
            if (DEBUG) {
                Log.d(TAG, "create startPreviewAction()");
            }
            mStartPreviewAction = new Runnable() {
                @Override
                public void run() {
                    if (mStartPreviewAction != null) {
                        mStartPreviewAction = null;
                        tryStartPreview();
                    }
                }
            };
        }
    }

    private void cancelStartPreview() {
        if (mStartPreviewAction != null) {
            if (DEBUG) {
                Log.d(TAG, "cancelAutoAction");
            }
            mHandler.removeCallbacksAndMessages(mStartPreviewAction);
            mStartPreviewAction = null;
            mbStartPreviewScheduled = false;
        }
    }

    private void onClickListItem(int position, boolean bAskPassword) {
        Camera camera = mCameraListAdapter.getCamera(position);
        if (camera != null) {
            if (camera.isPcServer()) {
                startCameraActivity(camera, CameraVideoActivity.class);
                Hachi.slideInFromRight(this, true);
            } else {
                startCameraActivity(camera, CameraControlActivity.class);
                Hachi.slideInFromRight(this, true);
            }
        } else {
            CameraManager.WifiItem wifiItem = mCameraListAdapter.getWifiItem(position);
            if (wifiItem != null) {
                if (wifiItem.mPassword != null) {
                    // connect it
                    CameraListActivity.this.startConnectCamera(wifiItem.mSSID, wifiItem.mPassword);
                } else {
                    // ask password
                    if (bAskPassword) {
                        askPassword(wifiItem.mSSID, null, false);
                    }
                }
            }
        }
    }

    private void askPassword(String ssid, String password, boolean bChangePassword) {
        GetCameraPassword action = new GetCameraPassword(this, ssid, password, bChangePassword) {
            @Override
            public void onGetPasswordOK(GetCameraPassword action, String ssid, String password) {
                // save password
                getCameraManager().setPassword(ssid, password);
                if (!action.mbChangePassword) {
                    CameraListActivity.this.startConnectCamera(ssid, password);
                }
            }

            @Override
            public void onClickScanCode(GetCameraPassword action, String ssid) {
                if (action.mbChangePassword) {
                    // TODO - change scanner
                }
                CameraListActivity.this.beginScanCameraCode();
            }
        };
        action.show();
    }

    private void tryConnectCamera() {
        if (mNewCameraSSID != null) {
            // TODO : if ssid does not exist, popup dialog
            WifiAdmin wifiAdmin = thisApp.getWifiAdmin();
            if (wifiAdmin != null) {
                wifiAdmin.connectTo(mNewCameraSSID, mNewCameraPassword);
                mNewCameraSSID = null;
            }
        }
    }

    private void startConnectCamera(String ssid, String password) {
        mNewCameraSSID = ssid;
        mNewCameraPassword = password;
        tryConnectCamera();
    }

    private void onCameraScaned(String wifiName, String wifiPassword) {
        if (DEBUG) {
            Log.d(TAG, "camera added: " + wifiName + ", " + wifiPassword);
        }
        // onActivityResult() is before onStart(),
        // so save the info and do connect in onStart();
        mNewCameraSSID = wifiName;
        mNewCameraPassword = wifiPassword;
        // save password
        getCameraManager().setPassword(mNewCameraSSID, mNewCameraPassword);
    }

    private void onWifiSetupDone(String ssid, String hostString) {
        if (DEBUG) {
            Log.d(TAG, "wifi setup done: " + ssid + ", " + hostString);
        }
        // onActivityResult() is before onStart(),
        mSetupWifi_SSID = ssid;
        mSetupWifi_hostString = hostString;
    }

    private void browseCameraVideo(Camera camera) {
        if (camera != null) {
            camera.getClient().cmd_CAM_WantIdle();
            startCameraActivity(camera, CameraVideoActivity.class);
            Hachi.slideInFromRight(CameraListActivity.this, false);
        }
    }

    private void onClickFolder(View view, int position) {
        Camera camera = mCameraListAdapter.getCamera(position);
        if (camera != null) {
            camera = mCameraListAdapter.findConnectedCamera(camera.getSSID(), camera.getHostString());
            browseCameraVideo(camera);
        }
    }

    private void onClickPowerOff_Reboot(Camera camera, final boolean bReboot) {
        final String ssid = camera.getSSID();
        final String hostString = camera.getHostString();
        final DialogBuilder builder = new DialogBuilder(this) {
            @Override
            protected void onClickPositiveButton() {
                Camera camera = mCameraListAdapter.findConnectedCamera(ssid, hostString);
                if (camera != null) {
                    dismiss();
                    if (bReboot) {
                        camera.getClient().cmd_Cam_Reboot();
                    } else {
                        camera.getClient().cmd_Cam_PowerOff();
                    }
                }
            }
        };
        builder.setTitle(getCameraName(Camera.getCameraStates(camera)));
        builder.setMsg(bReboot ? R.string.msg_confirm_reboot : R.string.msg_confirm_poweroff);
        builder.setButtons(DialogBuilder.DLG_OK_CANCEL);
        builder.show();
    }

    private void onClickWifiMode(Camera camera) {
        SelectWifiMode action = new SelectWifiMode(this, camera) {
            @Override
            protected void onChangeWifiMode(Camera camera, int newMode) {
                camera = mCameraListAdapter.isCameraConnected(camera);
                if (camera != null) {
                    camera.getClient().cmd_Network_ConnectHost(newMode, null);
                    if (mCameraListAdapter.removeConnectedCamera(camera)) {
                        camera.getClient().userCmd_ExitThread();
                    }
                }
            }

            @Override
            protected void onSetupWifiAP(Camera camera) {
                camera = mCameraListAdapter.isCameraConnected(camera);
                if (camera != null) {
                    CameraListActivity.this.startCameraActivity(camera, CameraWifiSetupActivity.class,
                            REQUEST_SETUP_WIFI_AP);
                    Hachi.slideInFromRight(CameraListActivity.this, true);
                }
            }
        };
        action.show();
    }

    private void onClickDropDown(View view, int position) {
        String title;
        String ssid;
        String hostString;
        Camera camera = mCameraListAdapter.getCamera(position);

        if (camera != null) {
            title = Camera.getCameraStates(camera).mCameraName;
            ssid = camera.getSSID();
            hostString = camera.getHostString();
        } else {
            CameraManager.WifiItem item = mCameraListAdapter.getWifiItem(position);
            if (item != null) {
                title = item.mSSID;
                ssid = item.mSSID;
                hostString = null;
            } else {
                return;
            }
        }

        CameraOperations action = new CameraOperations(this, camera, title, ssid, hostString) {

            @Override
            protected void onClickBrowseVideo(CameraOperations action) {
                if (action.mHostString != null) {
                    Camera camera = mCameraListAdapter.findConnectedCamera(action.mSSID, action.mHostString);
                    browseCameraVideo(camera);
                }
            }

            @Override
            protected void onClickSetup(CameraOperations action) {
                if (action.mHostString != null) {
                    Camera camera = mCameraListAdapter.findConnectedCamera(action.mSSID, action.mHostString);
                    if (camera != null) {
                        startCameraActivity(camera, CameraSetupActivity.class);
                        Hachi.slideInFromRight(CameraListActivity.this, false);
                    }
                }
            }

            @Override
            protected void onClickChangePassword(CameraOperations action) {
                String password = getCameraManager().getPassword(action.mSSID);
                askPassword(action.mSSID, password, true);
            }

            @Override
            protected void onClickPowerOff(CameraOperations action) {
                if (action.mHostString != null) {
                    Camera camera = mCameraListAdapter.findConnectedCamera(action.mSSID, action.mHostString);
                    if (camera != null) {
                        CameraListActivity.this.onClickPowerOff_Reboot(camera, false);
                    }
                }
            }

            @Override
            protected void onClickReboot(CameraOperations action) {
                if (action.mHostString != null) {
                    Camera camera = mCameraListAdapter.findConnectedCamera(action.mSSID, action.mHostString);
                    if (camera != null) {
                        CameraListActivity.this.onClickPowerOff_Reboot(camera, true);
                    }
                }
            }

            @Override
            protected void onClickWifiMode(CameraOperations action) {
                if (action.mHostString != null) {
                    Camera camera = mCameraListAdapter.findConnectedCamera(action.mSSID, action.mHostString);
                    if (camera != null) {
                        CameraListActivity.this.onClickWifiMode(camera);
                    }
                }
            }

        };

        action.show(view);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null)
            return;
        if (requestCode == REQUEST_SCAN_CAMERA) {
            if (resultCode == RESULT_OK) {
                Bundle b = data.getExtras();
                String wifiName = b.getString("wifiName");
                String wifiPassword = b.getString("wifiPassword");
                onCameraScaned(wifiName, wifiPassword);
            }
            return;
        }
        if (requestCode == REQUEST_SETUP_WIFI_AP) {
            if (resultCode == RESULT_OK) {
                Bundle b = data.getExtras();
                String ssid = b.getString("ssid");
                String hostString = b.getString("hostString");
                onWifiSetupDone(ssid, hostString);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        cancelStartPreview();
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        cancelStartPreview();
        return super.dispatchKeyEvent(ev);
    }

    private final CameraManager getCameraManager() {
        return ((Hachi) getApplication()).getCameraManager();
    }

    private void onServiceResolved(Camera.ServiceInfo serviceInfo) {
        WifiAdmin wifiAdmin = thisApp.getWifiAdmin();
        String ssid = wifiAdmin == null ? null : wifiAdmin.getCurrSSID();
        serviceInfo.ssid = ssid;
        mCameraListAdapter.connectCamera(serviceInfo);
    }

    private void onRescan() {
        if (mCameraListAdapter.getCount() == 0) {
            if (mSearchingAnim != null) {
                mSearchingAnim.stopAnimation();
            }
            mSearchingAnim = new ViewAnimation() {
                @Override
                protected void onAnimationDone(ViewAnimation animation, boolean bReverse) {
                    if (!bReverse) {
                        animation.startAnimation(200, 10, true);
                    }
                }
            };
            Animation anim = ViewAnimation.createAlphaAnimation(1.0f, 0.1f);
            mSearchingAnim.addAnimation(mViewSearching, anim);
            mSearchingAnim.startAnimation(500, 10, false);
        }
    }

    class MyCameraListAdapter extends CameraListAdapter {

        public MyCameraListAdapter(ListView listView) {
            super(CameraListActivity.this, getCameraManager(), listView);
        }

        @Override
        public void onCameraConnected(Camera camera) {
            if (mCameraListAdapter.getConnectedCameras() == 1) {
                camera = mCameraListAdapter.getCamera(0);
                if (!camera.isPcServer()) {
                    prepareStartPreview();
                    scheduleStartPreview();
                }
            }
        }

        @Override
        public void onCameraDisconnected(Camera camera) {
        }

        @Override
        public void onClickDropDown(View view, int position) {
            CameraListActivity.this.onClickDropDown(view, position);
        }

        @Override
        public void onClickFolder(View view, int position) {
            CameraListActivity.this.onClickFolder(view, position);
        }

    }

    final Hachi.WifiCallback mWifiCallback = new Hachi.WifiCallback() {

        @Override
        public void networkStateChanged(WifiAdmin wifiAdmin) {
            if (DEBUG) {
                Log.d(TAG, "networkStateChanged");
            }
            updateWifiState(wifiAdmin);
            updateNetwork(wifiAdmin);
        }

        @Override
        public void wifiScanResult(WifiAdmin wifiAdmin) {
            if (DEBUG) {
                Log.d(TAG, "wifiScanResult");
            }
            onScanWifiDone(wifiAdmin);
        }

        @Override
        public void onConnectError(WifiAdmin wifiAdmin) {
            // TODO
        }

        @Override
        public void onConnectDone(WifiAdmin wifiAdmin) {
            // TODO
        }

    };
}
