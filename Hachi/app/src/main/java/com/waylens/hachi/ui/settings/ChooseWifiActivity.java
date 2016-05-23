package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.entities.NetworkItemBean;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/5/23.
 */
public class ChooseWifiActivity extends BaseActivity {
    private NetworkItemAdapter mAddedNetworkItemAdapter;
    private NetworkItemAdapter mNetworkItemAdapter;

    private NetworkItemBean mSelectedNetworkItem = null;

    private EditText mEtPassword;

    @BindView(R.id.added_wifi_list)
    RecyclerView mAddedWifiList;

    @BindView(R.id.wifi_list)
    RecyclerView mWifiList;

    private MaterialDialog mPasswordDialog;

    private String mSavedPassword;


    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, ChooseWifiActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_choose_wifi);

        setupToolbar();

        mVdtCamera.scanHost(new VdtCamera.OnScanHostListener() {
            @Override
            public void OnScanHostResult(final List<NetworkItemBean> addedNetworkList, final List<NetworkItemBean> networkList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAddedNetworkItemAdapter.setNetworkList(addedNetworkList);
                        mNetworkItemAdapter.setNetworkList(networkList);
                    }
                });
            }
        });

        mAddedWifiList.setLayoutManager(new LinearLayoutManager(this));
        mAddedNetworkItemAdapter = new NetworkItemAdapter();
        mAddedWifiList.setAdapter(mAddedNetworkItemAdapter);

        mWifiList.setLayoutManager(new LinearLayoutManager(this));
        mNetworkItemAdapter = new NetworkItemAdapter();
        mWifiList.setAdapter(mNetworkItemAdapter);
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.choose_wifi);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public class NetworkItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final Context mContext;

        private List<NetworkItemBean> mNetworkList;

        public NetworkItemAdapter() {
            this.mContext = ChooseWifiActivity.this;
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

    private void onNetworkItemClicked(final NetworkItemBean itemBean) {
        mSelectedNetworkItem = itemBean;

        if (itemBean.added) {
            connect2AddedWifi(itemBean.ssid);
        } else {

            mPasswordDialog = new MaterialDialog.Builder(this)
                .title(itemBean.ssid)
                .customView(R.layout.dialog_network_password, true)
                .positiveText(R.string.join)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);

                        setNetwork2Camera(itemBean.ssid, mEtPassword.getText().toString());
                        MainActivity.launch(ChooseWifiActivity.this);
                    }
                })
                .build();
            mPasswordDialog.show();
            mEtPassword = (EditText) mPasswordDialog.getCustomView().findViewById(R.id.password);
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
}
