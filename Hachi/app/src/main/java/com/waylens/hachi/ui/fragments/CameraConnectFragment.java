package com.waylens.hachi.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.transee.viditcam.app.CameraSetupActivity;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.ui.activities.LiveViewActivity;
import com.waylens.hachi.ui.activities.TabSwitchable;
import com.waylens.hachi.ui.adapters.CameraListRvAdapter;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * List available Cameras
 * Created by Richard on 9/9/15.
 */
public class CameraConnectFragment extends BaseFragment implements CameraListRvAdapter.OnCameraActionListener {
    private static final String TAG = CameraConnectFragment.class.getSimpleName();

    @Bind(R.id.wifi_status)
    TextView mWifiStatusView;

    @Bind(R.id.wifi_icon)
    ImageView mWifiIcon;

    @Bind(R.id.rvCameraList)
    RecyclerView mCameraListView;

    @Bind(R.id.connectIndicator)
    ImageView mIvConnectIdicator;

    @Bind(R.id.btnEnterPreview)
    Button mBtnEnterPreview;

    @OnClick(R.id.btnEnterPreview)
    public void onBtnEnterPreviewClicked() {
        LiveViewActivity.launch(getActivity(), mVdtCameraManager.getConnectedCameras().get(0));
    }


    private VdtCameraManager mVdtCameraManager;
    private CameraListRvAdapter mCameraListAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVdtCameraManager = VdtCameraManager.getManager();
        mCameraListAdapter = new CameraListRvAdapter(getActivity(), mVdtCameraManager, this);


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

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraListAdapter.notifyDataSetChanged();
                    }
                });

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
                        toggleCameraConnected(true, false);
                    }
                });


                LiveViewActivity.launch(getActivity(), vdtCamera);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraListAdapter.notifyDataSetChanged();
                    }
                });


            }

            @Override
            public void onCameraDisconnected(VdtCamera vdtCamera) {
                Logger.t(TAG).d("onCameraDisconnected");
                if (getActivity() == null) {
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraListAdapter.notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onCameraStateChanged(VdtCamera vdtCamera) {
                Logger.t(TAG).d("onCameraStateChanged");
                if (getActivity() == null) {
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraListAdapter.notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onWifiListChanged() {
                Logger.t(TAG).d("onWifiListChanged");
                if (getActivity() == null) {
                    return;
                }

                mCameraListAdapter.notifyDataSetChanged();
            }
        });
    }

    private void initViews() {
        // Start connect indicator animation
        boolean conected =  VdtCameraManager.getManager().getConnectedCameras().size() > 0 ? true :
            false;

        toggleCameraConnected(conected, true);

    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_camera_connect, savedInstanceState);
        initViews();
        mCameraListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mCameraListView.setAdapter(mCameraListAdapter);

        return view;
    }

    @OnClick(R.id.wifi_status_container)
    public void launchWifiSetting() {
        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
    }

    @Override
    public void onResume() {
        super.onResume();
        WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context
            .WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        Logger.t(TAG).d("WifiInfo: " + wifiInfo.toString());
        mWifiStatusView.setText(wifiInfo.getSSID());
    }

    @Override
    public void onSetup(VdtCamera camera) {

        CameraSetupActivity.launch(getActivity(), camera);
    }

    @Override
    public void onBrowseVideo(VdtCamera camera) {
        Bundle args = new Bundle();
        args.putBoolean("isPcServer", camera.isPcServer());
        args.putString("ssid", camera.getSSID());
        args.putString("hostString", camera.getHostString());
        ((TabSwitchable) getActivity()).switchTab(0, args);
    }

    @Override
    public void onPreview(VdtCamera camera) {
        //CameraControlActivity.launch(getActivity(), camera);
        LiveViewActivity.launch(getActivity(), camera);
    }


    private void toggleCameraConnected(boolean connected, boolean showPreviewBtn) {
        if (!connected) {
            mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
            AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
            animationDrawable.start();
        } else {
            mIvConnectIdicator.setImageResource(R.drawable.camera_connecting_connection);
            mIvConnectIdicator.setBackgroundResource(android.R.color.transparent);
            if (showPreviewBtn) {
                mBtnEnterPreview.setVisibility(View.VISIBLE);
            }
        }
    }

}
