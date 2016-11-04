package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.waylens.hachi.R;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.entities.NetworkItemBean;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.MainActivity;


import java.util.List;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/5/23.
 */
public class ChooseWifiActivity extends BaseActivity {

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
            public void OnScanHostResult(final List<NetworkItemBean> networkList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNetworkItemAdapter.setNetworkList(networkList);
                    }
                });
            }
        });

        mAddedWifiList.setLayoutManager(new LinearLayoutManager(this));


        mWifiList.setLayoutManager(new LinearLayoutManager(this));
        mNetworkItemAdapter = new NetworkItemAdapter(this, new NetworkItemAdapter.OnNetworkItemClickedListener() {
            @Override
            public void onItemClicked(NetworkItemBean itemBean) {
                onNetworkItemClicked(itemBean);
            }
        });
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



    private void onNetworkItemClicked(final NetworkItemBean itemBean) {
        mSelectedNetworkItem = itemBean;

        if (itemBean.status == NetworkItemBean.CONNECT_STATUS_SAVED) {
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
