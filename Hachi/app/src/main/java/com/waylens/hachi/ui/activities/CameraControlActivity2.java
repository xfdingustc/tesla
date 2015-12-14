package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.transee.ccam.AbsCameraClient;
import com.transee.ccam.CameraClient;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.views.camerapreview.CameraLiveView;

import java.net.InetSocketAddress;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2015/12/10.
 */


public class CameraControlActivity2 extends BaseActivity {
    private static final String TAG = CameraControlActivity2.class.getSimpleName();

    private VdtCamera mVdtCamera;
    private static final String IS_PC_SERVER = "isPcServer";
    private static final String SSID = "ssid";
    private static final String HOST_STRING = "hostString";

    private Handler mHandler;

    public static void launch(Activity startingActivity, VdtCamera camera) {
        Intent intent = new Intent(startingActivity, CameraControlActivity2.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_PC_SERVER, camera.isPcServer());
        bundle.putString(SSID, camera.getSSID());
        bundle.putString(HOST_STRING, camera.getHostString());
        intent.putExtras(bundle);
        startingActivity.startActivity(intent);
    }


    @Bind(R.id.cameraPreview)
    CameraLiveView mLiveView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mHandler = new Handler();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_camera_control2);
        setupToolbar();
        initCameraPreview();
    }


    @Override
    protected void setupToolbar() {
        if (mToolbar != null) {
            mToolbar.setTitle(R.string.live_view);
        }
        super.setupToolbar();
    }

    private void initCameraPreview() {
        mVdtCamera = getCameraFromIntent(null);
        mLiveView.setBackgroundColor(Color.BLACK);
        if (mVdtCamera != null) {
            Logger.t(TAG).d("Start preview camera");
            InetSocketAddress serverAddr = mVdtCamera.getPreviewAddress();
            if (serverAddr == null) {
                mVdtCamera = null;
            } else {
                Logger.t(TAG).d("Start preview camera2");
                Logger.t(TAG).d("Start stream serverAddr: " + serverAddr);
                mVdtCamera.setOnStateChangeListener(mOnStateChangeListener);
                mLiveView.startStream(serverAddr, new CameraLiveViewCallback(), true);
            }
            // mCamera.getClient().cmdGetResolution();
            // mCamera.getClient().cmdGetQuality();
            // mCamera.getClient().cmdGetColorMode();

            AbsCameraClient client = mVdtCamera.getClient();
            client.cmd_CAM_WantPreview();
            client.cmd_Rec_get_RecMode();
            client.ack_Cam_get_time();
            client.cmd_audio_getMicState();
            client.cmd_Rec_List_Resolutions(); // see if still capture is supported
            ((CameraClient) client).userCmd_GetSetup();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //startVdbClient();
                }
            }, 1000);
        }

    }

    private final VdtCamera.OnStateChangeListener mOnStateChangeListener = new VdtCamera.OnStateChangeListener() {
        @Override
        public void onStateChanged(VdtCamera vdtCamera) {
            if (vdtCamera == mVdtCamera) {
                updateRecordState();
            }
        }

        @Override
        public void onBtStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onGpsStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onWifiStateChanged(VdtCamera vdtCamera) {

        }
    };

    private void updateRecordState() {

    }

    class CameraLiveViewCallback implements CameraLiveView.Callback {


        @Override
        public void onSingleTapUp() {
            //toggleToolbar();
        }

        @Override
        public void onIoErrorAsync(final int error) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //onMjpegConnectionError(error);
                }
            });
        }

    }

}
