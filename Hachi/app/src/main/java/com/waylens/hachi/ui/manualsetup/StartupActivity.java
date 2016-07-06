package com.waylens.hachi.ui.manualsetup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.MainActivity;


import butterknife.OnClick;
import butterknife.Optional;

/**
 * Created by Xiaofei on 2016/3/18.
 */
public class StartupActivity extends BaseActivity {
    private static final String TAG = StartupActivity.class.getSimpleName();

    private final int PERMISSION_REQUEST_CAMERA = 0;

    private static final int WIFI_SETTING = 0;

    @Optional
    @OnClick(R.id.btnGetStarted)
    public void onBtnGetStartedClicked() {
        if (checkIfCameraIsGranted()) {
            Logger.t(TAG).d("Camera permission is granted");
            ScanQrCodeActivity.launch(StartupActivity.this);
        } else {
            Logger.t(TAG).d("Camera permission is not granted");
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                .customView(R.layout.dialog_get_camera_pemission, true)
                .positiveText(R.string.understand)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        ActivityCompat.requestPermissions(StartupActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                        super.onPositive(dialog);
                    }
                })
                .build();
            dialog.show();
        }

    }

    @OnClick(R.id.manualSelectWifi)
    public void onManualSelectWifiClick() {
        startActivityForResult(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS), WIFI_SETTING);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                if (checkIfCameraIsGranted()) {
                    Logger.t(TAG).d("Grant camera permission successfully");
                    ScanQrCodeActivity.launch(StartupActivity.this);
                } else {
                    Logger.t(TAG).d("Grant camera permission failed");
//                    ManualSetupActivity.launch(StartupActivity.this);
                }
                break;
        }
    }

    @Optional
    @OnClick(R.id.skip)
    public void onSkipClicked() {
        MaterialDialog dialog =  new MaterialDialog.Builder(this)
            .content(R.string.skip_confirm)
            .positiveText(R.string.leave)
            .negativeText(android.R.string.cancel)
            .callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    Intent intent = new Intent(StartupActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    super.onPositive(dialog);
                }

                @Override
                public void onNegative(MaterialDialog dialog) {
                    super.onNegative(dialog);
                }
            })
            .build();
        dialog.show();

    }

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, StartupActivity.class);
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
            Logger.t(TAG).d("Camera permission is granted");

        }
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_startup2);
    }

    private boolean checkIfCameraIsGranted() {
        Camera camera = null;
        boolean ret = true;
        try {
            camera = Camera.open(0);
            ret = true;
        } catch (Exception e) {
            ret = false;
        } finally {
            if (camera != null) {
                camera.release();
            }
        }

        return ret;

    }
}
