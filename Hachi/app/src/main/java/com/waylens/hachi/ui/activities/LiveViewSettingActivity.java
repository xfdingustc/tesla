package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.CameraState;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;

import butterknife.Bind;
import butterknife.OnClick;
import info.hoang8f.android.segmented.SegmentedGroup;

/**
 * Created by Xiaofei on 2016/1/8.
 */
public class LiveViewSettingActivity extends BaseActivity {

    private static VdtCamera mSharedCamera;
    private static VdtCamera mCamera;

    private int mOriginRecordMode;
    private int mOriginVideoResolution;
    private int mOriginVideoFramerate;

    private int mChangedRecordMode;
    private int mChangedVideoResolution;
    private int mChangedVideoFramerate;



    public static void launch(Activity startActivity, VdtCamera camera) {
        Intent intent = new Intent(startActivity, LiveViewSettingActivity.class);
        mSharedCamera = camera;
        startActivity.startActivity(intent);
    }


    @Bind(R.id.btnContinuous)
    RadioButton mBtnContinuous;

    @Bind(R.id.btnManual)
    RadioButton mBtnManual;

    @Bind(R.id.tvRecordModeInfo)
    TextView mTvRecordModeInfo;

    @Bind(R.id.btn720p)
    RadioButton mBtn720p;

    @Bind(R.id.btn1080p)
    RadioButton mBtn1080p;

    @Bind(R.id.btn30fps)
    RadioButton mBtn30fps;

    @Bind(R.id.btn60fps)
    RadioButton mBtn60fps;

    @Bind(R.id.btn120fps)
    RadioButton mBtn120fps;

    @Bind(R.id.btnOk)
    Button mBtnOk;

    @OnClick(R.id.btnContinuous)
    public void onBtnContinuousClicked() {
        mTvRecordModeInfo.setText(getText(R.string.continuous_info));
        mChangedRecordMode |= CameraState.FLAG_LOOP_RECORD;
        checkIfChanged();
    }

    @OnClick(R.id.btnManual)
    public void onBtnManualClicked() {
        mTvRecordModeInfo.setText(getText(R.string.manual_info));
        mChangedRecordMode &= ~CameraState.FLAG_LOOP_RECORD;
        checkIfChanged();
    }

    @OnClick(R.id.btn720p)
    public void onBtn720pClicked() {
        mChangedVideoResolution = CameraState.VIDEO_RESOLUTION_720P;
        checkIfChanged();
    }

    @OnClick(R.id.btn1080p)
    public void onBtn1080pClicked() {
        mChangedVideoResolution = CameraState.VIDEO_RESOLUTION_1080P;
        checkIfChanged();
    }

    @OnClick(R.id.btn30fps)
    public void onBtn30fpsClicked() {
        mChangedVideoFramerate = CameraState.VIDEO_FRAMERATE_30FPS;
        checkIfChanged();
    }

    @OnClick(R.id.btn60fps)
    public void onBtn60fpsClicked() {
        mChangedVideoFramerate = CameraState.VIDEO_FRAMERATE_60FPS;
        checkIfChanged();
    }

    @OnClick(R.id.btn120fps)
    public void onBtn120fpsClicked() {
        mChangedVideoFramerate = CameraState.VIDEO_FRAMERATE_120FPS;
        checkIfChanged();
    }

    @OnClick(R.id.btnCancel)
    public void onBtnCancelClicked() {
        finish();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mCamera = mSharedCamera;
        mOriginRecordMode = mCamera.getState().getRecordMode();
        mOriginVideoResolution = mCamera.getState().getVideoResolution();
        mOriginVideoFramerate = mCamera.getState().getVideoFramerate();
        mChangedRecordMode = mOriginRecordMode;
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_live_view_setting);
        updateRecordMode();
        updateRecordQuality();


    }



    private void updateRecordMode() {
        int recordMode = mCamera.getState().getRecordMode();
        if ((recordMode & CameraState.FLAG_LOOP_RECORD) > 0) {
            mBtnContinuous.setChecked(true);
            mTvRecordModeInfo.setText(getText(R.string.continuous_info));
        } else {
            mBtnManual.setChecked(true);
            mTvRecordModeInfo.setText(getText(R.string.manual_info));
        }
    }

    private void updateRecordQuality() {
        int quality = mCamera.getState().getVideoResolution();
        if (quality == CameraState.VIDEO_RESOLUTION_1080P) {
            mBtn1080p.setChecked(true);
        } else {
            mBtn720p.setChecked(true);
        }

        int fps = mCamera.getState().getVideoFramerate();
        if (fps == CameraState.VIDEO_FRAMERATE_30FPS) {
            mBtn30fps.setChecked(true);
        } else if (fps == CameraState.VIDEO_FRAMERATE_60FPS) {
            mBtn60fps.setChecked(true);
        } else {
            mBtn120fps.setChecked(true);
        }

    }

    private void checkIfChanged() {
        boolean changed = false;
        if (mOriginVideoFramerate != mChangedVideoFramerate
            || mOriginVideoResolution != mChangedVideoResolution
            || mOriginRecordMode != mChangedRecordMode) {
            changed = true;
        }

        if (changed) {
            mBtnOk.setEnabled(true);
        } else {
            mBtnOk.setEnabled(false);
        }
    }
}
