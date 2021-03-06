package com.waylens.hachi.ui.liveview;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.camera.BtDevice;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.camera.events.CameraConnectionEvent;
import com.waylens.hachi.camera.events.CameraStateChangeEvent;
import com.waylens.hachi.camera.events.LineListEvent;
import com.waylens.hachi.camera.events.MarkLiveMsgEvent;
import com.waylens.hachi.camera.events.RectListEvent;
import com.waylens.hachi.rest.bean.Firmware;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
import com.waylens.hachi.snipe.toolbox.LiveRawDataRequest;
import com.waylens.hachi.snipe.vdb.ClipActionInfo;
import com.waylens.hachi.snipe.vdb.SpaceInfo;
import com.waylens.hachi.snipe.vdb.VdbReadyInfo;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.manualsetup.StartupActivity;
import com.waylens.hachi.ui.views.AnimationProgressBar;
import com.waylens.hachi.ui.views.RectListView;
import com.waylens.hachi.utils.FirmwareUpgradeHelper;
import com.waylens.hachi.utils.StringUtils;
import com.waylens.hachi.utils.rxjava.RxBus;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;
import com.waylens.hachi.view.gauge.GaugeView;
import com.xfdingustc.mjpegview.library.MjpegView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/10/26.
 */

public class LiveViewActivity extends BaseActivity {
    private static final String TAG = LiveViewActivity.class.getSimpleName();
    private Handler mHandler;
    private Timer mTimer;

    private UpdateRecordTimeTask mUpdateCameraStatusTimeTask;
    private UpdateStorageInfoTimeTask mUpdateStorageInfoTimerTask;

    private VdtCameraManager mVdtCameraManager = VdtCameraManager.getManager();
    private EventBus mEventBus = EventBus.getDefault();

    private Subscription mCameraStateChangeEventSubscription;
    private Subscription mUpdateCameraStatusEventSubscription;

    private Transition mDetailedInfoPanelTransition;

    private int mFabStartSrc;

    private int mFabStopSrc;

    private View mDetailInfoPanel;
    private ImageView mWifiMode;
    private TextView mWifiModeDescription;
    private TextView mHighlightSpace;
    private TextView mLoopRecordSpace;
    private TextView mRemoteStatus;
    private TextView mObdStatus;
    private TextView tvGpsStatus;
    private ImageView mDetailRemote;
    private ImageView ivDetailGps;
    private ImageView mDetailObd;

    @BindView(R.id.root_container)
    ViewGroup rootContainer;

    @BindView(R.id.camera_preview)
    MjpegView mLiveView;

    @BindView(R.id.spinner)
    Spinner mCameraSpinner;

    @BindView(R.id.tvCameraStatus)
    TextView mTvCameraRecStatus;

    @BindView(R.id.cardNotification)
    ImageView mCardNotification;

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

    @BindView(R.id.infoPanel)
    LinearLayout mInfoView;

    @BindView(R.id.remote_ctrl)
    ImageView mRemoteCtrl;

    @BindView(R.id.gpsStatus)
    ImageView ivGpsStatus;

    @BindView(R.id.obd)
    ImageView mObd;

    @BindView(R.id.storageView)
    AnimationProgressBar mStorageView;

    @BindView(R.id.recordDot)
    ImageView mRecordDot;

    @BindView(R.id.ivBatterStatus)
    ImageView mIvBatterStatus;


    @BindView(R.id.tvBatteryVol)
    TextView mTvBatteryVol;

    @BindView(R.id.cameraConnecting)
    LinearLayout mCameraConnecting;

    @BindView(R.id.noSignal)
    RelativeLayout mCameraNoSignal;

    @BindView(R.id.connectIndicator)
    ImageView mIvConnectIdicator;

    @BindView(R.id.errorPanel)
    LinearLayout mErrorPanel;

    @BindView(R.id.tvErrorIndicator)
    TextView mTvErrorIndicator;

    @BindView(R.id.rect_list_view)
    RectListView rectListView;


    @BindView(R.id.btnBookmark)
    ImageButton mBtnBookmark;


    @BindView(R.id.btnFullscreen)
    ImageButton mBtnFullScreen;

    @BindView(R.id.statusErrorLayout)
    FrameLayout mStatusErrorLayout;

