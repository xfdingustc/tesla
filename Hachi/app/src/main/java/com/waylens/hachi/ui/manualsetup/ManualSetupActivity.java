package com.waylens.hachi.ui.manualsetup;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.camera.connectivity.VdtCameraConnectivityManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.utils.ConnectivityHelper;


/**
 * Created by Xiaofei on 2016/3/21.
 */
public class ManualSetupActivity extends BaseActivity {
    private static final String TAG = ManualSetupActivity.class.getSimpleName();
    private BroadcastReceiver mWifiStateReceiver;
    private ApConnectFragment mApConnectFragment;

    private final int PERMISSION_REQUEST_CAMERA = 0;

    private final static String EXTRA_WIFI = "wifi";
    private final static String EXTRA_PASSWORD = "password";

    private String mWifi;
    private String mPassword;

    public static void launch(Activity activity, String wifi, String password) {
        Intent intent = new Intent(activity, ManualSetupActivity.class);
        intent.putExtra(EXTRA_WIFI, wifi);
        intent.putExtra(EXTRA_PASSWORD, password);
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
        mWifi = getIntent().getStringExtra(EXTRA_WIFI);
        mPassword = getIntent().getStringExtra(EXTRA_PASSWORD);
        ConnectivityHelper.setPreferredNetwork(ConnectivityManager.TYPE_WIFI);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_manual_setup);

        PackageManager pm = getPackageManager();
        boolean permission = pm.checkPermission("android.permission.CAMERA", "com.waylens.hachi") == PackageManager.PERMISSION_GRANTED;

        Logger.t(TAG).d("Permission: " + permission);

        if (permission) {
            mApConnectFragment = ApConnectFragment.newInstance(mWifi, mPassword);
            getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, mApConnectFragment).commit();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }
    }






    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mApConnectFragment = new ApConnectFragment();
                    getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, mApConnectFragment).commit();
                }
                break;
        }
    }

    @Override
    public void setupToolbar() {
        getToolbar().setTitle(R.string.initial_setup);
        getToolbar().setNavigationIcon(R.drawable.navbar_back);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.launch(ManualSetupActivity.this);
            }
        });
        super.setupToolbar();
    }
}
