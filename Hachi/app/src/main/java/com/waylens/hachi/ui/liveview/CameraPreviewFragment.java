package com.waylens.hachi.ui.liveview;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.eventbus.events.CameraStateChangeEvent;
import com.waylens.hachi.eventbus.events.MarkLiveMsgEvent;
import com.waylens.hachi.eventbus.events.MicStateChangeEvent;
import com.waylens.hachi.eventbus.events.RawDataItemEvent;
import com.waylens.hachi.hardware.vdtcamera.BtDevice;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.library.vdb.ClipActionInfo;
import com.waylens.hachi.library.vdb.SpaceInfo;
import com.waylens.hachi.library.vdb.VdbReadyInfo;
import com.waylens.hachi.library.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.library.vdb.rawdata.RawDataItem;
import com.waylens.hachi.library.snipe.SnipeError;
import com.waylens.hachi.library.snipe.VdbResponse;
import com.waylens.hachi.library.snipe.toolbox.GetSpaceInfoRequest;
import com.waylens.hachi.library.snipe.toolbox.LiveRawDataRequest;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.ui.manualsetup.ScanQrCodeActivity;
import com.waylens.hachi.ui.manualsetup.StartupActivity;
import com.waylens.hachi.ui.views.GaugeView;

import com.xfdingustc.mjpegview.library.MjpegView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Optional;

/**
 * Created by Xiaofei on 2016/3/7.
 */
public class CameraPreviewFragment extends BaseFragment implements FragmentNavigator {
    private static final String TAG = CameraPreviewFragment.class.getSimpleName();

    private Handler mHandler;
    private Timer mTimer;

    private UpdateRecordTimeTask mRecordTimeTask;

    private VdtCameraManager mVdtCameraManager = VdtCameraManager.getManager();
    private EventBus mEventBus = EventBus.getDefault();

    private int mFabStartSrc;

    private int mFabStopSrc;

    @BindView(R.id.camera_preview)
    MjpegView mLiveView;

    @Nullable
    @BindView(R.id.spinner)
    Spinner mCameraSpinner;

    @BindView(R.id.tvCameraStatus)
    TextView mTvCameraRecStatus;

    @BindView(R.id.tv_status_additional)
    TextView mTvStatusAdditional;

    @BindView(R.id.btnMicControl)
    ImageView mBtnMicControl;

    @BindView(R.id.fabStartStop)
    ImageButton mFabStartStop;

    @BindView(R.id.gaugeView)
    GaugeView mGaugeView;

    @BindView(R.id.liveViewLayout)
    FrameLayout mLiveViewLayout;

    @BindView(R.id.btnShowOverlay)
    ImageButton mBtnShowOverlay;

    @BindView(R.id.bookmark_message_view)
    View mBookmarkMsgView;


    @Nullable
    @BindView(R.id.infoPanel)
    LinearLayout mInfoView;

    @Nullable
    @BindView(R.id.remote_ctrl)
    ImageView mRemoteCtrl;

    @Nullable
    @BindView(R.id.obd)
    ImageView mObd;

    @Nullable
    @BindView(R.id.storageView)
    ProgressBar mStorageView;

    @BindView(R.id.recordDot)
    ImageView mRecordDot;

    @Nullable
    @BindView(R.id.wifiMode)
    ImageView mWifiMode;

    @Nullable
    @BindView(R.id.ivBatterStatus)
    ImageView mIvBatterStatus;

    @Nullable
    @BindView(R.id.ivIsChanging)
    ImageView mIsCharging;

    @Nullable
    @BindView(R.id.tvBatteryVol)
    TextView mTvBatteryVol;

    @Nullable
    @BindView(R.id.cameraConnecting)
    LinearLayout mCameraConnecting;

    @Nullable
    @BindView(R.id.noSignal)
    RelativeLayout mCameraNoSignal;

    @Nullable
    @BindView(R.id.connectIndicator)
    ImageView mIvConnectIdicator;

    @Nullable
    @BindView(R.id.errorPanel)
    LinearLayout mErrorPanel;

    @Nullable
    @BindView(R.id.tvErrorMessage)
    TextView mTvErrorMessage;

    @Nullable
    @BindView(R.id.tvErrorIndicator)
    TextView mTvErrorIndicator;


