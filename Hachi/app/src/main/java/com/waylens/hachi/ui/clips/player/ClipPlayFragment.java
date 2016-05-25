package com.waylens.hachi.ui.clips.player;

import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.text.format.DateUtils;
import android.view.Gravity;
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
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.eventbus.events.GaugeEvent;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/2/22.
 */
public class ClipPlayFragment extends BaseFragment implements SurfaceHolder.Callback {
    private static final String TAG = ClipPlayFragment.class.getSimpleName();

    private int mClipSetIndex;

    private UrlProvider mUrlProvider;

    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private MediaPlayer mAudioPlayer = new MediaPlayer();

    private String mAudioUrl;
    private VdbUrl mVdbUrl;

    private RawDataLoader mRawDataLoader;

    private boolean mIsFullScreen = false;

    private Timer mTimer;
    private UpdatePlayTimeTask mUpdatePlayTimeTask;

    private ClipPos mPreviousShownClipPos;
    private long mPreviousShowThumbnailRequestTime;


    private PositionAdjuster mPositionAdjuster;

    private BannerAdapter mBannerAdapter;


    private final int STATE_IDLE = 0;
    private final int STATE_PREPAREING = 1;
    private final int STATE_PREPARED = 2;
    private final int STATE_PLAYING = 3;
    private final int STATE_PAUSE = 4;
    private final int STATE_FAST_PREVIEW = 5;

    private int mCurrentState = STATE_IDLE;
    private EventBus mEventBus = EventBus.getDefault();

    private SurfaceHolder mSurfaceHolder;

    @BindView(R.id.surface_view)
    SurfaceView mSurfaceView;

    @BindView(R.id.vsCover)
    ViewSwitcher mVsCover;

    @BindView(R.id.clipCover)
    ImageView mClipCover;

    @BindView(R.id.coverBanner)
    ViewPager mCoverBanner;

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

    @BindView(R.id.root_container)
    FrameLayout mRootContainer;

    @BindView(R.id.fragment_view)
    LinearLayout mFragmentView;

    @BindView(R.id.control_panel)
    LinearLayout mControlPanel;

    @BindView(R.id.media_window)
    FrameLayout mMediaWindow;


    @OnClick(R.id.btnShowOverlay)
    public void onBtnShowOverlayClicked() {
        boolean isGaugeShown = !mWvGauge.isGaugeShown();
        mWvGauge.showGauge(isGaugeShown);
    }


    @OnClick(R.id.btnPlayPause)
    public void onBtnPlayPauseClicked() {
        switch (mCurrentState) {
            case STATE_IDLE:
            case STATE_PREPARED:
            case STATE_FAST_PREVIEW:
                start();
                break;
            case STATE_PAUSE:
                changeState(STATE_PLAYING);
                break;
            case STATE_PLAYING:
                changeState(STATE_PAUSE);
                break;
            case STATE_PREPAREING:
                break;
        }
    }

