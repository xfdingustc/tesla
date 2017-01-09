package com.waylens.hachi.ui.clips.player;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.exoplayer.util.PlayerControl;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.camera.events.CameraConnectionEvent;
import com.waylens.hachi.eventbus.events.ClipEditEvent;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.eventbus.events.GaugeEvent;
import com.waylens.hachi.glide_snipe_integration.SnipeGlideLoader;
import com.waylens.hachi.player.HachiPlayer;
import com.waylens.hachi.player.HlsRendererBuilder;
import com.waylens.hachi.player.Utils;
import com.waylens.hachi.snipe.remix.AvrproClipInfo;
import com.waylens.hachi.snipe.remix.AvrproLapData;
import com.waylens.hachi.snipe.remix.AvrproLapTimerResult;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipPos;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetManager;
import com.waylens.hachi.snipe.vdb.ClipSetPos;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;
import com.waylens.hachi.snipe.vdb.urls.VdbUrl;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.clips.player.multisegseekbar.MultiSegSeekbar;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.view.gauge.GaugeView;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/2/22.
 */
public class ClipPlayFragment extends BaseFragment implements SurfaceHolder.Callback, HachiPlayer.Listener {
    private static final String TAG = ClipPlayFragment.class.getSimpleName();
    public static final String VIDEO_TYPE_ARG = "video.type";
    public static final int VIDEO_TYPE_ORIDINARY = 0x0001;
    public static final int VIDEO_TYPE_LAPTIMER = 0x0002;

    private static final int FADE_OUT = 5;

    private int mClipSetIndex;

    private UrlProvider mUrlProvider;

    private HachiPlayer mMediaPlayer;
    private MediaPlayer mAudioPlayer;

    private PlayerControl mPlayerControl;

    private String mAudioUrl;
    private VdbUrl mVdbUrl;

    private RawDataLoader mInitDataLoader;
//  private RawDataLoader mRawDataLoader;

    private Timer mTimer;
    private UpdatePlayTimeTask mUpdatePlayTimeTask;

    private ClipPos mPreviousShownClipPos;
    private long mPreviousShowThumbnailRequestTime;

    private PositionAdjuster mPositionAdjuster;

    private EventBus mEventBus = EventBus.getDefault();

    private Handler mHandler;

    private ClipRawDataAdapter mRawDataAdapter;

    private boolean playerNeedsPrepare;

    private boolean mNeedSendPlayCompleteEvent = false;

    private int mVideoType = VIDEO_TYPE_ORIDINARY;

    private List<AvrproLapData> mLapData = new ArrayList<>();

    @BindView(R.id.surface_view)
    SurfaceView mSurfaceView;

    @BindView(R.id.clipCover)
    ImageView mClipCover;

    @BindView(R.id.progressLoading)
    ProgressBar mProgressLoading;

    @BindView(R.id.btnPlayPause)
    ImageButton mBtnPlayPause;

    @BindView(R.id.playProgress)
    TextView mTvProgress;

    @BindView(R.id.duration)
    TextView mDuration;

    @BindView(R.id.multiSegIndicator)
    MultiSegSeekbar mMultiSegSeekbar;

    @BindView(R.id.gaugeView)
    GaugeView mWvGauge;

    @BindView(R.id.btnFullscreen)
    ImageButton mBtnFullscreen;

    @BindView(R.id.fragment_view)
    LinearLayout mFragmentView;

    @BindView(R.id.control_panel)
    LinearLayout mControlPanel;

    @BindView(R.id.media_window)
    FrameLayout mMediaWindow;

    @BindView(R.id.btnShowOverlay)
    ImageButton mBtnShowOverlay;

    @BindString(R.string.app_name)
    String appName;


    @OnClick(R.id.btnShowOverlay)
    public void onBtnShowOverlayClicked() {
        boolean isGaugeVisible = mWvGauge.getVisibility() != View.VISIBLE;
        if (isGaugeVisible) {
            mBtnShowOverlay.setImageResource(R.drawable.ic_btn_gauge_overlay_s);
        } else {
            mBtnShowOverlay.setImageResource(R.drawable.ic_btn_gauge_overlay_n);
        }
        //mGaugeView.showGauge(mIsGaugeVisible);
        mWvGauge.setVisibility(isGaugeVisible ? View.VISIBLE : View.INVISIBLE);
    }


