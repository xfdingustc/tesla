package com.waylens.hachi.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.WifiAutoConnectManager;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.ui.views.camerapreview.CameraLiveView;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/3/21.
 */
public class ApConnectFragment extends BaseFragment {
    private static final String TAG = ApConnectFragment.class.getSimpleName();

    private String mSSID;
    private String mPassword;

    private WifiManager mWifiManager;
    private BroadcastReceiver mWifiStateReceiver;

    private Handler mUiHandler;

    private VdtCameraManager mVdtCameraManager = VdtCameraManager.getManager();

    public static ApConnectFragment newInstance(String ssid, String password) {
        ApConnectFragment fragment = new ApConnectFragment();
        Bundle bundle = new Bundle();
        bundle.putString("ssid", ssid);
        bundle.putString("password", password);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Bind(R.id.tvSsid)
    TextView mTvSsid;

    @Bind(R.id.tvPassword)
    TextView mTvPassword;

    @Bind(R.id.vsRootView)
    ViewSwitcher mVsRootView;

    @Bind(R.id.cameraLiveView)
    CameraLiveView mCameraLiveView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_ap_connect, savedInstanceState);
        initViews();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo.getSSID() == mSSID) {
            toggleCameraConnectView();
        } else {
            WifiAutoConnectManager wifiAutoConnectManager = new WifiAutoConnectManager
                (mWifiManager, new WifiAutoConnectManager.WifiAutoConnectListener() {
                    @Override
                    public void onAudoConnectStarted() {
                    }
                });
            wifiAutoConnectManager.connect(mSSID, mPassword, WifiAutoConnectManager
                .WifiCipherType.WIFICIPHER_WPA);
            registerReceiver();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mWifiStateReceiver);
    }

    @Override
    public void onCameraVdbConnected(VdtCamera camera) {
        super.onCameraVdbConnected(camera);
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                toggleCameraConnectView();
            }
        });

    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiStateReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    Logger.t(TAG).d("Network state changed " + wifiInfo.getSSID());

                } else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                    Logger.t(TAG).d("WIFI_STATE_CHANGED_ACTION");
                }
            }
        };
        getActivity().registerReceiver(mWifiStateReceiver, filter);
    }

    private void init() {
        Bundle bundle = getArguments();
        mSSID = bundle.getString("ssid");
        mPassword = bundle.getString("password");
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        mUiHandler = new Handler();

    }

    private void initViews() {
        mTvSsid.setText("SSID:" + mSSID);
        mTvPassword.setText("PASSWORD:" + mPassword);


    }

    private void toggleCameraConnectView() {
        if (mVsRootView == null) {
            return;
        }
        if (mVdtCameraManager.isConnected()) {
            mVsRootView.showNext();
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    launchCltConnectFragment();
                }
            }, 1000);

        }
    }

    private void launchCltConnectFragment() {
        ClientConnectFragment fragment = new ClientConnectFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
    }


}