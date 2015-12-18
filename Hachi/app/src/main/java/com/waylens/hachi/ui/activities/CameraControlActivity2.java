package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.text.Layout;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.CameraState;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.views.camerapreview.CameraLiveView;
import com.waylens.hachi.views.dashboard.DashboardLayout;
import com.waylens.hachi.views.dashboard.adapters.SimulatorRawDataAdapter;
import com.xfdingustc.far.FixedAspectRatioFrameLayout;

import java.net.InetSocketAddress;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/12/10.
 */


public class CameraControlActivity2 extends BaseActivity {
    private static final String TAG = CameraControlActivity2.class.getSimpleName();

    private VdtCamera mVdtCamera;
    private SimulatorRawDataAdapter mRawDataAdapter;

    private static final String IS_PC_SERVER = "isPcServer";
    private static final String SSID = "ssid";
    private static final String HOST_STRING = "hostString";


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

    @Bind(R.id.btnMicControl)
    ImageButton mBtnMicControl;

    @Bind(R.id.fabBookmark)
    FloatingActionButton mFabBookmark;

    @Bind(R.id.dashboard)
    DashboardLayout mDashboard;


    @Bind(R.id.liveViewLayout)
    FixedAspectRatioFrameLayout mLiveViewLayout;

    @Bind(R.id.btnShowOverlay)
    ImageButton mBtnShowOverlay;


    @OnClick(R.id.fabBookmark)
    public void onFabClick() {
        handleOnFabClicked();
    }

    @OnClick(R.id.btnShowOverlay)
    public void onBtnShowOverlayClick() {
        if (mDashboard.getVisibility() == View.VISIBLE) {
            mDashboard.setVisibility(View.INVISIBLE);
            mBtnShowOverlay.clearColorFilter();
        } else {
            mDashboard.setVisibility(View.VISIBLE);
            initDashboardLayout();
            mBtnShowOverlay.setColorFilter(getResources().getColor(R.color.style_color_primary));
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void init() {
        super.init();

        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_camera_control2);
        setupToolbar();
        initCameraPreview();


        updateMicControlButton();
        mRawDataAdapter = new SimulatorRawDataAdapter();
        mDashboard.setAdapter(mRawDataAdapter);

        startUpdateThread();
    }

    private void startUpdateThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    mDashboard.update(-1);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();
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
            InetSocketAddress serverAddr = mVdtCamera.getPreviewAddress();
            if (serverAddr == null) {
                mVdtCamera = null;
            } else {
                mVdtCamera.setOnStateChangeListener(mOnStateChangeListener);
                mLiveView.startStream(serverAddr, new CameraLiveViewCallback(), true);
            }

            mVdtCamera.startPreview();
            mVdtCamera.getRecordRecMode();
            mVdtCamera.getCameraTime();
            mVdtCamera.getAudioMicState();
            mVdtCamera.getRecordResolutionList();
            mVdtCamera.GetSetup();
            updateCameraState(mVdtCamera.getState());

        }

    }

    private void initDashboardLayout() {
        int width = mLiveViewLayout.getMeasuredWidth();
        int height = mLiveViewLayout.getMeasuredHeight();
        float widthScale = (float)width / DashboardLayout.NORMAL_WIDTH;
        float heightScale = (float)height / DashboardLayout.NORMAL_HEIGHT;
        mDashboard.setScaleX(widthScale);
        mDashboard.setScaleY(heightScale);
    }


    private void updateCameraState(CameraState state) {
        updateCameraStatusInfo(state);
        updateFloatActionButton(state);
    }


    private final VdtCamera.OnStateChangeListener mOnStateChangeListener = new VdtCamera.OnStateChangeListener() {
        @Override
        public void onStateChanged(VdtCamera vdtCamera) {
            updateCameraState(vdtCamera.getState());
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

    private void updateCameraStatusInfo(CameraState state) {
        switch (state.getRecordState()) {
            case CameraState.STATE_RECORD_UNKNOWN:
                mTvCameraStatus.setText(R.string.record_unknown);
                break;
            case CameraState.STATE_RECORD_STOPPED:
                mTvCameraStatus.setText(R.string.record_stopped);
                break;
            case CameraState.STATE_RECORD_STOPPING:
                mTvCameraStatus.setText(R.string.record_stopping);
                break;
            case CameraState.STATE_RECORD_STARTING:
                mTvCameraStatus.setText(R.string.record_starting);
                break;
            case CameraState.STATE_RECORD_RECORDING:
                //int recordMode = mVdtCamera.getRecordRecMode();
                mTvCameraStatus.setText(R.string.record_recording);
                break;
            case CameraState.STATE_RECORD_SWITCHING:
                mTvCameraStatus.setText(R.string.record_switching);
                break;
            default:
                break;
        }
    }

    private void updateFloatActionButton(CameraState state) {
        switch (state.getRecordState()) {
            case CameraState.STATE_RECORD_UNKNOWN:
                break;
            case CameraState.STATE_RECORD_STOPPED:
                mFabBookmark.setEnabled(true);
                mFabBookmark.setImageResource(R.drawable.ic_album_white_48dp);
                break;
            case CameraState.STATE_RECORD_STOPPING:
                mFabBookmark.setEnabled(false);
                break;
            case CameraState.STATE_RECORD_STARTING:
                mFabBookmark.setEnabled(false);
                break;
            case CameraState.STATE_RECORD_RECORDING:
                mFabBookmark.setEnabled(true);
                mFabBookmark.setImageResource(R.drawable.ic_stop_white_48dp);
                break;
            case CameraState.STATE_RECORD_SWITCHING:
                mFabBookmark.setEnabled(false);
                break;
            default:
                break;
        }

    }

    private void updateMicControlButton() {
        boolean micEnabled = mVdtCamera.isMicEnabled();
        if (micEnabled) {
            mBtnMicControl.setColorFilter(getResources().getColor(R.color.style_color_primary));
        } else {
            mBtnMicControl.clearColorFilter();
        }
    }


    private void handleOnFabClicked() {
        switch (mVdtCamera.getState().getRecordState()) {
            case CameraState.STATE_RECORD_RECORDING:
                mVdtCamera.stopRecording();
                break;
            case CameraState.STATE_RECORD_STOPPED:
                mVdtCamera.startRecording();
                break;
        }
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
