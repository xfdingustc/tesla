package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.activities.MainActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class BaseFragment extends Fragment {
    protected View mRootView;
    protected MaterialDialog mProgressDialog;

    protected VdtCamera mVdtCamera;
    protected VdbRequestQueue mVdbRequestQueue;


    protected HachiApi mHachi = HachiService.createHachiApiService();


    @Nullable
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @Nullable
    @BindView(R.id.tabs)
    TabLayout mTabLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initVdtCamera();
    }


    @NonNull
    protected View createFragmentView(LayoutInflater inflater, ViewGroup container, @LayoutRes int layoutResId,
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

        return mRootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.cancelAll();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


    protected void initVdtCamera() {
        mVdtCamera = VdtCameraManager.getManager().getCurrentCamera();
        if (mVdtCamera != null) {
            mVdbRequestQueue = mVdtCamera.getRequestQueue();

        }
    }






    public void setupToolbar() {
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(R.drawable.btn_menu);

            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity) getActivity()).showDrawer();
                }
            });
            mToolbar.setTitleTextColor(getResources().getColor(R.color.app_text_color_primary));
        }
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    public TabLayout getTablayout() {
        return mTabLayout;
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

    protected void showMessage(int resId) {
        Snackbar.make(mRootView, resId, Snackbar.LENGTH_SHORT).show();
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