    @BindView(R.id.controlPanel)
    RelativeLayout mControlPanel;

    @BindView(R.id.tv_space_left)
    TextView mTvSpaceLeft;

    @BindView(R.id.pull)
    ImageView mPull;



    @BindView(R.id.viewstub_detailed_info)
    ViewStub detailedInfo;

    @BindView(R.id.shutter_panel)
    LinearLayout shutterPanel;

    @BindString(R.string.on)
    String strOn;

    @BindString(R.string.no_gps_signal)
    String strGpsOff;

    @BindString(R.string.off)
    String strOff;


    @OnClick(R.id.pull)
    public void onPullClicked() {
        inflateDetailedPanel();
        if (mDetailInfoPanel.getVisibility() != View.VISIBLE) {
            TransitionManager.beginDelayedTransition(rootContainer, mDetailedInfoPanelTransition);
            mDetailInfoPanel.setVisibility(View.VISIBLE);
            ObjectAnimator dropDownAnimator = ObjectAnimator.ofFloat(mPull, View.ROTATION, 0, 180)
                .setDuration(300);
            dropDownAnimator.setInterpolator(new FastOutSlowInInterpolator());
            dropDownAnimator.start();
        } else {
            TransitionManager.beginDelayedTransition(rootContainer, mDetailedInfoPanelTransition);
            mDetailInfoPanel.setVisibility(View.GONE);
            ObjectAnimator dropDownAnimator = ObjectAnimator.ofFloat(mPull, View.ROTATION, 180, 0)
                .setDuration(300);
            dropDownAnimator.setInterpolator(new FastOutSlowInInterpolator());
            dropDownAnimator.start();

        }

    }

    @OnClick(R.id.btnMicControl)
    public void onBtnMicControlClicked() {
        if (mVdtCamera != null) {
            mVdtCamera.setMicEnabled(!mVdtCamera.isMicEnabled());
        }
    }


