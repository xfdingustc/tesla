package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.hardware.vdtcamera.CameraState;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.views.camerapreview.CameraLiveView;

import java.net.InetSocketAddress;

import butterknife.Bind;
import butterknife.OnClick;

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

    private boolean mIsRecording = false;

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

    @Bind(R.id.tvCameraStatus)
    TextView mTvCameraStatus;

    @Bind(R.id.fabBookmark)
    FloatingActionButton mFabBookmark;


    @OnClick(R.id.fabBookmark)
    public void onFabClick() {
        if (mIsRecording) {
            addBookmark();
        } else {
            startRecording();
        }
    }




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

            mVdtCamera.startPreview();
            mVdtCamera.getRecordRecMode();
            mVdtCamera.getCameraTime();
            mVdtCamera.getAudioMicState();
            mVdtCamera.getRecordResolutionList();
            mVdtCamera.GetSetup();
            updateRecordState();
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
        CameraState states = mVdtCamera.getState();
        switch (states.mRecordState) {
            default:
            case CameraState.STATE_RECORD_UNKNOWN:
                mTvCameraStatus.setText(R.string.record_unknown);
                break;
            case CameraState.STATE_RECORD_STOPPED:
                mTvCameraStatus.setText(R.string.record_stopped);
                toggleFab(false);
                break;
            case CameraState.STATE_RECORD_STOPPING:
                mTvCameraStatus.setText(R.string.record_stopping);
                break;
            case CameraState.STATE_RECORD_STARTING:
                mTvCameraStatus.setText(R.string.record_starting);
                break;
            case CameraState.STATE_RECORD_RECORDING:
                mTvCameraStatus.setText(R.string.record_recording);
                break;
            case CameraState.STATE_RECORD_SWITCHING:
                mTvCameraStatus.setText(R.string.record_switching);
                break;
        }
    }

    private void toggleFab(boolean isRecording) {
        mIsRecording = isRecording;
        if (mIsRecording) {
            mFabBookmark.setImageResource(R.drawable.ic_bookmark_white_48dp);
        } else {
            mFabBookmark.setImageResource(R.drawable.ic_album_white_48dp);
        }
    }

    private void addBookmark() {

    }

    private void startRecording() {
        mVdtCamera.startRecording();
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
