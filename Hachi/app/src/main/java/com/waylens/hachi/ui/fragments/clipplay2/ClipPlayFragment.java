package com.waylens.hachi.ui.fragments.clipplay2;

import android.app.DialogFragment;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.activities.ClipModifyActivity;
import com.waylens.hachi.ui.activities.EnhancementActivity;
import com.waylens.hachi.ui.views.GaugeView;
import com.waylens.hachi.ui.views.multisegseekbar.MultiSegSeekbar;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;
import com.waylens.hachi.vdb.ClipSetPos;
import com.waylens.hachi.vdb.urls.VdbUrl;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/2/22.
 */
public class ClipPlayFragment extends DialogFragment {
    private static final String TAG = ClipPlayFragment.class.getSimpleName();

    private int mClipSetIndex;

    private VdtCamera mVdtCamera;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;

    private UrlProvider mUrlProvider;

    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private MediaPlayer mAudioPlayer = new MediaPlayer();

    private String mAudioUrl;
    private VdbUrl mVdbUrl;

    private float mAudioPlayerVolume = 1.0f;

    private Handler mUiHandler;


    private RawDataLoader mRawDataLoader;


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

    @Bind(R.id.textureView)
    TextureView mTextureView;

    @Bind(R.id.vsCover)
    ViewSwitcher mVsCover;

    @Bind(R.id.clipCover)
    ImageView mClipCover;

    @Bind(R.id.coverBanner)
    ViewPager mCoverBanner;

    @Bind(R.id.progressLoading)
    ProgressBar mProgressLoading;

    @Bind(R.id.btnPlayPause)
    ImageButton mBtnPlayPause;

    @Bind(R.id.playProgress)
    TextView mTvProgress;

    @Bind(R.id.clipPlayFragToolbar)
    Toolbar mToolbar;

    @Bind(R.id.multiSegIndicator)
    MultiSegSeekbar mMultiSegSeekbar;

    @Bind(R.id.gaugeView)
    GaugeView mWvGauge;


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


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventClipSetChanged(ClipSetPosChangeEvent event) {
        final ClipSetPos clipSetPos = event.getClipSetPos();
        boolean refreshThumbnail = false;
        if (!event.getBroadcaster().equals("clipplay")) {
            refreshThumbnail = true;
        }
        setClipSetPos(clipSetPos, refreshThumbnail);

    }


    private void start() {
        startPreparingClip(mMultiSegSeekbar.getCurrentClipSetPos(), false);
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
        if (getShowsDialog()) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        View view = inflater.inflate(R.layout.fragment_clip_play, container, false);
        ButterKnife.bind(this, view);
        initViews();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //mSurfaceView.getHolder().addCallback(this);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

                mBtnPlayPause.setEnabled(true);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getShowsDialog()) {
            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            getDialog().getWindow().setLayout(dm.widthPixels, getDialog().getWindow().getAttributes().height);
        }

        mTimer = new Timer();
        mUpdatePlayTimeTask = new UpdatePlayTimeTask();
        mTimer.schedule(mUpdatePlayTimeTask, 1000, 1000);
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
        mUiHandler = new Handler();
        mVdbRequestQueue = mVdtCamera.getRequestQueue();//Snipe.newRequestQueue(getActivity(), mVdtCamera);
        mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);


    }

    private void initViews() {
        mBtnPlayPause.setEnabled(false);
        setupToolbar();
        if (getClipSet() == null) {
            return;
        }

        ClipPos clipPos = new ClipPos(getClipSet().getClip(0));


        if (mCoverMode == CoverMode.NORMAL) {
            mVdbImageLoader.displayVdbImage(clipPos, mClipCover);
        } else {
            mVsCover.showNext();
            setupCoverBanner();
        }


        setupMultiSegSeekBar();

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


    private void setupToolbar() {
        mToolbar.setNavigationIcon(R.drawable.navbar_close);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mToolbar.inflateMenu(R.menu.menu_clip_play_fragment);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_to_share:
                        dismiss();
                        //ShareActivity.launch(getActivity(), mClipSetIndex);
                        return true;
                    case R.id.menu_to_enhance:
                        dismiss();
                        ClipSet clipSet = ClipSetManager.getManager().getClipSet(mClipSetIndex);
                        ArrayList<Clip> clipList = new ArrayList<>();
                        for (Clip clip : clipSet.getClipList()) {
                            clipList.add(clip);
                        }
                        EnhancementActivity.launch(getActivity(), clipList, EnhancementActivity.LAUNCH_MODE_ENHANCE);
                        return true;
                    case R.id.menu_to_modify:
                        dismiss();
                        ClipModifyActivity.launch(getActivity(), getClipSet().getClip(0));
                        return true;
                }
                return false;
            }
        });

        if (getShowsDialog()) {
            mToolbar.setVisibility(View.VISIBLE);
        } else {
            mToolbar.setVisibility(View.GONE);
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

    public void notifyClipSetChanged() {
        //mMultiSegSeekbar.setClipList(mClipSetIndex);
        mMultiSegSeekbar.notifyDateSetChanged();
        //refreshProgressBar();
    }


    protected void openVideo() {
        if (mTextureView == null) {
            return;
        }

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
                    ClipSetPos clipSetPos = new ClipSetPos(0, getClipSet().getClip(0).getStartTimeMs());
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
            mMediaPlayer.setSurface(new Surface(mTextureView.getSurfaceTexture()));
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
            Log.e("test", "", e);
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


    public void setAudioPlayerVolume(float volume) {
        if (mAudioPlayer != null) {
            mAudioPlayer.setVolume(volume, volume);
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
