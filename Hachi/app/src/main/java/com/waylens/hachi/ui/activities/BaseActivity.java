package com.waylens.hachi.ui.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;

import com.android.volley.RequestQueue;
import com.bugtags.library.Bugtags;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.VolleyUtil;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/7/29.
 */
public class BaseActivity extends AppCompatActivity {

    /**
     * Bugtags call
     */
    @Override
    protected void onResume() {
        super.onResume();
        Bugtags.onResume(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        Bugtags.onPause(this);
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Bugtags.onDispatchTouchEvent(this, event);
        return super.dispatchTouchEvent(event);
    }

    /**
     * end Bugtags
     */

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
    protected void onStart() {
        super.onStart();
        setupToolbar();
    }

    @Override
    public void setContentView(int layoutResID) {
        String theme = PreferenceUtils.getString(PreferenceUtils.APP_THEME, "dark");
        if (theme.equals("dark")) {
            setTheme(R.style.DarkTheme);
        } else {
            setTheme(R.style.LightTheme);
        }
        super.setContentView(layoutResID);
        ButterKnife.bind(this);
        //setupToolbar();
    }

    public void setupToolbar() {
        if (mToolbar != null) {
            mToolbar.setTitleTextColor(getResources().getColor(R.color.app_text_color_primary));
        }
    }

    @Override
    public void setTitle(int titleResID) {
        if (mToolbar != null) {
            mToolbar.setTitle(titleResID);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (mToolbar != null) {
            mToolbar.setTitle(title);
        }
    }

    protected void setHomeAsUpIndicator(int indicatorResID) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(indicatorResID);
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
