package com.waylens.hachi.ui.community;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.exoplayer.util.PlayerControl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.CachedJsonRequest;
import com.waylens.hachi.player.HachiPlayer;
import com.waylens.hachi.player.HlsRendererBuilder;
import com.waylens.hachi.player.Utils;
import com.waylens.hachi.rest.HachiApiRx;
import com.waylens.hachi.rest.body.LapInfo;
import com.waylens.hachi.rest.response.MomentInfo;
import com.waylens.hachi.rest.response.MomentPlayInfo;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.snipe.remix.AvrproLapData;
import com.waylens.hachi.snipe.remix.AvrproLapTimerResult;
import com.waylens.hachi.snipe.remix.AvrproLapsHeader;
import com.waylens.hachi.snipe.utils.ToStringUtils;
import com.waylens.hachi.snipe.vdb.rawdata.GpsData;
import com.waylens.hachi.snipe.vdb.rawdata.IioData;
import com.waylens.hachi.snipe.vdb.rawdata.ObdData;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;
import com.waylens.hachi.snipe.vdb.rawdata.WeatherData;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.utils.DebugHelper;
import com.waylens.hachi.view.gauge.GaugeView;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;
import com.xfdingustc.mdplaypausebutton.PlayPauseButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;
import rx.schedulers.Schedulers;


public class MomentPlayFragment extends BaseFragment implements SurfaceHolder.Callback, HachiPlayer.Listener {
    private static final String TAG = MomentPlayFragment.class.getSimpleName();

    private static final String EXTRA_MOMENT_ID = "extra_moment_id";
    private static final String EXTRA_MOMENT_COVER = "extra_moment_cover";

    private static final int FADE_OUT = 1;
    private static final int ADD_VIEW_COUNT = 2;


    private static final int DEFAULT_TIMEOUT = 3000;
    private static final long MAX_PROGRESS = 1000L;

    protected static final int RAW_DATA_STATE_UNKNOWN = -1;
    protected static final int RAW_DATA_STATE_READY = 0;
    protected static final int RAW_DATA_STATE_ERROR = 1;


    protected int mRawDataState = RAW_DATA_STATE_UNKNOWN;

    protected boolean mOverlayShouldDisplay = true;

    private boolean mViewCountSend = false;

    private HachiPlayer mMediaPlayer;
    private PlayerControl mPlayerControl;

    private SurfaceHolder mSurfaceHolder;

    private VideoHandler mHandler;

    private MomentInfo mMoment;

    private long mMomentId;

    private String mMomentCover;

    private MomentPlayInfo mMomentPlayInfo;

    private List<RawDataTimeInfo> mRawDataTimeInfoList = new ArrayList<>();

    private boolean mIsActivityStopped = false;

    private Timer mTimer;
    private UpdatePlayTimeTask mUpdatePlayTimeTask;

    private RequestQueue mRequestQueue;

    JSONObject mLapInfo;

    private MomentRawDataAdapter mRawDataAdapter = new MomentRawDataAdapter();

    private boolean playerNeedsPrepare;

    @BindView(R.id.video_thumbnail)
    ImageView mVsCover;

    @BindView(R.id.video_controllers)
    FrameLayout mVideoController;

    @BindView(R.id.btn_play_pause)
    PlayPauseButton mBtnPlayPause;

    @BindView(R.id.video_surface)
    SurfaceView mSurfaceView;

    @BindView(R.id.progress_loading)
    ProgressBar mProgressLoading;

    @BindView(R.id.video_seek_bar)
    SeekBar mVideoSeekBar;

    @BindView(R.id.btn_fullscreen)
    ImageView mBtnFullScreen;

    @BindView(R.id.text_video_time)
    TextView mVideoTime;

    @BindView(R.id.text_video_duration)
    TextView mVideoDuration;

    @BindView(R.id.infoPanel)
    LinearLayout mInfoPanel;

    @BindView(R.id.gaugeView)
    GaugeView mGaugeView;

    @BindView(R.id.bottom_progress_bar)
    ProgressBar mBottomProgressBar;

    @BindView(R.id.btnShowOverlay)
    ImageButton mBtnShowOverlay;

    @OnClick(R.id.btn_play_pause)
    public void onBtnPlayClicked() {
        if (mPlayerControl == null) {
            start();
        } else {
            if (mPlayerControl.isPlaying()) {
                mPlayerControl.pause();
            } else {
                mPlayerControl.start();
            }
        }

    }

