package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.CameraState;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipInfoMsgHandler;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.snipe.toolbox.LiveRawDataRequest;
import com.waylens.hachi.snipe.toolbox.MarkLiveMsgHandler;
import com.waylens.hachi.snipe.toolbox.RawDataMsgHandler;
import com.waylens.hachi.ui.views.PercentageView;
import com.waylens.hachi.vdb.ClipActionInfo;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;
import com.waylens.hachi.vdb.RemoteClip;
import com.waylens.hachi.views.camerapreview.CameraLiveView;
import com.waylens.hachi.views.dashboard.DashboardLayout;
import com.waylens.hachi.views.dashboard.adapters.SimulatorRawDataAdapter;
import com.xfdingustc.far.FixedAspectRatioFrameLayout;

import java.net.InetSocketAddress;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/12/10.
 */


public class CameraControlActivity2 extends BaseActivity {
    private static final String TAG = CameraControlActivity2.class.getSimpleName();
    private static final String TAG_GET_BOOKMARK_COUNT = "get.bookmark.count";

    private VdtCamera mVdtCamera;
    private SimulatorRawDataAdapter mRawDataAdapter;

    private static final String IS_PC_SERVER = "isPcServer";
    private static final String SSID = "ssid";
    private static final String HOST_STRING = "hostString";

    private int mCurrentOrientation;
    private Handler mHandler = new Handler();
    private VdbRequestQueue mVdbRequestQueue;
    int mBookmarkCount = -1;
    int mBookmarkClickCount;

    public static void launch(Activity startingActivity, VdtCamera camera) {
        Intent intent = new Intent(startingActivity, CameraControlActivity2.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_PC_SERVER, camera.isPcServer());
        bundle.putString(SSID, camera.getSSID());
        bundle.putString(HOST_STRING, camera.getHostString());
        intent.putExtras(bundle);
        startingActivity.startActivity(intent);
    }


    @Bind(R.id.cameraPreview)
    CameraLiveView mLiveView;

    @Nullable
    @Bind(R.id.tvCameraStatus)
    TextView mTvCameraStatus;

    @Nullable
    @Bind(R.id.tv_status_additional)
    TextView mTvStatusAdditional;

    @Bind(R.id.btnMicControl)
    ImageView mBtnMicControl;

    @Bind(R.id.fabBookmark)
    ImageButton mFabBookmark;

    @Bind(R.id.dashboard)
    DashboardLayout mDashboard;

    @Bind(R.id.liveViewLayout)
    FixedAspectRatioFrameLayout mLiveViewLayout;

    @Bind(R.id.btnShowOverlay)
    ImageButton mBtnShowOverlay;

    @Bind(R.id.btnFullscreen)
    ImageButton mBtnFullScreen;

    @Bind(R.id.btnWaterLine)
    ImageButton mBtnWaterLine;

    @Nullable
    @Bind(R.id.sharp_view)
    View mSharpView;

    @Nullable
    @Bind(R.id.bookmark_message_view)
    View mBookmarkMsgView;


    @Nullable
    @Bind(R.id.infoPanel)
    LinearLayout mInfoView;

    @Nullable
    @Bind(R.id.storageView)
    PercentageView mStorageView;

    @Nullable
    @Bind(R.id.legend)
    LinearLayout mLegendLayout;


    @OnClick(R.id.fabBookmark)
    public void onFabClick() {
        handleOnFabClicked();
    }

    @OnClick(R.id.btnShowOverlay)
    public void onBtnShowOverlayClick() {
        if (mDashboard.getVisibility() == View.VISIBLE) {
            hideOverlay();
        } else {
            mDashboard.setVisibility(View.VISIBLE);
            initDashboardLayout();
            mBtnShowOverlay.setColorFilter(getResources().getColor(R.color.style_color_primary));
            requestLiveRawData();
            mSharpView.setVisibility(View.INVISIBLE);
            mBtnWaterLine.clearColorFilter();
        }
    }


