package com.waylens.hachi.ui.manualsetup;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.MainActivity;


/**
 * Created by Xiaofei on 2016/3/21.
 */
public class ManualSetupActivity extends BaseActivity {
    private static final String TAG = ManualSetupActivity.class.getSimpleName();
    private BroadcastReceiver mWifiStateReceiver;
    private ScanQrCodeFragment mScanQrCodeFragment;

    private final int PERMISSION_REQUEST_CAMERA = 0;


    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, ManualSetupActivity.class);
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
        setContentView(R.layout.activity_manual_setup);
//        checkIfCameraIsGranted();

        PackageManager pm = getPackageManager();
        boolean permission = pm.checkPermission("android.permission.CAMERA", "com.waylens.hachi") == PackageManager.PERMISSION_GRANTED;

        Logger.t(TAG).d("Permission: " + permission);

        if (permission) {
            mScanQrCodeFragment = new ScanQrCodeFragment();
            getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, mScanQrCodeFragment).commit();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mScanQrCodeFragment = new ScanQrCodeFragment();
                    getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, mScanQrCodeFragment).commit();
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