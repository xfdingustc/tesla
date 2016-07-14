package com.waylens.hachi.ui.clips.player;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
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
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.eventbus.events.ClipEditEvent;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.eventbus.events.GaugeEvent;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.player.HachiPlayer;
import com.waylens.hachi.player.HlsRendererBuilder;
import com.waylens.hachi.player.Utils;
import com.waylens.hachi.snipe.glide.SnipeGlideLoader;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.clips.player.multisegseekbar.MultiSegSeekbar;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.views.GaugeView;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;
import com.waylens.hachi.vdb.ClipSetPos;
import com.waylens.hachi.vdb.rawdata.RawDataItem;
import com.waylens.hachi.vdb.urls.VdbUrl;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/2/22.
 */
public class ClipPlayFragment extends BaseFragment implements SurfaceHolder.Callback, HachiPlayer.Listener {
    private static final String TAG = ClipPlayFragment.class.getSimpleName();

    private static final int LOADING_STAGE_RAW_DATA = 0;
    private static final int LOADING_STAGE_GET_PLAYBACK_URL = 1;
    private static final int LOADING_STAGE_PREPARE_AUDIO = 2;
    private static final int LOADING_STAGE_PREPARE_VIDEO = 3;

    private static final int FADE_OUT = 5;

    private int mClipSetIndex;

    private UrlProvider mUrlProvider;

    private HachiPlayer mMediaPlayer;
    private MediaPlayer mAudioPlayer;

    private PlayerControl mPlayerControl;

    private String mAudioUrl;
    private VdbUrl mVdbUrl;

    private RawDataLoader mRawDataLoader;

    private Timer mTimer;
    private UpdatePlayTimeTask mUpdatePlayTimeTask;

    private ClipPos mPreviousShownClipPos;
    private long mPreviousShowThumbnailRequestTime;


    private PositionAdjuster mPositionAdjuster;

    private EventBus mEventBus = EventBus.getDefault();

    private SurfaceHolder mSurfaceHolder;

    private Handler mHandler;

    private boolean playerNeedsPrepare;

    private boolean mNeedSendPlayCompleteEvent = false;

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


    @OnClick(R.id.btnShowOverlay)
    public void onBtnShowOverlayClicked() {
        int visibility = mWvGauge.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
        mWvGauge.setVisibility(visibility);
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

    @OnClick(R.id.btnFullscreen)
    public void onBtnFullscreenClicked() {
        if (!isFullScreen()) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mControlPanel.setBackgroundColor(Color.argb(0x7f, 0, 0, 0));
            showControlPanel();
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mControlPanel.setBackgroundResource(0);
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
            mBtnFullscreen.setImageResource(R.drawable.screen_narrow);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            mControlPanel.setLayoutParams(params);
        } else {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.BELOW, mFragmentView.getId());
            mControlPanel.setLayoutParams(params);
            mBtnFullscreen.setImageResource(R.drawable.screen_full);
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


    @Subscribe
    public void onEventClipSetChanged(ClipSetChangeEvent event) {
        Logger.t(TAG).d("on Clip Set chang event clip count: " + getClipSet().getCount());
        releasePlayer();
        mBtnPlayPause.setImageResource(R.drawable.playbar_play);
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

//                mMediaPlayer.seekTo(0);
//                ClipSetPos clipSetPos = new ClipSetPos(0, getClipSet().getClip(0).getStartTimeMs());
//                setClipSetPos(clipSetPos, true);
                break;
        }
        if (mAudioUrl != null && mAudioPlayer != null) {
            updateAudioPlayerState(playWhenReady, playbackState);
        }
        updateControls(playWhenReady, playbackState);
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
                mBtnPlayPause.setImageResource(R.drawable.playbar_play);
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
                    mBtnPlayPause.setImageResource(R.drawable.playbar_pause);
                } else {
                    mBtnPlayPause.setImageResource(R.drawable.playbar_play);
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
        mHandler = new ControlPanelHandler(this);
        initViews();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();
        mTimer = new Timer();
        mUpdatePlayTimeTask = new UpdatePlayTimeTask();
        mTimer.schedule(mUpdatePlayTimeTask, 0, 100);
        mWvGauge.showGauge(true);
        //mWvGauge.setEnhanceMode();
        mEventBus.register(this);
        mEventBus.register(mMultiSegSeekbar);
        mEventBus.register(mWvGauge);

    }

