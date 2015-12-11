package com.waylens.hachi.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.PopupWindow;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.hardware.VdtCameraManager;
import com.transee.ccam.CameraState;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
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
        thisApp = (Hachi)getApplication();
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

    protected void startCameraActivity(VdtCamera vdtCamera, Class<?> cls, int requestCode) {
        Intent intent = new Intent(this, cls);
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_LOCAL, false);
        bundle.putBoolean(IS_PC_SERVER, vdtCamera.isPcServer());
        bundle.putString(SSID, vdtCamera.getSSID());
        bundle.putString(HOST_STRING, vdtCamera.getHostString());
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
    protected void startCameraActivity(VdtCamera vdtCamera, Class<?> cls) {
        startCameraActivity(vdtCamera, cls, -1);
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


    protected boolean mbRotating;
    public final boolean isRotating() {
        return mbRotating;
    }

    protected int mOrientation;
    // API
    public final boolean isLandscape() {
        return mOrientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    // API
    public final boolean isPortrait() {
        return mOrientation == Configuration.ORIENTATION_PORTRAIT;
    }

    protected PopupWindow mPopupWindow;
    public void setPopupWindow(PopupWindow popupWindow, boolean bHookDismiss) {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
        mPopupWindow = popupWindow;
        if (bHookDismiss) {
            mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    mPopupWindow = null;
                }
            });
        }
    }

    public PopupWindow createPopupWindow(View layout) {
        Drawable background = getResources().getDrawable(R.drawable.new_menu_bg);
        Rect prect = new Rect();
        background.getPadding(prect);

        int width = layout.getMeasuredWidth() + prect.left + prect.right;
        int height = layout.getMeasuredHeight() + prect.top + prect.bottom;

        PopupWindow window = new PopupWindow(layout, width, height, true);

        // window.setWindowLayoutMode(LayoutParams.WRAP_CONTENT,
        // LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(background);
        window.setOutsideTouchable(true);
        // window.setFocusable(true);
        window.setAnimationStyle(android.R.style.Animation_Dialog);
        window.update();

        setPopupWindow(window, true);

        return window;
    }

}