    @BindView(R.id.btnBookmark)
    ImageButton mBtnBookmark;


    @BindView(R.id.btnFullscreen)
    ImageButton mBtnFullScreen;

    @BindView(R.id.statusErrorLayout)
    FrameLayout mStatusErrorLayout;

    @BindView(R.id.controlPanel)
    RelativeLayout mControlPanel;

    @OnClick(R.id.btnMicControl)
    public void onBtnMicControlClicked() {
        if (mVdtCamera != null) {
            boolean isMicOn = mVdtCamera.isMicOn();
            mVdtCamera.setMicOn(!isMicOn);
        }
    }


    @OnClick(R.id.btnFullscreen)
    public void onBtnFullScreenClicked() {
        if (!isFullScreen()) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @OnClick(R.id.fabStartStop)
    public void onFabStartStopClicked() {
        switch (mVdtCamera.getRecordState()) {
            case VdtCamera.STATE_RECORD_RECORDING:
                mVdtCamera.stopRecording();
                break;
            case VdtCamera.STATE_RECORD_STOPPED:
                mVdtCamera.startRecording();
                break;
        }
    }

    @OnClick(R.id.btnBookmark)
    public void onFabClick() {
        mVdtCamera.markLiveVideo();
    }

    @OnClick(R.id.btnShowOverlay)
    public void onBtnShowOverlayClick() {
        boolean visibility = mGaugeView.getVisibility() == View.VISIBLE ? false : true;
        showOverlay(visibility);
    }


    @Optional
    @OnClick(R.id.add_new_camera)
    public void onAddNewCameraClicked() {
        StartupActivity.launch(getActivity());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraConnection(CameraConnectionEvent event) {
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_CONNECTED:
                Logger.t(TAG).d("on Camera connected");
                initVdtCamera();
                setupToolbar();
                initViews();
                initCameraPreview();
                break;
            case CameraConnectionEvent.VDT_CAMERA_CONNECTING:
                Logger.t(TAG).d("on Camera connecting");
                mCameraNoSignal.setVisibility(View.GONE);
                mCameraConnecting.setVisibility(View.VISIBLE);
                break;
            case CameraConnectionEvent.VDT_CAMERA_DISCONNECTED:
                if (mVdtCamera != null) {
                    mLiveView.stopStream();
                }
                mCameraNoSignal.setVisibility(View.VISIBLE);
                mCameraConnecting.setVisibility(View.GONE);
                mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
                AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
                animationDrawable.start();
                getToolbar().getMenu().clear();
                break;
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraInfoChanged(CameraStateChangeEvent event) {
        if (event.getWhat() == CameraStateChangeEvent.CAMERA_STATE_INFO) {
            setupToolbar();
            return;
        }

        if (mVdtCamera != event.getCamera()) {
            return;
        }

        switch (event.getWhat()) {
            case CameraStateChangeEvent.CAMERA_STATE_REC:
                updateCameraState();
                break;
            case CameraStateChangeEvent.CAMERA_STATE_REC_DURATION:
                int recordTime = (Integer) event.getExtra();
                if (mVdtCamera.getRecordState() == VdtCamera.STATE_RECORD_RECORDING) {
                    mTvCameraRecStatus.setText(DateUtils.formatElapsedTime(recordTime));
                    mRecordDot.setVisibility(View.VISIBLE);

                    Logger.t(TAG).d("record state: " + mVdtCamera.getRecordState());
                }
                break;
            case CameraStateChangeEvent.CAMERA_STATE_REC_ERROR:
                int error = (Integer) event.getExtra();
                Logger.t(TAG).d("On Rec Error: " + error);
                if (mErrorPanel != null) {

                    mErrorPanel.setVisibility(View.VISIBLE);
                    mTvErrorIndicator.setText(R.string.recording_error);
                    switch (error) {
                        case VdtCamera.ERROR_START_RECORD_NO_CARD:
                            mTvErrorMessage.setText(R.string.error_msg_no_card);
                    }

                }
                break;

        }

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventUpdateCameraStatus(UpdateCameraStatusEvent event) {
        updateCameraState();

    }

    @Subscribe
    public void onEventRawDataItemList(RawDataItemEvent event) {
        if (mVdtCamera != event.getCamera()) {
            return;
        }

        List<RawDataItem> itemList = event.getRawDataItemList();
        if (itemList != null) {
            mGaugeView.updateRawDateItem(itemList);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMicInfor(MicStateChangeEvent event) {
        updateMicControlButton();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventVdbReadyChanged(VdbReadyInfo event) {
        Logger.t(TAG).d("change progressbar in camera preview page");
        updateSpaceInfo();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMarkLiveMsg(MarkLiveMsgEvent event) {
        if (event.getClipActionInfo().action == ClipActionInfo.CLIP_ACTION_CREATED) {
            showMessage();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateCameraStatusInfo();
                    hideMessage();
                }
            }, 1000);
        }
        ;
    }


    @Override
    protected String getRequestTag() {
        return TAG;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_camera_preview, savedInstanceState);
        init();
        startPreview();
        return view;
    }

    @Override
    public void setupToolbar() {
        if (getToolbar() == null) {
            return;
        }

        if (mVdtCameraManager.getConnectedCameras().size() > 1) {
            List<String> cameraNames = new ArrayList<>();
            List<VdtCamera> connectedCameras = mVdtCameraManager.getConnectedCameras();
            for (int i = 0; i < connectedCameras.size(); i++) {
                VdtCamera camera = connectedCameras.get(i);
                Logger.t(TAG).d("add one camera: " + camera.getName());
                cameraNames.add(camera.getName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.item_spinner, cameraNames);
            mCameraSpinner.setAdapter(adapter);

            mCameraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                        Logger.t(TAG).d("Item Position clicked: " + position);
                    changeCurrentCamera(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            getToolbar().setTitle("");
            mCameraSpinner.setVisibility(View.VISIBLE);

        } else {
            getToolbar().setTitle(R.string.live_view);
            mCameraSpinner.setVisibility(View.GONE);
        }

        getToolbar().getMenu().clear();
        if (VdtCameraManager.getManager().isConnected()) {
            getToolbar().inflateMenu(R.menu.menu_live_view);
        }
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.cameraInfo:
                        toggleInfoView();
                        break;
                    case R.id.cameraSetting:
                        LiveViewSettingActivity.launch(getActivity());
                        break;
                }
                return false;
            }
        });

        super.setupToolbar();
    }


    @Override
    public void onStart() {
        super.onStart();
        initVdtCamera();
        if (mVdtCamera != null) {
            if (mCameraNoSignal != null) {
                mCameraNoSignal.setVisibility(View.GONE);
                mCameraConnecting.setVisibility(View.GONE);
            }
            initViews();
        } else {
            handleOnCameraDisconnected();
        }
        if (getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            mBtnBookmark.setImageResource(R.drawable.camera_control_bookmark_land);
            mFabStopSrc = R.drawable.camera_control_stop_land;
            mFabStartSrc = R.drawable.camera_control_start_land;
        } else {
            mBtnBookmark.setImageResource(R.drawable.camera_control_bookmark);
            mFabStopSrc = R.drawable.camera_control_stop;
            mFabStartSrc = R.drawable.camera_control_start;
        }

        if (!mEventBus.isRegistered(this)) {
            mEventBus.register(this);
        }


        setupToolbar();
        if (isVisible()) {
            startPreview();
        }

    }



    @Override
    public void onStop() {
        super.onStop();
        stopPreview();
        mEventBus.unregister(this);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isFullScreen()) {
            mBtnBookmark.setImageResource(R.drawable.camera_control_bookmark_land);
            mFabStopSrc = R.drawable.camera_control_stop_land;
            mFabStartSrc = R.drawable.camera_control_start_land;

            getToolbar().setVisibility(View.GONE);

            mInfoView.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            params1.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            mLiveViewLayout.setLayoutParams(params1);

            RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            params2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            //Logger.t(TAG).d("mControlPanel height: " + mControlPanel.getLayoutParams().height);
            mControlPanel.setLayoutParams(params2);
            //Logger.t(TAG).d("mControlPanel height: " + mControlPanel.getLayoutParams().height);
            RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            params3.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            params3.removeRule(RelativeLayout.BELOW);
            mStatusErrorLayout.setLayoutParams(params3);

            mBtnFullScreen.setImageResource(R.drawable.screen_narrow);
        } else {
            mBtnBookmark.setImageResource(R.drawable.camera_control_bookmark);
            mFabStopSrc = R.drawable.camera_control_stop;
            mFabStartSrc = R.drawable.camera_control_start;

            getToolbar().setVisibility(View.VISIBLE);

            mInfoView.setVisibility(View.VISIBLE);

            RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            params1.addRule(RelativeLayout.BELOW, mInfoView.getId());
            params1.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            mStatusErrorLayout.setLayoutParams(params1);

            RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            params2.addRule(RelativeLayout.BELOW, mStatusErrorLayout.getId());
            mLiveViewLayout.setLayoutParams(params2);

            RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            params3.addRule(RelativeLayout.BELOW, mLiveViewLayout.getId());
            mControlPanel.setLayoutParams(params3);

            mBtnFullScreen.setImageResource(R.drawable.screen_full);
        }
        updateFloatActionButton();
        ((BaseActivity) getActivity()).setImmersiveMode(isFullScreen());
    }

    @Override
    public boolean onInterceptBackPressed() {
        if (getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            ((BaseActivity) getActivity()).setImmersiveMode(false);
            return true;
        } else {
            return false;
        }
    }


    private boolean isFullScreen() {
        int orientation = getActivity().getRequestedOrientation();
        return orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }

    public void startPreview() {
        initCameraPreview();


        mTimer = new Timer();
        mRecordTimeTask = new UpdateRecordTimeTask();
        mTimer.schedule(mRecordTimeTask, 1000, 1000);
    }

    public void stopPreview() {
        if (mTimer != null) {
            mTimer.cancel();
        }

        stopCameraPreview();
    }

    private void changeCurrentCamera(int position) {
        stopCameraPreview();

        mVdtCameraManager.setCurrentCamera(position);
        mVdtCamera = mVdtCameraManager.getCurrentCamera();
        mVdbRequestQueue = mVdtCamera.getRequestQueue();
        mEventBus.post(new CameraConnectionEvent(CameraConnectionEvent.VDT_CAMERA_SELECTED_CHANGED, null));
//        Logger.t(TAG).d("changed vdtcamera to " + mVdtCamera.getName());
        initCameraPreview();
    }

    private void handleOnCameraConnected() {
        if (mCameraNoSignal != null) {
            mCameraNoSignal.setVisibility(View.GONE);
            mCameraConnecting.setVisibility(View.GONE);
        }

    }


    private void handleOnCameraDisconnected() {
        if (mCameraConnecting != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCameraNoSignal.setVisibility(View.VISIBLE);
                    mCameraConnecting.setVisibility(View.GONE);
                    mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
                    AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
                    animationDrawable.start();
                    getToolbar().getMenu().clear();
                }
            });
        }
    }

    protected void init() {
        mHandler = new Handler();

    }

    private void initViews() {
        updateMicControlButton();
        // Start record red dot indicator animation
        AnimationDrawable animationDrawable = (AnimationDrawable) mRecordDot.getBackground();
        animationDrawable.start();
        handleOnCameraConnected();
        updateCameraState();
        updateSpaceInfo();
        mTvStatusAdditional.setVisibility(View.GONE);
    }

    private void initCameraPreview() {
        if (mVdtCamera != null) {
            InetSocketAddress serverAddr = mVdtCamera.getPreviewAddress();
            if (serverAddr == null) {
                mVdtCamera = null;
                return;
            }

            Logger.t(TAG).d("start preview view");
            mVdtCamera.startPreview();
            mLiveView.startStream(serverAddr);
            mVdtCamera.getRecordRecMode();
            mVdtCamera.getRecordTime();
            mVdtCamera.getAudioMicState();
            mVdtCamera.getRecordResolutionList();
            mVdtCamera.getSetup();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateCameraState();
                    updateCameraInfoPanel();
                }
            });

            openLiveViewData();
        }

    }

    private void stopCameraPreview() {
        mLiveView.stopStream();
        closeLiveRawData();
    }


    private void updateCameraState() {
        updateCameraInfoPanel();
        updateCameraStatusInfo();
        updateFloatActionButton();
        toggleRecordDot();
    }

    private void updateSpaceInfo() {
        if (mInfoView != null && mInfoView.getVisibility() == View.VISIBLE) {
            GetSpaceInfoRequest request = new GetSpaceInfoRequest(new VdbResponse.Listener<SpaceInfo>() {
                @Override
                public void onResponse(SpaceInfo response) {
                    Logger.t(TAG).d("response: " + response.toString());
                    mStorageView.getProgressDrawable().clearColorFilter();
                    mStorageView.setMax((int) (response.total / (1024 * 1024)));
                    mStorageView.setProgress((int) (response.marked / (1024 * 1024)));
                    mStorageView.setSecondaryProgress((int) (response.used / (1024 * 1024)));
                }
            }, new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    mStorageView.getProgressDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                }
            });

            if (mVdbRequestQueue != null) {
                mVdbRequestQueue.add(request);
            }
        }
    }


    private void updateMicControlButton() {
        if (mVdtCamera != null) {
            boolean micEnabled = mVdtCamera.isMicEnabled();
            if (micEnabled) {
                mBtnMicControl.setImageResource(R.drawable.btn_mic_on);
            } else {
                mBtnMicControl.setImageResource(R.drawable.btn_mic_off);
            }
        }
    }


    private void openLiveViewData() {
        if (mVdbRequestQueue != null) {
            LiveRawDataRequest request = new LiveRawDataRequest(RawDataBlock.F_RAW_DATA_GPS +
                RawDataBlock.F_RAW_DATA_ACC + RawDataBlock.F_RAW_DATA_ODB, new
                VdbResponse.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
//                    Logger.t(TAG).d("LiveRawDataResponse: " + response);
                    }
                }, new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Log.e(TAG, "LiveRawDataResponse ERROR", error);
                }
            });

            mVdbRequestQueue.add(request);
        }
    }


    private void closeLiveRawData() {
        if (mVdbRequestQueue != null) {
            LiveRawDataRequest request = new LiveRawDataRequest(0, new
                VdbResponse.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
//                    Logger.t(TAG).d("LiveRawDataResponse: " + response);
                    }
                }, new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("LiveRawDataResponse ERROR", error);
                }
            });
            mVdbRequestQueue.add(request);
