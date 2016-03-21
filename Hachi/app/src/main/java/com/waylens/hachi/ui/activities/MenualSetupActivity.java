package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.ScanQrCodeFragment;

/**
 * Created by Xiaofei on 2016/3/21.
 */
public class MenualSetupActivity extends BaseActivity {
    private static final String TAG = MenualSetupActivity.class.getSimpleName();
    private BroadcastReceiver mWifiStateReceiver;
    private ScanQrCodeFragment mScanQrCodeFragment;


    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, MenualSetupActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mScanQrCodeFragment = new ScanQrCodeFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, mScanQrCodeFragment).commit();


    }

    @Override
    protected void init() {
        super.init();

        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_menual_setup);
    }

    @Override
    public void setupToolbar() {
        mToolbar.setTitle(R.string.initial_setup);
        mToolbar.setNavigationIcon(R.drawable.navbar_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.launch(MenualSetupActivity.this);
            }
        });
        super.setupToolbar();
    }
}
