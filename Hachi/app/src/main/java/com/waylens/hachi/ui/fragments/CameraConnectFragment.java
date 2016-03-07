package com.waylens.hachi.ui.fragments;

import android.content.Context;
import android.content.pm.ActivityInfo;
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

    @Bind(R.id.btnWifiInfo)
    Button mBtnWifiInfo;

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
        mBtnWifiInfo.setText(wifiInfo.getSSID());
        getToolbar().getMenu().clear();
        getToolbar().inflateMenu(R.menu.menu_live_view);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_camera_connect, savedInstanceState);
        init();
        return view;
    }


    private void init() {
        mVdtCameraManager = VdtCameraManager.getManager();
        mVdtCameraManager.addCallback(new VdtCameraManager.Callback() {
            @Override
            public void onCameraConnecting(VdtCamera vdtCamera) {

            }

            @Override
            public void onCameraConnected(VdtCamera vdtCamera) {
                Logger.t(TAG).d("on Camera Connected");
                if (getActivity() == null) {
                    return;
                }

            }

            @Override
            public void onCameraVdbConnected(VdtCamera vdtCamera) {
                Logger.t(TAG).d("camera vdb connected");
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toggleCameraConnected();
                    }
                });

            }

            @Override
            public void onCameraDisconnected(VdtCamera vdtCamera) {
                Logger.t(TAG).d("onCameraDisconnected");
                if (getActivity() == null) {
                    return;
                }
            }

            @Override
            public void onCameraStateChanged(VdtCamera vdtCamera) {
                Logger.t(TAG).d("onCameraStateChanged");
                if (getActivity() == null) {
                    return;
                }
            }

            @Override
            public void onWifiListChanged() {
                Logger.t(TAG).d("onWifiListChanged");
                if (getActivity() == null) {
                    return;
                }

            }
        });
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


    @Override
    public void onStart() {
        super.onStart();
        setTitle(R.string.live_view);
        if (mTabLayout != null) {
            mTabLayout.setVisibility(View.GONE);
        }
        if (mVdtCameraManager.isConnected()) {
            //LiveViewActivity.launch(getActivity(), mVdtCameraManager.getConnectedCameras().get
            // (0));
            //getActivity().getActionBar().setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //((MainActivity2)getActivity()).switchFragment(MainActivity2.TAB_TAG_SOCIAL);
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
