package com.waylens.hachi.ui.manualsetup;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.manualsetup.qrcode.camera.CameraManager;
import com.waylens.hachi.ui.manualsetup.qrcode.decode.DecodeUtils;
import com.waylens.hachi.ui.manualsetup.qrcode.util.BeepManager;
import com.waylens.hachi.ui.manualsetup.qrcode.util.CommonUtils;
import com.waylens.hachi.ui.manualsetup.qrcode.util.InactivityTimer;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.OnClick;


/**
 * Created by Xiaofei on 2016/3/14.
 */
public class ScanQrCodeActivity extends BaseActivity implements SurfaceHolder.Callback {
    private static final String TAG = ScanQrCodeActivity.class.getSimpleName();

    private static final int WIFI_SETTING = 0;

    private String mWifiName;
    private String mWifiPassword;

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;

    private boolean hasSurface;

    private InactivityTimer mInactivityTimer;
    private BeepManager mBeepManager;

    private Rect cropRect;

    private int dataMode = DecodeUtils.DECODE_DATA_MODE_QRCODE;

    private int mQrcodeCropWidth = 0;
    private int mQrcodeCropHeight = 0;
    private int mBarcodeCropWidth = 0;
    private int mBarcodeCropHeight = 0;


    @BindView(R.id.capture_container)
    View captureContainer;

    @BindView(R.id.cropWindow)
    FrameLayout captureCropView;

    @BindView(R.id.surface_view)
    SurfaceView capturePreview;

    @BindView(R.id.scanLine)
    ImageView mScanLine;

//    @BindView(R.id.manualSelectWifi)
//    TextView mManualSelectWifi;

    @OnClick(R.id.how_to_find_camera_wifi)
    public void onHowToFindCameraWifiClicked() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .title("Camera Screen")
            .customView(R.layout.dialog_enter_ap, true)
            .positiveText("OK, Got it!")
            .show();
    }




    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, ScanQrCodeActivity.class);
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

    @Override
    public void onResume() {
        super.onResume();
        cameraManager = new CameraManager(getApplication());

        handler = null;

        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(capturePreview.getHolder());
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            capturePreview.getHolder().addCallback(this);
        }

        mInactivityTimer.onResume();

    }

    @Override
    public void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }

        mBeepManager.close();
        mInactivityTimer.onPause();
        cameraManager.closeDriver();

        if (!hasSurface) {
            capturePreview.getHolder().removeCallback(this);
        }

//        if (null != mScanMaskObjectAnimator && mScanMaskObjectAnimator.isStarted()) {
//            mScanMaskObjectAnimator.cancel();
//        }
        super.onPause();

    }

    @Override
    public void onDestroy() {
        mInactivityTimer.shutdown();
        super.onDestroy();
    }




    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            Logger.t(TAG).e("*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
//            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        initCamera(surfaceHolder);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        hasSurface = false;
    }



    private void initViews() {
        setContentView(R.layout.fragment_qr_code_scan);
        setTranslucentStatus(true);
        hasSurface = false;
        mInactivityTimer = new InactivityTimer(this);
        mBeepManager = new BeepManager(this);


    }


    protected void setTranslucentStatus(boolean on) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
    }

    public void handleDecode(String result, Bundle bundle) {
        mInactivityTimer.onActivity();
        mBeepManager.playBeepSoundAndVibrate();

        if (!CommonUtils.isEmpty(result) && CommonUtils.isUrl(result)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(result));
            startActivity(intent);
        } else {
            Logger.t(TAG).d(result);
//            bundle.putString(ResultActivity.BUNDLE_KEY_SCAN_RESULT, result);
//            readyGo(ResultActivity.class, bundle);
            if (parseWifiInfo(result)) {
                launchApConnectFragment();
            }
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        Logger.t(TAG).d("init camera");
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Logger.t(TAG).w("initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager);
            }

            onCameraPreviewSuccess();
        } catch (IOException ioe) {
            Logger.t(TAG).w(ioe.toString());
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Logger.t(TAG).w("Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }

    }

    private void displayFrameworkBugMessageAndExit() {
//        captureErrorMask.setVisibility(View.VISIBLE);
        final MaterialDialog.Builder builder = new MaterialDialog.Builder(this);
        builder.cancelable(true);
        builder.title(R.string.app_name);
//        builder.content(R.string.tips_open_camera_error);
        builder.positiveText(android.R.string.ok);
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                super.onPositive(dialog);
                finish();
            }
        });
        builder.show();
    }


    private void onCameraPreviewSuccess() {
        Logger.t(TAG).d("camera preview success");
        initCrop();
//        captureErrorMask.setVisibility(View.GONE);

//        ViewHelperjectAnimator.setRepeatMode(ObjectAnimator.RESTART);
//        mScanMaskOb.setPivotX(captureScanMask, 0.0f);
//        ViewHelper.setPivotY(captureScanMask, 0.0f);
//
//        mScanMaskObjectAnimator = ObjectAnimator.ofFloat(captureScanMask, "scaleY", 0.0f, 1.0f);
//        mScanMaskObjectAnimator.setDuration(2000);
//        mScanMaskObjectAnimator.setInterpolator(new DecelerateInterpolator());
//        mScanMaskObjectAnimator.setRepeatCount(-1);
//        mScanMaskObjectAnimator.start();

        TranslateAnimation animation = new TranslateAnimation(0, 0, 0, captureCropView.getMeasuredHeight());
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.setDuration(1200);
        mScanLine.startAnimation(animation);
    }


    public Rect getCropRect() {
//        Logger.t(TAG).d("crop rect: " + cropRect.toString());
        return cropRect;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public int getDataMode() {
        return dataMode;
    }

    public Handler getHandler() {
        return handler;
    }

    private void launchApConnectFragment() {
//        ApConnectFragment fragment = ApConnectFragment.newInstance(mWifiName, mWifiPassword);
//        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();

        ManualSetupActivity.launch(this, mWifiName, mWifiPassword);

    }

    public void initCrop() {
        int cameraWidth = cameraManager.getCameraResolution().y;
        int cameraHeight = cameraManager.getCameraResolution().x;

        int[] location = new int[2];
        captureCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1];

        int cropWidth = captureCropView.getWidth();
        int cropHeight = captureCropView.getHeight();

        int containerWidth = captureContainer.getWidth();
        int containerHeight = captureContainer.getHeight();

        Logger.t(TAG).d("Camera: " + cameraWidth + " x " + cameraHeight + " crop: " + cropWidth + " x " + cropHeight +  " container: " + containerWidth + " x " + containerHeight + " crop location: " + cropLeft + " ~ " + cropTop);

        int x = cropLeft * cameraWidth / containerWidth;
        int y = cropTop * cameraHeight / containerHeight;

        int width = cropWidth * cameraWidth / containerWidth;
        int height = cropHeight * cameraHeight / containerHeight;

        setCropRect(new Rect(x, y, width + x, height + y));
    }

    public void setCropRect(Rect cropRect) {
        this.cropRect = cropRect;
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
