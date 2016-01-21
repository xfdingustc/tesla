package com.waylens.hachi.ui.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.android.volley.RequestQueue;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.utils.VolleyUtil;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/7/29.
 */
public class BaseActivity extends AppCompatActivity {

    @Nullable
    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Nullable
    @Bind(R.id.app_bar_layout)
    AppBarLayout mAppBarLayout;

    public Hachi thisApp;

    protected RequestQueue mRequestQueue;

    protected void init() {
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(this);
        mRequestQueue.start();

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisApp = (Hachi) getApplication();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.bind(this);
        setupToolbar();
    }

    protected void setupToolbar() {
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            mToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        }
    }

    @Nullable
    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Nullable
    public AppBarLayout getAppBarLayout() {
        return mAppBarLayout;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }


    static final private String IS_LOCAL = "isLocal";
    static final private String IS_PC_SERVER = "isPcServer";
    static final private String SSID = "ssid";
    static final private String HOST_STRING = "hostString";


    // API

    protected boolean isServerActivity(Bundle bundle) {
        return bundle.getBoolean(IS_PC_SERVER, false);
    }


    protected String getServerAddress(Bundle bundle) {
        return bundle.getString(HOST_STRING);
    }

    protected VdtCamera getCameraFromIntent(Bundle bundle) {
        if (bundle == null) {
            bundle = getIntent().getExtras();
        }
        String ssid = bundle.getString(SSID);
        String hostString = bundle.getString(HOST_STRING);
        if (ssid == null || hostString == null) {
            return null;
        }
        VdtCameraManager vdtCameraManager = VdtCameraManager.getManager();
        VdtCamera vdtCamera = vdtCameraManager.findConnectedCamera(ssid, hostString);
        return vdtCamera;
    }


}