    @OnClick(R.id.video_surface)
    public void onSurfaceClicked() {
//        onBtnPlayClicked();
        if (mMomentPlayInfo == null || mMomentPlayInfo.videoUrl == null) {
            return;
        }
        showControllers();
    }

    @OnClick(R.id.btn_fullscreen)
    public void onBtnFullScreenClicked() {
        if (!isFullScreen(getResources().getConfiguration())) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @OnClick(R.id.btnShowOverlay)
    public void onBtnShowOverlayClicked() {
        boolean isGaugeVisible = mGaugeView.getVisibility() != View.VISIBLE;
        if (isGaugeVisible) {
            mBtnShowOverlay.setImageResource(R.drawable.ic_btn_gauge_overlay_s);
        } else {
            mBtnShowOverlay.setImageResource(R.drawable.ic_btn_gauge_overlay_n);
        }
        //mGaugeView.showGauge(mIsGaugeVisible);
        mGaugeView.setVisibility(isGaugeVisible ? View.VISIBLE : View.INVISIBLE);
    }

    public static MomentPlayFragment newInstance(long momentId, String thumbnail) {
        MomentPlayFragment fragment = new MomentPlayFragment();
        Bundle args = new Bundle();
        args.putLong(EXTRA_MOMENT_ID, momentId);
        args.putString(EXTRA_MOMENT_COVER, thumbnail);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected String getRequestTag() {
        return TAG;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
//        mMoment = (MomentInfo) args.getSerializable("moment");
        mMomentId = args.getLong(EXTRA_MOMENT_ID);
        mMomentCover = args.getString(EXTRA_MOMENT_COVER);

        mRequestQueue = Volley.newRequestQueue(getActivity(), 500 * 1000 * 1000);
        mRequestQueue.start();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);

        getActivity().getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    showControllers();
                }
            }
        });
        getMomentPlayInfo();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_moment_play, container, false);
        ButterKnife.bind(this, view);
        mHandler = new VideoHandler(this);
        mGaugeView.setGaugeMode(GaugeView.MODE_MOMENT);
        initViews();
        return view;
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        Logger.t(TAG).d("playWhenReady: " + playWhenReady + " playbackState: " + playbackState);
        switch (playbackState) {
            case HachiPlayer.STATE_ENDED:
                setProgress(0, mPlayerControl.getDuration());
                releasePlayer();

//                ClipSetPos clipSetPos = new ClipSetPos(0, getClipSet().getClip(0).getStartTimeMs());
//                setClipSetPos(clipSetPos, true);
                break;
        }
        updateControls(playWhenReady, playbackState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isFullScreen(newConfig)) {
            mBtnFullScreen.setImageResource(R.drawable.ic_fullscreen_exit);
        } else {
            mBtnFullScreen.setImageResource(R.drawable.ic_fullscreen);
        }
        ((BaseActivity) getActivity()).setImmersiveMode(isFullScreen(newConfig));
    }


    private void updateControls(boolean playwhenReady, int playbackState) {
        switch (playbackState) {
            case HachiPlayer.STATE_IDLE:
            case HachiPlayer.STATE_ENDED:
                mVsCover.setVisibility(View.VISIBLE);
                mProgressLoading.setVisibility(View.INVISIBLE);
                mBtnPlayPause.toggle(true);
                break;
            case HachiPlayer.STATE_PREPARING:
                mVsCover.setVisibility(View.VISIBLE);
                mProgressLoading.setVisibility(View.VISIBLE);
                break;

            case HachiPlayer.STATE_BUFFERING:
                mProgressLoading.setVisibility(View.VISIBLE);
                mBtnPlayPause.setVisibility(View.GONE);
                break;
            case HachiPlayer.STATE_READY:
                mProgressLoading.setVisibility(View.GONE);
                mBtnPlayPause.setVisibility(View.VISIBLE);
                mVsCover.setVisibility(View.INVISIBLE);
                if (playwhenReady) {
                    mBtnPlayPause.toggle(false);
                } else {
                    mBtnPlayPause.toggle(true);
                }
                break;
        }
    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

    }

    private void initViews() {
        Glide.with(this)
            .load(mMomentCover)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(mVsCover);
        mBtnPlayPause.toggle(true);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceView.getHolder().addCallback(this);
    }


    @Override
    public void onStart() {
        super.onStart();
        mTimer = new Timer();
        mUpdatePlayTimeTask = new UpdatePlayTimeTask();
        mTimer.schedule(mUpdatePlayTimeTask, 0, 100);
        mIsActivityStopped = false;
    }


    @Override
    public void onStop() {
        super.onStop();
        mTimer.cancel();
        mIsActivityStopped = true;
        if (mMediaPlayer != null && mPlayerControl != null) {
            mPlayerControl.pause();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mRawDataState = RAW_DATA_STATE_UNKNOWN;
        releasePlayer();
    }

    public void doGaugeSetting(MomentInfo momentInfo, ArrayList<Long> timePoints) {
        if (!momentInfo.moment.overlay.isEmpty()) {
            Logger.t(TAG).d("setting gauge!!!");
            for (Map.Entry<String, String> entry : momentInfo.moment.overlay.entrySet()) {
                Logger.t(TAG).d("Key = " + entry.getKey() + ", Value = " + entry.getValue());
                if (entry.getKey().equals("theme") && TextUtils.isEmpty(entry.getValue())) {
                    mGaugeView.setVisibility(View.GONE);
                    mBtnShowOverlay.setVisibility(View.GONE);
                }
            }


            if (momentInfo.moment.momentType != null && momentInfo.moment.momentType.startsWith("RACING")) {
                momentInfo.moment.overlay.put("CountDown", "S");
            }
            mGaugeView.changeGaugeSetting(momentInfo.moment.overlay, timePoints);
        } else {
            Logger.t(TAG).d("overlay empty");
        }
    }


    private boolean isFullScreen(Configuration newConfig) {
        return newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
    }


    private void start() {
        openVideo(true);
    }


    private void releasePlayer() {
        if (mMediaPlayer != null) {
//            debugViewHelper.stop();
//            debugViewHelper = null;
//            playerPosition = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mPlayerControl = null;
//            eventLogger.endSession();
//            eventLogger = null;
        }
    }

    private void openVideo(boolean playWhenReady) {
        if (mMomentPlayInfo.videoUrl == null || mSurfaceView == null || mSurfaceHolder == null) {
            Logger.t(TAG).d("source: " + mMomentPlayInfo.videoUrl + " surface view: " + mSurfaceView + " surface holder: " + mSurfaceHolder);
            return;
        }

        if (mIsActivityStopped) {
            return;
        }

        if (mMediaPlayer == null) {
            String userAgent = Utils.getUserAgent(getActivity(), getString(R.string.app_name));
            mMediaPlayer = new HachiPlayer(new HlsRendererBuilder(getActivity(), userAgent, mMomentPlayInfo.videoUrl));
            mMediaPlayer.addListener(this);
            mMediaPlayer.seekTo(0);
            playerNeedsPrepare = true;
            mPlayerControl = mMediaPlayer.getPlayerControl();

        }
        if (playerNeedsPrepare) {
            mMediaPlayer.prepare();
            playerNeedsPrepare = false;
        }
        mMediaPlayer.setSurface(mSurfaceView.getHolder().getSurface());
        mMediaPlayer.setPlayWhenReady(playWhenReady);

        mVideoSeekBar.setOnSeekBarChangeListener(new SeekBarChangeEvent());
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setSurface(holder.getSurface());
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mMediaPlayer != null) {
            mMediaPlayer.blockingClearSurface();
        }

    }


    protected void setProgress(int position, int duration) {
//        Logger.t(TAG).d("position: " + position + " duration: " + duration);
        if (mVideoSeekBar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                mVideoSeekBar.setMax(duration);
                mVideoSeekBar.setProgress(position);
            }
        }

        if (mBottomProgressBar != null) {
            if (duration > 0) {
                mBottomProgressBar.setMax(duration);
                mBottomProgressBar.setProgress(position);
            }
        }
        updateVideoTime(position, duration);

    }


    public void onBackPressed() {
        mGaugeView.setVisibility(View.GONE);
        mSurfaceView.setVisibility(View.GONE);
        mVsCover.setVisibility(View.VISIBLE);
    }

    private void showControllers() {
        if (mVideoController == null) {
            return;
        }
        if (mInfoPanel == null) {
            return;
        }
        mInfoPanel.setVisibility(View.VISIBLE);
        mVideoController.setVisibility(View.VISIBLE);
        mHandler.removeMessages(FADE_OUT);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT), 3000);

    }


    private void hideControllers() {
        mVideoController.setVisibility(View.GONE);
        mInfoPanel.setVisibility(View.GONE);
    }


    private void showProgress() {
        if (mMediaPlayer == null) {
            return;
        }
        int position = mPlayerControl.getCurrentPosition();
        int duration = mPlayerControl.getDuration();
        setProgress(position, duration);
        mRawDataAdapter.updateCurrentTime(position);

    }

    @SuppressLint("SetTextI18n")
    private void updateVideoTime(int position, int duration) {
        mVideoTime.setText(DateUtils.formatElapsedTime(position / 1000));
        mVideoDuration.setText(DateUtils.formatElapsedTime(duration / 1000));
    }


    private void onLoadRawDataSuccessfully() {
        if (DebugHelper.showLapTimer()) {
            mGaugeView.setLapInfo(mLapInfo, mRawDataAdapter.mGPSData);
        }
        mRawDataState = RAW_DATA_STATE_READY;
        Logger.t(TAG).d("Raw data load finished");
        mProgressLoading.setVisibility(View.GONE);
        if (mSurfaceHolder == null) {
            return;
        }
        openVideo(true);
    }


    private void onLoadRawDataError(String msg) {
        Logger.t(TAG).d("msg: " + msg);
        mRawDataState = RAW_DATA_STATE_ERROR;
        mProgressLoading.setVisibility(View.GONE);
        openVideo(true);
    }

    private MomentPlayInfo updateMomentInfo(MomentPlayInfo momentPlayInfo, MomentInfo momentInfo) {
        mMoment = momentInfo;
        return momentPlayInfo;
    }

    private void getMomentPlayInfo() {
        if (mMomentId == MomentInfo.MomentBasicInfo.INVALID_MOMENT_ID) {
            return;
        }

        Observable<MomentInfo> observableMomentInfo = mHachi.getMomentInfoRx(mMomentId)
            .subscribeOn(Schedulers.newThread());

        Observable<MomentPlayInfo> observableMomentPlayInfo = mHachi.getMomentPlayInfo(mMomentId)
            .subscribeOn(Schedulers.newThread());

        Observable.zip(observableMomentInfo, observableMomentPlayInfo, new Func2<MomentInfo, MomentPlayInfo, MomentPlayInfo>() {

            @Override
            public MomentPlayInfo call(MomentInfo momentInfo, MomentPlayInfo momentPlayInfo) {
                Logger.t(TAG).d("set momentInfo " + momentInfo);
                Logger.t(TAG).d("moment play info: " + momentPlayInfo.toString());
                mMoment = momentInfo;
                //Logger.t(TAG).d(momentInfo.moment.overlay.toString());
                return momentPlayInfo;
            }
        })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<MomentPlayInfo>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    Logger.t(TAG).d(e.toString());

                }

                @Override
                public void onNext(MomentPlayInfo momentPlayInfo) {
//                    loadRawData(momentPlayInfo.rawDataUrl.get(0).url);
                    mMomentPlayInfo = momentPlayInfo;
                    Logger.t(TAG).d("moment play inf:" + momentPlayInfo.beginTime);
                    MomentInfo.MomentBasicInfo momentBasicInfo = mMoment.moment;
                    Logger.t(TAG).d("Moment Play Fragment! " + mMoment.moment.toString());
                    long momentCaptureTime = -1;
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    try {
                        Date date = dateFormat.parse(mMoment.moment.captureTime);
                        momentCaptureTime = date.getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Logger.t(TAG).d(momentBasicInfo.momentType);

                    Logger.t(TAG).d("1");
                    ArrayList<Long> arrayList = null;
                    if (momentBasicInfo.momentType != null && momentBasicInfo.momentType.startsWith("RACING")) {
                        arrayList = new ArrayList<Long>(6);
                        if (momentBasicInfo.momentType.startsWith("RACING_AU")) {
                            arrayList.add(0, (long) -1);
                            arrayList.add(1, momentBasicInfo.momentTimingInfo.t2);
                            arrayList.add(2, momentBasicInfo.momentTimingInfo.t3_2 + arrayList.get(1));
                            arrayList.add(3, momentBasicInfo.momentTimingInfo.t4_2 + arrayList.get(1));
                            if (momentBasicInfo.momentTimingInfo.t5_2 > 0) {
                                arrayList.add(4, momentBasicInfo.momentTimingInfo.t5_2 + arrayList.get(1));
                            } else {
                                arrayList.add(4, (long) -1);
                            }
                            if (momentBasicInfo.momentTimingInfo.t6_2 > 0) {
                                arrayList.add(5, momentBasicInfo.momentTimingInfo.t6_2 + arrayList.get(1));
                            } else {
                                arrayList.add(5, (long) -1);
                            }
                        } else if (momentBasicInfo.momentType.startsWith("RACING_CD")) {
                            arrayList.add(0, momentBasicInfo.momentTimingInfo.t1);
                            arrayList.add(1, (long) -1);
                            arrayList.add(2, momentBasicInfo.momentTimingInfo.t3_1 + arrayList.get(0));
                            arrayList.add(3, momentBasicInfo.momentTimingInfo.t4_1 + arrayList.get(0));
                            if (momentBasicInfo.momentTimingInfo.t5_2 > 0) {
                                arrayList.add(4, momentBasicInfo.momentTimingInfo.t5_1 + arrayList.get(0));
                            } else {
                                arrayList.add(4, (long) -1);
                            }
                            if (momentBasicInfo.momentTimingInfo.t6_2 > 0) {
                                arrayList.add(5, momentBasicInfo.momentTimingInfo.t6_1 + arrayList.get(0));
                            } else {
                                arrayList.add(5, (long) -1);
                            }
                        }
                    }
                    doGaugeSetting(mMoment, arrayList);
                    calcRawDataTimeInfo();
                    loadRawData(0);

                }
            });

        mProgressLoading.setVisibility(View.VISIBLE);
    }

    private void calcRawDataTimeInfo() {
        int offset = 0;
//        Logger.t(TAG).d("rawDataUrl size: " + mMomentPlayInfo.rawDataUrl.size());
        for (int i = 0; i < mMomentPlayInfo.rawDataUrl.size(); i++) {
            MomentPlayInfo.RawDataUrl rawDataUrl = mMomentPlayInfo.rawDataUrl.get(i);
            RawDataTimeInfo timeInfo = new RawDataTimeInfo();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Logger.t(TAG).d(dateFormat.getTimeZone().getDisplayName());
            Logger.t(TAG).d("capture time:" + rawDataUrl.captureTime);
            try {
                Date date = dateFormat.parse(rawDataUrl.captureTime);
                timeInfo.captureTime = date.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            timeInfo.offset = offset;
            offset += rawDataUrl.duration;

            mRawDataTimeInfoList.add(timeInfo);
//            Logger.t(TAG).d("add time info: " + timeInfo.toString() + " index: " + i);
        }
    }


    private void loadRawData(final int index) {
        if (mMomentPlayInfo.rawDataUrl == null || index >= mMomentPlayInfo.rawDataUrl.size()) {
            onLoadRawDataSuccessfully();
            return;
        }
        Logger.t(TAG).d(mMomentPlayInfo.rawDataUrl.get(index).url);

        CachedJsonRequest request = new CachedJsonRequest.Builder()
            .url(mMomentPlayInfo.rawDataUrl.get(index).url)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
//                    Logger.t(TAG).json(response.toString());
//                    Logger.t(TAG).d("finish loading index: " + index);
                    if (parseRawData(index, response)) {
                        int nextIndex = index + 1;
                        if (nextIndex < mMomentPlayInfo.rawDataUrl.size()) {
                            loadRawData(nextIndex);
                        } else {
                            onLoadRawDataSuccessfully();
                        }
                    } else {
                        onLoadRawDataError("Load Raw data error");
                    }
                }
            })
            .errorListener(new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                    onLoadRawDataError("ErrorCode: " + errorInfo.errorCode);
                }
            })
            .build();
        request.setTag(TAG);
        mRequestQueue.add(request);


    }

    private boolean parseRawData(int index, JSONObject response) {
        int offset = mRawDataTimeInfoList.get(index).offset;

        try {
            JSONObject obd = response.optJSONObject("obd");
            if (obd != null) {
                JSONArray captureTime = obd.optJSONArray("captureTime");
                JSONArray speed = obd.optJSONArray("speed");
                JSONArray rpm = obd.optJSONArray("rpm");
                JSONArray temperature = obd.optJSONArray("temperature");
                JSONArray tp = obd.optJSONArray("tp");
                JSONArray imp = obd.optJSONArray("imp");
                JSONArray bp = obd.optJSONArray("bp");
                JSONArray bhp = obd.optJSONArray("bhp");
                for (int i = 0; i < captureTime.length(); i++) {
                    mRawDataAdapter.addObdData(
                        captureTime.getLong(i) + offset,
                        speed != null ? speed.getInt(i) : 0,
                        rpm != null ? rpm.getInt(i) : 0,
                        temperature != null ? temperature.getInt(i) : 0,
                        tp != null ? tp.getInt(i) : 0,
                        imp != null ? imp.getInt(i) : 0,
                        bp != null ? bp.getInt(i) : 0,
                        bhp != null ? bhp.getInt(i) : 0);
                }
            }
            JSONObject acc = response.optJSONObject("acc");
            if (acc != null) {
                JSONArray captureTime = acc.optJSONArray("captureTime");
                JSONArray acceleration = acc.optJSONArray("acceleration");
                if (acceleration != null) {
                    for (int i = 0; i < captureTime.length(); i++) {
                        JSONObject accObj = acceleration.optJSONObject(i);
                        if (accObj != null) {
                            mRawDataAdapter.addAccData(
                                captureTime.getLong(i) + offset,
                                accObj.optInt("accelX"), accObj.optInt("accelY"), accObj.optInt("accelZ"),
                                accObj.optInt("gyroX"), accObj.optInt("gyroY"), accObj.optInt("gyroZ"),
                                accObj.optInt("magnX"), accObj.optInt("magnY"), accObj.optInt("magnZ"),
                                accObj.optInt("eulerHeading"), accObj.optInt("eulerRoll"), accObj.optInt("eulerPitch"),
                                accObj.optInt("quaternionW"), accObj.optInt("quaternionX"), accObj.optInt("quaternionY"), accObj.optInt("quaternionZ"),
                                accObj.optInt("pressure")
                            );
                        }
                    }
                }
            }
            JSONObject gps = response.optJSONObject("gps");
            if (gps != null) {
                JSONArray captureTime = gps.optJSONArray("captureTime");
                JSONArray coordinates = gps.optJSONObject("coordinate").optJSONArray("coordinates");
                JSONArray speed = gps.optJSONArray("speed");

                for (int i = 0; i < captureTime.length(); i++) {
                    JSONArray coordinateObj = coordinates.optJSONArray(i);
                    if (coordinateObj != null) {
                        mRawDataAdapter.addGpsData(
                            captureTime.getLong(i) + offset,
                            coordinateObj.optDouble(0),
                            coordinateObj.optDouble(1),
                            coordinateObj.optDouble(2),
                            speed.getDouble(i)
                        );
                    }
                }
            }
            JSONObject weather = response.optJSONObject("weather");
            if (weather != null) {
                JSONArray captureTime = weather.getJSONArray("captureTime");
                JSONObject hourlyData = weather.getJSONArray("hourly").getJSONObject(0);
                Logger.t(TAG).d(hourlyData.toString());
                if (hourlyData != null && hourlyData.length() != 0) {
                    for (int i = 0; i < captureTime.length(); i++) {
                        mRawDataAdapter.addWeatherData(
                            captureTime.getLong(i) + offset,
                            hourlyData.getInt("tempF"),
                            hourlyData.getInt("windSpeedMiles"),
                            hourlyData.getInt("pressure"),
                            hourlyData.getInt("humidity"),
                            hourlyData.getInt("weatherCode")
                        );
                    }
                }
            }
            mLapInfo = response.optJSONObject("lapTimer");
            if (mLapInfo != null) {
                AvrproLapsHeader lapsHeader = new AvrproLapsHeader(
                        mLapInfo.optInt("totalLaps"),
                        mLapInfo.optInt("bestLapTime"),
                        mLapInfo.optInt("topSpeedKmh")
                    );
                JSONArray laptimeList = mLapInfo.optJSONArray("lapTimeList");
                AvrproLapData[] lapDatas = new AvrproLapData[laptimeList.length()];
                for (int i = 0; i < laptimeList.length(); i++) {
                    JSONObject perLap = laptimeList.getJSONObject(i);
                    JSONArray delta = perLap.optJSONArray("deltaMsToBest");
                    int[] deltaMsToBest = new int[delta.length()];
                    for (int j = 0; j < delta.length(); j++) {
                        deltaMsToBest[j] = delta.optInt(j, 0);
                    }
                    AvrproLapData lapData = new AvrproLapData(
                            perLap.optInt("totalLapTime"),
                            perLap.optInt("startOffsetMs"),
                            perLap.optInt("checkIntervalMs"),
                            deltaMsToBest);
                    lapDatas[i] = lapData;
                }
            }
            return true;
        } catch (JSONException e) {
            Logger.t(TAG).d(e.toString());
        }
        return true;
    }


    private class MomentRawDataAdapter {
        public List<RawDataItem> mOBDData = new ArrayList<>();
        public List<RawDataItem> mAccData = new ArrayList<>();
        public List<RawDataItem> mGPSData = new ArrayList<>();
        RawDataItem mWeatherData = null;

        private int mObdDataIndex = 0;
        private int mAccDataIndex = 0;
        private int mGpsDataIndex = 0;

        public void reset() {
            mObdDataIndex = 0;
            mAccDataIndex = 0;
            mGpsDataIndex = 0;
        }


        public void addObdData(long captureTime, int speed, int rpm, int temperature, int tp, int psi, int bp, int bhp) {
            RawDataItem item = new RawDataItem(RawDataItem.DATA_TYPE_OBD, captureTime);
            //Logger.t(TAG).d(String.format("speed:%1$d, rmp:%2$d, psi:%3$d, captureTime:%4$d", speed, rpm, psi, captureTime));
            ObdData data = new ObdData(speed, temperature, rpm, tp, psi, false);
            item.data = data;
            mOBDData.add(item);
        }

        public void addAccData(long captureTime, int accX, int accY, int accZ,
                               int gyroX, int gyroY, int gyroZ,
                               int magnX, int magnY, int magnZ,
                               int eulerHeading, int eulerRoll, int eulerPitch,
                               int quaternionW, int quaternionX, int quaternionY, int quaternionZ,
                               int pressure) {
            RawDataItem item = new RawDataItem(RawDataItem.DATA_TYPE_IIO, captureTime);
            //Logger.t(TAG).d("acc captureTime:" + captureTime);
            IioData data = new IioData();

            data.accX = accX;
            data.accY = accY;
            data.accZ = accZ;
            data.euler_roll = eulerRoll;
            data.euler_pitch = eulerPitch;

            item.data = data;
            mAccData.add(item);
        }

        public void addGpsData(long captureTime, double longitude, double latitude, double altitude, double speed) {
            RawDataItem item = new RawDataItem(RawDataItem.DATA_TYPE_GPS, captureTime);
            GpsData data = new GpsData();
            data.coord.lat = latitude;
            data.coord.lat_orig = latitude;
            data.coord.lng = longitude;
            data.coord.lng_orig = longitude;
            data.altitude = altitude;
            data.speed = speed;
            item.data = data;

            mGPSData.add(item);
        }

        public void addWeatherData(long captureTime, int tempF, int windSpeedMiles, int pressure, int humidity, int weatherCode) {
            RawDataItem item = new RawDataItem(RawDataItem.DATA_TYPE_WEATHER, captureTime);
            WeatherData data = new WeatherData(tempF, windSpeedMiles, pressure, humidity, weatherCode);
            item.data = data;
            mWeatherData = item;

        }

        public void updateCurrentTime(int currentTime) {
            List<RawDataItem> rawDataItemList = new ArrayList<RawDataItem>();
            RawDataItem rawDataItem = null;
            if ((rawDataItem = checkIfUpdated(mAccData, 0, currentTime)) != null) {
                mAccDataIndex++;
                rawDataItemList.add(rawDataItem);
            }
            if ((rawDataItem = checkIfUpdated(mGPSData, 0, currentTime)) != null) {
                mGpsDataIndex++;
                rawDataItemList.add(rawDataItem);
            }
            if ((rawDataItem = checkIfUpdated(mOBDData, 0, currentTime)) != null) {
                mObdDataIndex++;
                //Logger.t(TAG).d(((ObdData)rawDataItem.data).toString());
                rawDataItemList.add(rawDataItem);
            }
            if (!rawDataItemList.isEmpty() && (rawDataItem = mWeatherData) != null) {
                rawDataItemList.add(rawDataItem);
            }
            if (!rawDataItemList.isEmpty()) {
//                Logger.t(TAG).d("update raw data!");
                mGaugeView.updateRawDateItem(rawDataItemList);
            } else {
                Logger.t(TAG).d("raw data empty!");
            }
        }

        private RawDataItem checkIfUpdated(List<RawDataItem> list, int fromPosition, int currentTime) {
            int index = fromPosition;
            if (index >= list.size()) {
                return null;
            }
            int low = 0;
            int high = list.size() - 1;
            int mid, res = -1;
            if (list.get(low).getPtsMs() > currentTime || list.get(high).getPtsMs() < currentTime) {
                return null;
            }
            while (low < high) {
                mid = (low + high) / 2;
                if (list.get(mid).getPtsMs() == currentTime) {
                    res = mid;
                    break;
                } else if (list.get(mid).getPtsMs() < currentTime) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            RawDataItem updateItem = null;
            if (res != -1) {
                updateItem = new RawDataItem(list.get(res));
            } else {
                updateItem = new RawDataItem(list.get(low));
            }

            long startTime = getRawDataIndex(currentTime);
            if (Math.abs(updateItem.getPtsMs() - currentTime) <= 5000) {
                updateItem.setPtsMs(startTime + updateItem.getPtsMs());
                return updateItem;
            } else {
                return null;
            }

        }

        private long getRawDataIndex(int currentTime) {
            for (int i = 1; i < mRawDataTimeInfoList.size(); i++) {
                if (currentTime < mRawDataTimeInfoList.get(i).offset) {
                    return mRawDataTimeInfoList.get(i - 1).captureTime;
                }
            }
            return mRawDataTimeInfoList.get(mRawDataTimeInfoList.size() - 1).captureTime;
        }
    }


    public class RawDataTimeInfo {
        private long captureTime;

        private int offset;

        @Override
        public String toString() {
            return ToStringUtils.getString(this);
        }
    }

    public class UpdatePlayTimeTask extends TimerTask {

        @Override
        public void run() {
            if (mPlayerControl != null && mPlayerControl.isPlaying()) {
                refreshProgressBar();

            }
        }

        private void refreshProgressBar() {
            if (mPlayerControl == null || !mPlayerControl.isPlaying()) {
                return;
            }
            final int currentPos = mPlayerControl.getCurrentPosition();

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mPlayerControl != null) {
                        setProgress(currentPos, mPlayerControl.getDuration());
                        if (!mViewCountSend && currentPos > 3000) {
                            HachiApiRx.addMomentViewCount(mMomentId)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new SimpleSubscribe<SimpleBoolResponse>() {
                                    @Override
                                    public void onNext(SimpleBoolResponse simpleBoolResponse) {
                                        Logger.t(TAG).d("result: " + simpleBoolResponse.result);
                                    }
                                });
                            mViewCountSend = true;
                        }
                        //
                        mRawDataAdapter.updateCurrentTime(currentPos);
                        mGaugeView.setPlayTime(currentPos);
                    }
                }
            });
        }
    }

    private static class VideoHandler extends Handler {
        private WeakReference<MomentPlayFragment> mFragmentRef;

        VideoHandler(MomentPlayFragment fragment) {
            super();
            mFragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            MomentPlayFragment fragment = mFragmentRef.get();
            if (fragment == null) {
                return;
            }
            switch (msg.what) {
                case FADE_OUT:
                    fragment.hideControllers();
                    break;
            }
        }
    }

    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {
        int progress;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (mPlayerControl != null && mPlayerControl.isPlaying()) {
                this.progress = progress * mPlayerControl.getDuration() / seekBar.getMax();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mMediaPlayer == null || mPlayerControl == null) {
                return;
            }
            Logger.t(TAG).d("duration: " + mPlayerControl.getDuration() + " progress: " + seekBar.getProgress() + " max: " + seekBar.getMax());
            mMediaPlayer.seekTo(((long) mPlayerControl.getDuration() * seekBar.getProgress()) / seekBar.getMax());
        }

    }


}
