package com.waylens.hachi.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.transee.ccam.Camera;
import com.transee.ccam.CameraState;
import com.waylens.hachi.R;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/7/29.
 */
public class BaseActivity extends AppCompatActivity {

    @Nullable
    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    protected RequestQueue mRequestQueue;

    protected void init() {
        mRequestQueue = Volley.newRequestQueue(this);
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }


    static final private String IS_LOCAL = "isLocal";
    static final private String IS_PC_SERVER = "isPcServer";
    static final private String SSID = "ssid";
    static final private String HOST_STRING = "hostString";

    protected void startCameraActivity(Camera camera, Class<?> cls, int requestCode) {
        Intent intent = new Intent(this, cls);
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_LOCAL, false);
        bundle.putBoolean(IS_PC_SERVER, camera.isPcServer());
        bundle.putString(SSID, camera.getSSID());
        bundle.putString(HOST_STRING, camera.getHostString());
        if (requestCode < 0) {
            intent.putExtras(bundle);
            startActivity(intent);
        } else {
            bundle.putBoolean("requested", true);
            intent.putExtras(bundle);
            startActivityForResult(intent, requestCode);
        }
    }

    // API
    protected void startCameraActivity(Camera camera, Class<?> cls) {
        startCameraActivity(camera, cls, -1);
    }


    protected void startLocalActivity(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_LOCAL, true);
        intent.putExtras(bundle);
        startActivity(intent);
    }


    protected String getCameraName(CameraState states) {
        if (states.mCameraName.length() == 0) {
            return getResources().getString(R.string.lable_camera_noname);
        } else {
            return states.mCameraName;
        }
    }

}
