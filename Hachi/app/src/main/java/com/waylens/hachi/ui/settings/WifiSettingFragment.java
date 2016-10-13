package com.waylens.hachi.ui.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.hardware.WifiAutoConnectManager;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.Refreshable;
import com.waylens.hachi.utils.StringUtils;
import com.waylens.hachi.camera.entities.NetworkItemBean;
import com.waylens.hachi.camera.events.CameraConnectionEvent;
import com.waylens.hachi.camera.events.NetworkEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.w3c.dom.Text;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import android.net.Network;

/**
 * Created by Xiaofei on 2016/5/23.
 */
public class WifiSettingFragment extends BaseFragment implements WifiAutoConnectManager.WifiAutoConnectListener{
    private static final String TAG = WifiSettingFragment.class.getSimpleName();

    public static final int CONNECTION_STAGE_CAMERA_2_ROUTE = 0;
    public static final int CONNECTION_STAGE_PHONE_2_ROUTE = 1;
    public static final int CONNECTION_STAGE_PHONE_2_CAMERA = 2;

    private NetworkItemAdapter mNetworkItemAdapter;

    private NetworkItemBean mSelectedNetworkItem = null;

    private EditText mEtPassword;

    @BindView(R.id.title)
    TextView mTvWifiTitle;

    @BindView(R.id.summary)
    TextView mTvWifiMode;

    @BindView(R.id.wifi_mode)
    LinearLayout mWifiMode;

    @BindView(R.id.wifi_list)
    RecyclerView mWifiList;

    @BindView(R.id.tvSsid)
    TextView mTvSsid;

    @BindView(R.id.tvInfo)
    TextView mTvInfo;

/*    @BindView(R.id.pull_to_refresh)
    PullToRefreshView mPullToRefreshView;*/

    private MaterialDialog mPasswordDialog;

    private String mSavedPassword;

    private EventBus mEventBus = EventBus.getDefault();

    private WifiManager mWifiManager;

    private BroadcastReceiver mWifiStateReceiver;

    private VdtCamera.OnScanHostListener mOnScanHostListener;

    private int mStage = -1;

    @BindView(R.id.btn_refresh)
    ImageButton btnRefresh;

    @OnClick(R.id.btn_refresh)
    public void onBtnRefreshClicked() {
        mVdtCamera.scanHost(mOnScanHostListener);
        Animation operatingAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
        btnRefresh.startAnimation(operatingAnim);
        Logger.t(TAG).d("btn click");
        btnRefresh.setEnabled(false);
    }

    @Override
    protected String getRequestTag() {
        return TAG;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraConnection(CameraConnectionEvent event) {
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_CONNECTED:
                Snackbar.make(WifiSettingFragment.this.mWifiList, getString(R.string.connect_successfully), Snackbar.LENGTH_SHORT).show();
                MainActivity.launch(getActivity());
                getActivity().finish();
                break;
            case CameraConnectionEvent.VDT_CAMERA_CONNECTING_FAILED:
                MaterialDialog dialog = new MaterialDialog.Builder(this.getActivity())
                        .positiveText(R.string.ok)
                        .negativeText(R.string.cancel)
                        .content(R.string.restart_app)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                final Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(getActivity().getPackageName());
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("need_delay", true);
                                startActivity(intent);
                                System.exit(0);
                            }
                        })
                        .build();
                dialog.show();
            default:
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
                    switchConnectionStage(CONNECTION_STAGE_PHONE_2_ROUTE);
                    WifiAutoConnectManager wifiAutoConnectManager = new WifiAutoConnectManager(mWifiManager, this);
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

    private void init() {
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        mEventBus.register(WifiSettingFragment.this);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_wifi_setting, savedInstanceState);
        initViews();
        return view;

    }

    private void initViews() {
        mWifiList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mNetworkItemAdapter = new NetworkItemAdapter(getActivity());
        mWifiList.setAdapter(mNetworkItemAdapter);
        mOnScanHostListener = new VdtCamera.OnScanHostListener() {
            @Override
            public void OnScanHostResult(final List<NetworkItemBean> networkList) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
/*                        mPullToRefreshView.setRefreshing(false);*/
                        Logger.t(TAG).d(networkList.size());
                        btnRefresh.clearAnimation();
                        btnRefresh.setEnabled(true);
                        mNetworkItemAdapter.setNetworkList(networkList);

                    }
            });
        }};
        onBtnRefreshClicked();
        Observable.timer(4L, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        btnRefresh.clearAnimation();
                        btnRefresh.setEnabled(true);
                    }
                });
        mVdtCamera.scanHost(mOnScanHostListener);
        initWifiMode();
