package com.waylens.hachi.ui.fragments;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.ui.activities.LiveViewActivity;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * List available Cameras
 * Created by Richard on 9/9/15.
 */
public class CameraConnectFragment extends BaseFragment {
    private static final String TAG = CameraConnectFragment.class.getSimpleName();
    private VdtCameraManager mVdtCameraManager;


    @Bind(R.id.connectIndicator)
    ImageView mIvConnectIdicator;


    @Bind(R.id.btnEnterPreview)
    Button mBtnEnterPreview;

    @OnClick(R.id.btnEnterPreview)
    public void onBtnEnterPreviewClicked() {
        LiveViewActivity.launch(getActivity(), mVdtCameraManager.getConnectedCameras().get(0));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                        toggleCameraConnected(true);
                    }
                });


//                LiveViewActivity.launch(getActivity(), vdtCamera);

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
    }

    private void initViews() {
        // Start connect indicator animation

        toggleCameraConnected(mVdtCameraManager.isConnected());

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_camera_connect, savedInstanceState);
        initViews();

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context
            .WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

    }


    @Override
    public void onStart() {
        super.onStart();
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




    private void toggleCameraConnected(boolean connected) {
        if (!connected) {
            mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
            AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
            animationDrawable.start();
        } else {
            mIvConnectIdicator.setImageResource(R.drawable.camera_connecting_connection);
            mIvConnectIdicator.setBackgroundResource(android.R.color.transparent);
            if (connected) {
                mBtnEnterPreview.setVisibility(View.VISIBLE);
            }
        }
    }

}
