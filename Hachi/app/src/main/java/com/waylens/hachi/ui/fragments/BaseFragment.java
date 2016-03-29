package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.MainActivity;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class BaseFragment extends Fragment  {
    protected View mRootView;
    protected MaterialDialog mProgressDialog;

    protected VdtCamera mVdtCamera;
    protected VdbRequestQueue mVdbRequestQueue;
    protected VdbImageLoader mVdbImageLoader;

    @Nullable
    @Bind(R.id.toolbar)
    public Toolbar mToolbar;

    @Nullable
    @Bind(R.id.tabs)
    TabLayout mTabLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVdtCamera = getCamera();
        if (mVdtCamera != null) {
            mVdbRequestQueue = Snipe.newRequestQueue(getActivity(), mVdtCamera);
            mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
        }
    }

    @NonNull
    protected View createFragmentView(LayoutInflater inflater, ViewGroup container, int layoutResId,
                                      Bundle savedInstanceState) {
        if (mRootView != null) {
            ViewGroup parent = (ViewGroup) mRootView.getParent();
            if (parent != null) {
                parent.removeView(mRootView);
            }
        } else {
            mRootView = inflater.inflate(layoutResId, container, false);

        }
        ButterKnife.bind(this, mRootView);
        setupToolbar();

        VdtCameraManager cameraManager = VdtCameraManager.getManager();
        cameraManager.addCallback(mVdtCameraMangerCallback);

        return mRootView;
    }

    @Override
    public void onDestroyView() {
        VdtCameraManager cameraManager = VdtCameraManager.getManager();
        cameraManager.removeCallback(mVdtCameraMangerCallback);
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
    
    private VdtCameraManager.Callback mVdtCameraMangerCallback = new VdtCameraManager.Callback() {
        @Override
        public void onCameraConnecting(VdtCamera vdtCamera) {
            BaseFragment.this.onCameraConnecting(vdtCamera);
        }

        @Override
        public void onCameraConnected(VdtCamera vdtCamera) {

        }

        @Override
        public void onCameraVdbConnected(VdtCamera vdtCamera) {
            BaseFragment.this.onCameraVdbConnected(vdtCamera);
        }

        @Override
        public void onCameraDisconnected(VdtCamera vdtCamera) {
            BaseFragment.this.onCameraDisconnected(vdtCamera);
        }

        @Override
        public void onCameraStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onWifiListChanged() {

        }
    };

    protected  void onCameraConnecting(VdtCamera vdtCamera) {

    }

    protected void onCameraDisconnected(VdtCamera vdtCamera) {
        //
    }


    public void onCameraVdbConnected(VdtCamera camera) {
        mVdtCamera = camera;
        mVdbRequestQueue = Snipe.newRequestQueue(getActivity(), camera);
    }


    public void setupToolbar() {
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(R.drawable.ic_menu_grey600_24dp);

            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity)getActivity()).showDrawer();
                }
            });
            mToolbar.setTitleTextColor(getResources().getColor(R.color.app_text_color_primary));
        }
    }



    public void showDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            return;
        }
        mProgressDialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.loading)
                .progress(true, 0)
                .progressIndeterminateStyle(false)
                .build();

        mProgressDialog.show();
    }

    public void hideDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    void showMessage(int resId) {
        //Should not call this method if UI has been already destroyed.
        try {
            Snackbar.make(mRootView, resId, Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("test", "", e);
        }
    }

    void showMessage(String message) {
        Snackbar.make(mRootView, message, Snackbar.LENGTH_SHORT).show();
    }


    protected VdtCamera getCamera() {
        Bundle args = getArguments();
        VdtCamera camera = null;

        VdtCameraManager vdtCameraManager = VdtCameraManager.getManager();
        if (args != null) {
            String ssid = args.getString("ssid");
            String hostString = args.getString("hostString");
            if (ssid != null && hostString != null) {
                camera = vdtCameraManager.findConnectedCamera(ssid, hostString);
            } else if (vdtCameraManager.getConnectedCameras().size() > 0) {
                camera = vdtCameraManager.getConnectedCameras().get(0);
            }
        } else {
            if (vdtCameraManager.getConnectedCameras().size() > 0) {
                camera = vdtCameraManager.getConnectedCameras().get(0);
            }
        }
        return camera;
    }



}