    @OnClick(R.id.btnPlayPause)
    public void onBtnPlayPauseClicked() {
        if (getClipSet().getCount() == 0) {
            MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.no_clip_selected)
                .positiveText(R.string.ok)
                .show();
            return;
        }
        if (mPlayerControl == null) {
            startPreparingClip(mMultiSegSeekbar.getCurrentClipSetPos(), true);
        } else {
            if (mPlayerControl.isPlaying()) {
                mPlayerControl.pause();
            } else {
                mPlayerControl.start();
            }
        }

    }

    public boolean isVideoPlaying() {
        return mPlayerControl != null && mPlayerControl.isPlaying();
    }

    @OnClick(R.id.btnFullscreen)
    public void onBtnFullscreenClicked() {
        if (!isFullScreen()) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            showControlPanel();
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        }

    }

    @OnClick(R.id.surface_view)
    public void onSurfaceClicked() {
        showControlPanel();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isFullScreen()) {
            mBtnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp2px(48));
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            mControlPanel.setLayoutParams(params);
//            mControlPanel.setBackgroundColor(Color.argb(0x7f, 0, 0, 0));

        } else {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp2px(48));
            params.addRule(RelativeLayout.BELOW, mFragmentView.getId());
            mControlPanel.setLayoutParams(params);
            mBtnFullscreen.setImageResource(R.drawable.ic_fullscreen);
