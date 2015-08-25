package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.AnimationDrawable;
import android.net.NetworkInfo;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.transee.common.ViewAnimation;
import com.transee.viditcam.actions.GetCameraPassword;
import com.transee.viditcam.actions.GetServerAddress;
import com.transee.viditcam.actions.SelectWifiMode;
import com.transee.viditcam.app.AddCameraActivity;
import com.waylens.camera.CameraDiscovery;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.hardware.VdtCameraManager;
import com.waylens.hachi.hardware.WifiAdmin;
import com.waylens.hachi.hardware.WifiAdminManager;
import com.waylens.hachi.ui.adapters.CameraListRvAdapter;

import butterknife.Bind;
import butterknife.OnClick;

public class CameraListActivity extends BaseActivity {
    private static final String TAG = CameraListActivity.class.getSimpleName();

    static public final String PREF_CAMERA_LIST = "cameraList";
    static public final String PREF_AUTO_PREVIEW = "autoPreview";
    static public final String PREF_SERVER_IP = "serverIP";

    // intent to scan camera bar code
    private static final int REQUEST_SCAN_CAMERA = 0;
    private static final int REQUEST_SETUP_WIFI_AP = 1;


    private View mViewSearching;
    private ViewAnimation mSearchingAnim;

    private Handler mHandler;
    private Runnable mStartPreviewAction;
    private boolean mbAutoPreview;
    private boolean mbStartPreviewScheduled;
    private AnimationDrawable mWifiAnimation;

    private String mNewCameraSSID;
    private String mNewCameraPassword;

    private String mSetupWifi_SSID;
    private String mSetupWifi_hostString;

    private CameraListRvAdapter mCameraListAdapter;

    private VdtCameraManager mVdtCameraManager = VdtCameraManager.getManager();

    @Bind(R.id.rvCameraList)
    RecyclerView mRvCameraList;

    @Bind(R.id.btnWifi)
    TextView mTextWifi;

    @Bind(R.id.imageWifi)
    ImageView mWifiIcon;

    @OnClick(R.id.btnTestServer)
    public void onBtnTestServerClicked() {
        onClickTestServer();
    }

    @OnClick(R.id.linearLayout2)
    public void onBtnWifiBtnClicked() {
        onClickWifiButton();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        mHandler = new Handler();
        thisApp = (Hachi) getApplication();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_camera_list);

        mRvCameraList.setLayoutManager(new LinearLayoutManager(this));

        mCameraListAdapter = new CameraListRvAdapter(this);
        mRvCameraList.setAdapter(mCameraListAdapter);

        /*
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onClickListItem(position, true);
            }
        }); */


        WifiAdmin wifiAdmin = WifiAdminManager.getManager().getWifiAdmin();
        if (wifiAdmin != null) {
            updateWifiState(wifiAdmin);
            updateNetwork();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mCameraListAdapter.stopAndClear();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mbAutoPreview = getAutoPreview(this);
        mbStartPreviewScheduled = false;
        WifiAdmin wifiAdmin = WifiAdminManager.getManager().attachWifiAdmin(mWifiCallback);
        onScanWifiDone(wifiAdmin);
        tryConnectCamera();
        // try resume the action - switch wifi mode
        /*
        if (mSetupWifi_SSID != null) {
            Camera camera = mCameraListAdapter.findConnectedCamera(mSetupWifi_SSID, mSetupWifi_hostString);
            mSetupWifi_SSID = null;
            if (camera != null) {
                onClickWifiMode(camera);
            }
        }
        */
    }

    @Override
    protected void onStop() {
        super.onStop();
        setWifiIcon(0);
        stopDiscovery();
        WifiAdminManager.getManager().detachWifiAdmin(mWifiCallback, true);
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
        //mCameraListAdapter.filterScanResult(wifiAdmin.getScanResult());
        mVdtCameraManager.filterScanResult(wifiAdmin.getScanResult());
        mCameraListAdapter.notifyDataSetChanged();
    }

