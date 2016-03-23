package com.waylens.hachi.ui.fragments.camerapreview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.CameraState;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.hardware.vdtcamera.WifiState;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipInfoMsgHandler;
import com.waylens.hachi.snipe.toolbox.LiveRawDataRequest;
import com.waylens.hachi.snipe.toolbox.MarkLiveMsgHandler;
import com.waylens.hachi.ui.activities.LiveViewActivity;
import com.waylens.hachi.ui.activities.LiveViewSettingActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.views.camerapreview.CameraLiveView;
import com.waylens.hachi.vdb.ClipActionInfo;

import java.net.InetSocketAddress;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/3/7.
 */
public class CameraPreviewFragment extends BaseFragment {
    private static final String TAG = CameraPreviewFragment.class.getSimpleName();

    int mBookmarkCount = -1;
    int mBookmarkClickCount;

    private Handler mHandler;
    private LiveRawDataAdapter mRawDataAdapter;

    @Bind(R.id.cameraPreview)
    CameraLiveView mLiveView;

    @Bind(R.id.tvCameraStatus)
    TextView mTvCameraStatus;

    @Bind(R.id.tv_status_additional)
    TextView mTvStatusAdditional;

    @Bind(R.id.btnMicControl)
    ImageView mBtnMicControl;

    @Bind(R.id.fabBookmark)
    ImageButton mFabBookmark;

    @Bind(R.id.wvGauge)
    WebView mWvGauge;

    @Bind(R.id.liveViewLayout)
    FrameLayout mLiveViewLayout;

    @Bind(R.id.btnShowOverlay)
    ImageButton mBtnShowOverlay;

    @Bind(R.id.btnFullscreen)
    ImageButton mBtnFullScreen;


    @Nullable
    @Bind(R.id.sharp_view)
    View mSharpView;

    @Bind(R.id.bookmark_message_view)
    View mBookmarkMsgView;


    @Nullable
    @Bind(R.id.infoPanel)
    LinearLayout mInfoView;

    @Nullable
    @Bind(R.id.storageView)
    ProgressBar mStorageView;

    @Bind(R.id.recordDot)
    View mRecordDot;

    @Nullable
    @Bind(R.id.wifiMode)
    ImageView mWifiMode;

    @Nullable
    @Bind(R.id.ivBatterStatus)
    ImageView mIvBatterStatus;

    @Nullable
    @Bind(R.id.ivIsChanging)
    ImageView mIsCharging;

    @Nullable
    @Bind(R.id.tvBatteryVol)
    TextView mTvBatteryVol;

    @Nullable
    @Bind(R.id.cameraConnecting)
    LinearLayout mCameraConnecting;

    @Nullable
    @Bind(R.id.connectIndicator)
    ImageView mIvConnectIdicator;

    boolean mIsGaugeVisible;

    LocalBroadcastManager mLocalBroadcastManager;

    @OnClick(R.id.btnFullscreen)
    public void onBtnFullScreenClicked() {
        if (getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            //getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            LiveViewActivity.launch(getActivity(), mVdtCamera, mIsGaugeVisible);
        }
    }

    @OnClick(R.id.fabBookmark)
    public void onFabClick() {
        handleOnFabClicked();
    }

    @OnClick(R.id.btnShowOverlay)
    public void onBtnShowOverlayClick() {
        showOverlay(!mIsGaugeVisible);
    }

    void showOverlay(boolean isGaugeVisible) {
        if (isGaugeVisible) {
            mIsGaugeVisible = true;
            mWvGauge.setVisibility(View.VISIBLE);
            mBtnShowOverlay.setColorFilter(getResources().getColor(R.color.style_color_primary));
            requestLiveRawData();
            mSharpView.setVisibility(View.INVISIBLE);
        } else {
            mIsGaugeVisible = false;
            hideOverlay();
        }
    }

    public static CameraPreviewFragment newInstance(VdtCamera vdtCamera) {
        CameraPreviewFragment fragment = new CameraPreviewFragment();
        Bundle bundle = new Bundle();
        bundle.putString("ssid", vdtCamera.getSSID());
        bundle.putString("hostString", vdtCamera.getHostString());
        fragment.setArguments(bundle);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        if (getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            view = createFragmentView(inflater, container, R.layout.fragment_camera_preview_land, savedInstanceState);
        } else {
            view = createFragmentView(inflater, container, R.layout.fragment_camera_preview, savedInstanceState);
        }
        init();
        return view;
    }