//            mControlPanel.setBackgroundResource(0);
        }
        ((BaseActivity) getActivity()).setImmersiveMode(isFullScreen());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventClipSetPosChanged(ClipSetPosChangeEvent event) {
        final ClipSetPos clipSetPos = event.getClipSetPos();
//        Logger.t(TAG).d("Receive ClipSetPosChangeEvent: " + event.getClipSetPos().getClipTimeMs());
        boolean refreshThumbnail = false;
//        Logger.t(TAG).d("broadcast: " + event.getBroadcaster());
        if (!event.getBroadcaster().equals(TAG)) {
            refreshThumbnail = true;
            if (event.getIntent() == ClipSetPosChangeEvent.INTENT_SHOW_THUMBNAIL) {
//                changeState(STATE_FAST_PREVIEW);
                enterFastPreview();
            }
        }

        setClipSetPos(clipSetPos, refreshThumbnail);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventClipSetChanged(ClipSetChangeEvent event) {
        Logger.t(TAG).d("on Clip Set chang event clip count: " + getClipSet().getCount());
        releasePlayer();
        mBtnPlayPause.setImageResource(R.drawable.ic_play_arrow);
        updateProgressTextView(0, getClipSet().getTotalLengthMs());
        if (getClipSet().getCount() == 0) {
            mClipCover.setVisibility(View.INVISIBLE);
            mControlPanel.setVisibility(View.GONE);
            updateProgressTextView(0, 0);
            mMediaWindow.setVisibility(View.INVISIBLE);
        } else {
            mClipCover.setVisibility(View.VISIBLE);
            mControlPanel.setVisibility(View.VISIBLE);
            mMediaWindow.setVisibility(View.VISIBLE);
            ClipSetPos clipSetPos = new ClipSetPos(0, getClipSet().getClip(0).editInfo.selectedStartValue);
            setClipSetPos(clipSetPos, true);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventVdtCameraConnectionEvent(CameraConnectionEvent event) {
        if (event.getWhat() == CameraConnectionEvent.VDT_CAMERA_DISCONNECTED) {
            releasePlayer();
        }
    }

    @Subscribe
    public void onGaugeEvent(GaugeEvent event) {
        switch (event.getWhat()) {
            case GaugeEvent.EVENT_WHAT_SHOW:
                Boolean shouldShow = (Boolean) event.getExtra();
                mWvGauge.showGauge(shouldShow);
                break;

        }

    }

    @Subscribe
    public void onClipEditEvent(ClipEditEvent event) {
        releasePlayer();
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

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {

        switch (playbackState) {
            case HachiPlayer.STATE_ENDED:
                Logger.t(TAG).d("playback ended");
                releasePlayer();
                break;
        }
        if (mAudioUrl != null && mAudioPlayer != null) {
            updateAudioPlayerState(playWhenReady, playbackState);
        }
        updateControls(playWhenReady, playbackState);
    }

    public GaugeView getGaugeView() {
        return mWvGauge;
    }

    public void onBackPressed() {
        mSurfaceView.setVisibility(View.GONE);
        mWvGauge.setVisibility(View.GONE);
        mClipCover.setVisibility(View.VISIBLE);

    }

    private void updateAudioPlayerState(boolean playwhenReady, int playbackState) {
        Logger.t(TAG).d("playWhenReady: " + playwhenReady + " playbackState: " + playbackState);
        switch (playbackState) {
            case HachiPlayer.STATE_IDLE:
                break;
            case HachiPlayer.STATE_ENDED:
                mAudioPlayer.reset();
                break;
            case HachiPlayer.STATE_BUFFERING:
                mAudioPlayer.pause();
                break;
            case HachiPlayer.STATE_READY:
                if (playwhenReady) {
                    Logger.t(TAG).d("start audio player");
                    mAudioPlayer.start();
                } else {
                    mAudioPlayer.pause();
                }
                break;
        }
    }

    private void updateControls(boolean playwhenReady, int playbackState) {
        switch (playbackState) {
            case HachiPlayer.STATE_IDLE:
            case HachiPlayer.STATE_ENDED:
                mClipCover.setVisibility(View.VISIBLE);
                mProgressLoading.setVisibility(View.INVISIBLE);
                mBtnPlayPause.setImageResource(R.drawable.ic_play_arrow);
                break;
            case HachiPlayer.STATE_PREPARING:
                mClipCover.setVisibility(View.VISIBLE);
                mProgressLoading.setVisibility(View.VISIBLE);
                break;

            case HachiPlayer.STATE_BUFFERING:
                mProgressLoading.setVisibility(View.VISIBLE);
                break;
            case HachiPlayer.STATE_READY:
                mProgressLoading.setVisibility(View.GONE);
                mClipCover.setVisibility(View.INVISIBLE);
                if (playwhenReady) {
                    mBtnPlayPause.setImageResource(R.drawable.ic_pause_black);
                } else {
                    mBtnPlayPause.setImageResource(R.drawable.ic_play_arrow);
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


    public enum ClipMode {
        SINGLE,
        MULTI
    }

    public enum CoverMode {
        NORMAL,
        BANNER,
    }

    public ClipMode mClipMode = ClipMode.SINGLE;
    public CoverMode mCoverMode = CoverMode.NORMAL;


    public static ClipPlayFragment newInstance(VdtCamera camera, int clipSetIndex, UrlProvider urlProvider) {
        return newInstance(camera, clipSetIndex, urlProvider, ClipMode.SINGLE, CoverMode.NORMAL);
    }

    public static ClipPlayFragment newInstance(VdtCamera camera, int clipSetIndex, UrlProvider urlProvider, ClipMode clipMode) {
        return newInstance(camera, clipSetIndex, urlProvider, clipMode, CoverMode.NORMAL);
    }

    public static ClipPlayFragment newInstance(VdtCamera camera, int clipSetIndex, UrlProvider urlProvider, ClipMode clipMode, CoverMode coverMode) {
        ClipPlayFragment fragment = new ClipPlayFragment();
        fragment.mVdtCamera = camera;
        fragment.mClipSetIndex = clipSetIndex;
        fragment.mUrlProvider = urlProvider;
        fragment.mClipMode = clipMode;
        fragment.mCoverMode = coverMode;
        return fragment;
    }

    public static ClipPlayFragment newInstance(VdtCamera camera, int clipSetIndex, UrlProvider urlProvider, ClipMode clipMode, CoverMode coverMode, int videoType) {
        ClipPlayFragment fragment = new ClipPlayFragment();
        fragment.mVdtCamera = camera;
        fragment.mClipSetIndex = clipSetIndex;
        fragment.mUrlProvider = urlProvider;
        fragment.mClipMode = clipMode;
        fragment.mCoverMode = coverMode;
        Bundle args = new Bundle();
        args.putInt(VIDEO_TYPE_ARG, videoType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected String getRequestTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_clip_play, container, false);
        ButterKnife.bind(this, view);
        mWvGauge.setGaugeMode(GaugeView.MODE_MOMENT);
        mWvGauge.clearPendingActions();
        mHandler = new ControlPanelHandler(this);
        initViews();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initRawDataView();
    }

    @Override
    public void onStart() {
        super.onStart();
        mTimer = new Timer();
        mUpdatePlayTimeTask = new UpdatePlayTimeTask();
        mTimer.schedule(mUpdatePlayTimeTask, 0, 100);
        //mWvGauge.setEnhanceMode();
        mEventBus.register(this);
        mEventBus.register(mMultiSegSeekbar);
        mEventBus.register(mWvGauge);

    }

    @Override
    public void onStop() {
        super.onStop();
        Logger.t(TAG).d("on stop");
        mTimer.cancel();
        mEventBus.unregister(this);
        mEventBus.unregister(mMultiSegSeekbar);
        mEventBus.unregister(mWvGauge);

    }

    @Override
    public void onPause() {
        super.onPause();
        pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.t(TAG).d("on destroy");
        releasePlayer();
    }

    public void initRawDataView() {
        mInitDataLoader = new RawDataLoader(mClipSetIndex);
        mInitDataLoader.loadRawDataRx()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe() {
                @Override
                public void onNext(Object o) {
                    Logger.t(TAG).d("on next");
                }

                @Override
                public void onCompleted() {
                    super.onCompleted();
                    Logger.t(TAG).d("load raw data finished");
                    if (getActivity() == null || getActivity().isDestroyed()) {
                        Logger.t(TAG).d("destroyed");
                        return;
                    }
                    mRawDataAdapter = new ClipRawDataAdapter(getClipSet());
                    mRawDataAdapter.setRawDataLoader(mInitDataLoader);
                    mWvGauge.setAdapter(mRawDataAdapter);
//                  Logger.t(TAG).d("init gauge view!");
                    //startPreparingClip(mMultiSegSeekbar.getCurrentClipSetPos(), true);
                }
            });
    }



    private void init() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            mVideoType = arguments.getInt(VIDEO_TYPE_ARG, VIDEO_TYPE_ORIDINARY);
        }
        initVdtCamera();
        setRetainInstance(true);
    }

    private void initViews() {
        setupToolbar();
        if (getClipSet() == null) {
            return;
        }
        mWvGauge.setVisibility(View.INVISIBLE);

        ClipPos clipPos = new ClipPos(getClipSet().getClip(0));

        Glide.with(this)
            .using(new SnipeGlideLoader(mVdbRequestQueue))
            .load(clipPos)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .dontAnimate()
            .into(mClipCover);


        setupMultiSegSeekBar();
        mSurfaceView.getHolder().addCallback(this);
        updateProgressTextView(0, getClipSet().getTotalSelectedLengthMs());
        switch (mVideoType) {
            case VIDEO_TYPE_ORIDINARY:
                mWvGauge.initGaugeViewBySetting();
                break;
            case VIDEO_TYPE_LAPTIMER:
                mWvGauge.initGaugeViewBySetting("rifle");
                mBtnShowOverlay.setImageResource(R.drawable.ic_btn_gauge_overlay_s);
                mWvGauge.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }


    private void preparePlayer(boolean playWhenReady) {
        if (isDetached()) {
            Logger.t(TAG).d("detached");
            return;
        }

        Logger.t(TAG).d("prepare player");
        if (mMediaPlayer == null) {
            String userAgent = Hachi.getUserAgent();
            mMediaPlayer = new HachiPlayer(new HlsRendererBuilder(getActivity(), userAgent, mVdbUrl.url));
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
    }


    private void releasePlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
            mPlayerControl = null;
        }

        if (mAudioPlayer != null) {
            mAudioPlayer.reset();
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
        mBtnPlayPause.setImageResource(R.drawable.ic_play_arrow);
//        mMultiSegSeekbar.reset();
        mNeedSendPlayCompleteEvent = true;

    }

    public void pause() {
        if (mMediaPlayer != null && mPlayerControl != null) {
            mPlayerControl.pause();
        }
        if (mAudioPlayer != null) {
            mAudioPlayer.pause();
        }
    }


    private ClipSet getClipSet() {
        return ClipSetManager.getManager().getClipSet(mClipSetIndex);
    }


    private void setupMultiSegSeekBar() {
        mMultiSegSeekbar.setClipList(mClipSetIndex);

        if (mClipMode == ClipMode.MULTI) {
            mMultiSegSeekbar.setMultiStyle(true);
        } else {
            mMultiSegSeekbar.setMultiStyle(false);
        }
        mMultiSegSeekbar.setOnMultiSegSeekbarChangListener(new MultiSegSeekbar.OnMultiSegSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(MultiSegSeekbar seekBar) {
                enterFastPreview();
            }

            @Override
            public void onProgressChanged(MultiSegSeekbar seekBar, ClipSetPos clipSetPos) {
                setClipSetPos(clipSetPos, true);
                mEventBus.post(new ClipSetPosChangeEvent(clipSetPos, TAG));
            }

            @Override
            public void onStopTrackingTouch(MultiSegSeekbar seekBar) {
                startPreparingClip(seekBar.getCurrentClipSetPos(), false);
            }
        });
    }

    public void enterFastPreview() {
        if (mPlayerControl != null && mPlayerControl.isPlaying()) {
            releasePlayer();
        }

        mClipCover.setVisibility(View.VISIBLE);
    }


    public void showClipPosThumbnail(Clip clip, long timeMs) {
//        changeState(STATE_FAST_PREVIEW);
        ClipPos clipPos = new ClipPos(clip, timeMs, ClipPos.TYPE_POSTER, false);
        showThumbnail(clipPos);
    }

    public void showThumbnail(ClipPos clipPos) {
        if (clipPos != null) {
            if (mPreviousShownClipPos != null && mPreviousShownClipPos.getClipId().equals(clipPos.getClipId())) {
                long timeDiff = Math.abs(mPreviousShownClipPos.getClipTimeMs() - clipPos.getClipTimeMs());
                if (timeDiff < 1000) {
                    return;
                }

                long lastRequestOffset = System.currentTimeMillis() - mPreviousShowThumbnailRequestTime;
                if (lastRequestOffset < 1000) {
                    return;
                }
            }

//            Logger.t(TAG).d("show cilpPos " + mClipCover.getVisibility());
//            mVdbImageLoader.displayVdbImage(clipPos, mClipCover, true, false);
            clipPos.setIgnorable(true);
            Glide.with(this)
                .using(new SnipeGlideLoader(mVdbRequestQueue))
                .load(clipPos)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate()
                .placeholder(mClipCover.getDrawable())
                .into(mClipCover);

            mPreviousShownClipPos = clipPos;
            mPreviousShowThumbnailRequestTime = System.currentTimeMillis();
        }
    }


    public void startPreparingClip(final ClipSetPos clipSetPos, final boolean loadRawData) {
        if (mBtnPlayPause.isEnabled() == false) {
            return;
        }

        mProgressLoading.setVisibility(View.VISIBLE);


        Observable urlObservable = mUrlProvider.getUrlRx(getClipSet().getTimeOffsetByClipSetPos(clipSetPos))
            .doOnNext(new Action1<VdbUrl>() {
                @Override
                public void call(VdbUrl vdbUrl) {
                    mVdbUrl = vdbUrl;
                    mPositionAdjuster = mUrlProvider.getPostionAdjuster();
                }
            });


        Observable loadAudioPlayerObservable = Observable.just("")
            .doOnNext(new Action1<String>() {
                @Override
                public void call(String s) {
                    if (mAudioUrl != null) {
                        mAudioPlayer = new MediaPlayer();
                        try {
                            mAudioPlayer.setDataSource(mAudioUrl);
                            mAudioPlayer.prepare();
                            mAudioPlayer.setLooping(true);
                            mAudioPlayer.start();
                            mAudioPlayer.pause();
                        } catch (IOException e) {
                            Logger.e("", e);
                        }
                    }
                }
            });


        Observable.zip(urlObservable, loadAudioPlayerObservable, new Func2() {
            @Override
            public Object call(Object o, Object o2) {
                return new Object();
            }
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber() {
                @Override
                public void onCompleted() {
                    Logger.t(TAG).d("prepare finished");
                    if (getActivity() == null || getActivity().isDestroyed()) {
                        Logger.t(TAG).d("destroyed");
                        return;
                    }
                    preparePlayer(true);
                }

                @Override
                public void onError(Throwable e) {
                    Logger.t(TAG).d("load error");
                }

                @Override
                public void onNext(Object o) {

                }
            });
    }

    public void setAudioUrl(String audioUrl) {
        Logger.t(TAG).d("audio url: " + audioUrl);
        mAudioUrl = audioUrl;
        releasePlayer();
    }


    public void setUrlProvider(UrlProvider urlProvider, long startTime) {
        mUrlProvider = urlProvider;
        if (mUrlProvider instanceof ClipUrlProvider) {
            mPositionAdjuster = new ClipPositionAdjuster(startTime, mVdbUrl);
        } else {
            mPositionAdjuster = new PlaylistPositionAdjuster(mVdbUrl);
        }
    }

    public void setClipSetPos(ClipSetPos clipSetPos, boolean refreshThumbnail) {

        ClipPos clipPos = getClipSet().getClipPosByClipSetPos(clipSetPos);
        if (refreshThumbnail == true) {
            showThumbnail(clipPos);
        }

        long timeOffset = getClipSet().getTimeOffsetByClipSetPos(clipSetPos);

        updateProgressTextView(timeOffset, getClipSet().getTotalSelectedLengthMs());

        if (mRawDataAdapter != null) {
            mRawDataAdapter.setClipSetPos(clipSetPos);
        }

    }

    public ClipSetPos getClipSetPos() {
        if (mMediaPlayer != null && mPlayerControl.isPlaying()) {
            return getClipSet().getClipSetPosByTimeOffset(getCurrentPlayingTime());
        } else {
            return mMultiSegSeekbar.getCurrentClipSetPos();
        }
    }

    public int getCurrentPlayingTime() {
        if (mMediaPlayer == null || mPlayerControl == null) {
            return 0;
        }

        int currentPos = mPlayerControl.getCurrentPosition();
        if (mPositionAdjuster != null) {
            currentPos = mPositionAdjuster.getAdjustedPostion(currentPos);
        }
        return currentPos;
    }

    private void updateProgressTextView(long currentPosition, long duration) {
        mTvProgress.setText(DateUtils.formatElapsedTime(currentPosition / 1000));
        mDuration.setText(DateUtils.formatElapsedTime(duration / 1000));
    }

    private boolean isFullScreen() {
        int orientation = getActivity().getRequestedOrientation();
        return orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }

    private void showControlPanel() {
        if (mControlPanel == null) {
            return;
        }
        Logger.t(TAG).d("show ControlPanel");
        mControlPanel.setVisibility(View.VISIBLE);
        mHandler.removeMessages(FADE_OUT);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT), 3000);
    }

    private void hideControlPanel() {
        mControlPanel.setVisibility(View.GONE);
    }

    public void setLapTimerData(AvrproLapTimerResult result, AvrproClipInfo clipInfo) {
        mLapData.addAll(Arrays.asList(result.lapList));
        mWvGauge.setLapTimerData(result, clipInfo);
    }

    private static class ControlPanelHandler extends Handler {
        private WeakReference<ClipPlayFragment> mFragmentRef;

        ControlPanelHandler(ClipPlayFragment fragment) {
            super();
            mFragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            ClipPlayFragment fragment = mFragmentRef.get();
            if (fragment == null) {
                return;
            }
            Logger.t(TAG).d("handle message");
            switch (msg.what) {
                case FADE_OUT:
                    try {
                        if (fragment.isFullScreen()) {
                            fragment.hideControlPanel();
                        }
                    } catch (Exception e) {
                        Logger.t(TAG).d("fragment is be GCed!");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public class UpdatePlayTimeTask extends TimerTask {

        @Override
        public void run() {
            if (mMediaPlayer != null && mPlayerControl != null && mPlayerControl.isPlaying()) {
                refreshProgressBar(getCurrentPlayingTime());
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mWvGauge.setPlayTime(getCurrentPlayingTime());
                    }
                });
            }

            if (mNeedSendPlayCompleteEvent) {
                refreshProgressBar(0);
                mNeedSendPlayCompleteEvent = false;
            }
        }

        private void refreshProgressBar(final int currentPos) {
            if (mEventBus != null) {
                ClipSetPos clipSetPos = getClipSet().getClipSetPosByTimeOffset(currentPos);
                ClipSetPosChangeEvent event = new ClipSetPosChangeEvent(clipSetPos, TAG);
                mEventBus.post(event);
            }

        }
    }

}
