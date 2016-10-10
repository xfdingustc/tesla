package com.waylens.hachi.ui.manualsetup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.connectivity.VdtCameraConnectivityManager;
import com.waylens.hachi.camera.entities.NetworkItemBean;
import com.waylens.hachi.camera.events.CameraConnectionEvent;
import com.waylens.hachi.camera.events.NetworkEvent;
import com.waylens.hachi.hardware.WifiAutoConnectManager;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.views.radarview.RadarView;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/3/21.
 */
public class ClientConnectFragment extends BaseFragment implements WifiAutoConnectManager.WifiAutoConnectListener {
    private static final String TAG = ClientConnectFragment.class.getSimpleName();

    public static final int CONNECTION_STAGE_CAMERA_2_ROUTE = 0;
    public static final int CONNECTION_STAGE_PHONE_2_ROUTE = 1;
    public static final int CONNECTION_STAGE_PHONE_2_CAMERA = 2;

    @BindView(R.id.rvWifiList)
    RecyclerView mRvWifiList;


    @BindView(R.id.text_line1)
    TextView mTextLine1;

    @BindView(R.id.text_line2)
    TextView mTextLine2;

    @BindView(R.id.vsConnect)
    ViewAnimator mVsConnect;

    @BindView(R.id.connectIndicator)
    ImageView mIvConnectIdicator;

    @BindView(R.id.connection_left)
    ImageView mIvConnectionLeft;

    @BindView(R.id.connection_right)
    ImageView mIvConnectionRight;

    @BindView(R.id.wifi_scan_radar)
    RadarView mWifiScanRadar;

    @BindView(R.id.bottom_layout)
    View bottomLayout;

    @BindView(R.id.btn_refresh)
    ImageView btnRefresh;

    @OnClick(R.id.ll_refresh)
    public void onBtnRefreshClicked() {
        startScanWifi();
        Animation operatingAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
        btnRefresh.startAnimation(operatingAnim);
        btnRefresh.setEnabled(false);

    }


    private NetworkItemAdapter mNetworkItemAdapter;

    private boolean mNetworkItemShouldUpdated = true;


    private VdtCamera.OnScanHostListener mOnScanHostListener;

    private MaterialDialog mPasswordDialog;

    private EditText mEtPassword;

    private WifiManager mWifiManager;

    //    private Timer mTimer;
    private ScanWifiTimeTask mScanWifiTimeTask;

    private EventBus mEventBus = EventBus.getDefault();
    private NetworkItemBean mSelectedNetworkItem = null;
    private String mSavedPassword;

    private BroadcastReceiver mWifiStateReceiver;


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraConnection(CameraConnectionEvent event) {
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_CONNECTED:
                MainActivity.launch(getActivity());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraNetwork(NetworkEvent event) {
        switch (event.getWhat()) {
            case NetworkEvent.NETWORK_EVENT_WHAT_ADDED:
                mVdtCamera.connectNetworkHost(mSelectedNetworkItem.ssid);
                break;
            case NetworkEvent.NETWORK_EVENT_WHAT_CONNECTED:
                Integer connectResult = (Integer) event.getExtra1();
                Logger.t(TAG).d("connect result: " + connectResult);
                if (connectResult == 0) {
                    mSelectedNetworkItem.status = NetworkItemBean.CONNECT_STATUS_AUTHENTICATION_PROBLEM;
                    mNetworkItemAdapter.notifyDataSetChanged();
                    mEventBus.unregister(this);
                } else {
                    bottomLayout.setVisibility(View.GONE);
                    showCameraConnect2Wifi();
                    switchConnectionStage(CONNECTION_STAGE_PHONE_2_ROUTE);
                    WifiAutoConnectManager wifiAutoConnectManager = new WifiAutoConnectManager(mWifiManager, this);
                    wifiAutoConnectManager.connect(mSelectedNetworkItem.ssid, mSavedPassword, WifiAutoConnectManager
                        .WifiCipherType.WIFICIPHER_WPA);
                }
                break;
        }
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


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_client_connect, savedInstanceState);
        initViews();
        return view;
    }

    @Override
    public void onAutoConnectStarted() {

    }

    @Override
    public void onAutoConnectError(String errorMsg) {

    }

