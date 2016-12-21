package com.waylens.hachi.ui.manualsetup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Window;
import android.view.WindowManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.rxqrcode.RxQrCode;
import com.google.zxing.Result;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * Created by Xiaofei on 2016/12/21.
 */

public class ScanQrCodeActivity2 extends BaseActivity implements CameraCompat.ErrorHandler {
    private Subject<Result, Result> mScanResult = PublishSubject.<Result>create().toSerialized();
    private Subscription mScanResultSubscription;
    private Subscription mPreviewSubscription;

    private String mWifiName;
    private String mWifiPassword;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, ScanQrCodeActivity2.class);
        activity.startActivity(intent);
    }


    @OnClick(R.id.how_to_find_camera_wifi)
    public void onHowToFindCameraWifiClicked() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .title("Camera Screen")
            .customView(R.layout.dialog_enter_ap, true)
            .positiveText("OK, Got it!")
            .show();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        mPreviewSubscription = RxQrCode.scanFromCamera(savedInstanceState, getSupportFragmentManager(), R.id.preview_container, this)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(mScanResult);
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    @Override
    public void onError(@CameraCompat.ErrorCode int code) {

    }

    private void initViews() {
        setContentView(R.layout.activity_scan_qr_code);
        setTranslucentStatus(true);
        observeScanResult();
    }

    protected void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreviewSubscription != null && !mPreviewSubscription.isUnsubscribed()) {
            mPreviewSubscription.unsubscribe();
            mPreviewSubscription = null;
        }
        stopObserveScanResult();
    }

    private void observeScanResult() {
        mScanResultSubscription = mScanResult.subscribe(new Action1<Result>() {
            @Override
            public void call(Result result) {
                stopObserveScanResult();

                if (parseWifiInfo(result.getText())) {
                    ManualSetupActivity.launch(ScanQrCodeActivity2.this, mWifiName, mWifiPassword);
                }
            }
        });
    }

    private void stopObserveScanResult() {
        if (mScanResultSubscription != null && !mScanResultSubscription.isUnsubscribed()) {
            mScanResultSubscription.unsubscribe();
            mScanResultSubscription = null;
        }
    }

    private boolean parseWifiInfo(String result) {
        Pattern pattern = Pattern.compile("<a>(\\w*)</a>.*<p>(\\w*)</?p>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

        Matcher matcher = pattern.matcher(result);
        if (matcher.find() && matcher.groupCount() == 2) {
            mWifiName = matcher.group(1);
            mWifiPassword = matcher.group(2);

            return true;
        }
        return false;
    }

}