    private void startDiscovery() {
        CameraDiscovery.discoverCameras(this, new CameraDiscovery.Callback() {
            @Override
            public void onCameraFound(NsdServiceInfo cameraService) {
                String serviceName = cameraService.getServiceName();
                boolean bIsPcServer = serviceName.equals("Vidit Studio");
                final VdtCamera.ServiceInfo serviceInfo = new VdtCamera.ServiceInfo(
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

    private void updateNetwork() {
        startDiscovery();
    }

    private void setWifiIcon(int resId) {
        if (mWifiAnimation != null) {
            Logger.t(TAG).d("=== stop wifi animation ===");
            mWifiAnimation.stop();
            mWifiAnimation = null;
        }
        if (resId != 0) {
            mWifiIcon.setImageResource(resId);
        }
    }

    private void startWifiAnimation() {
        if (mWifiAnimation == null) {
            Logger.t(TAG).d("--- start wifi animation ---");
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


    private void onClickWifiButton() {
        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
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
            }
        };
        action.show();
    }

    private void tryStartPreview() {
        Logger.t(TAG).d("tryStartPreview");
        if (!isFinishing()) {
            /*
            if (mCameraListAdapter.getConnectedCameras() > 0) {
                onClickListItem(0, false);
            }
            */
        }
    }

    private void scheduleStartPreview() {
        Logger.t(TAG).d("scheduleStartPreview, scheduled: " + mbStartPreviewScheduled);
        if (mStartPreviewAction != null && !mbStartPreviewScheduled) {
            mbStartPreviewScheduled = true;
            mHandler.postDelayed(mStartPreviewAction, 1000);
        }
    }

    private void prepareStartPreview() {
        if (mStartPreviewAction == null && mbAutoPreview) {
            Logger.t(TAG).d("create startPreviewAction()");
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
            Logger.t(TAG).d("cancelAutoAction");
            mHandler.removeCallbacksAndMessages(mStartPreviewAction);
            mStartPreviewAction = null;
            mbStartPreviewScheduled = false;
        }
    }

    /*
    private void onClickListItem(int position, boolean bAskPassword) {
        Camera camera = mCameraListAdapter.getCamera(position);
        if (camera != null) {
            if (camera.isPcServer()) {
                BrowseCameraActivity.launch(this, camera.isPcServer(), camera.getSSID(), camera.getHostString());
            } else {
                startCameraActivity(camera, CameraControlActivity.class);
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
    */

    private void askPassword(String ssid, String password, boolean bChangePassword) {
        GetCameraPassword action = new GetCameraPassword(this, ssid, password, bChangePassword) {
            @Override
            public void onGetPasswordOK(GetCameraPassword action, String ssid, String password) {
                // save password
                VdtCameraManager.getManager().setPassword(ssid, password);
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
            WifiAdmin wifiAdmin = WifiAdminManager.getManager().getWifiAdmin();
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
        Logger.t(TAG).d("camera added: " + wifiName + ", " + wifiPassword);
        // onActivityResult() is before onStart(),
        // so save the info and do connect in onStart();
        mNewCameraSSID = wifiName;
        mNewCameraPassword = wifiPassword;
        // save password
        VdtCameraManager.getManager().setPassword(mNewCameraSSID, mNewCameraPassword);
    }

    private void onWifiSetupDone(String ssid, String hostString) {
        Logger.t(TAG).d("wifi setup done: " + ssid + ", " + hostString);
        // onActivityResult() is before onStart(),
        mSetupWifi_SSID = ssid;
        mSetupWifi_hostString = hostString;
    }

    private void browseCameraVideo(VdtCamera vdtCamera) {
        if (vdtCamera != null) {
            vdtCamera.getClient().cmd_CAM_WantIdle();
            //CameraVideoActivity.launch(this, camera.isPcServer(), camera.getSSID(), camera
            //    .getHostString());
            BrowseCameraActivity.launch(this, vdtCamera);
        }
    }

    private void onClickFolder(View view, int position) {
        /*
        Camera camera = mCameraListAdapter.getCamera(position);
        if (camera != null) {
            camera = mCameraListAdapter.findConnectedCamera(camera.getSSID(), camera.getHostString());
            browseCameraVideo(camera);
        }
        */
    }


    /*

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
    } */

    private void onClickWifiMode(VdtCamera vdtCamera) {
        SelectWifiMode action = new SelectWifiMode(this, vdtCamera) {
            @Override
            protected void onChangeWifiMode(VdtCamera camera, int newMode) {
                /*
                camera = mCameraListAdapter.isCameraConnected(camera);
                if (camera != null) {
                    camera.getClient().cmd_Network_ConnectHost(newMode, null);
                    if (mCameraListAdapter.removeConnectedCamera(camera)) {
                        camera.getClient().userCmd_ExitThread();
                    }
                }*/
            }

            @Override
            protected void onSetupWifiAP(VdtCamera camera) {
                /*
                camera = mCameraListAdapter.isCameraConnected(camera);
                if (camera != null) {
                    CameraListActivity.this.startCameraActivity(camera, CameraWifiSetupActivity.class,
                        REQUEST_SETUP_WIFI_AP);
                } */
            }
        };
        action.show();
    }

    /*
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
    } */

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


    private void onServiceResolved(VdtCamera.ServiceInfo serviceInfo) {
        WifiAdmin wifiAdmin = WifiAdminManager.getManager().getWifiAdmin();
        serviceInfo.ssid = wifiAdmin == null ? null : wifiAdmin.getCurrSSID();
        connectCamera(serviceInfo);
    }

    public void connectCamera(VdtCamera.ServiceInfo serviceInfo) {
        mVdtCameraManager.connectCamera(serviceInfo);
    }

    /*
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
    } */


    /*
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

    } */

    final WifiAdminManager.WifiCallback mWifiCallback = new WifiAdminManager.WifiCallback() {

        @Override
        public void networkStateChanged(WifiAdmin wifiAdmin) {
            Logger.t(TAG).d("networkStateChanged");
            updateWifiState(wifiAdmin);
            updateNetwork();
        }

        @Override
        public void wifiScanResult(WifiAdmin wifiAdmin) {
            //Logger.t(TAG).d("wifiScanResult");
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