    @Override
    public void setupToolbar() {
        if (mToolbar != null) {
            mToolbar.setTitle(R.string.live_view);
            mToolbar.getMenu().clear();
            if (VdtCameraManager.getManager().isConnected()) {
                mToolbar.inflateMenu(R.menu.menu_live_view);
            }
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.cameraInfo:

                            toggleInfoView();
                            break;
                        case R.id.cameraSetting:
                            LiveViewSettingActivity.launch(getActivity(), mVdtCamera);
                            break;
                    }
                    return false;
                }
            });
        }
        super.setupToolbar();
    }

    @Override
    public void onStart() {
        super.onStart();
        initCameraPreview();
        showOverlay(mIsGaugeVisible);
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onStop() {
        if (mVdtCamera != null) {
            mVdtCamera.setOnStateChangeListener(null);
        }

        if (mLiveView != null) {
            mLiveView.stopStream();
        }

        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver,
                new IntentFilter(LiveViewActivity.ACTION_IS_GAUGE_VISIBLE));

        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.onDestroyView();
    }

    @Override
    public void onCameraVdbConnected(VdtCamera camera) {
        super.onCameraVdbConnected(camera);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCameraConnecting == null) {
                    return;
                }
                mCameraConnecting.setVisibility(View.GONE);
                mToolbar.getMenu().clear();
                mToolbar.inflateMenu(R.menu.menu_live_view);
                initViews();
                initCameraPreview();
            }
        });
    }

    @Override
    protected void onCameraDisconnected(VdtCamera vdtCamera) {
        super.onCameraDisconnected(vdtCamera);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                showConnectingStatus();
            }
        });
    }

    protected void init() {
        mVdbRequestQueue = Snipe.newRequestQueue();
        mHandler = new Handler();
        if (VdtCameraManager.getManager().isConnected()) {
            mVdtCamera = VdtCameraManager.getManager().getConnectedCameras().get(0);
            if (mCameraConnecting != null) {
                mCameraConnecting.setVisibility(View.GONE);
            }
        } else {
            showConnectingStatus();
        }

        if (mVdtCamera != null) {
            initViews();
        }
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
    }

    void showConnectingStatus() {
        if (mCameraConnecting == null) {
            return;
        }
        mCameraConnecting.setVisibility(View.VISIBLE);
        mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
        AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
        animationDrawable.start();
        mToolbar.getMenu().clear();
    }

    private void initViews() {
        updateMicControlButton();
        // Start record red dot indicator animation
        AnimationDrawable animationDrawable = (AnimationDrawable) mRecordDot.getBackground();
        animationDrawable.start();


        initGaugeWebView();
    }

    private void initCameraPreview() {
        mLiveView.setBackgroundColor(Color.BLACK);
        if (mVdtCamera != null) {
            InetSocketAddress serverAddr = mVdtCamera.getPreviewAddress();
            if (serverAddr == null) {
                mVdtCamera = null;
                return;
            } else {
                mVdtCamera.startPreview();
                mVdtCamera.setOnStateChangeListener(mOnStateChangeListener);
                mVdtCamera.setOnRecStateChangeListener(new VdtCamera.OnRecStateChangeListener() {
                    @Override
                    public void onRecStateChanged(int newState, boolean isStill) {
                        updateCameraState(mVdtCamera.getState());
                    }

                    @Override
                    public void onRecDurationChanged(int duration) {

                    }

                    @Override
                    public void onRecError(int error) {

                    }
                });
                mLiveView.startStream(serverAddr, null, true);
            }

            mVdtCamera.getRecordRecMode();
            mVdtCamera.getCameraTime();
            mVdtCamera.getAudioMicState();
            mVdtCamera.getRecordResolutionList();
            mVdtCamera.GetSetup();
            updateCameraState(mVdtCamera.getState());

        }

    }


    private final VdtCamera.OnStateChangeListener mOnStateChangeListener = new VdtCamera.OnStateChangeListener() {
        @Override
        public void onStateChanged(VdtCamera vdtCamera) {
            updateCameraState(vdtCamera.getState());
        }

        @Override
        public void onBtStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onGpsStateChanged(VdtCamera vdtCamera) {

        }

        @Override
        public void onWifiStateChanged(VdtCamera vdtCamera) {

        }
    };


    private void updateCameraState(final CameraState state) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateCameraStatusInfo(state);
                updateFloatActionButton(state);
                toggleRecordDot(state);
            }
        });

    }


    private void updateMicControlButton() {
        boolean micEnabled = mVdtCamera.isMicEnabled();
        if (micEnabled) {
            mBtnMicControl.setColorFilter(getResources().getColor(R.color.style_color_primary));
        } else {
            mBtnMicControl.clearColorFilter();
        }
    }


    void requestLiveRawData() {
        registerMessageHandler();
    }

    void registerMessageHandler() {

        ClipInfoMsgHandler clipInfoMsgHandler = new ClipInfoMsgHandler(
                new VdbResponse.Listener<ClipActionInfo>() {
                    @Override
                    public void onResponse(ClipActionInfo response) {
                        Logger.t(TAG).e(response.toString());
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Logger.t(TAG).e("ClipInfoMsgHandler ERROR", error);
                    }
                });
        mVdbRequestQueue.registerMessageHandler(clipInfoMsgHandler);

        MarkLiveMsgHandler markLiveMsgHandler = new MarkLiveMsgHandler(
                new VdbResponse.Listener<ClipActionInfo>() {
                    @Override
                    public void onResponse(ClipActionInfo response) {
                        Logger.t(TAG).e(response.toString());
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Logger.t(TAG).e("MarkLiveMsgHandler ERROR", error);
                    }
                });
        mVdbRequestQueue.registerMessageHandler(markLiveMsgHandler);
    }


    void hideOverlay() {
        mWvGauge.setVisibility(View.INVISIBLE);
        mBtnShowOverlay.clearColorFilter();
        closeLiveRawData();
    }


    void closeLiveRawData() {
        LiveRawDataRequest request = new LiveRawDataRequest(0, new
                VdbResponse.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
                        Logger.t(TAG).d("LiveRawDataResponse: " + response);
                    }
                }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Logger.t(TAG).e("LiveRawDataResponse ERROR", error);
            }
        });
        mVdbRequestQueue.add(request);
        mVdbRequestQueue.unregisterMessageHandler(VdbCommand.Factory.MSG_RawData);
        mVdbRequestQueue.unregisterMessageHandler(VdbCommand.Factory.MSG_ClipInfo);
        mVdbRequestQueue.unregisterMessageHandler(VdbCommand.Factory.VDB_MSG_MarkLiveClipInfo);
    }

    private void handleOnFabClicked() {
        switch (mVdtCamera.getRecordState()) {
            case VdtCamera.STATE_RECORD_RECORDING:
                if (isInCarMode(mVdtCamera.getState())) {
                    mVdtCamera.markLiveVideo();
                    mBookmarkClickCount++;
                    showMessage();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            updateCameraStatusInfo(mVdtCamera.getState());
                            hideMessage();
                        }
                    }, 1000 * 3);
                } else {
                    mVdtCamera.stopRecording();
                }
                break;
            case VdtCamera.STATE_RECORD_STOPPED:
                mVdtCamera.startRecording();
                break;
        }
    }

    boolean isInCarMode(CameraState state) {
        if (state != null) {
            boolean isInCarMode = (mVdtCamera.getRecordMode() == VdtCamera.REC_MODE_AUTOSTART_LOOP);
            return isInCarMode;
        }

        return false;
    }


    private void initGaugeWebView() {
        mWvGauge.getSettings().setJavaScriptEnabled(true);
        mWvGauge.setBackgroundColor(Color.TRANSPARENT);
        mWvGauge.setVisibility(View.INVISIBLE);
        mWvGauge.loadUrl("file:///android_asset/api.html");
        mRawDataAdapter = new LiveRawDataAdapter(mVdbRequestQueue, mWvGauge);
    }

    void showMessage() {
        mBookmarkMsgView.setVisibility(View.VISIBLE);
    }

    private void updateCameraStatusInfo(CameraState state) {
        int recState = mVdtCamera.getRecordState();
        switch (recState) {
            case VdtCamera.STATE_RECORD_UNKNOWN:
                mTvCameraStatus.setText(R.string.record_unknown);
                break;
            case VdtCamera.STATE_RECORD_STOPPED:
                mTvCameraStatus.setText(R.string.record_stopped);
                break;
            case VdtCamera.STATE_RECORD_STOPPING:
                mTvCameraStatus.setText(R.string.record_stopping);
                break;
            case VdtCamera.STATE_RECORD_STARTING:
                mTvCameraStatus.setText(R.string.record_starting);
                break;
            case VdtCamera.STATE_RECORD_RECORDING:
                if (isInCarMode(state)) {
                    mTvCameraStatus.setText(R.string.continuous_recording);
                    if (mBookmarkCount != -1) {
                        updateTvStatusAdditional(getResources().getQuantityString(R.plurals.number_of_bookmarks,
                                mBookmarkCount + mBookmarkClickCount,
                                mBookmarkCount + mBookmarkClickCount), View.VISIBLE);
                        mTvStatusAdditional.setVisibility(View.VISIBLE);

                    }
                } else {
                    mTvCameraStatus.setText(R.string.record_recording);
                    mTvStatusAdditional.setVisibility(View.GONE);
                }
                break;
            case VdtCamera.STATE_RECORD_SWITCHING:
                mTvCameraStatus.setText(R.string.record_switching);
                break;
            default:
                break;
        }
        if (recState != VdtCamera.STATE_RECORD_RECORDING) {
            mTvStatusAdditional.setVisibility(View.GONE);
        }
    }

    private void updateTvStatusAdditional(String text, int visible) {
        if (mTvStatusAdditional != null) {
            mTvStatusAdditional.setText(text);
            mTvStatusAdditional.setVisibility(visible);
        }
    }


    void hideMessage() {
        mBookmarkMsgView.setVisibility(View.GONE);
    }

    private void updateFloatActionButton(CameraState state) {
        switch (mVdtCamera.getRecordState()) {
            case VdtCamera.STATE_RECORD_UNKNOWN:
                break;
            case VdtCamera.STATE_RECORD_STOPPED:
                mFabBookmark.setEnabled(true);
                mFabBookmark.setImageResource(R.drawable.camera_control_start);
                break;
            case VdtCamera.STATE_RECORD_STOPPING:
                mFabBookmark.setEnabled(false);
                break;
            case VdtCamera.STATE_RECORD_STARTING:
                mFabBookmark.setEnabled(false);
                break;
            case VdtCamera.STATE_RECORD_RECORDING:
                mFabBookmark.setEnabled(true);
                if (isInCarMode(state)) {
                    mFabBookmark.setImageResource(R.drawable.camera_control_bookmark);
                } else {
                    mFabBookmark.setImageResource(R.drawable.camera_control_stop);
                }
                break;
            case VdtCamera.STATE_RECORD_SWITCHING:
                mFabBookmark.setEnabled(false);
                break;
            default:
                break;
        }

    }

    private void toggleRecordDot(CameraState state) {
        if (mVdtCamera.getRecordState() == VdtCamera.STATE_RECORD_RECORDING) {
            mRecordDot.setVisibility(View.VISIBLE);
        } else {
            mRecordDot.setVisibility(View.GONE);
        }

    }

    private void toggleInfoView() {
        if (mInfoView != null) {
            int visibility = mInfoView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            mInfoView.setVisibility(visibility);
            if (visibility == View.VISIBLE) {
                updateCameraInfoPanel();
            }
        }
    }

    private void updateCameraInfoPanel() {
        if (mInfoView == null) {
            return;
        }

        // update battery info:
        int batteryStatus = mVdtCamera.getBatteryState();
        if (batteryStatus != VdtCamera.STATE_BATTERY_CHARGING) {
            mIsCharging.setVisibility(View.INVISIBLE);
        } else {
            mIsCharging.setVisibility(View.VISIBLE);
        }

        int batterVol = mVdtCamera.getBatteryVolume();
        if (batterVol < 25) {
            mIvBatterStatus.setImageResource(R.drawable.rec_info_battery_4);
        } else if (batterVol < 50) {
            mIvBatterStatus.setImageResource(R.drawable.rec_info_battery_3);
        } else if (batterVol < 75) {
            mIvBatterStatus.setImageResource(R.drawable.rec_info_battery_2);
        } else if (batterVol < 100) {
            mIvBatterStatus.setImageResource(R.drawable.rec_info_battery_1);
        }

        String batterVolString = "" + batterVol + "%";
        mTvBatteryVol.setText(batterVolString);

        // update Wifi info:
        //WifiState wifiState = mVdtCamera.getWifiStates();
        Logger.t(TAG).d("WifiMode: " + mVdtCamera.getWifiMode());
        int wifiMode = mVdtCamera.getWifiMode();
        if (wifiMode == VdtCamera.WIFI_MODE_AP) {
            mWifiMode.setImageResource(R.drawable.rec_info_camera_mode_ap);
        } else if (wifiMode == VdtCamera.WIFI_MODE_CLIENT) {
            mWifiMode.setImageResource(R.drawable.rec_info_camera_mode_client);
        }

        // update storage info;
        VdtCamera.StorageInfo storageInfo = mVdtCamera.getStorageInfo();

        Logger.t(TAG).d("totalSpace: " + storageInfo.totalSpace + " freeSpace: " + storageInfo.freeSpace);

        mStorageView.setMax(storageInfo.totalSpace);
        mStorageView.setProgress(storageInfo.totalSpace - storageInfo.freeSpace);
    }

    /**
     * This receiver is used to sync gauge settings during screen rotation.
     */
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIsGaugeVisible = intent.getBooleanExtra(LiveViewActivity.EXTRA_IS_GAUGE_VISIBLE, false);
        }
    };
}
