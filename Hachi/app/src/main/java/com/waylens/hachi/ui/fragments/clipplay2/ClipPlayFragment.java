package com.waylens.hachi.ui.fragments.clipplay2;

import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.activities.EnhancementActivity;
import com.waylens.hachi.ui.views.multisegseekbar.MultiSegSeekbar;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.urls.VdbUrl;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/2/22.
 */
public class ClipPlayFragment extends DialogFragment {
    private static final String TAG = ClipPlayFragment.class.getSimpleName();

    private ClipSet mClipSet;

    private VdtCamera mVdtCamera;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;

    private UrlProvider mUrProvider;

    private MediaPlayer mMediaPlayer = new MediaPlayer();

    private Handler mUiHandler;

    private Config mConfig;

    private PositionAdjuster mPositionAdjuster;

    private final int STATE_NONE = 0;
    private final int STATE_PREPAREING = 1;
    private final int STATE_PREPARED = 2;
    private final int STATE_PLAYING = 3;
    private final int STATE_PAUSE = 4;
    private final int STATE_FAST_PREVIEW = 5;

    private int mCurrentState = STATE_NONE;

    private final int PENDING_ACTION_NONE = 0;
    private final int PENDING_ACTION_START = 1;

    private int mPendingAction = PENDING_ACTION_NONE;

    @Bind(R.id.textureView)
    TextureView mTextureView;

    @Bind(R.id.clipCover)
    ImageView mClipCover;

    @Bind(R.id.progressLoading)
    ProgressBar mProgressLoading;

    @Bind(R.id.btnPlayPause)
    ImageButton mBtnPlayPause;

    @Bind(R.id.playProgress)
    TextView mTvProgress;

    @Bind(R.id.videoSeekBar)
    SeekBar mSeekBar;

    @Bind(R.id.controlPanel)
    LinearLayout mControlPanel;

    @Bind(R.id.vsBar)
    ViewSwitcher mVsBar;

    @Bind(R.id.multiSegIndicator)
    MultiSegSeekbar mMultiSegSeekbar;

    @OnClick(R.id.btnDismiss)
    public void onBtnDismissClicked() {
        dismiss();
    }

    @OnClick(R.id.btnPlayPause)
    public void onBtnPlayPauseClicked() {
        switch (mCurrentState) {
            case STATE_PREPARED:
            case STATE_PAUSE:
            case STATE_FAST_PREVIEW:
                changeState(STATE_PLAYING);
                refreshProgressBar();
                break;
            case STATE_PLAYING:
                changeState(STATE_PAUSE);
                break;
            case STATE_PREPAREING:
                mPendingAction = PENDING_ACTION_START;
                break;
        }

    }


    @OnClick(R.id.btnEnhance)
    public void onBtnEnhanceClicked() {
        dismiss();

        EnhancementActivity.launch(getActivity(), mClipSet.getClipList());
    }


    public static class Config {
        public static int PROGRESS_BAR_STYLE_SINGLE = 0;
        public int progressBarStyle = PROGRESS_BAR_STYLE_SINGLE;
        public boolean showControlPanel = true;
    }


    public static ClipPlayFragment newInstance(VdtCamera camera, ClipSet clipSet,
                                               UrlProvider vdtUrlProvider,
                                               Config config) {
        ClipPlayFragment fragment = new ClipPlayFragment();
        fragment.mVdtCamera = camera;
        fragment.mClipSet = clipSet;
        fragment.mUrProvider = vdtUrlProvider;
        fragment.mConfig = config;
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
                startPreparingClip(mClipSet.getClip(0).getStartTimeMs());
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
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaPlayer.stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMediaPlayer.reset();
    }

