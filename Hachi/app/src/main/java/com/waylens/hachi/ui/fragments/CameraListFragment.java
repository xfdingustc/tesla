package com.waylens.hachi.ui.fragments;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.transee.viditcam.app.CameraSetupActivity;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.DeviceScanner;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.ui.activities.CameraControlActivity2;
import com.waylens.hachi.ui.activities.TabSwitchable;
import com.waylens.hachi.ui.adapters.CameraListRvAdapter;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * List available Cameras
 * Created by Richard on 9/9/15.
 */
public class CameraListFragment extends BaseFragment implements CameraListRvAdapter.OnCameraActionListener {
    private static final String TAG = CameraListFragment.class.getSimpleName();

    @Bind(R.id.wifi_status)
    TextView mWifiStatusView;

    @Bind(R.id.wifi_icon)
    ImageView mWifiIcon;

    @Bind(R.id.rvCameraList)
    RecyclerView mCameraListView;

    private AnimationDrawable mWifiAnimation;
    private VdtCameraManager mVdtCameraManager;
    private CameraListRvAdapter mCameraListAdapter;

    private DeviceScanner mDeviceScanner = null;

    Handler mHandler;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mVdtCameraManager = VdtCameraManager.getManager();
        mCameraListAdapter = new CameraListRvAdapter(getActivity(), mVdtCameraManager, this);
        mVdtCameraManager.addCallback(new VdtCameraManager.Callback() {
            @Override
            public void onCameraConnecting(VdtCamera vdtCamera) {

            }

            @Override
            public void onCameraConnected(VdtCamera vdtCamera) {
                Logger.t(TAG).d("on Camera Connected");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraListAdapter.notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onCameraVdbConnected(VdtCamera vdtCamera) {
                Logger.t(TAG).d("camera vdb connected");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraListAdapter.notifyDataSetChanged();
                    }
                });


            }

            @Override
            public void onCameraDisconnected(VdtCamera vdtCamera) {
                Logger.t(TAG).d("onCameraDisconnected");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraListAdapter.notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onCameraStateChanged(VdtCamera vdtCamera) {
                Logger.t(TAG).d("onCameraStateChanged");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraListAdapter.notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onWifiListChanged() {
                Logger.t(TAG).d("onWifiListChanged");
                mCameraListAdapter.notifyDataSetChanged();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_camera_list, savedInstanceState);
        mCameraListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mCameraListView.setAdapter(mCameraListAdapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
//        mWifiAdmin = WifiAdminManager.getManager().attachWifiAdmin(mWifiCallback);
//        if (mWifiAdmin != null) {
//            updateWifiState(mWifiAdmin);
//        }

    }

    @Override
    public void onStop() {
        super.onStop();
//        WifiAdminManager.getManager().detachWifiAdmin(mWifiCallback, true);
//        stopDiscovery();
    }

    @OnClick(R.id.wifi_status_container)
    public void launchWifiSetting() {
        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
    }

//    private void updateWifiState(WifiAdmin wifiAdmin) {
//        if (wifiAdmin.isConnecting()) {
//            startWifiAnimation();
//            String fmt = getResources().getString(R.string.lable_connecting_to);
//            String title = String.format(fmt, wifiAdmin.getTargetSSID());
//            mWifiStatusView.setText(title);
//        } else {
//            NetworkInfo info = wifiAdmin.getNetworkInfo();
//            switch (info.getState()) {
//                default:
//                case DISCONNECTED:
//                    setWifiIcon(R.drawable.btn_wifi_off);
//                    mWifiStatusView.setText(R.string.btn_wlan_off);
//                    stopDiscovery();
//                    break;
//                case CONNECTING:
//                    startWifiAnimation();
//                    mWifiStatusView.setText(getString(R.string.lable_connecting_to, wifiAdmin.getCurrSSID()));
//                    break;
//                case DISCONNECTING:
//                    startWifiAnimation();
//                    mWifiStatusView.setText(getString(R.string.lable_disconnecting, wifiAdmin.getCurrSSID()));
//                    break;
//                case CONNECTED:
//                    setWifiIcon(R.drawable.btn_wifi_on);
//                    String wifiName = wifiAdmin.getCurrSSID();
//                    mWifiStatusView.setText(wifiName);
//                    startDiscovery();
//                    break;
//            }
//        }
//    }

    private void setWifiIcon(int resId) {
        if (mWifiAnimation != null) {
            mWifiAnimation.stop();
            mWifiAnimation = null;
        }
        if (resId != 0) {
            mWifiIcon.setImageResource(resId);
        }
    }

    private void startWifiAnimation() {
        if (mWifiAnimation == null) {
            mWifiAnimation = (AnimationDrawable) getResources().getDrawable(R.drawable.wifi);
            mWifiAnimation.setBounds(0, 0, mWifiAnimation.getIntrinsicWidth(), mWifiAnimation.getIntrinsicHeight());
            mWifiIcon.setImageDrawable(mWifiAnimation);
            mWifiAnimation.start();
        }
    }


    private void onServiceResolved(VdtCamera.ServiceInfo serviceInfo) {
        //serviceInfo.ssid = mWifiAdmin == null ? null : mWifiAdmin.getCurrSSID();
        mVdtCameraManager.connectCamera(serviceInfo);
    }

    private void onScanWifiDone(/*WifiAdmin wifiAdmin*/) {
//        mVdtCameraManager.filterScanResult(wifiAdmin.getScanResult());
//        mCameraListAdapter.notifyDataSetChanged();
    }

//    final WifiAdminManager.WifiCallback mWifiCallback = new WifiAdminManager.WifiCallback() {
//
//        @Override
//        public void networkStateChanged(WifiAdmin wifiAdmin) {
//            updateWifiState(wifiAdmin);
//        }
//
//        @Override
//        public void wifiScanResult(WifiAdmin wifiAdmin) {
//            onScanWifiDone(mWifiAdmin);
//        }
//
//        @Override
//        public void onConnectError(WifiAdmin wifiAdmin) {
//            // TODO
//        }
//
//        @Override
//        public void onConnectDone(WifiAdmin wifiAdmin) {
//            // TODO
//        }
//
//    };

    @Override
    public void onSetup(VdtCamera camera) {

        CameraSetupActivity.launch(getActivity(), camera);
    }

    @Override
    public void onBrowseVideo(VdtCamera camera) {
        Bundle args = new Bundle();
        args.putBoolean("isPcServer", camera.isPcServer());
        args.putString("ssid", camera.getSSID());
        args.putString("hostString", camera.getHostString());
        ((TabSwitchable) getActivity()).switchTab(0, args);
    }

    @Override
    public void onPreview(VdtCamera camera) {
        //CameraControlActivity.launch(getActivity(), camera);
        CameraControlActivity2.launch(getActivity(), camera);
    }


}
