package com.waylens.hachi.ui.fragments;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ViewAnimator;
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.ui.activities.LiveViewActivity;
import com.waylens.hachi.ui.fragments.camerapreview.CameraPreviewFragment;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * List available Cameras
 * Created by Richard on 9/9/15.
 */
public class CameraConnectFragment extends BaseFragment implements FragmentNavigator {
    private static final String TAG = CameraConnectFragment.class.getSimpleName();
    private VdtCameraManager mVdtCameraManager;

    @Bind(R.id.connectIndicator)
    ImageView mIvConnectIdicator;

    @Bind(R.id.btnEnterPreview)
    Button mBtnEnterPreview;

    @Bind(R.id.vsRoot)
    ViewSwitcher mVsRoot;




    private TabLayout mTabLayout;

    @OnClick(R.id.btnEnterPreview)
    public void onBtnEnterPreviewClicked() {
        LiveViewActivity.launch(getActivity(), mVdtCameraManager.getConnectedCameras().get(0));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context
                .WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        //mBtnWifiInfo.setText(wifiInfo.getSSID());
        getToolbar().getMenu().clear();
        getToolbar().inflateMenu(R.menu.menu_live_view);

        Logger.t(TAG).d("onResume");
        if (mTabLayout != null) {
            mTabLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setTitle(R.string.live_view);
        Logger.t(TAG).d("onStart");
        if (mVdtCameraManager.isConnected()) {
            //LiveViewActivity.launch(getActivity(), mVdtCameraManager.getConnectedCameras().get
            // (0));
            //getActivity().getActionBar().setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Logger.t(TAG).d("onRause");
        //((MainActivity2)getActivity()).switchFragment(MainActivity2.TAB_TAG_SOCIAL);

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_camera_connect, savedInstanceState);
        init();
        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        CameraPreviewFragment fragment = CameraPreviewFragment.newInstance(getCamera());
        getChildFragmentManager().beginTransaction().replace(R.id.cameraPreviewFragmentContainer, fragment).commit();
    }

    @Override
    public void onCameraVdbConnected(VdtCamera camera) {
        super.onCameraVdbConnected(camera);
        if (getActivity() == null || mVsRoot == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toggleCameraConnected();
            }
        });
    }

    private void init() {
        mVdtCameraManager = VdtCameraManager.getManager();

        initViews();
    }

    private void initViews() {
        mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
        AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
        animationDrawable.start();
        toggleCameraConnected();
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mTabLayout = (TabLayout) getActivity().findViewById(R.id.tabs);
    }







    private void toggleCameraConnected() {
        boolean isConnected = mVdtCameraManager.isConnected();
        if (!isConnected) {
            int childIndex = mVsRoot.getDisplayedChild();
            if (childIndex == 1) {
                mVsRoot.showNext();
            }
        } else {
            int childIndex = mVsRoot.getDisplayedChild();
            if (childIndex == 0) {
                mVsRoot.showNext();
            }
            CameraPreviewFragment fragment = CameraPreviewFragment.newInstance(getCamera());
            getChildFragmentManager().beginTransaction().replace(R.id.cameraPreviewFragmentContainer, fragment).commit();
        }
    }


    @Override
    public boolean onInterceptBackPressed() {
        if (getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        return true;
    }

}
