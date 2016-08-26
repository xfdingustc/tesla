package com.waylens.hachi.ui.liveview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.xfdingustc.snipe.control.VdtCamera;
import com.xfdingustc.snipe.control.VdtCameraManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

/**
 * Created by Xiaofei on 2016/1/8.
 */
public class LiveViewSettingActivity extends BaseActivity {


    private VdtCamera mCamera;

    private int mOriginRecordMode;
    private int mOriginVideoResolution;
    private int mOriginVideoFramerate;
    private int mOriginOverlayState;

    private int mChangedRecordMode;
    private int mChangedVideoResolution;
    private int mChangedVideoFramerate;
    private int mChangedOverlayState;

//    private OptionsPickerView mQualityPickerView;

    private ArrayList<String> mResolutionList = new ArrayList<>();
    private ArrayList<ArrayList<String>> mFrameRateList = new ArrayList<>();


    public static void launch(Activity startActivity) {
        Intent intent = new Intent(startActivity, LiveViewSettingActivity.class);
        startActivity.startActivity(intent);
    }


    @Subscribe
    public void onEventVideoSettingChange(VideoSettingChangEvent event) {
        switch (event.getWhat()) {
            case VideoSettingChangEvent.WHAT_FRAMERATE:
                mChangedVideoFramerate = event.getValue();
                break;
            case VideoSettingChangEvent.WHAT_RESOLUTION:
                mChangedVideoResolution = event.getValue();
                break;
            case VideoSettingChangEvent.WHAT_TIMESTAMP:
                mChangedOverlayState = event.getValue();
            default:
                break;
        }
        checkIfChanged();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void init() {
        super.init();
        mCamera = VdtCameraManager.getManager().getCurrentCamera();
        mOriginRecordMode = mCamera.getRecordMode();
        mOriginVideoResolution = mChangedVideoResolution = mCamera.getVideoResolution();
        mOriginVideoFramerate = mChangedVideoFramerate = mCamera.getVideoFramerate();
        mOriginOverlayState = mChangedOverlayState = mCamera.getOverlayState();
        mChangedRecordMode = mOriginRecordMode;
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_live_view_setting);
        setupToolbar();
        LiveViewSettingFragment fragment = new LiveViewSettingFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
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


    private void checkIfChanged() {
        boolean changed = false;
        if (mOriginVideoFramerate != mChangedVideoFramerate
            || mOriginVideoResolution != mChangedVideoResolution
            || mOriginRecordMode != mChangedRecordMode
            || mOriginOverlayState != mChangedOverlayState) {
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
                            MaterialDialog dialog = new MaterialDialog.Builder(LiveViewSettingActivity.this)
                                .title(R.string.change_camera_setting)
                                .content(R.string.change_camera_setting_hint)
                                .positiveText(R.string.ok)
                                .negativeText(R.string.cancel)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        doApplyChanges();
                                        finish();
                                    }
                                }).show();

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
            mCamera.stopRecording();
        }

        if (mOriginVideoFramerate != mChangedVideoFramerate) {
//            mCamera.setV
        }

        if (mOriginOverlayState != mChangedOverlayState) {
            mCamera.setOverlayState(mChangedOverlayState);
            mCamera.getOverlayState();
        }
        finish();
    }
}