    @OnClick(R.id.btnFullscreen)
    public void onBtnFullScreenClicked() {
        if (!isFullScreen()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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
        boolean visibility = mGaugeView.getVisibility() != View.VISIBLE;
        showOverlay(visibility);
    }


    @OnClick(R.id.add_new_camera)
    public void onAddNewCameraClicked() {
        StartupActivity.launch(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraConnection(CameraConnectionEvent event) {
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_CONNECTED:
                Logger.t(TAG).d("on Camera connected");
                initCamera();
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
                handleCameraListChanged();
                break;
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventRectList(RectListEvent event) {
        rectListView.showRects(event.rectList, event.sourceRect);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventLineList(LineListEvent event) {
        rectListView.showLines(event.lineList, event.sourceRect);
    }


    public void onHandleCameraStateChangeEvent(CameraStateChangeEvent event) {
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

//                    Logger.t(TAG).d("record state: " + mVdtCamera.getRecordState());
                }
                break;
            case CameraStateChangeEvent.CAMERA_STATE_REC_ERROR:
                int error = (Integer) event.getExtra();
                Logger.t(TAG).d("On Rec Error: " + error);

                mErrorPanel.setVisibility(View.VISIBLE);
                switch (error) {
                    case VdtCamera.ERROR_START_RECORD_NO_CARD:
                        mTvErrorIndicator.setText(R.string.error_msg_no_card);
                        break;
                    case VdtCamera.ERROR_START_RECORD_CARD_ERROR:
                        mTvErrorIndicator.setText(R.string.error_sdcard_error);
                        break;
                    case VdtCamera.ERROR_START_RECORD_CARD_FULL:
                        mTvErrorIndicator.setText(R.string.error_sdcard_full);
                        break;
                }

                break;

            case CameraStateChangeEvent.CAMERA_STATE_BT_DEVICE_STATUS_CHANGED:
                updateBtDeviceState();
                break;

            case CameraStateChangeEvent.CAMEAR_STATE_MICROPHONE_STATUS_CHANGED:
                updateMicControlButton();
                break;
            case CameraStateChangeEvent.CAMEAR_STATE_REC_ROTATE:
                boolean ifRoate = (Boolean) event.getExtra();
                mGaugeView.setRotate(ifRoate);
                break;
        }

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
                    updateRecordState();
                    hideMessage();
                }
            }, 1000);
        }
    }


    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, LiveViewActivity.class);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

        setContentView(R.layout.activity_live_view);
        mCardNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MaterialDialog dialog = new MaterialDialog.Builder(LiveViewActivity.this)
                    .content(getString(R.string.low_card_space))
                    .positiveText(R.string.ok)
                    .build();
                dialog.show();
            }
        });
        mGaugeView.setGaugeMode(GaugeView.MODE_CAMERA);

        // Check if firmware need update:
        FirmwareUpgradeHelper.getNewerFirmwareRx()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<Firmware>() {
                @Override
                public void onNext(Firmware firmware) {
                    if (firmware != null) {
                        DialogHelper.showUpgradFirmwareConfirmDialog(LiveViewActivity.this, firmware, null);
                    }
                }
            });

    }


    @Override
    public void setupToolbar() {
        setupSpinner();
        getToolbar().getMenu().clear();
        if (VdtCameraManager.getManager().isConnected()) {
            getToolbar().inflateMenu(R.menu.menu_live_view);
        }
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.cameraSetting:
                        LiveViewSettingActivity.launch(LiveViewActivity.this);
                        break;
                }
                return false;
            }
        });
        getToolbar().setNavigationIcon(R.drawable.ic_close);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        super.setupToolbar();
    }


    @Override
    public void onStart() {
        super.onStart();
        initCamera();
        if (mVdtCamera != null) {
            mCameraNoSignal.setVisibility(View.GONE);
            mCameraConnecting.setVisibility(View.GONE);
            initViews();
        } else {
            handleOnCameraDisconnected();
        }
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
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

        registerRxBusEvent();


        setupToolbar();

        startPreview();


    }


    @Override
    public void onStop() {
        super.onStop();
        stopPreview();
        unregisterRxBusEvent();
        mEventBus.unregister(this);

    }

    public void handleCameraListChanged() {
        setupSpinner();
        if (mVdtCamera == null) {
            mLiveView.stopStream();
            mCameraNoSignal.setVisibility(View.VISIBLE);
            mCameraConnecting.setVisibility(View.GONE);
            mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
            AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
            animationDrawable.start();
            getToolbar().getMenu().clear();
        }
    }

    private void setupSpinner() {
        if (mVdtCameraManager.getConnectedCameras().size() > 1) {
            List<String> cameraNames = new ArrayList<>();
            List<VdtCamera> connectedCameras = mVdtCameraManager.getConnectedCameras();
            int index = -1;
            for (int i = 0; i < connectedCameras.size(); i++) {
                VdtCamera camera = connectedCameras.get(i);
                if (camera.getAddress() == mVdtCamera.getAddress()) {
                    index = i;
                }
                Logger.t(TAG).d("add one camera: " + camera.getName());
                cameraNames.add(camera.getName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, cameraNames);
            mCameraSpinner.setAdapter(adapter);
            if (index < 0) {
                mCameraSpinner.setSelection(0);
                changeCurrentCamera(0);
            } else {
                mCameraSpinner.setSelection(index);
            }
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
        } else if (mVdtCameraManager.getConnectedCameras().size() == 1) {
            if (mVdtCamera.getAddress() != mVdtCameraManager.getCurrentCamera().getAddress()) {
                changeCurrentCamera(0);
            }
            getToolbar().setTitle(R.string.live_view);
            mCameraSpinner.setVisibility(View.GONE);
        } else {
            getToolbar().setTitle(R.string.live_view);
            mVdtCamera = null;
            mCameraSpinner.setVisibility(View.GONE);
        }

    }

    protected void init() {
        mHandler = new Handler();
        mDetailedInfoPanelTransition = getTransition(R.transition.auto);
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
        mGaugeView.setRotate(mVdtCamera.getIfRotated());
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
            mVdtCamera.getSetup();
            updateCameraState();
            updateBtDeviceState();
            openLiveViewData();
        }

    }

    private void updateGpsStatus(List<RawDataItem> itemList) {
        inflateDetailedPanel();
        for (RawDataItem item : itemList) {
            if (item.getType() == RawDataItem.DATA_TYPE_GPS) {
                ivGpsStatus.setImageResource(R.drawable.gps_on);
                ivDetailGps.setImageResource(R.drawable.gps_on);
                tvGpsStatus.setText(strOn);
                return;
            }
        }

        ivGpsStatus.setImageResource(R.drawable.gps_off);
        ivDetailGps.setImageResource(R.drawable.gps_off);
        tvGpsStatus.setText(strGpsOff);

    }

    private void registerRxBusEvent() {
        mCameraStateChangeEventSubscription = RxBus.getDefault().toObserverable(CameraStateChangeEvent.class)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<CameraStateChangeEvent>() {
                @Override
                public void onNext(CameraStateChangeEvent cameraStateChangeEvent) {
                    onHandleCameraStateChangeEvent(cameraStateChangeEvent);
                }
            });

        mUpdateCameraStatusEventSubscription = RxBus.getDefault().toObserverable(UpdateCameraStatusEvent.class)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<UpdateCameraStatusEvent>() {
                @Override
                public void onNext(UpdateCameraStatusEvent updateCameraStatusEvent) {
                    updateCameraState();
                }
            });
    }


    private void unregisterRxBusEvent() {
        if (!mCameraStateChangeEventSubscription.isUnsubscribed()) {
            mCameraStateChangeEventSubscription.unsubscribe();
        }

        if (!mUpdateCameraStatusEventSubscription.isUnsubscribed()) {
            mUpdateCameraStatusEventSubscription.unsubscribe();
        }
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
            mDetailInfoPanel.setVisibility(View.GONE);
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

            mBtnFullScreen.setImageResource(R.drawable.ic_fullscreen_exit);

            RelativeLayout.LayoutParams shutterParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            shutterParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            shutterPanel.setLayoutParams(shutterParam);
        } else {
            mBtnBookmark.setImageResource(R.drawable.camera_control_bookmark);
            mFabStopSrc = R.drawable.camera_control_stop;
            mFabStartSrc = R.drawable.camera_control_start;

            getToolbar().setVisibility(View.VISIBLE);

            mInfoView.setVisibility(View.VISIBLE);


            RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            params2.addRule(RelativeLayout.BELOW, mInfoView.getId());
            mLiveViewLayout.setLayoutParams(params2);

            RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            params3.addRule(RelativeLayout.BELOW, mLiveViewLayout.getId());
            mControlPanel.setLayoutParams(params3);

            mBtnFullScreen.setImageResource(R.drawable.ic_fullscreen);

            RelativeLayout.LayoutParams shutterParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            shutterParam.addRule(RelativeLayout.BELOW, mControlPanel.getId());
            shutterPanel.setLayoutParams(shutterParam);
        }
        updateFloatActionButton();
        setImmersiveMode(isFullScreen());
    }

    @Override
    public void onBackPressed() {
        if (this.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setImmersiveMode(false);
        } else {
            super.onBackPressed();
        }
    }

    private boolean isFullScreen() {
        int orientation = getRequestedOrientation();
        return orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }

    public void startPreview() {
        initCameraPreview();

        Logger.t(TAG).d("start preview");
        if (mTimer == null) {
            mTimer = new Timer();
            mUpdateCameraStatusTimeTask = new UpdateRecordTimeTask();
            mUpdateStorageInfoTimerTask = new UpdateStorageInfoTimeTask();
            mTimer.schedule(mUpdateCameraStatusTimeTask, 1000, 1000);
            mTimer.schedule(mUpdateStorageInfoTimerTask, 1000, 10000);
        } else {
            Logger.t(TAG).d("already has timer");
        }
    }

    public void stopPreview() {
        if (mTimer != null) {
            Logger.t(TAG).d("close timer");
            mTimer.cancel();
            mTimer = null;
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
        initViews();
        initCameraPreview();
    }

    private void handleOnCameraConnected() {
        mCameraNoSignal.setVisibility(View.GONE);
        mCameraConnecting.setVisibility(View.GONE);
    }


    private void handleOnCameraDisconnected() {

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


    private void stopCameraPreview() {
        if (mLiveView != null) {
            mLiveView.stopStream();
        }
        closeLiveRawData();
    }


    private void updateCameraState() {
        updateCameraInfoPanel();
        updateRecordState();
        updateFloatActionButton();
        toggleRecordDot();
    }

    private void updateSpaceInfo() {
        if (mInfoView == null || mInfoView.getVisibility() != View.VISIBLE) {
            return;
        }

        SnipeApiRx.getSpaceInfoRx()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<SpaceInfo>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    mStorageView.getProgressDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                }

                @Override
                public void onNext(SpaceInfo spaceInfo) {
                    Logger.t(TAG).d("response: " + spaceInfo.toString());
                    mStorageView.getProgressDrawable().clearColorFilter();
                    mStorageView.setMax((int) (spaceInfo.total / (1000 * 1000)));
                    mStorageView.setProgress((int) (spaceInfo.marked / (1000 * 1000)));
                    mStorageView.setSecondaryProgress((int) (spaceInfo.total / (1000 * 1000)));
                    mTvSpaceLeft.setText(StringUtils.getSpaceString(spaceInfo.getLoopedSpace()) + " " + getString(R.string.ready_to_record));

                    inflateDetailedPanel();
                    mHighlightSpace.setText(StringUtils.getSpaceString(spaceInfo.marked));
                    mLoopRecordSpace.setText(StringUtils.getSpaceString(spaceInfo.getLoopedSpace()));
                    Logger.t(TAG).d(spaceInfo.total - spaceInfo.used);
                    if (spaceInfo.getLoopedSpace() < (long) 8 * 1000 * 1000 * 1000) {
                        Logger.t(TAG).d("show notification");
                        mCardNotification.setVisibility(View.VISIBLE);
                    } else {
                        mCardNotification.setVisibility(View.INVISIBLE);
                    }
                }
            });

    }


    private void updateMicControlButton() {
        if (mVdtCamera != null) {
            boolean micEnabled = mVdtCamera.isMicEnabled();
            if (micEnabled) {
                mBtnMicControl.setImageResource(R.drawable.ic_mic);
            } else {
                mBtnMicControl.setImageResource(R.drawable.ic_mic_off);
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
//                        Logger.t(TAG).d("LiveRawDataResponse: " + response);
                    }
                }, new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("LiveRawDataResponse ERROR", error);
                }
            });

            mVdbRequestQueue.add(request);

            mVdtCamera.setOnRawDataItemUpdateListener(mRawDataUpdateHandler);
        }
    }

    private VdtCamera.OnRawDataUpdateListener mRawDataUpdateHandler = new VdtCamera.OnRawDataUpdateListener() {

        @Override
        public void OnRawDataUpdate(VdtCamera camera, List<RawDataItem> item) {
            if (mVdtCamera != camera) {
                return;
            }

            if (item != null) {
                mGaugeView.updateRawDateItem(item);
                updateGpsStatus(item);
            }
        }
    };


    private void closeLiveRawData() {
        if (mVdbRequestQueue != null) {
            LiveRawDataRequest request = new LiveRawDataRequest(0, new
                VdbResponse.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
//                        Logger.t(TAG).d("LiveRawDataResponse: " + response);
                    }
                }, new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
//                    Logger.t(TAG).e("LiveRawDataResponse ERROR", error);
                }
            });
            mVdbRequestQueue.add(request);
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
            mBtnShowOverlay.setImageResource(R.drawable.ic_btn_gauge_overlay_s);
        } else {
            mBtnShowOverlay.setImageResource(R.drawable.ic_btn_gauge_overlay_n);
        }
        //mGaugeView.showGauge(mIsGaugeVisible);
        mGaugeView.setVisibility(isGaugeVisible ? View.VISIBLE : View.INVISIBLE);
    }


    private void updateRecordState() {
        int recState = mVdtCamera.getRecordState();
//        Logger.t(TAG).d("rec state: " + recState);
        switch (recState) {
            case VdtCamera.STATE_RECORD_UNKNOWN:
                mTvCameraRecStatus.setText(R.string.record_unknown);
                break;
            case VdtCamera.STATE_RECORD_STOPPED:
                mTvCameraRecStatus.setText(R.string.record_stopped);
                mStorageView.showIndicator(false);
                break;
            case VdtCamera.STATE_RECORD_STOPPING:
                mTvCameraRecStatus.setText(R.string.record_stopping);
                break;
            case VdtCamera.STATE_RECORD_STARTING:
                mTvCameraRecStatus.setText(R.string.record_starting);
                mErrorPanel.setVisibility(View.GONE);
                break;
            case VdtCamera.STATE_RECORD_RECORDING:
                if (isInCarMode()) {
                    String recStatusText = mTvCameraRecStatus.getText().toString();
                    if (recStatusText.isEmpty()) {
                        mTvCameraRecStatus.setText(R.string.continuous_recording);
                    }
                    if (!mStorageView.isAnimating()) {
                        mStorageView.showIndicator(true);
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


    private void updateCameraInfoPanel() {
        if (mInfoView.getVisibility() != View.VISIBLE) {
            return;
        }

        int batterVol = mVdtCamera.getBatteryVolume();


        mIvBatterStatus.setImageResource(BatteryImageViewResHelper.getBatteryViewRes(mVdtCamera.getBatteryVolume(), mVdtCamera.getBatteryState()));

        String batterVolString = "" + batterVol + "%";
        mTvBatteryVol.setText(batterVolString);

        // update Wifi info:
        //WifiState wifiState = mVdtCamera.getWifiStates();
//        Logger.t(TAG).d("WifiMode: " + mVdtCamera.getWifiMode());
        inflateDetailedPanel();
        int wifiMode = mVdtCamera.getWifiMode();
        if (wifiMode == VdtCamera.WIFI_MODE_AP) {
            mWifiMode.setImageResource(R.drawable.rec_info_camera_mode_ap);
            mWifiModeDescription.setText("Your phone is directly connected to camera");
        } else if (wifiMode == VdtCamera.WIFI_MODE_CLIENT) {
            mWifiMode.setImageResource(R.drawable.rec_info_camera_mode_client);
            mWifiModeDescription.setText("Your phone is connected to camera through router");
        }
    }


    private void inflateDetailedPanel() {
        if (mDetailInfoPanel == null) {
            View view = detailedInfo.inflate();
            mDetailInfoPanel = view.findViewById(R.id.detail_info_panel);
            mHighlightSpace = (TextView) view.findViewById(R.id.highlight_space);
            mLoopRecordSpace = (TextView) view.findViewById(R.id.loop_record_space);
            mRemoteStatus = (TextView) view.findViewById(R.id.remote_ctrl_status);
            mObdStatus = (TextView) view.findViewById(R.id.obd_ctrl_status);
            tvGpsStatus = (TextView) view.findViewById(R.id.gps_status);
            mWifiMode = (ImageView) view.findViewById(R.id.wifiMode);
            mWifiModeDescription = (TextView) view.findViewById(R.id.wifi_mode_description);
            mDetailRemote = (ImageView) view.findViewById(R.id.detail_remote);
            ivDetailGps = (ImageView) view.findViewById(R.id.detail_gps);
            mDetailObd = (ImageView) view.findViewById(R.id.detail_obd);
        }
    }

    private void updateBtDeviceState() {
        BtDevice obdState = mVdtCamera.getObdDevice();
        BtDevice remoteCtrState = mVdtCamera.getRemoteCtrlDevice();
        inflateDetailedPanel();

        if (obdState.getState() != BtDevice.BT_DEVICE_STATE_ON) {
            mObd.setAlpha(0.2f);
            mDetailObd.setAlpha(0.2f);
            mObdStatus.setText(strOff);
        } else {
            mObd.setAlpha(1.0f);
            mDetailObd.setAlpha(1.0f);
            mObdStatus.setText(strOn);
        }

        if (remoteCtrState.getState() != BtDevice.BT_DEVICE_STATE_ON) {
            mRemoteCtrl.setAlpha(0.2f);
            mDetailRemote.setAlpha(0.2f);
            mRemoteStatus.setText(strOff);
        } else {
            mRemoteCtrl.setAlpha(1.0f);
            mDetailRemote.setAlpha(1.0f);
            mRemoteStatus.setText(strOn);
        }
    }


    private static class UpdateRecordTimeTask extends TimerTask {

        @Override
        public void run() {
            VdtCamera vdtCamera = VdtCameraManager.getManager().getCurrentCamera();
            if (vdtCamera != null) {
                vdtCamera.getRecordTime();
                RxBus.getDefault().post(new UpdateCameraStatusEvent());
            }
        }
    }

    private class UpdateStorageInfoTimeTask extends TimerTask {

        @Override
        public void run() {
            if (mVdtCamera != null && mVdtCamera.getRecordState() == VdtCamera.STATE_RECORD_RECORDING) {
                updateSpaceInfo();
            }
        }
    }
}
