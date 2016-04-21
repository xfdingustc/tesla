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
import com.waylens.hachi.eventbus.events.RawDataItemEvent;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipInfoMsgHandler;
import com.waylens.hachi.snipe.toolbox.LiveRawDataRequest;
import com.waylens.hachi.snipe.toolbox.MarkLiveMsgHandler;
import com.waylens.hachi.ui.activities.LiveViewActivity;
import com.waylens.hachi.ui.activities.VdtCameraSettingActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.views.GaugeView;
import com.waylens.hachi.ui.views.camerapreview.CameraLiveView;
import com.waylens.hachi.vdb.ClipActionInfo;
import com.waylens.hachi.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.vdb.rawdata.RawDataItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/3/7.
 */
public class CameraPreviewFragment extends BaseFragment {
    private static final String TAG = CameraPreviewFragment.class.getSimpleName();

    private int mBookmarkCount = -1;
    private int mBookmarkClickCount;

    private Handler mHandler;

    private Timer mTimer;
    private UpdateRecordTimeTask mRecordTimeTask;


    private boolean mIsGaugeVisible;

    private LocalBroadcastManager mLocalBroadcastManager;

    private VdtCameraManager mVdtCameraManager = VdtCameraManager.getManager();

    private EventBus mEventBus = EventBus.getDefault();

    @Bind(R.id.cameraPreview)
    CameraLiveView mLiveView;

    @Nullable
    @Bind(R.id.spinner)
    Spinner mCameraSpinner;

    @Bind(R.id.tvCameraStatus)
    TextView mTvCameraRecStatus;

    @Bind(R.id.tv_status_additional)
    TextView mTvStatusAdditional;

    @Bind(R.id.btnMicControl)
    ImageView mBtnMicControl;

    @Bind(R.id.fabBookmark)
    ImageButton mFabBookmark;

    @Bind(R.id.gaugeView)
    GaugeView mGaugeView;

    @Bind(R.id.liveViewLayout)
    FrameLayout mLiveViewLayout;

    @Bind(R.id.btnShowOverlay)
    ImageButton mBtnShowOverlay;


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
    @Bind(R.id.noSignal)
    RelativeLayout mCameraNoSignal;

    @Nullable
    @Bind(R.id.connectIndicator)
    ImageView mIvConnectIdicator;

    @Nullable
    @Bind(R.id.errorPanel)
    LinearLayout mErrorPanel;

    @Nullable
    @Bind(R.id.tvErrorMessage)
    TextView mTvErrorMessage;

    @Nullable
    @Bind(R.id.tvErrorIndicator)
    TextView mTvErrorIndicator;