    @Override
    public void onAutoConnectStatus(String status) {
        mTextLine1.setText(status);
        mTextLine2.setVisibility(View.INVISIBLE);
    }


    private void init() {
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
    }

    private void initViews() {
        mWifiScanRadar.startScan();
        mRvWifiList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mNetworkItemAdapter = new NetworkItemAdapter();
        mRvWifiList.setAdapter(mNetworkItemAdapter);

        mOnScanHostListener = new VdtCamera.OnScanHostListener() {
            @Override
            public void OnScanHostResult(final List<NetworkItemBean> networkList) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mVsConnect.setDisplayedChild(1);
                        bottomLayout.setVisibility(View.VISIBLE);
                        btnRefresh.clearAnimation();
                        btnRefresh.setEnabled(true);
                        if (!mNetworkItemShouldUpdated) {
                            return;
                        }

                        mNetworkItemAdapter.setNetworkList(networkList);
                        mTextLine1.setText(R.string.choose_your_home_wifi);
                        mTextLine2.setVisibility(View.VISIBLE);
                    }
                });


                //mNetworkItemAdapter.setList(networkList);
            }
        };

        startScanWifi();
    }


    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(this);
        if (mWifiStateReceiver != null) {
            getActivity().unregisterReceiver(mWifiStateReceiver);
            mWifiStateReceiver = null;
        }
        stopScanWifi();
    }

    private void startScanWifi() {
//        mTimer = new Timer();
        mScanWifiTimeTask = new ScanWifiTimeTask();
//        mTimer.schedule(mScanWifiTimeTask, 1000, 15000);
        refreshWifiList();
    }

    private void stopScanWifi() {
//        if (mTimer != null) {
//            mTimer.cancel();
//        }
    }

    private void refreshWifiList() {
        Logger.t(TAG).d("start scan host: ");
        mVdtCamera.scanHost(mOnScanHostListener);
    }

    private void onNetworkItemClicked(final NetworkItemBean itemBean) {
        mSelectedNetworkItem = itemBean;

        mPasswordDialog = new MaterialDialog.Builder(getActivity())
            .title(itemBean.ssid)
            .customView(R.layout.dialog_network_password, true)
            .positiveText(R.string.connect)
            .callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    super.onPositive(dialog);
                    setNetwork2Camera(itemBean.ssid, mEtPassword.getText().toString());
//                    showCameraConnect2Wifi();
                    mEventBus.register(ClientConnectFragment.this);
                    stopScanWifi();
                    itemBean.status = NetworkItemBean.CONNECT_STATUS_AUTHENTICATION;
                    mNetworkItemAdapter.notifyDataSetChanged();
                    mNetworkItemShouldUpdated = false;
                }
            })
            .build();
        mPasswordDialog.show();
        mEtPassword = (EditText) mPasswordDialog.getCustomView().findViewById(R.id.password);


    }


    private void showCameraConnect2Wifi() {
        mVsConnect.setDisplayedChild(2);
        mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
        AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
        animationDrawable.start();
    }

    private void switchConnectionStage(int stage) {
        switch (stage) {
            case CONNECTION_STAGE_CAMERA_2_ROUTE:
                mIvConnectionLeft.setImageResource(R.drawable.camera_connecting_camera);
                mIvConnectionRight.setImageResource(R.drawable.camera_connecting_router);
                break;
            case CONNECTION_STAGE_PHONE_2_ROUTE:
                mIvConnectionLeft.setImageResource(R.drawable.camera_connecting_phone);
                mIvConnectionRight.setImageResource(R.drawable.camera_connecting_router);
                break;
            case CONNECTION_STAGE_PHONE_2_CAMERA:
                mIvConnectionLeft.setImageResource(R.drawable.camera_connecting_phone);
                mIvConnectionRight.setImageResource(R.drawable.camera_connecting_camera);
                break;
        }
    }

    private void connect2AddedWifi(String ssid) {
        mVdtCamera.connectNetworkHost(ssid);

    }

    private void setNetwork2Camera(final String ssid, final String password) {
        mVdtCamera.addNetworkHost(ssid, password);
        mSavedPassword = password;
        registerReceiver();
    }


    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiStateReceiver = new WifiStateReceiver();
        getActivity().registerReceiver(mWifiStateReceiver, filter);
    }


    public class NetworkItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final Context mContext;

        private List<NetworkItemBean> mNetworkList;

        public NetworkItemAdapter() {
            this.mContext = getActivity();
        }


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_network, parent, false);
            return new NetworkItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            NetworkItemBean networkItem = mNetworkList.get(position);
            NetworkItemViewHolder viewHolder = (NetworkItemViewHolder) holder;
