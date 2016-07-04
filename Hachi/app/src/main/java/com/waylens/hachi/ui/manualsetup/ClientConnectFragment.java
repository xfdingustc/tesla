package com.waylens.hachi.ui.manualsetup;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.hardware.WifiAutoConnectManager;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.events.NetworkEvent;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.entities.NetworkItemBean;
import com.waylens.hachi.ui.fragments.BaseFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/3/21.
 */
public class ClientConnectFragment extends BaseFragment {
    private static final String TAG = ClientConnectFragment.class.getSimpleName();

    public static final int CONNECTION_STAGE_CAMERA_2_ROUTE = 0;
    public static final int CONNECTION_STAGE_PHONE_2_ROUTE = 1;
    public static final int CONNECTION_STAGE_PHONE_2_CAMERA = 2;

    @BindView(R.id.rvWifiList)
    RecyclerView mRvWifiList;


    @BindView(R.id.loadingProgress)
    ProgressBar mLoadingProgress;

    @BindView(R.id.vsConnect)
    ViewSwitcher mVsConnect;

    @BindView(R.id.connectIndicator)
    ImageView mIvConnectIdicator;

    @BindView(R.id.connection_left)
    ImageView mIvConnectionLeft;

    @BindView(R.id.connection_right)
    ImageView mIvConnectionRight;


    private NetworkItemAdapter mNetworkItemAdapter;


    private VdtCamera.OnScanHostListener mOnScanHostListener;

    private MaterialDialog mPasswordDialog;

    private EditText mEtPassword;

    private WifiManager mWifiManager;

    private Timer mTimer;
    private ScanWifiTimeTask mScanWifiTimeTask;

    private EventBus mEventBus = EventBus.getDefault();
    private NetworkItemBean mSelectedNetworkItem = null;
    private String mSavedPassword;


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
                } else {
                    showCameraConnect2Wifi();
                    switchConnectionStage(CONNECTION_STAGE_PHONE_2_ROUTE);
                    WifiAutoConnectManager wifiAutoConnectManager = new WifiAutoConnectManager
                        (mWifiManager, new WifiAutoConnectManager.WifiAutoConnectListener() {
                            @Override
                            public void onAutoConnectStarted() {
                            }
                        });
                    wifiAutoConnectManager.connect(mSelectedNetworkItem.ssid, mSavedPassword, WifiAutoConnectManager
                        .WifiCipherType.WIFICIPHER_WPA);
                }
                break;
        }
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


    private void init() {
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
    }

    private void initViews() {

        mRvWifiList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mNetworkItemAdapter = new NetworkItemAdapter();
        mRvWifiList.setAdapter(mNetworkItemAdapter);

        mOnScanHostListener = new VdtCamera.OnScanHostListener() {
            @Override
            public void OnScanHostResult(final List<NetworkItemBean> networkList) {
//                Logger.t(TAG).d("get network list: " + networkList.size());
                mLoadingProgress.post(new Runnable() {
                    @Override
                    public void run() {
                        mLoadingProgress.setVisibility(View.GONE);
                    }
                });


                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNetworkItemAdapter.setNetworkList(networkList);
                    }
                });


                //mNetworkItemAdapter.setList(networkList);
            }
        };

        startScanWifi();
    }

    @Override
    public void onStart() {
        super.onStart();
        mEventBus.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(this);
        stopScanWifi();
    }

    private void startScanWifi() {
        mTimer = new Timer();
        mScanWifiTimeTask = new ScanWifiTimeTask();
        mTimer.schedule(mScanWifiTimeTask, 1000, 5000);
    }

    private void stopScanWifi() {
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    private void refreshWifiList() {
        Logger.t(TAG).d("start scan host: ");
        mLoadingProgress.post(new Runnable() {
            @Override
            public void run() {
                mLoadingProgress.setVisibility(View.VISIBLE);
            }
        });

        mVdtCamera.scanHost(mOnScanHostListener);
    }

    private void onNetworkItemClicked(final NetworkItemBean itemBean) {
        mSelectedNetworkItem = itemBean;


        mPasswordDialog = new MaterialDialog.Builder(getActivity())
            .title(itemBean.ssid)
            .customView(R.layout.dialog_network_password, true)
            .positiveText(R.string.join)
            .callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    super.onPositive(dialog);
                    setNetwork2Camera(itemBean.ssid, mEtPassword.getText().toString());
//                    showCameraConnect2Wifi();
                    stopScanWifi();
                    itemBean.status = NetworkItemBean.CONNECT_STATUS_AUTHENTICATION;
                    mNetworkItemAdapter.notifyDataSetChanged();
                }
            })
            .build();
        mPasswordDialog.show();
        mEtPassword = (EditText) mPasswordDialog.getCustomView().findViewById(R.id.password);


    }


    private void showCameraConnect2Wifi() {
        mVsConnect.showNext();
        mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
        AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
        animationDrawable.start();

//        stopScanWifi();
//        switchConnectionStage(CONNECTION_STAGE_CAMERA_2_ROUTE);

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


        //registerReceiver();
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

            if (networkItem.singalLevel >= -30) {
                viewHolder.ivWifiSignal.setImageResource(R.drawable.settings_signal_1);
            } else if (networkItem.singalLevel >= -60) {
                viewHolder.ivWifiSignal.setImageResource(R.drawable.settings_signal_2);
            } else if (networkItem.singalLevel >= -90) {
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

            public NetworkItemViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);

                mContainer.setTag(NetworkItemViewHolder.this);
                mContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        NetworkItemViewHolder viewHolder = (NetworkItemViewHolder) v.getTag();
                        NetworkItemBean itemBean = mNetworkList.get(viewHolder.getPosition());
                        onNetworkItemClicked(itemBean);
                    }
                });
            }
        }
    }


    private class ScanWifiTimeTask extends TimerTask {

        @Override
        public void run() {
            refreshWifiList();

        }
    }

}