    @OnClick(R.id.btnFullscreen)
    public void onBtnFullScreenClicked() {
        if (getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            getActivity().finish();
        } else {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraConnection(CameraConnectionEvent event) {
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_CONNECTED:
                Logger.t(TAG).d("on Camera connected");
                setupToolbar();
                initViews();
                initCameraPreview();
                break;
            case CameraConnectionEvent.VDT_CAMERA_CONNECTING:
                mCameraNoSignal.setVisibility(View.GONE);
                mCameraConnecting.setVisibility(View.VISIBLE);
                break;
            case CameraConnectionEvent.VDT_CAMERA_DISCONNECTED:
                mCameraNoSignal.setVisibility(View.VISIBLE);
                mCameraConnecting.setVisibility(View.GONE);
                mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
                AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
                animationDrawable.start();
                mToolbar.getMenu().clear();
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

    @Subscribe
    public void onEventRawDataItem(RawDataItemEvent event) {
        if (mVdtCamera != event.getCamera()) {
            return;
        }

        RawDataItem item = event.getRawDataItem();
        mGaugeView.updateRawDateItem(item);
    }


    public static CameraPreviewFragment newInstance(VdtCamera vdtCamera, boolean isGaugeVisible) {
        CameraPreviewFragment fragment = new CameraPreviewFragment();
        Bundle bundle = new Bundle();
        bundle.putString("ssid", vdtCamera.getSSID());
        bundle.putString("hostString", vdtCamera.getHostString());
        bundle.putBoolean("isGaugeVisible", isGaugeVisible);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle == null) {
            mIsGaugeVisible = false;
        } else {
            mIsGaugeVisible = bundle.getBoolean("isGaugeVisible", false);
        }

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
                mToolbar.setTitle("");
                mCameraSpinner.setVisibility(View.VISIBLE);

            } else {
                mToolbar.setTitle(R.string.live_view);
                mCameraSpinner.setVisibility(View.GONE);
            }

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
                            VdtCameraSettingActivity.launch(getActivity(), mVdtCamera);
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
    public void onResume() {
        super.onResume();
        openLiveViewData();
        mEventBus.register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        closeLiveRawData();
        mEventBus.unregister(this);
    }

    @Override
    public void onStop() {
        super.onStop();


        if (mLiveView != null) {
            mLiveView.stopStream();
        }

        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, new IntentFilter(LiveViewActivity.ACTION_IS_GAUGE_VISIBLE));

        if (mTimer != null) {
            mTimer.cancel();
        }


    }

    @Override
    public void onDestroyView() {
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.onDestroyView();
    }


    @Override
    protected void onCameraConnecting(VdtCamera vdtCamera) {
        super.onCameraConnecting(vdtCamera);
        handleOnCameraConnecting();
    }

    @Override
    protected void onCameraDisconnected(VdtCamera vdtCamera) {
        super.onCameraDisconnected(vdtCamera);
        handleOnCameraDisconnected();

    }


    private void changeCurrentCamera(int position) {
        closeLiveRawData();
        stopCameraPreview();

        mVdtCameraManager.setCurrentCamera(position);
        mVdtCamera = mVdtCameraManager.getCurrentCamera();
        mVdbRequestQueue = mVdtCamera.getRequestQueue();
        mEventBus.post(new CameraConnectionEvent(CameraConnectionEvent.VDT_CAMERA_SELECTED_CHANGED, null));
//        Logger.t(TAG).d("changed vdtcamera to " + mVdtCamera.getName());
        initCameraPreview();
        openLiveViewData();
    }

    private void handleOnCameraConnected() {
        if (mCameraNoSignal != null) {
            mCameraNoSignal.setVisibility(View.GONE);
            mCameraConnecting.setVisibility(View.GONE);
        }

    }

    private void handleOnCameraConnecting() {
        if (mCameraNoSignal != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCameraNoSignal.setVisibility(View.GONE);
                    mCameraConnecting.setVisibility(View.VISIBLE);
                }
            });

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
                    mToolbar.getMenu().clear();
                }
            });
        }
    }

    protected void init() {
        mHandler = new Handler();
        if (VdtCameraManager.getManager().isConnected()) {
            mVdtCamera = getCamera();
            mVdbRequestQueue = mVdtCamera.getRequestQueue();
            if (mCameraNoSignal != null) {
                mCameraNoSignal.setVisibility(View.GONE);
                mCameraConnecting.setVisibility(View.GONE);
            }
        } else {
            handleOnCameraDisconnected();
        }

        if (mVdtCamera != null) {
            initViews();
        }
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
    }

    private void initViews() {
        mTimer = new Timer();
        mRecordTimeTask = new UpdateRecordTimeTask();
        mTimer.schedule(mRecordTimeTask, 1000, 1000);

        updateMicControlButton();
        // Start record red dot indicator animation
        AnimationDrawable animationDrawable = (AnimationDrawable) mRecordDot.getBackground();
        animationDrawable.start();
        handleOnCameraConnected();

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
                mLiveView.startStream(serverAddr, null, true);
            }

            mVdtCamera.getRecordRecMode();
            mVdtCamera.getRecordTime();
            mVdtCamera.getAudioMicState();
            mVdtCamera.getRecordResolutionList();
            mVdtCamera.GetSetup();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateCameraState();
                }
            });


        }

    }

    private void stopCameraPreview() {
        mLiveView.stopStream();
    }


    private void updateCameraState() {
        updateCameraStatusInfo();
        updateFloatActionButton();
        toggleRecordDot();
    }


    private void updateMicControlButton() {
        boolean micEnabled = mVdtCamera.isMicEnabled();
        if (micEnabled) {
            mBtnMicControl.setColorFilter(getResources().getColor(R.color.style_color_primary));
        } else {
            mBtnMicControl.clearColorFilter();
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
            mVdbRequestQueue.unregisterMessageHandler(VdbCommand.Factory.MSG_ClipInfo);
            mVdbRequestQueue.unregisterMessageHandler(VdbCommand.Factory.VDB_MSG_MarkLiveClipInfo);
        }
    }

    private void handleOnFabClicked() {
        switch (mVdtCamera.getRecordState()) {
            case VdtCamera.STATE_RECORD_RECORDING:
                if (isInCarMode()) {
                    mVdtCamera.markLiveVideo();
                    mBookmarkClickCount++;
                    showMessage();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            updateCameraStatusInfo();
                            hideMessage();
                        }
                    }, 1000);
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
            mIsGaugeVisible = true;
            mBtnShowOverlay.setColorFilter(getResources().getColor(R.color.style_color_primary));
        } else {
            mIsGaugeVisible = false;
            mBtnShowOverlay.clearColorFilter();
        }
        mGaugeView.showGauge(mIsGaugeVisible);
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
                    if (mBookmarkCount != -1) {
                        updateTvStatusAdditional(getResources().getQuantityString(R.plurals.number_of_bookmarks,
                            mBookmarkCount + mBookmarkClickCount,
                            mBookmarkCount + mBookmarkClickCount), View.VISIBLE);
                        mTvStatusAdditional.setVisibility(View.VISIBLE);
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
                if (isInCarMode()) {
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

    private void toggleRecordDot() {
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
//        Logger.t(TAG).d("WifiMode: " + mVdtCamera.getWifiMode());
        int wifiMode = mVdtCamera.getWifiMode();
        if (wifiMode == VdtCamera.WIFI_MODE_AP) {
            mWifiMode.setImageResource(R.drawable.rec_info_camera_mode_ap);
        } else if (wifiMode == VdtCamera.WIFI_MODE_CLIENT) {
            mWifiMode.setImageResource(R.drawable.rec_info_camera_mode_client);
        }

        // update storage info;
        VdtCamera.StorageInfo storageInfo = mVdtCamera.getStorageInfo();

//        Logger.t(TAG).d("totalSpace: " + storageInfo.totalSpace + " freeSpace: " + storageInfo.freeSpace);

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

    private class UpdateRecordTimeTask extends TimerTask {

        @Override
        public void run() {
            if (mVdtCamera != null) {
                mVdtCamera.getRecordTime();
//                updateCameraState();
            }
        }
    }


}
