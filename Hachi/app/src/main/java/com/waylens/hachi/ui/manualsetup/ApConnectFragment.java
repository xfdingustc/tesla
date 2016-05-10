package com.waylens.hachi.ui.manualsetup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.hardware.WifiAutoConnectManager;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.liveview.camerapreview.CameraLiveView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.InetSocketAddress;

import butterknife.BindView;
import butterknife.OnClick;


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
    private EventBus mEventBus = EventBus.getDefault();


    @BindView(R.id.tvSsid)
    TextView mTvSsid;

    @BindView(R.id.tvPassword)
    TextView mTvPassword;

    @BindView(R.id.vsRootView)
    ViewSwitcher mVsRootView;

    @BindView(R.id.cameraLiveView)
    CameraLiveView mLiveView;

    @BindView(R.id.skip)
    TextView mSkip;

    @BindView(R.id.connectHomeWifi)
    Button mConnectHomeWifi;

    @BindView(R.id.connectIndicator)
    ImageView mIvConnectIdicator;

    @OnClick(R.id.skip)
    public void onSkipClicked() {
        MainActivity.launch(getActivity());
    }

    @OnClick(R.id.connectHomeWifi)
    public void onConnectHomeWifiClick() {
        launchCltConnectFragment();
    }

    public static ApConnectFragment newInstance(String ssid, String password) {
        ApConnectFragment fragment = new ApConnectFragment();
        Bundle bundle = new Bundle();
        bundle.putString("ssid", ssid);
        bundle.putString("password", password);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraConnection(CameraConnectionEvent event) {
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_CONNECTED:
                toggleCameraConnectView(event.getVdtCamera());
                break;
        }
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
        mEventBus.register(this);

        mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
        AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
        animationDrawable.start();
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo.getSSID() == mSSID) {
            toggleCameraConnectView(mVdtCamera);
        } else {
            WifiAutoConnectManager wifiAutoConnectManager = new WifiAutoConnectManager
                (mWifiManager, new WifiAutoConnectManager.WifiAutoConnectListener() {
                    @Override
                    public void onAutoConnectStarted() {
                    }
                });
            wifiAutoConnectManager.connect(mSSID, mPassword, WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA);
            registerReceiver();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Logger.t(TAG).d("on stop");
        mEventBus.unregister(this);
        getActivity().unregisterReceiver(mWifiStateReceiver);
        mLiveView.stopStream();
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
//                    Logger.t(TAG).d("Network state changed " + wifiInfo.getSSID());

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mLiveView.setClipToOutline(true);
            mLiveView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
        }


    }

    private void toggleCameraConnectView(final VdtCamera camera) {
        if (mVsRootView == null) {
            return;
        }
        if (mVdtCameraManager.isConnected()) {
            mVsRootView.showNext();
            initCamera();
            mConnectHomeWifi.setVisibility(View.VISIBLE);
//            mUiHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    launchCltConnectFragment();
//                }
//            }, 1000);

            startCameraPreview();

        }
    }

    private void startCameraPreview() {
        mLiveView.setBackgroundColor(Color.BLACK);
        if (mVdtCamera != null) {
            InetSocketAddress serverAddr = mVdtCamera.getPreviewAddress();
            if (serverAddr == null) {
                mVdtCamera = null;
                return;
            }
            mVdtCamera.startPreview();
            mLiveView.startStream(serverAddr, null, true);
//            mVdtCamera.getRecordRecMode();
//            mVdtCamera.getRecordTime();
//            mVdtCamera.getAudioMicState();
//            mVdtCamera.getRecordResolutionList();
//            mVdtCamera.getSetup();

        }
    }

    private void launchCltConnectFragment() {
        ClientConnectFragment fragment = new ClientConnectFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
    }


}