    private void init() {
        mUiHandler = new Handler();
        mVdbRequestQueue = Snipe.newRequestQueue(getActivity(), mVdtCamera);
        mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);


    }

    private void initViews() {
        ClipPos clipPos = new ClipPos(mClipSet.getClip(0));
        mVdbImageLoader.displayVdbImage(clipPos, mClipCover);

        if (!mConfig.showControlPanel) {
            mControlPanel.setVisibility(View.GONE);
        }

        if (mClipSet.getCount() > 1) {
            mVsBar.showNext();
            mMultiSegSeekbar.setClipList(mClipSet.getClipList());
            mMultiSegSeekbar.setOnMultiSegSeekbarChangListener(new MultiSegSeekbar.OnMultiSegSeekBarChangeListener() {
                @Override
                public void onStartTrackingTouch(MultiSegSeekbar seekBar) {
                    changeState(STATE_FAST_PREVIEW);
                }

                @Override
                public void onProgressChanged(MultiSegSeekbar seekBar, int progress) {
                    if (mCurrentState == STATE_FAST_PREVIEW) {
                        int time = (int) (((float) progress * mClipSet.getTotalSelectedLengthMs()) / seekBar.getMax());
                        ClipPos clipPos = mClipSet.findClipPosByTimePosition(time);
                        if (clipPos != null) {
                            mVdbImageLoader.displayVdbImage(clipPos, mClipCover, true, false);
                        }
                    }
                }

                @Override
                public void onStopTrackingTouch(MultiSegSeekbar seekBar) {

                }
            });

        } else {

            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                Logger.t(TAG).d("onProgressChanged");
                    if (mCurrentState == STATE_FAST_PREVIEW) {
                        long seekBarTimeMs = getSeekbarTimeMs();
                        ClipPos clipPos = new ClipPos(mClipSet.getClip(0), seekBarTimeMs, ClipPos.TYPE_POSTER, false);
                        mVdbImageLoader.displayVdbImage(clipPos, mClipCover, true, false);
                    }

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    Logger.t(TAG).d("onStartTrackingTouch");
                    changeState(STATE_FAST_PREVIEW);

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    Logger.t(TAG).d("onStopTrackingTouch");
                    startPreparingClip(getSeekbarTimeMs());
                }
            });

            mSeekBar.setMax(mClipSet.getClip(0).getDurationMs());
            mSeekBar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY));
            mSeekBar.getThumb().setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        }

    }

    public void setActiveClip(int position, Clip clip) {
        changeState(STATE_FAST_PREVIEW);
        ClipPos clipPos = new ClipPos(clip, 0, ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, mClipCover, true, false);
        mMultiSegSeekbar.setActiveClip(position);
    }

    public void showClipPosThumbnail(Clip clip, long timeMs) {
        changeState(STATE_FAST_PREVIEW);
        ClipPos clipPos = new ClipPos(clip, timeMs, ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, mClipCover, true, false);
    }

    public void notifyClipSetChanged() {
        mMultiSegSeekbar.invalidate();
        refreshProgressBar();
    }


    protected void openVideo(VdbUrl url) {
        try {
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Logger.t(TAG).d("Prepare finished!!!");
                    changeState(STATE_PREPARED);

                    if (mPendingAction == PENDING_ACTION_START) {
                        changeState(STATE_PLAYING);
                        mPendingAction = PENDING_ACTION_NONE;
                    }

                    refreshProgressBar();
                }
            });
//            mMediaPlayer.setOnCompletionListener(this);
//            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(url.url);
            mMediaPlayer.setSurface(new Surface(mTextureView.getSurfaceTexture()));
            mMediaPlayer.prepareAsync();
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
        } catch (IOException e) {
            Logger.t(TAG).e("", e);
        }
    }


    private void startPreparingClip(long clipTimeMs) {
        mUrProvider.getUri(clipTimeMs, new ClipUrlProvider.OnUriLoadedListener() {

            @Override
            public void onUriLoaded(VdbUrl url) {
                Logger.t(TAG).d("Get playback url: " + url.url);
                if (mClipSet.getCount() == 1) {
                    mPositionAdjuster = new ClipPositionAdjuster(mClipSet.getClip(0), url);
                } else {
                    mPositionAdjuster = new PlaylistPositionAdjuster();
                }
                openVideo(url);
            }


        });

        changeState(STATE_PREPAREING);
    }

    private void changeState(int targetState) {
        switch (targetState) {
            case STATE_PREPAREING:
                mClipCover.setVisibility(View.VISIBLE);
                mProgressLoading.setVisibility(View.VISIBLE);
                break;
            case STATE_PREPARED:
                mProgressLoading.setVisibility(View.GONE);
                break;
            case STATE_PLAYING:
                mClipCover.setVisibility(View.INVISIBLE);
                mBtnPlayPause.setImageResource(R.drawable.playbar_pause);
                mMediaPlayer.start();
                mProgressLoading.setVisibility(View.GONE);
                break;
            case STATE_PAUSE:
                mBtnPlayPause.setImageResource(R.drawable.playbar_play);
                mMediaPlayer.pause();
                break;
            case STATE_FAST_PREVIEW:
                mClipCover.setVisibility(View.VISIBLE);
                mBtnPlayPause.setImageResource(R.drawable.playbar_play);
                mMediaPlayer.pause();
                break;
        }
        mCurrentState = targetState;
    }

    private void refreshProgressBar() {
        int currentPos = mMediaPlayer.getCurrentPosition();
        int duration = mClipSet.getTotalSelectedLengthMs();

        int adjustedPosition = mPositionAdjuster.getAdjustedPostion(currentPos);

//        Logger.t(TAG).d("duration: " + duration + " currentPos: " + currentPos);

        String timeText = DateUtils.formatElapsedTime(adjustedPosition / 1000) + "/" + DateUtils
                .formatElapsedTime(duration / 1000);

        mTvProgress.setText(timeText);

        if (mClipSet.getCount() == 1) {
            mSeekBar.setProgress(adjustedPosition);
        } else {
            int progress = (int)((float)adjustedPosition * mMultiSegSeekbar.getMax() / duration);
            mMultiSegSeekbar.setProgress(progress);
        }

        if (mMediaPlayer.isPlaying()) {
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshProgressBar();
                }
            }, 50);
        }
    }

    public long getSeekbarTimeMs() {
        Clip clip = mClipSet.getClip(0);
        return clip.getStartTimeMs() + ((long) clip.getDurationMs() * mSeekBar.getProgress()) / mSeekBar.getMax();
    }


}