    @OnClick(R.id.btnFullscreen)
    public void onBtnFullscreenClicked() {
        mIsFullScreen = !mIsFullScreen;
        if (mIsFullScreen) {
            hideSystemUI();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            mBtnFullscreen.setImageResource(R.drawable.screen_narrow);

            mFragmentView.removeView(mControlPanel);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.BOTTOM;
            mMediaWindow.addView(mControlPanel, params);


        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            mMediaWindow.removeView(mControlPanel);
            mFragmentView.addView(mControlPanel);

            mBtnFullscreen.setImageResource(R.drawable.screen_full);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventClipSetChanged(ClipSetPosChangeEvent event) {
        final ClipSetPos clipSetPos = event.getClipSetPos();
        boolean refreshThumbnail = false;
        if (!event.getBroadcaster().equals("clipplay")) {
            refreshThumbnail = true;
        }
        setClipSetPos(clipSetPos, refreshThumbnail);

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


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mMediaPlayer == null) {
            return;
        }
        mMediaPlayer.setDisplay(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
    }

    private void start() {
        startPreparingClip(mMultiSegSeekbar.getCurrentClipSetPos(), true);
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
//        if (getShowsDialog()) {
//            DisplayMetrics dm = new DisplayMetrics();
//            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
//            getDialog().getWindow().setLayout(dm.widthPixels, getDialog().getWindow().getAttributes().height);
//        }

        mTimer = new Timer();
        mUpdatePlayTimeTask = new UpdatePlayTimeTask();
        mTimer.schedule(mUpdatePlayTimeTask, 0, 100);
        mEventBus.register(this);
        mEventBus.register(mMultiSegSeekbar);
        mEventBus.register(mWvGauge);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopPlayer();
        mTimer.cancel();
        mEventBus.unregister(this);
        mEventBus.unregister(mMultiSegSeekbar);
        mEventBus.unregister(mWvGauge);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMediaPlayer.reset();
    }

    private void init() {
        initCamera();
        setRetainInstance(true);
    }

    private void initViews() {
        setupToolbar();
        if (getClipSet() == null) {
            return;
        }

        mWvGauge.showGauge(false);

        ClipPos clipPos = new ClipPos(getClipSet().getClip(0));


        if (mCoverMode == CoverMode.NORMAL) {
            mVdbImageLoader.displayVdbImage(clipPos, mClipCover);
        } else {
            mVsCover.showNext();
            setupCoverBanner();
        }


        setupMultiSegSeekBar();

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceView.getHolder().addCallback(this);

    }

    private void setupCoverBanner() {
        mBannerAdapter = new BannerAdapter(getActivity(), mVdbImageLoader);
        final ClipSet clipSet = getClipSet();
        for (int i = 0; i < clipSet.getCount(); i++) {
            Clip clip = clipSet.getClip(i);
            mBannerAdapter.addClipPos(new ClipPos(clip));
        }

        mCoverBanner.setAdapter(mBannerAdapter);
        mCoverBanner.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                ClipSetPos clipSetPos = new ClipSetPos(position, getClipSet().getClip(position).getStartTimeMs());
                mEventBus.post(new ClipSetPosChangeEvent(clipSetPos, TAG));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void hideSystemUI() {
        int uiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;

        getActivity().getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
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
                changeState(STATE_FAST_PREVIEW);
            }

            @Override
            public void onProgressChanged(MultiSegSeekbar seekBar, ClipSetPos clipSetPos) {
                if (mCurrentState == STATE_FAST_PREVIEW) {
                    setClipSetPos(clipSetPos, true);
                }

                mEventBus.post(new ClipSetPosChangeEvent(clipSetPos, TAG));
            }

            @Override
            public void onStopTrackingTouch(MultiSegSeekbar seekBar) {
                //startPreparingClip(getSeekbarTimeMs());
            }
        });
    }


    public void showClipPosThumbnail(Clip clip, long timeMs) {
        changeState(STATE_FAST_PREVIEW);
        ClipPos clipPos = new ClipPos(clip, timeMs, ClipPos.TYPE_POSTER, false);
        showThumbnail(clipPos);
    }

    public void showThumbnail(ClipPos clipPos) {
        if (clipPos != null && mVdbImageLoader != null) {
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

            //          Logger.t(TAG).d("show cilpPos");
            mVdbImageLoader.displayVdbImage(clipPos, mClipCover, true, false);
            mPreviousShownClipPos = clipPos;
            mPreviousShowThumbnailRequestTime = System.currentTimeMillis();
        }
    }


    protected void openVideo() {


        try {
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Logger.t(TAG).d("Prepare finished!!!");
                    changeState(STATE_PREPARED);

                    changeState(STATE_PLAYING);
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    ClipSetPos clipSetPos = new ClipSetPos(0, getClipSet().getClip(0).editInfo.selectedStartValue);
                    mEventBus.post(new ClipSetPosChangeEvent(clipSetPos, TAG));
                    changeState(STATE_IDLE);
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    switch (what) {
                        case MediaPlayer.MEDIA_ERROR_IO:
                            break;
                    }
                    return false;
                }
            });
            mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    switch (what) {
                        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                            mProgressLoading.setVisibility(View.VISIBLE);
                            break;
                        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                            mProgressLoading.setVisibility(View.GONE);
                            break;
                    }
                    return false;
                }
            });

            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(mVdbUrl.url);
