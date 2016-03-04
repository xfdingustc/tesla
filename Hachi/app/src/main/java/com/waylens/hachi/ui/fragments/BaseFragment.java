package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
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
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.activities.BaseActivity;

import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class BaseFragment extends Fragment {
    protected View mRootView;
    protected MaterialDialog mProgressDialog;

    protected VdtCamera mVdtCamera;
    protected VdbRequestQueue mVdbRequestQueue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVdtCamera = getCamera();
        if (mVdtCamera != null) {
            mVdbRequestQueue = Snipe.newRequestQueue(getActivity(), mVdtCamera);
        } else {
            VdtCameraManager cameraManager = VdtCameraManager.getManager();
            cameraManager.addCallback(new VdtCameraManager.Callback() {
                @Override
                public void onCameraConnecting(VdtCamera vdtCamera) {

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

                }

                @Override
                public void onCameraStateChanged(VdtCamera vdtCamera) {

                }

                @Override
                public void onWifiListChanged() {

                }
            });
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
        return mRootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    public void onCameraVdbConnected(VdtCamera camera) {
        mVdtCamera = camera;
        mVdbRequestQueue = Snipe.newRequestQueue(getActivity(), camera);
    }

    public void showToolbar() {
        Toolbar toolbar = ((BaseActivity) getActivity()).getToolbar();
        if (toolbar != null) {
            toolbar.setVisibility(View.VISIBLE);
        }
    }

    public void hideToolbar() {
        Toolbar toolbar = ((BaseActivity) getActivity()).getToolbar();
        if (toolbar != null) {
            toolbar.setVisibility(View.GONE);
        }
    }

    public void setTitle(String title) {
        Toolbar toolbar = ((BaseActivity) getActivity()).getToolbar();
        if (toolbar == null) {
            return;
        }
        toolbar.setTitle(title);
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

    public ViewPager getViewPager() {
        return null;
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
