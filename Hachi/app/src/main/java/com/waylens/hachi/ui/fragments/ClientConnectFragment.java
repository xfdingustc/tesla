package com.waylens.hachi.ui.fragments;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.WifiAutoConnectManager;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.ui.entities.NetworkItemBean;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/3/21.
 */
public class ClientConnectFragment extends BaseFragment {
    private static final String TAG = ClientConnectFragment.class.getSimpleName();

    @Bind(R.id.rvWifiList)
    RecyclerView mRvWifiList;
    private List<NetworkItemBean> mNetworkList;

    private NetworkItemAdapter mNetworkItemAdapter;

    private VdtCamera.OnScanHostListener mOnScanHostListener;

    private Handler mHandler;

    private MaterialDialog mPasswordDialog;

    private EditText mEtPassword;

    private WifiManager mWifiManager;

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
        mHandler = new Handler();

        mOnScanHostListener = new VdtCamera.OnScanHostListener() {
            @Override
            public void OnScanHostResult(List<NetworkItemBean> networkList) {
                Logger.t(TAG).d("get network list: " + networkList.size());
                mNetworkList = networkList;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNetworkItemAdapter.notifyDataSetChanged();
                    }
                });


                //mNetworkItemAdapter.setList(networkList);
            }
        };

        refreshWifiList();

    }

    @Override
    public void onCameraVdbConnected(VdtCamera camera) {
        super.onCameraVdbConnected(camera);
        refreshWifiList();
    }

    private void refreshWifiList() {
        mVdtCamera.scanHost(mOnScanHostListener);
    }

    private void onNetworkItemClicked(final int position) {
        final NetworkItemBean itemBean = mNetworkList.get(position);
        mPasswordDialog = new MaterialDialog.Builder(getActivity())
            .title(itemBean.ssid)
            .customView(R.layout.dialog_network_password, false)
            .positiveText(R.string.join)
            .callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    super.onPositive(dialog);
                    setNetwork2Camera(itemBean.ssid, mEtPassword.getText().toString());
                }
            })
            .build();
        mPasswordDialog.show();
        mEtPassword = (EditText)mPasswordDialog.getCustomView().findViewById(R.id.password);
    }

    private void setNetwork2Camera(String ssid, String password) {
        mVdtCamera.addNetworkHost(ssid, password);
        WifiAutoConnectManager wifiAutoConnectManager = new WifiAutoConnectManager
            (mWifiManager, new WifiAutoConnectManager.WifiAutoConnectListener() {
                @Override
                public void onAudoConnectStarted() {
                }
            });
        wifiAutoConnectManager.connect(ssid, password, WifiAutoConnectManager
            .WifiCipherType.WIFICIPHER_WPA);
        //registerReceiver();
    }


    public class NetworkItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final Context mContext;


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
            NetworkItemViewHolder viewHolder = (NetworkItemViewHolder)holder;
            Logger.t(TAG).d("set ssid: " + networkItem.ssid);
            viewHolder.tvSsid.setText(networkItem.ssid);
        }


        @Override
        public int getItemCount() {
            int size = mNetworkList == null ? 0 : mNetworkList.size();
            Logger.t(TAG).d("size: " + size);
            return size;
        }

        public class NetworkItemViewHolder extends RecyclerView.ViewHolder {

            @Bind(R.id.wifiContainer)
            LinearLayout mContainer;

            @Bind(R.id.tvSsid)
            TextView tvSsid;

            public NetworkItemViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);

                mContainer.setTag(NetworkItemViewHolder.this);
                mContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        NetworkItemViewHolder viewHolder = (NetworkItemViewHolder)v.getTag();
                        onNetworkItemClicked(viewHolder.getPosition());
                    }
                });
            }
        }
    }


}