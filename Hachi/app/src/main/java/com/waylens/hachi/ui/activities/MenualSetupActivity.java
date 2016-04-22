package com.waylens.hachi.ui.activities;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.menualsetup.ScanQrCodeFragment;

/**
 * Created by Xiaofei on 2016/3/21.
 */
public class MenualSetupActivity extends BaseActivity {
    private static final String TAG = MenualSetupActivity.class.getSimpleName();
    private BroadcastReceiver mWifiStateReceiver;
    private ScanQrCodeFragment mScanQrCodeFragment;

    private final int PERMISSION_REQUEST_CAMERA = 0;


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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mScanQrCodeFragment = new ScanQrCodeFragment();
            getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, mScanQrCodeFragment).commit();
        } else {
//            MaterialDialog dialog = new MaterialDialog.Builder(this)
//                    .title(R.string.grant_camera_permission)
//                    .positiveText(R.string.grant)
//                    .negativeText(R.string.skip)
//                    .callback(new MaterialDialog.ButtonCallback() {
//                        @Override
//                        public void onPositive(MaterialDialog dialog) {
//                            super.onPositive(dialog);
//                        }
//                    })
//                    .build();
//            dialog.show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }
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
