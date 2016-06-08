package com.waylens.hachi.ui.liveview;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.ui.activities.BaseActivity;

import butterknife.BindView;


/**
 * Created by Xiaofei on 2016/3/22.
 */
public class LiveViewActivity extends BaseActivity {
    private boolean mIsGaugeVisible;

    private static final String IS_PC_SERVER = "isPcServer";
    private static final String SSID = "ssid";
    private static final String HOST_STRING = "hostString";
    public static final String EXTRA_IS_GAUGE_VISIBLE = "extra.is.gauge.visible";
    public static final String ACTION_IS_GAUGE_VISIBLE = "broadcast.action.is.gauge.visible";


    @BindView(R.id.fragmentContainer)
    FrameLayout mFragmentContainer;

    public static void launch(Activity startingActivity, VdtCamera camera, boolean isGaugeVisible) {
        Intent intent = new Intent(startingActivity, LiveViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_PC_SERVER, camera.isPcServer());
        bundle.putString(SSID, camera.getSSID());
        bundle.putString(HOST_STRING, camera.getHostString());
        bundle.putBoolean(EXTRA_IS_GAUGE_VISIBLE, isGaugeVisible);
        intent.putExtras(bundle);
        startingActivity.startActivity(intent);
    }



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mIsGaugeVisible = intent.getBooleanExtra(EXTRA_IS_GAUGE_VISIBLE, false);
        initViews();
        setImmersiveMode(true);
    }

    private void initViews() {

        setContentView(R.layout.activity_live_view);
        VdtCamera vdtCamera = getCameraFromIntent(getIntent().getExtras());
        CameraPreviewFragment fragment = CameraPreviewFragment.newInstance(vdtCamera, mIsGaugeVisible);
        getFragmentManager().beginTransaction().add(R.id.fragmentContainer, fragment).commit();
    }


}