//            mMediaPlayer.setSurface(new Surface(mTextureView.getSurfaceTexture()));
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.prepareAsync();

        } catch (IOException e) {
            Logger.t(TAG).e("", e);
        }
    }

    private void openAudio() {
        if (mAudioUrl == null) {
            openVideo();
            return;
        }

        mAudioPlayer = new MediaPlayer();
        try {
            mAudioPlayer.setDataSource(mAudioUrl);
            mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    openVideo();
                }
            });
            mAudioPlayer.prepareAsync();
        } catch (IOException e) {
            Logger.e("", e);
        }

    }


    private void startPreparingClip(final ClipSetPos clipSetPos, boolean loadRawData) {
        if (mBtnPlayPause.isEnabled() == false) {
            return;
        }
        if (loadRawData == false) {
            startLoadPlaybackUrl(clipSetPos);
        } else {
            mRawDataLoader = new RawDataLoader(mClipSetIndex, mVdbRequestQueue);
            mRawDataLoader.startLoad(new RawDataLoader.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete() {
                    startLoadPlaybackUrl(clipSetPos);
                }
            });
        }
        changeState(STATE_PREPAREING);
    }

    private void startLoadPlaybackUrl(ClipSetPos clipSetPos) {
        long startTimeMs = getClipSet().getTimeOffsetByClipSetPos(clipSetPos);
        Logger.t(TAG).d("startTimeMs: " + startTimeMs);
        mUrlProvider.getUri(startTimeMs, new UrlProvider.OnUrlLoadedListener() {

            @Override
            public void onUrlLoaded(VdbUrl url) {
                if (url == null) {
                    Snackbar.make(mBtnPlayPause, R.string.get_url_failed, Snackbar.LENGTH_SHORT).show();
                    return;
                }
//                Logger.t(TAG).d("Get playback url: " + url.url);
                mVdbUrl = url;
                if (mUrlProvider instanceof ClipUrlProvider) {
                    mPositionAdjuster = new ClipPositionAdjuster(getClipSet().getClip(0), url);
                } else {
                    mPositionAdjuster = new PlaylistPositionAdjuster(url);
                }
                openAudio();
            }


        });
    }

    private void changeState(int targetState) {
        switch (targetState) {
            case STATE_IDLE:
                mVsCover.setVisibility(View.VISIBLE);
                mProgressLoading.setVisibility(View.INVISIBLE);
                mBtnPlayPause.setImageResource(R.drawable.playbar_play);
                break;
            case STATE_PREPAREING:
                mVsCover.setVisibility(View.VISIBLE);
                mProgressLoading.setVisibility(View.VISIBLE);
                break;
            case STATE_PREPARED:
                mProgressLoading.setVisibility(View.GONE);
                break;
            case STATE_PLAYING:
                mVsCover.setVisibility(View.INVISIBLE);
                mBtnPlayPause.setImageResource(R.drawable.playbar_pause);
                startPlayer();
                mProgressLoading.setVisibility(View.GONE);
                break;
            case STATE_PAUSE:
                mBtnPlayPause.setImageResource(R.drawable.playbar_play);
                pausePlayer();
                break;
            case STATE_FAST_PREVIEW:
                mVsCover.setVisibility(View.VISIBLE);
                mBtnPlayPause.setImageResource(R.drawable.playbar_play);
                stopPlayer();
                break;
        }
        mCurrentState = targetState;
    }

    private void startPlayer() {
        mMediaPlayer.start();
        if (mAudioUrl != null) {
            mAudioPlayer.start();
        }
    }

    private void pausePlayer() {
        mMediaPlayer.pause();
        if (mAudioUrl != null) {
            mAudioPlayer.pause();
        }
    }

    private void stopPlayer() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        if (mAudioUrl != null && mAudioPlayer.isPlaying()) {
            mAudioPlayer.stop();
        }
    }


    public void setAudioUrl(String audioUrl) {
        Logger.t(TAG).d("audio url: " + audioUrl);
        mAudioUrl = audioUrl;
        if (audioUrl == null && mAudioPlayer != null && mAudioPlayer.isPlaying()) {
            mAudioPlayer.stop();
        }
    }


    public void setUrlProvider(UrlProvider urlProvider) {
        mUrlProvider = urlProvider;
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
            for (RawDataItem item : rawDataItemList) {
                mWvGauge.updateRawDateItem(item);
            }
        }

    }

    public ClipSetPos getClipSetPos() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            return getClipSet().getClipSetPosByTimeOffset(getCurrentPlayingTime());
        } else {
            return mMultiSegSeekbar.getCurrentClipSetPos();
        }
    }

    public int getCurrentPlayingTime() {
        if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
            return 0;
        }

        int currentPos = mMediaPlayer.getCurrentPosition();
        if (mPositionAdjuster != null) {
            currentPos = mPositionAdjuster.getAdjustedPostion(currentPos);
        }
        return currentPos;
    }


    private void updateProgressTextView(long currentPosition, long duration) {
        String timeText = DateUtils.formatElapsedTime(currentPosition / 1000) + "/" + DateUtils.formatElapsedTime(duration / 1000);
        mTvProgress.setText(timeText);
    }

    public interface ClipPlayFragmentContainer {
        ClipPlayFragment getClipPlayFragment();
    }

    public class UpdatePlayTimeTask extends TimerTask {

        @Override
        public void run() {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                refreshProgressBar();

            }
        }

        private void refreshProgressBar() {
            final int currentPos = getCurrentPlayingTime();

//            final int duration = getClipSet().getTotalSelectedLengthMs();
            if (mEventBus != null) {
                ClipSetPos clipSetPos = getClipSet().getClipSetPosByTimeOffset(currentPos);
                ClipSetPosChangeEvent event = new ClipSetPosChangeEvent(clipSetPos, "clipplay");
                mEventBus.post(event);
            }


            if (mRawDataLoader != null) {
                //mRawDataLoader.updateGaugeView(currentPos, mWvGauge);
            }

        }
    }

}