    @OnClick(R.id.btnFullscreen)
    public void onBtnFullscreenClick() {
        toggleFullScreen();
    }

    @OnClick(R.id.btnWaterLine)
    public void onBtnWaterLineClicked() {
        toggleSharpView();
    }


    @Nullable
    @OnClick(R.id.btn_info)
    public void onBtnInfoViewClicked() {
        toggleInfoView();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getBookmarkCount();
    }


    @Override
    protected void onPause() {
        super.onPause();
        hideOverlay();
    }


    @Override
    protected void onDestroy() {
        if (mVdtCamera != null) {
            mVdtCamera.setOnStateChangeListener(null);
        }
        super.onDestroy();

    }

    @Override
    protected void init() {
        super.init();
        mVdbRequestQueue = Snipe.newRequestQueue();
        initViews();
    }

    private void initViews() {
        mCurrentOrientation = getResources().getConfiguration().orientation;
        if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        }
        setContentView(R.layout.activity_camera_control2);

        if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mBtnFullScreen.setImageResource(R.drawable.screen_narrow);
        } else {
            mBtnFullScreen.setImageResource(R.drawable.screen_full);
        }
        setupToolbar();
        initCameraPreview();

        updateMicControlButton();
        mRawDataAdapter = new SimulatorRawDataAdapter();
        mDashboard.setAdapter(mRawDataAdapter);
        if (mInfoView != null) {
            mInfoView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void setupToolbar() {
        if (mToolbar != null) {
            mToolbar.setTitle(R.string.live_view);
        }
        super.setupToolbar();
    }

    private void initCameraPreview() {
        mVdtCamera = getCameraFromIntent(null);
        mLiveView.setBackgroundColor(Color.BLACK);
        if (mVdtCamera != null) {
            InetSocketAddress serverAddr = mVdtCamera.getPreviewAddress();
            if (serverAddr == null) {
                mVdtCamera = null;
            } else {
                mVdtCamera.setOnStateChangeListener(mOnStateChangeListener);
                mLiveView.startStream(serverAddr, null, true);
            }

            mVdtCamera.startPreview();
            mVdtCamera.getRecordRecMode();
            mVdtCamera.getCameraTime();
            mVdtCamera.getAudioMicState();
            mVdtCamera.getRecordResolutionList();
            mVdtCamera.GetSetup();
            updateCameraState(mVdtCamera.getState());

        }

    }

    private void initDashboardLayout() {
        int width = mLiveViewLayout.getMeasuredWidth();
        int height = mLiveViewLayout.getMeasuredHeight();
        float widthScale = (float) width / DashboardLayout.NORMAL_WIDTH;
        float heightScale = (float) height / DashboardLayout.NORMAL_HEIGHT;
        mDashboard.setScaleX(widthScale);
        mDashboard.setScaleY(heightScale);
    }

    private void updateCameraState(final CameraState state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateCameraStatusInfo(state);
                updateFloatActionButton(state);
            }
        });
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

    private void updateCameraStatusInfo(CameraState state) {
        int recState = state.getRecordState();
        switch (recState) {
            case CameraState.STATE_RECORD_UNKNOWN:
                mTvCameraStatus.setText(R.string.record_unknown);
                break;
            case CameraState.STATE_RECORD_STOPPED:
                mTvCameraStatus.setText(R.string.record_stopped);
                break;
            case CameraState.STATE_RECORD_STOPPING:
                mTvCameraStatus.setText(R.string.record_stopping);
                break;
            case CameraState.STATE_RECORD_STARTING:
                mTvCameraStatus.setText(R.string.record_starting);
                break;
            case CameraState.STATE_RECORD_RECORDING:
                if (isInCarMode(state)) {
                    if (mTvCameraStatus != null) {
                        mTvCameraStatus.setText(R.string.continuous_recording);
                        if (mBookmarkCount != -1) {
                            updateTvStatusAdditional(getResources().getQuantityString(R.plurals.number_of_bookmarks,
                                mBookmarkCount + mBookmarkClickCount,
                                mBookmarkCount + mBookmarkClickCount), View.VISIBLE);

                        }
                    }
                } else {
                    mTvCameraStatus.setText(R.string.record_recording);
                }
                break;
            case CameraState.STATE_RECORD_SWITCHING:
                mTvCameraStatus.setText(R.string.record_switching);
                break;
            default:
                break;
        }
        if (recState != CameraState.STATE_RECORD_RECORDING) {
            if (mTvStatusAdditional != null) {
                mTvStatusAdditional.setVisibility(View.GONE);
            }
        }
    }


    boolean isInCarMode(CameraState state) {
        return state != null && state.getRecordMode() == CameraState.Rec_Mode_AutoStart;
    }

    private void updateFloatActionButton(CameraState state) {
        switch (state.getRecordState()) {
            case CameraState.STATE_RECORD_UNKNOWN:
                break;
            case CameraState.STATE_RECORD_STOPPED:
                mFabBookmark.setEnabled(true);
                mFabBookmark.setBackground(getResources().getDrawable(R.drawable.camera_control_start));
                break;
            case CameraState.STATE_RECORD_STOPPING:
                mFabBookmark.setEnabled(false);
                break;
            case CameraState.STATE_RECORD_STARTING:
                mFabBookmark.setEnabled(false);
                break;
            case CameraState.STATE_RECORD_RECORDING:
                mFabBookmark.setEnabled(true);
                if (isInCarMode(state)) {
                    mFabBookmark.setBackground(getResources().getDrawable(R.drawable
                        .camera_control_bookmark));
                } else {
                    mFabBookmark.setBackground(getResources().getDrawable(R.drawable.camera_control_stop));
                }
                break;
            case CameraState.STATE_RECORD_SWITCHING:
                mFabBookmark.setEnabled(false);
                break;
            default:
                break;
        }

    }

    private void updateMicControlButton() {
        boolean micEnabled = mVdtCamera.isMicEnabled();
        if (micEnabled) {
            mBtnMicControl.setColorFilter(getResources().getColor(R.color.style_color_primary));
        } else {
            mBtnMicControl.clearColorFilter();
        }
    }


    private void handleOnFabClicked() {
        switch (mVdtCamera.getState().getRecordState()) {
            case CameraState.STATE_RECORD_RECORDING:
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
            case CameraState.STATE_RECORD_STOPPED:
                mVdtCamera.startRecording();
                break;
        }
    }

    void hideMessage() {
        mBookmarkMsgView.setVisibility(View.GONE);
    }

    void showMessage() {
        mBookmarkMsgView.setVisibility(View.VISIBLE);
    }

    void hideOverlay() {
        mDashboard.setVisibility(View.INVISIBLE);
        mBtnShowOverlay.clearColorFilter();
        closeLiveRawData();
    }

    void getBookmarkCount() {
        Bundle parameter = new Bundle();
        parameter.putInt(ClipSetRequest.PARAMETER_TYPE, RemoteClip.TYPE_MARKED);
        parameter.putInt(ClipSetRequest.PARAMETER_FLAG, ClipSetRequest.FLAG_CLIP_EXTRA);
        mVdbRequestQueue.add(new ClipSetRequest(ClipSetRequest.METHOD_GET, parameter,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet clipSet) {
                    if (clipSet != null) {
                        mBookmarkCount = clipSet.getCount();
                        mBookmarkClickCount = 0;
                        Log.e("test", "Current bookmarks: " + mBookmarkCount);
                        if (mVdtCamera != null) {
                            updateCameraStatusInfo(mVdtCamera.getState());
                        }
                    }
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Log.e("test", "ClipSetRequest: " + error);
                }
            }).setTag(TAG_GET_BOOKMARK_COUNT));
    }

    void registerMessageHandler() {
        RawDataMsgHandler rawDataMsgHandler = new RawDataMsgHandler(new VdbResponse.Listener<RawDataItem>() {
            @Override
            public void onResponse(RawDataItem response) {
                //Log.e("test", String.format("RawDataMsgHandler: Type[%d]:[%s]", response.dataType, response.object));
                if (mDashboard != null) {
                    mDashboard.updateLive(response);
                }
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e("test", "RawDataMsgHandler ERROR", error);
            }
        });
        mVdbRequestQueue.registerMessageHandler(rawDataMsgHandler);

        ClipInfoMsgHandler clipInfoMsgHandler = new ClipInfoMsgHandler(
            new VdbResponse.Listener<ClipActionInfo>() {
                @Override
                public void onResponse(ClipActionInfo response) {
                    Log.e("test", response.toString());
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Log.e("test", "ClipInfoMsgHandler ERROR", error);
                }
            });
        mVdbRequestQueue.registerMessageHandler(clipInfoMsgHandler);

        MarkLiveMsgHandler markLiveMsgHandler = new MarkLiveMsgHandler(
            new VdbResponse.Listener<ClipActionInfo>() {
                @Override
                public void onResponse(ClipActionInfo response) {
                    Log.e("test", response.toString());
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Log.e("test", "MarkLiveMsgHandler ERROR", error);
                }
            });
        mVdbRequestQueue.registerMessageHandler(markLiveMsgHandler);
    }

    void requestLiveRawData() {
        LiveRawDataRequest request = new LiveRawDataRequest(RawDataBlock.F_RAW_DATA_GPS +
            RawDataBlock.F_RAW_DATA_ACC + RawDataBlock.F_RAW_DATA_ODB, new
            VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Log.e("test", "LiveRawDataResponse: " + response);
                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e("test", "LiveRawDataResponse ERROR", error);
            }
        });
        mVdbRequestQueue.add(request);
        registerMessageHandler();
    }

    void closeLiveRawData() {
        LiveRawDataRequest request = new LiveRawDataRequest(0, new
            VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Log.e("test", "LiveRawDataResponse: " + response);
                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e("test", "LiveRawDataResponse ERROR", error);
            }
        });
        mVdbRequestQueue.add(request);
        mVdbRequestQueue.unregisterMessageHandler(VdbCommand.Factory.MSG_RawData);
        mVdbRequestQueue.unregisterMessageHandler(VdbCommand.Factory.MSG_ClipInfo);
        mVdbRequestQueue.unregisterMessageHandler(VdbCommand.Factory.VDB_MSG_MarkLiveClipInfo);
    }

    private void toggleFullScreen() {
        if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void toggleSharpView() {
        if (mSharpView.getVisibility() == View.VISIBLE) {
            mSharpView.setVisibility(View.INVISIBLE);
            mBtnWaterLine.clearColorFilter();
        } else {
            mSharpView.setVisibility(View.VISIBLE);
            mBtnWaterLine.setColorFilter(getResources().getColor(R.color.style_color_primary));
            hideOverlay();
        }
    }

    private void toggleInfoView() {
        if (mInfoView != null) {
            int visibility = mInfoView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            mInfoView.setVisibility(visibility);
            if (visibility == View.VISIBLE) {
                updateCameraStorageInfo();
            }
        }
    }


    private void updateCameraStorageInfo() {
        VdtCamera.StorageInfo storageInfo = mVdtCamera.getStorageInfo();

        Logger.t(TAG).d("Total Space: " + storageInfo.totalSpace + " Free space: " + storageInfo
            .freeSpace);
        mStorageView.setMax(storageInfo.totalSpace);
        mStorageView.setProgress(storageInfo.totalSpace - storageInfo.freeSpace);

    }

    private void updateTvStatusAdditional(String text, int visible) {
        if (mTvStatusAdditional != null) {
            mTvStatusAdditional.setText(text);
            mTvStatusAdditional.setVisibility(visible);
        }
    }


}