    @Override
    public void onStop() {
        super.onStop();
        mTimer.cancel();
        mEventBus.unregister(this);
        mEventBus.unregister(mMultiSegSeekbar);
        mEventBus.unregister(mWvGauge);
        if (mMediaPlayer != null && mPlayerControl != null) {
            mPlayerControl.pause();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void initRawDataView() {
        mRawDataLoader = new RawDataLoader(mClipSetIndex, mVdbRequestQueue);
        mRawDataLoader.loadRawData();
        ClipSetPos clipSetPos = new ClipSetPos(0, getClipSet().getClip(0).editInfo.selectedStartValue);
        setClipSetPos(clipSetPos, true);
    }


    private void init() {
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

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceView.getHolder().addCallback(this);

        updateProgressTextView(0, getClipSet().getTotalSelectedLengthMs());

    }


    private void preparePlayer(boolean playWhenReady) {
        if (mMediaPlayer == null) {
            String userAgent = Utils.getUserAgent(getActivity(), getString(R.string.app_name));
            mMediaPlayer = new HachiPlayer(new HlsRendererBuilder(getActivity(), userAgent, mVdbUrl.url));
            mMediaPlayer.addListener(this);
            mMediaPlayer.seekTo(0);
            playerNeedsPrepare = true;
            mPlayerControl = mMediaPlayer.getPlayerControl();
        }
        if (playerNeedsPrepare) {
            mMediaPlayer.prepare();
            playerNeedsPrepare = false;
            updateButtonVisibilities();
        }

        mMediaPlayer.setSurface(mSurfaceView.getHolder().getSurface());
        mMediaPlayer.setPlayWhenReady(playWhenReady);
    }

    private void updateButtonVisibilities() {
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
        mBtnPlayPause.setImageResource(R.drawable.playbar_play);
//        mMultiSegSeekbar.reset();
        mNeedSendPlayCompleteEvent = true;

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
//                changeState(STATE_FAST_PREVIEW);
                enterFastPreview();
            }

            @Override
            public void onProgressChanged(MultiSegSeekbar seekBar, ClipSetPos clipSetPos) {
//                if (mCurrentState == STATE_FAST_PREVIEW) {
                setClipSetPos(clipSetPos, true);
//                }

                mEventBus.post(new ClipSetPosChangeEvent(clipSetPos, TAG));
            }

            @Override
            public void onStopTrackingTouch(MultiSegSeekbar seekBar) {
                startPreparingClip(seekBar.getCurrentClipSetPos(), false);
            }
        });
    }

    private void enterFastPreview() {
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
//                    Logger.t(TAG).d("Ignore clippos request");
                    return;
                }

                long lastRequestOffset = System.currentTimeMillis() - mPreviousShowThumbnailRequestTime;
                if (lastRequestOffset < 1000) {
                    return;
                }
            }

//            Logger.t(TAG).d("show cilpPos " + mClipCover.getVisibility());
//            mVdbImageLoader.displayVdbImage(clipPos, mClipCover, true, false);
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


    private void startPreparingClip(final ClipSetPos clipSetPos, final boolean loadRawData) {
        if (mBtnPlayPause.isEnabled() == false) {
            return;
        }


        Observable.create(new Observable.OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> subscriber) {
                Logger.t(TAG).d("start loading ");
                // First load raw data into memory
                if (loadRawData) {
                    mRawDataLoader = new RawDataLoader(mClipSetIndex, mVdbRequestQueue);
                    mRawDataLoader.loadRawData();
                    subscriber.onNext(LOADING_STAGE_RAW_DATA);
                }


                // Second fetch playback url
                long startTimeMs = getClipSet().getTimeOffsetByClipSetPos(clipSetPos);
                mVdbUrl = mUrlProvider.getUriSync(startTimeMs);
                Logger.t(TAG).d("get playback url: " + mVdbUrl.url);
                if (mUrlProvider instanceof ClipUrlProvider) {
                    mPositionAdjuster = new ClipPositionAdjuster(getClipSet().getClip(0).editInfo.selectedStartValue, mVdbUrl);
                } else {
                    mPositionAdjuster = new PlaylistPositionAdjuster(mVdbUrl);
                }
                subscriber.onNext(LOADING_STAGE_GET_PLAYBACK_URL);

                if (mVdbUrl == null) {

                }

                // prepare audio if audio is set:
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
                subscriber.onNext(LOADING_STAGE_PREPARE_AUDIO);
                subscriber.onNext(LOADING_STAGE_PREPARE_VIDEO);

            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        //Logger.t(TAG).d("loading staget: " + integer);
                        if (integer == LOADING_STAGE_PREPARE_VIDEO) {
                            preparePlayer(true);
                        }
                    }
                });

    }


    public void setAudioUrl(String audioUrl) {
        Logger.t(TAG).d("audio url: " + audioUrl);
        mAudioUrl = audioUrl;
        if (audioUrl == null && mAudioPlayer != null && mAudioPlayer.isPlaying()) {
            mAudioPlayer.stop();
        }
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

        if (mRawDataLoader != null) {
            List<RawDataItem> rawDataItemList = mRawDataLoader.getRawDataItemList(clipSetPos);
            if (rawDataItemList != null && !rawDataItemList.isEmpty()) {
                mWvGauge.updateRawDateItem(rawDataItemList);
            }
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
        String timeText = DateUtils.formatElapsedTime(currentPosition / 1000) + "/" + DateUtils.formatElapsedTime(duration / 1000);
        mTvProgress.setText(timeText);
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
            }

            if (mNeedSendPlayCompleteEvent) {
                refreshProgressBar(0);
                mNeedSendPlayCompleteEvent = false;
            }


        }

        private void refreshProgressBar(final int currentPos) {
//            final int currentPos = getCurrentPlayingTime();

//            final int duration = getClipSet().getTotalSelectedLengthMs();
            if (mEventBus != null) {
                ClipSetPos clipSetPos = getClipSet().getClipSetPosByTimeOffset(currentPos);
                ClipSetPosChangeEvent event = new ClipSetPosChangeEvent(clipSetPos, TAG);
                mEventBus.post(event);
            }


            if (mRawDataLoader != null) {
                //mRawDataLoader.updateGaugeView(currentPos, mWvGauge);
            }

        }
    }

}