//            Logger.t(TAG).d("set ssid: " + networkItem.ssid);
            viewHolder.tvSsid.setText(networkItem.ssid);
            if (networkItem.flags != null && !networkItem.flags.isEmpty()) {
                viewHolder.ivWifiCipher.setVisibility(View.VISIBLE);
            } else {
                viewHolder.ivWifiCipher.setVisibility(View.INVISIBLE);
            }

            switch (networkItem.status) {
                case NetworkItemBean.CONNECT_STATUS_NONE:
                    viewHolder.wifiStatus.setVisibility(View.GONE);
                    break;
                case NetworkItemBean.CONNECT_STATUS_SAVED:
                    viewHolder.wifiStatus.setVisibility(View.VISIBLE);
                    viewHolder.wifiStatus.setText(R.string.saved);
                    break;
                case NetworkItemBean.CONNECT_STATUS_AUTHENTICATION:
                    viewHolder.wifiStatus.setVisibility(View.VISIBLE);
                    viewHolder.wifiStatus.setText(R.string.authenticating);
                    break;
                case NetworkItemBean.CONNECT_STATUS_AUTHENTICATION_PROBLEM:
                    viewHolder.wifiStatus.setVisibility(View.VISIBLE);
                    viewHolder.wifiStatus.setText(R.string.authentication_problem);
                    break;
            }

            if (networkItem.signalLevel >= -55) {
                viewHolder.ivWifiSignal.setImageResource(R.drawable.settings_signal_1);
            } else if (networkItem.signalLevel >= -70) {
                viewHolder.ivWifiSignal.setImageResource(R.drawable.settings_signal_2);
            } else if (networkItem.signalLevel >= -85) {
                viewHolder.ivWifiSignal.setImageResource(R.drawable.settings_signal_3);
            } else {
                viewHolder.ivWifiSignal.setImageResource(R.drawable.settings_signal_4);
            }
        }


        @Override
        public int getItemCount() {
            int size = mNetworkList == null ? 0 : mNetworkList.size();
            return size;
        }

        public void setNetworkList(List<NetworkItemBean> networkList) {
            mNetworkList = networkList;
            notifyDataSetChanged();
        }

        public class NetworkItemViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.wifiContainer)
            LinearLayout mContainer;

            @BindView(R.id.tvSsid)
            TextView tvSsid;

            @BindView(R.id.ivWifiCipher)
            ImageView ivWifiCipher;

            @BindView(R.id.ivWifiSignal)
            ImageView ivWifiSignal;

            @BindView(R.id.wifi_status)
            TextView wifiStatus;

            @OnClick(R.id.wifiContainer)
            public void onWifiContainerClicked(View v) {
                NetworkItemViewHolder viewHolder = (NetworkItemViewHolder) v.getTag();
                NetworkItemBean itemBean = mNetworkList.get(viewHolder.getPosition());
                onNetworkItemClicked(itemBean);
            }

            public NetworkItemViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                mContainer.setTag(NetworkItemViewHolder.this);

            }
        }
    }


    private class ScanWifiTimeTask extends TimerTask {

        @Override
        public void run() {
            refreshWifiList();

        }
    }

    private class WifiStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                NetworkInfo.State state = networkInfo.getState();

                String currentSsid = wifiInfo.getSSID();
                if (state == NetworkInfo.State.CONNECTED) {
                    if (currentSsid != null && currentSsid.equals("\"" + mSelectedNetworkItem.ssid + "\"")) {
                        Logger.t(TAG).d("Network state changed " + wifiInfo.getSSID() + " state: " + state);
                        mTextLine1.setText(R.string.wifi_status_connected);
                        switchConnectionStage(CONNECTION_STAGE_PHONE_2_CAMERA);
                    } else {
//                        mTextLine1.setText(R.string.wifi_status_ssid_incorrect);
                    }
                }

            }
        }
    }

}
