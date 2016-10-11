package com.waylens.hachi.ui.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.RequestQueue;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.VolleyUtil;



import butterknife.BindView;
import butterknife.ButterKnife;

//import com.bugtags.library.Bugtags;

/**
 * Created by Xiaofei on 2015/7/29.
 */
public class BaseActivity extends AppCompatActivity {
    private static final String TAG = BaseActivity.class.getSimpleName();
    static final private String IS_LOCAL = "isLocal";
    static final private String IS_PC_SERVER = "isPcServer";
    static final private String SSID = "ssid";
    static final private String HOST_STRING = "hostString";


    public Hachi thisApp;

    protected RequestQueue mRequestQueue;
    protected VdtCamera mVdtCamera;
    protected VdbRequestQueue mVdbRequestQueue;


    protected MaterialDialog mProgressDialog;


    private boolean mIsImmersive = false;

    @Nullable
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @Nullable
    @BindView(R.id.app_bar_layout)
    AppBarLayout mAppBarLayout;


    protected void init() {
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(this);
        mRequestQueue.start();
        initCamera();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisApp = (Hachi) getApplication();
    }


    @Override
    public void setContentView(int layoutResID) {
        String theme = PreferenceUtils.getString(PreferenceUtils.APP_THEME, "dark");
        if (theme.equals("dark")) {
            Logger.t(TAG).d("dark theme");
            setTheme(R.style.DarkTheme);
        } else {
            Logger.t(TAG).d("light theme");
            setTheme(R.style.LightTheme);
        }
        super.setContentView(layoutResID);
        ButterKnife.bind(this);
        //setupToolbar();
    }

    public void setupToolbar() {
//        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        if (mToolbar != null) {
            mToolbar.setTitleTextColor(getResources().getColor(R.color.app_text_color_primary));
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
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


    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Nullable
    public AppBarLayout getAppBarLayout() {
        return mAppBarLayout;
    }


    protected void initCamera() {
        mVdtCamera = VdtCameraManager.getManager().getCurrentCamera();
//        Logger.t("EnhancementActivity").d("get current camera: " + mVdtCamera);
        if (mVdtCamera != null) {
            mVdbRequestQueue = mVdtCamera.getRequestQueue();//Snipe.newRequestQueue(getActivity(), mVdtCamera);

        }
    }


    public void showDialog(String title) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            return;
        }
        mProgressDialog = new MaterialDialog.Builder(this)
            .title(title)
            .progress(true, 0)
            .progressIndeterminateStyle(true)
            .build();

        mProgressDialog.show();
    }

    public void hideDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }


    public void setImmersiveMode(boolean immersiveMode) {
        if (immersiveMode) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            mIsImmersive = true;
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                0);
            mIsImmersive = false;
        }
    }

    public void hideStatusBar() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }


}
