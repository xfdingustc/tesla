package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.EditText;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.smartconfig.NetworkUtil;
import com.waylens.hachi.hardware.smartconfig.SmartConfigConstants;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/3/15.
 */
public class SmartConfigActivity extends BaseActivity {
    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, SmartConfigActivity.class);
        activity.startActivity(intent);
    }


    @Bind(R.id.smartconfig_network_name_field)
    EditText mEtNetworkName;

    @Bind(R.id.smartconfig_network_pass_field)
    EditText mEtNetworkPasswd;

    BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mEtNetworkName.setText(NetworkUtil.getWifiName(SmartConfigActivity.this));
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(networkChangeReceiver, new IntentFilter(SmartConfigConstants.NETWORK_CHANGE_BROADCAST_ACTION));
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_smart_config);
    }
}
