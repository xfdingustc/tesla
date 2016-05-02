package com.waylens.hachi.ui.liveview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.bigkoo.pickerview.OptionsPickerView;
import com.waylens.hachi.R;

import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.ui.activities.BaseActivity;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/1/8.
 */
public class LiveViewSettingActivity extends BaseActivity {

    private static VdtCamera mSharedCamera;
    private VdtCamera mCamera;

    private int mOriginRecordMode;
    private int mOriginVideoResolution;
    private int mOriginVideoFramerate;

    private int mChangedRecordMode;
    private int mChangedVideoResolution;
    private int mChangedVideoFramerate;

    private OptionsPickerView mQualityPickerView;

    private ArrayList<String> mResolutionList = new ArrayList<>();
    private ArrayList<ArrayList<String>> mFrameRateList = new ArrayList<>();



    public static void launch(Activity startActivity, VdtCamera camera) {
        Intent intent = new Intent(startActivity, LiveViewSettingActivity.class);
        mSharedCamera = camera;
        startActivity.startActivity(intent);
    }


//    @Bind(R.id.btnContinuous)
//    RadioButton mBtnContinuous;
//
//    @Bind(R.id.btnManual)
//    RadioButton mBtnManual;

//    @Bind(R.id.tvRecordModeInfo)
//    TextView mTvRecordModeInfo;

    @Bind(R.id.resolution_framerate)
    TextView mResolutionFramerate;


//    @OnClick(R.id.btnContinuous)
//    public void onBtnContinuousClicked() {
//        mTvRecordModeInfo.setText(getText(R.string.continuous_info));
//        mChangedRecordMode |= VdtCamera.FLAG_LOOP_RECORD;
//        mChangedRecordMode |= VdtCamera.FLAG_AUTO_RECORD;
//        checkIfChanged();
//    }
//
//    @OnClick(R.id.btnManual)
//    public void onBtnManualClicked() {
//        mTvRecordModeInfo.setText(getText(R.string.manual_info));
//        mChangedRecordMode &= ~VdtCamera.FLAG_LOOP_RECORD;
//        mChangedRecordMode &= ~VdtCamera.FLAG_AUTO_RECORD;
//        checkIfChanged();
//    }

    @OnClick(R.id.resolution_framerate)
    public void onResolutionFramerateClicked() {
        mQualityPickerView.show();
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
        mOriginRecordMode = mCamera.getRecordMode();
        mOriginVideoResolution = mChangedVideoResolution = mCamera.getVideoResolution();
        mOriginVideoFramerate = mChangedVideoFramerate = mCamera.getVideoFramerate();
        mChangedRecordMode = mOriginRecordMode;
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_live_view_setting);
//        updateRecordMode();
        updateRecordQuality();
        initRecordQualityOptionPickerView();
    }




    @Override
    public void setupToolbar() {
        getToolbar().setTitle(R.string.recording_setting);
        getToolbar().setNavigationIcon(R.drawable.navbar_close);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        super.setupToolbar();
    }

//    private void updateRecordMode() {
//        int recordMode = mCamera.getRecordMode();
//        if ((recordMode & VdtCamera.FLAG_LOOP_RECORD) > 0) {
//            mBtnContinuous.setChecked(true);
//            mTvRecordModeInfo.setText(getText(R.string.continuous_info));
//        } else {
//            mBtnManual.setChecked(true);
//            mTvRecordModeInfo.setText(getText(R.string.manual_info));
//        }
//    }

    private void updateRecordQuality() {
        String resolution = null;
        String frameRate;

        if (mChangedVideoResolution == VdtCamera.VIDEO_RESOLUTION_1080P) {
            resolution = "1080P";
        } else if (mChangedVideoResolution == VdtCamera.VIDEO_RESOLUTION_720P) {
            resolution = "720P";
        }


        if (mChangedVideoFramerate == VdtCamera.VIDEO_FRAMERATE_30FPS) {
            frameRate = "30fps";
        } else if (mChangedVideoFramerate == VdtCamera.VIDEO_FRAMERATE_60FPS) {
            frameRate = "60fps";
        } else {
            frameRate = "120fps";
        }


        mResolutionFramerate.setText(resolution + "/" + frameRate);

    }

    private void initRecordQualityOptionPickerView() {
        mQualityPickerView = new OptionsPickerView(this);

        mResolutionList.add("1080P");
        mResolutionList.add("720P");

        ArrayList<String> framerateItem_01 = new ArrayList<>();
        framerateItem_01.add("30fps");
        framerateItem_01.add("60fps");
        framerateItem_01.add("120fps");

        ArrayList<String> framerateItem_02 = new ArrayList<>();
        framerateItem_02.add("30fps");
        framerateItem_02.add("60fps");
        framerateItem_02.add("120fps");

        mFrameRateList.add(framerateItem_01);
        mFrameRateList.add(framerateItem_02);

        mQualityPickerView.setPicker(mResolutionList, mFrameRateList, true);
        mQualityPickerView.setCyclic(false, false, false);

        mQualityPickerView.setOnoptionsSelectListener(new OptionsPickerView.OnOptionsSelectListener() {
            @Override
            public void onOptionsSelect(int options1, int option2, int options3) {
                if (options1 == 0) {
                    mChangedVideoResolution = VdtCamera.VIDEO_RESOLUTION_1080P;
                } else {
                    mChangedVideoResolution = VdtCamera.VIDEO_RESOLUTION_720P;
                }

                if (option2 == 0) {
                    mChangedVideoFramerate = VdtCamera.VIDEO_FRAMERATE_30FPS;
                } else if (option2 == 1) {
                    mChangedVideoFramerate = VdtCamera.VIDEO_FRAMERATE_60FPS;
                } else {
                    mChangedVideoFramerate = VdtCamera.VIDEO_FRAMERATE_120FPS;
                }

                updateRecordQuality();
                checkIfChanged();
            }
        });
    }

    private void checkIfChanged() {
        boolean changed = false;
        if (mOriginVideoFramerate != mChangedVideoFramerate
            || mOriginVideoResolution != mChangedVideoResolution
            || mOriginRecordMode != mChangedRecordMode) {
            changed = true;
        }

        getToolbar().getMenu().clear();
        if (changed) {
            getToolbar().inflateMenu(R.menu.recording_setting);
            getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.apply:
                            doApplyChanges();
                            finish();
                            break;
                    }
                    return false;
                }
            });
        } else {

        }
    }

    // TODO: We need finish this logic
    private void doApplyChanges() {
        if (mOriginRecordMode != mChangedRecordMode) {
            mCamera.setRecordRecMode(mChangedRecordMode);

            if (mChangedRecordMode == VdtCamera.REC_MODE_AUTOSTART_LOOP) {
                mCamera.startRecording();
            } else if (mChangedRecordMode == VdtCamera.REC_MODE_MANUAL) {
                mCamera.stopRecording();
            }


        }

        if (mOriginVideoResolution != mChangedVideoResolution || mOriginVideoFramerate != mChangedVideoFramerate) {
            mCamera.setVideoResolution(mChangedVideoResolution, mChangedVideoFramerate);
        }

        if (mOriginVideoFramerate != mChangedVideoFramerate) {
//            mCamera.setV
        }
    }
}
