package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

    @OnClick(R.id.btnContinuous)
    public void onBtnContinuousClicked() {
        mTvRecordModeInfo.setText(getText(R.string.continuous_info));
    }

    @OnClick(R.id.btnManual)
    public void onBtnManualClicked() {
        mTvRecordModeInfo.setText(getText(R.string.manual_info));
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
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_live_view_setting);
        updateRecordMode();

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
}