//        mVdbRequestQueue.unregisterMessageHandler(VdbCommand.Factory.MSG_RawData);

        }
    }

    private void handleOnFabClicked() {
        switch (mVdtCamera.getRecordState()) {
            case VdtCamera.STATE_RECORD_RECORDING:
                if (isInCarMode()) {
                    mVdtCamera.markLiveVideo();
                } else {
                    mVdtCamera.stopRecording();
                }
                break;
            case VdtCamera.STATE_RECORD_STOPPED:
                mVdtCamera.startRecording();
                break;
        }
    }


    private boolean isInCarMode() {
        boolean isInCarMode = (mVdtCamera.getRecordMode() == VdtCamera.REC_MODE_AUTOSTART_LOOP);
//        Logger.t(TAG).d("record mode: " + mVdtCamera.getRecordMode() + " isInCarMode: " + isInCarMode);
        return isInCarMode;
    }


    private void showMessage() {
        mBookmarkMsgView.setVisibility(View.VISIBLE);
    }

    private void showOverlay(boolean isGaugeVisible) {
        if (isGaugeVisible) {
            mBtnShowOverlay.setImageResource(R.drawable.btn_gauge_overlay_s);
        } else {
            mBtnShowOverlay.setImageResource(R.drawable.btn_gauge_overlay_n);
        }
        //mGaugeView.showGauge(mIsGaugeVisible);
        mGaugeView.setVisibility(isGaugeVisible ? View.VISIBLE : View.INVISIBLE);
    }


    private void updateCameraStatusInfo() {
        int recState = mVdtCamera.getRecordState();
//        Logger.t(TAG).d("rec state: " + recState);
        switch (recState) {
            case VdtCamera.STATE_RECORD_UNKNOWN:
                mTvCameraRecStatus.setText(R.string.record_unknown);
                break;
            case VdtCamera.STATE_RECORD_STOPPED:
                mTvCameraRecStatus.setText(R.string.record_stopped);
                break;
            case VdtCamera.STATE_RECORD_STOPPING:
                mTvCameraRecStatus.setText(R.string.record_stopping);
                break;
            case VdtCamera.STATE_RECORD_STARTING:
                mTvCameraRecStatus.setText(R.string.record_starting);
                if (mErrorPanel != null) {
                    mErrorPanel.setVisibility(View.GONE);
                }
                break;
            case VdtCamera.STATE_RECORD_RECORDING:
                if (isInCarMode()) {
                    String recStatusText = mTvCameraRecStatus.getText().toString();
                    if (recStatusText == null || recStatusText.isEmpty()) {
                        mTvCameraRecStatus.setText(R.string.continuous_recording);
                    }
                } else {
                    mTvStatusAdditional.setVisibility(View.GONE);
                }

                break;
            case VdtCamera.STATE_RECORD_SWITCHING:
                mTvCameraRecStatus.setText(R.string.record_switching);
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

    private void updateFloatActionButton() {
        switch (mVdtCamera.getRecordState()) {
            case VdtCamera.STATE_RECORD_UNKNOWN:
                break;
            case VdtCamera.STATE_RECORD_STOPPED:
                mFabStartStop.setEnabled(true);
                mFabStartStop.setImageResource(mFabStartSrc);
                mBtnBookmark.setVisibility(View.GONE);
                break;
            case VdtCamera.STATE_RECORD_STOPPING:
                mFabStartStop.setEnabled(false);
                break;
            case VdtCamera.STATE_RECORD_STARTING:
                mFabStartStop.setEnabled(false);
                break;
            case VdtCamera.STATE_RECORD_RECORDING:
                mFabStartStop.setEnabled(true);
                if (isInCarMode()) {
                    mBtnBookmark.setVisibility(View.VISIBLE);
                }
                mFabStartStop.setImageResource(mFabStopSrc);
                break;
            case VdtCamera.STATE_RECORD_SWITCHING:
                mFabStartStop.setEnabled(false);
                mBtnBookmark.setVisibility(View.GONE);
                break;
            default:
                break;
        }

    }

    private void toggleRecordDot() {
        if (mVdtCamera.getRecordState() == VdtCamera.STATE_RECORD_RECORDING) {
            mRecordDot.setVisibility(View.VISIBLE);
            AnimationDrawable animationDrawable = (AnimationDrawable) mRecordDot.getBackground();
            animationDrawable.start();
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
        if (mInfoView == null || mInfoView.getVisibility() != View.VISIBLE) {
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
        } else if (batterVol <= 100) {
            mIvBatterStatus.setImageResource(R.drawable.rec_info_battery_1);
        }

        String batterVolString = "" + batterVol + "%";
        mTvBatteryVol.setText(batterVolString);

        // update Wifi info:
        //WifiState wifiState = mVdtCamera.getWifiStates();
//        Logger.t(TAG).d("WifiMode: " + mVdtCamera.getWifiMode());
        int wifiMode = mVdtCamera.getWifiMode();
        if (wifiMode == VdtCamera.WIFI_MODE_AP) {
            mWifiMode.setImageResource(R.drawable.rec_info_camera_mode_ap);
        } else if (wifiMode == VdtCamera.WIFI_MODE_CLIENT) {
            mWifiMode.setImageResource(R.drawable.rec_info_camera_mode_client);
        }


        BtDevice obdState = mVdtCamera.getObdDevice();
        BtDevice remoteCtrState = mVdtCamera.getRemoteCtrlDevice();

        if (obdState.getState() != BtDevice.BT_DEVICE_STATE_ON) {
            mObd.setAlpha(0.2f);
        } else {
            mObd.setAlpha(1.0f);
        }

        if (remoteCtrState.getState() != BtDevice.BT_DEVICE_STATE_ON) {
            mRemoteCtrl.setAlpha(0.2f);
        } else {
            mRemoteCtrl.setAlpha(1.0f);
        }


    }


    private static class UpdateRecordTimeTask extends TimerTask {

        @Override
        public void run() {
            VdtCamera vdtCamera = VdtCameraManager.getManager().getCurrentCamera();
            if (vdtCamera != null) {
                vdtCamera.getRecordTime();
                EventBus.getDefault().post(new UpdateCameraStatusEvent());

            }
        }
    }
}