/*        mPullToRefreshView.setOnRefreshListener(new PullToRefreshView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPullToRefreshView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPullToRefreshView.setRefreshing(false);
                        btnRefresh.clearAnimation();
                        btnRefresh.setEnabled(true);
                    }
                }, 4000);

                mVdtCamera.scanHost(mOnScanHostListener);
            }
        });*/
    }

    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(this);
        if (mWifiStateReceiver != null) {
            getActivity().unregisterReceiver(mWifiStateReceiver);
            mWifiStateReceiver = null;
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiStateReceiver = new WifiStateReceiver();
        getActivity().registerReceiver(mWifiStateReceiver, filter);
    }



    private void initWifiMode() {
        int wifiMode = 0;
        switch (mVdtCamera.getWifiMode()) {
            case VdtCamera.WIFI_MODE_AP:
                mTvWifiMode.setText(R.string.access_point);
                wifiMode = 0;
                break;
            case VdtCamera.WIFI_MODE_CLIENT:
                mTvWifiMode.setText(R.string.client);
                wifiMode = 1;
                break;
            case VdtCamera.WIFI_MODE_OFF:
                mTvWifiMode.setText(R.string.off);
                wifiMode = 2;
                break;
        }

        Logger.t(TAG).d(mVdtCamera.getSSID());
        if(!TextUtils.isEmpty(mVdtCamera.getSSID())) {
            mTvSsid.setText(mVdtCamera.getSSID());
        } else {
            if (mVdtCamera.getWifiMode() != VdtCamera.WIFI_MODE_OFF) {
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                String ssid = wifiInfo.getSSID();
                int len = ssid.length();
                if (len > 2) {
                    String str = ssid.substring(1, len - 1);
                    mTvSsid.setText(str);
                }
            }
        }

        final int wifiModeIndex = wifiMode;
      mWifiMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.change_wifi_mode)
                    .items(R.array.wifi_mode_list)
                    .itemsCallbackSingleChoice(wifiModeIndex, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                            return false;
                        }
                    })
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            int selectIndex = dialog.getSelectedIndex();
                            if (selectIndex == wifiModeIndex) {
                                return;
                            }

                            showChangeWifiModeAlertDialog(selectIndex);
                        }
                    })
                    .show();
            }
        });
    }


    @Override
    public void onAutoConnectStarted() {

    }

    @Override
    public void onAutoConnectError(String errorMsg) {

    }

    @Override
    public void onAutoConnectStatus(String status) {

    }


    public class NetworkItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final Context mContext;

        private List<NetworkItemBean> mNetworkList;

        public NetworkItemAdapter(Context context) {
            this.mContext = context;
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

            if (networkItem.signalLevel >= -30) {
                viewHolder.ivWifiSignal.setImageResource(R.drawable.settings_signal_1);
            } else if (networkItem.signalLevel >= -60) {
                viewHolder.ivWifiSignal.setImageResource(R.drawable.settings_signal_2);
            } else if (networkItem.signalLevel >= -90) {
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

                mContainer.setTag(WifiSettingFragment.NetworkItemAdapter.NetworkItemViewHolder.this);
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

    private void onNetworkItemClicked(final NetworkItemBean itemBean) {
        if(mSelectedNetworkItem !=null && mSelectedNetworkItem.status == NetworkItemBean.CONNECT_STATUS_AUTHENTICATION) {
            Snackbar.make(WifiSettingFragment.this.mWifiList, "Connecting process is not finished yet.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        mSelectedNetworkItem = itemBean;
        /*
        if (itemBean.status == NetworkItemBean.CONNECT_STATUS_SAVED) {
            connect2AddedWifi(itemBean.ssid);
        } else {  */

        mPasswordDialog = new MaterialDialog.Builder(this.getActivity())
                .title(itemBean.ssid)
                .customView(R.layout.dialog_network_password, true)
                .positiveText(R.string.join)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            if (!mEventBus.isRegistered(WifiSettingFragment.this)) {
                                mEventBus.register(WifiSettingFragment.this);
                            }
                            setNetwork2Camera(itemBean.ssid, mEtPassword.getText().toString());
                            //MainActivity.launch(WifiSettingFragment.this.getActivity());
                            itemBean.status = NetworkItemBean.CONNECT_STATUS_AUTHENTICATION;
                            mNetworkItemAdapter.notifyDataSetChanged();
                        }
                    })
                    .build();
        mPasswordDialog.show();
        mEtPassword = (EditText) mPasswordDialog.getCustomView().findViewById(R.id.password);
    }

    private void connect2AddedWifi(String ssid) {
        mVdtCamera.connectNetworkHost(ssid);

    }

    private void setNetwork2Camera(final String ssid, final String password) {
        mVdtCamera.addNetworkHost(ssid, password);
        mSavedPassword = password;
        registerReceiver();
    }



    private void showChangeWifiModeAlertDialog(final int selectIndex) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
            .title(R.string.change_wifi_mode)
            .content(R.string.change_wifi_mode_alert)
            .positiveText(android.R.string.ok)
            .negativeText(android.R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    doChangeWifiMode(selectIndex);
                }
            })
            .show();
    }

    private void doChangeWifiMode(int selectIndex) {
        Logger.t(TAG).d("change wifi mode to " + selectIndex);
        mVdtCamera.setWifiMode(selectIndex);
        VdtCameraManager.getManager().onCameraDisconnected(mVdtCamera);
        MainActivity.launch(getActivity());
    }

    private void switchConnectionStage(final int stage) {
        switch (stage) {
            case CONNECTION_STAGE_CAMERA_2_ROUTE:
                mTvInfo.setText(R.string.camera_to_router);
                break;
            case CONNECTION_STAGE_PHONE_2_ROUTE:
                mTvInfo.setText(R.string.phone_to_router);
                break;
            case CONNECTION_STAGE_PHONE_2_CAMERA:
                mTvInfo.setText(R.string.phone_to_camera);
                break;
        }
        Observable.timer(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        if (mStage == stage) {
                            mTvInfo.setText("");
                        }
                    }
                });
        mStage = stage;
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
                    }
                    switchConnectionStage(CONNECTION_STAGE_PHONE_2_CAMERA);
                }
            }
        }
    }
}
