package com.waylens.hachi.ui.manualsetup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Outline;
import android.graphics.drawable.AnimationDrawable;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.camera.connectivity.VdtCameraConnectivityManager;
import com.waylens.hachi.camera.events.CameraConnectionEvent;
import com.waylens.hachi.hardware.WifiAutoConnectManager;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.xfdingustc.mjpegview.library.MjpegView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.InetSocketAddress;

import butterknife.BindView;
import butterknife.OnClick;


/**
 * Created by Xiaofei on 2016/3/21.
 */
public class ApConnectFragment extends BaseFragment implements WifiAutoConnectManager.WifiAutoConnectListener {
    private static final String TAG = ApConnectFragment.class.getSimpleName();

    private static final int CAMERA_CONNECTING = 0;
    private static final int CAMERA_CONNECTED = 1;

    private String mSSID;
    private String mPassword;
    private WifiManager mWifiManager;

    private Handler mHandler;

    private VdtCameraManager mVdtCameraManager = VdtCameraManager.getManager();
    private EventBus mEventBus = EventBus.getDefault();

    private BroadcastReceiver mWifiStateReceiver;

    private boolean mConnected2CameraWifi = false;


    @BindView(R.id.tvSsid)
    TextView mTvSsid;

    @BindView(R.id.tvPassword)
    TextView mTvPassword;

    @BindView(R.id.vsRootView)
    ViewAnimator mVsRootViewAnimator;

    @BindView(R.id.live_view)
    MjpegView mLiveView;

    @BindView(R.id.skip)
    TextView mSkip;

    @BindView(R.id.connectHomeWifi)
    Button mConnectHomeWifi;

    @BindView(R.id.connectIndicator)
    ImageView mIvConnectIdicator;

    @BindView(R.id.tv_network_status)
    TextView mTvNetworkStatus;

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
    protected String getRequestTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraConnection(CameraConnectionEvent event) {
        Logger.t(TAG).d("On Receive camera connect event: " + event.getWhat());
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_CONNECTED:
                toggleCameraConnectView();
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        Logger.t(TAG).d("wifiInfo ssid: " + wifiInfo.getSSID() + " qrssid: " + mSSID);
        if (TextUtils.isEmpty(mSSID) || TextUtils.isEmpty(mPassword)) {
            Logger.t(TAG).d("From manualy selected");
            toggleCameraConnectView();
            return;
        }
        if (wifiInfo.getSSID().equals("\"" + mSSID + "\"")) {
            Logger.t(TAG).d("already connected");
            toggleCameraConnectView();
        } else {
            WifiAutoConnectManager wifiAutoConnectManager = new WifiAutoConnectManager(mWifiManager, this);
            wifiAutoConnectManager.connect(mSSID, mPassword, WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA);
            registerReceiver();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mEventBus.isRegistered(this)) {
            mEventBus.register(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(this);
        if (mWifiStateReceiver != null) {
            getActivity().unregisterReceiver(mWifiStateReceiver);
            mWifiStateReceiver = null;
        }
        mLiveView.stopStream();
    }

    @Override
    public void onAutoConnectStarted() {

    }

    @Override
    public void onAutoConnectError(String errorMsg) {

    }

    @Override
    public void onAutoConnectStatus(String status) {
        mTvNetworkStatus.setText(status);
    }


    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiStateReceiver = new WifiStateReceiver();
        getActivity().registerReceiver(mWifiStateReceiver, filter);
    }

    private void init() {
        Bundle bundle = getArguments();
        mHandler = new Handler();
        mSSID = bundle.getString("ssid");
        mPassword = bundle.getString("password");
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
    }

    private void initViews() {
        if (TextUtils.isEmpty(mSSID)) {
            mTvSsid.setVisibility(View.GONE);
        }
        if (TextUtils.isEmpty(mPassword)) {
            mTvPassword.setVisibility(View.GONE);
        }
        mTvSsid.setText("SSID:" + mSSID);
        mTvPassword.setText("PASSWORD:" + mPassword);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mLiveView.setClipToOutline(true);
            mLiveView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    }
                }
            });
        }


        mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
        AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
        animationDrawable.start();


    }


    private void toggleCameraConnectView() {
        if (mVsRootViewAnimator == null) {
            return;
        }
        if (mVdtCameraManager.isConnected()) {
            mVsRootViewAnimator.setDisplayedChild(CAMERA_CONNECTED);
            initVdtCamera();
            mConnectHomeWifi.setVisibility(View.VISIBLE);

            startCameraPreview();

        }
    }

    private void startCameraPreview() {
        if (mVdtCamera != null) {
            InetSocketAddress serverAddr = mVdtCamera.getPreviewAddress();
            if (serverAddr == null) {
                mVdtCamera = null;
                return;
            }
            mVdtCamera.startPreview();
            mLiveView.startStream(serverAddr);
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


    private class WifiStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                NetworkInfo.State state = networkInfo.getState();

                String currentSsid = wifiInfo.getSSID();
                if (state == NetworkInfo.State.CONNECTED && !mConnected2CameraWifi) {
                    if (currentSsid != null && currentSsid.equals("\"" + mSSID + "\"")) {
                        Logger.t(TAG).d("Network state changed " + wifiInfo.getSSID() + " state: " + state);
                        mConnected2CameraWifi = true;
                        mTvNetworkStatus.setText(R.string.wifi_status_connected);
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mTvNetworkStatus.setText(R.string.close_cellar_data_access_hint);
                            }
                        }, 5000);

                    } else {
                        mTvNetworkStatus.setText(R.string.wifi_status_ssid_incorrect);
                    }
                }

            }
        }
    }


}
