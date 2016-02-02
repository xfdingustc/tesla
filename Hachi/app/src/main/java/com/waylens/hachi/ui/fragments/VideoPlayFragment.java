package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.views.OnViewDragListener;
import com.waylens.hachi.ui.views.DragLayout;
import com.waylens.hachi.ui.views.dashboard.DashboardLayout;
import com.xfdingustc.far.FixedAspectRatioFrameLayout;

import java.io.IOException;
import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Video Player
 * Created by Richard on 10/26/15.
 */
public abstract class VideoPlayFragment extends Fragment implements View.OnClickListener,
    MediaPlayer.OnPreparedListener, SurfaceHolder.Callback, MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener, ViewTreeObserver.OnGlobalLayoutListener {

    private static final String TAG = VideoPlayFragment.class.getSimpleName();

    protected static final String REQUEST_TAG = "RETRIEVE_RAW_DATA";

    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;

    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private static final int DEFAULT_TIMEOUT = 3000;
    static final long MAX_PROGRESS = 1000L;

    protected static final int RAW_DATA_STATE_UNKNOWN = -1;
    protected static final int RAW_DATA_STATE_READY = 0;
    protected static final int RAW_DATA_STATE_ERROR = 1;

    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    protected int mRawDataState = RAW_DATA_STATE_UNKNOWN;

    protected boolean mOverlayShouldDisplay = true;

    public static VideoPlayFragment fullScreenPlayer;

    @Bind(R.id.video_root_container)
    FixedAspectRatioFrameLayout mRootContainer;

    @Bind(R.id.waylens_video_container)
    DragLayout mVideoContainer;

    @Bind(R.id.overlayLayout)
    DashboardLayout mDashboardLayout;

    @Bind(R.id.video_controllers)
    FrameLayout mVideoController;

    @Bind(R.id.btn_play)
    ImageView mBtnPlay;

    @Bind(R.id.video_surface)
    SurfaceView mSurfaceView;

    @Bind(R.id.progress_loading)
    ProgressBar mProgressLoading;

    @Bind(R.id.progress_bar)
    ProgressBar mProgressBar;

    @Bind(R.id.btn_fullscreen)
    ImageView mBtnFullScreen;

    @Bind(R.id.text_video_time)
    TextView mVideoTime;

    @Bind(R.id.infoPanel)
    LinearLayout mInfoPanel;

    protected OnViewDragListener mDragListener;

    SurfaceHolder mSurfaceHolder;

    private MediaPlayer mMediaPlayer;

    VideoHandler mHandler;

    String mVideoSource;

    HashMap<String, String> mHeaders;

    boolean mIsFullScreen;

    ViewGroup mRootView;

    int mPausePosition;
    int mVideoWidth;
    int mVideoHeight;
    boolean mSurfaceDestroyed;

    HandlerThread mNonUIThread;
    Handler mNonUIHandler;

    OnProgressListener mProgressListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new VideoHandler(this);
        mNonUIThread = new HandlerThread("ReleaseMediaPlayer");
        mNonUIThread.start();
        mNonUIHandler = new Handler(mNonUIThread.getLooper());


    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRootView = (ViewGroup) getActivity().findViewById(android.R.id.content);
        getActivity().getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    showController(0);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_play, container, false);
        ButterKnife.bind(this, view);
        return view;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mVideoContainer.setOnClickListener(this);
        mBtnPlay.setOnClickListener(this);
        mSurfaceView.getHolder().addCallback(this);
        mBtnFullScreen.setOnClickListener(this);
        mProgressBar.setMax((int) MAX_PROGRESS);
        mVideoController.setVisibility(View.INVISIBLE);
        if (mDragListener != null) {
            mVideoContainer.setOnViewDragListener(mDragListener);
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        mDashboardLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);


    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeMessages(FADE_OUT);
        mHandler.removeMessages(SHOW_PROGRESS);
        if (mMediaPlayer != null) {
            release(true);
        }
        mVideoSource = null;
        mNonUIThread.quitSafely();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
        if (fullScreenPlayer == this) {
            fullScreenPlayer = null;
        }
        mDragListener = null;
    }

    protected void setSource(String source) {
        setSource(source, null);
    }

    protected void setSource(String source, HashMap<String, String> headers) {
        mVideoSource = source;
        mHeaders = headers;
        mTargetState = STATE_PLAYING;
        mPausePosition = 0;
        openVideo();
    }

    private FrameLayout.LayoutParams mPortraitParams;
    private FrameLayout.LayoutParams mPortraitInfoPanelParems;

    public void setFullScreen(boolean fullScreen) {
        int orientation = getActivity().getRequestedOrientation();


        if (fullScreen || orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            || orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            hideSystemUI();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mPortraitParams = (FrameLayout.LayoutParams) mDashboardLayout.getLayoutParams();
            mPortraitInfoPanelParems = (FrameLayout.LayoutParams) mInfoPanel.getLayoutParams();

            mRootContainer.removeView(mVideoContainer);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mRootView.addView(mVideoContainer, params);
            mBtnFullScreen.setImageResource(R.drawable.ic_fullscreen_exit_white_36dp);

            mRootContainer.removeView(mDashboardLayout);
            mRootView.addView(mDashboardLayout, params);
            calculateDashboardScaling(mRootView, true);

            mRootContainer.removeView(mInfoPanel);
            mRootView.addView(mInfoPanel, mPortraitInfoPanelParems);

            fullScreenPlayer = this;
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mRootView.removeView(mVideoContainer);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mRootContainer.addView(mVideoContainer, params);
            mBtnFullScreen.setImageResource(R.drawable.ic_fullscreen_white_36dp);
            fullScreenPlayer = null;


            mRootView.removeView(mDashboardLayout);
            mRootContainer.addView(mDashboardLayout, mPortraitParams);
            calculateDashboardScaling(mRootContainer, false);

            mRootView.removeView(mInfoPanel);
            mRootContainer.addView(mInfoPanel, mPortraitInfoPanelParems);
        }
        mIsFullScreen = fullScreen;
    }

    @Override
    public void onGlobalLayout() {
        if (mRootContainer != null) {
            calculateDashboardScaling(mRootContainer, false);

        }
    }

    private float mWidthScale = 0;

    private void calculateDashboardScaling(View parent, boolean landScape) {
        float scale = 1.0f;
        if (!landScape) {
            int width = parent.getMeasuredWidth();

            if (mWidthScale == 0) {
                mWidthScale = (float) width / DashboardLayout.NORMAL_WIDTH;

            }

            scale = mWidthScale;
        }

        mDashboardLayout.setScaleX(scale);
        mDashboardLayout.setScaleY(scale);
        mDashboardLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }


    private void hideSystemUI() {
        int uiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        /*
        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }*/

        getActivity().getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:
                playVideo();
                break;
            case R.id.waylens_video_container:
                toggleController();
                break;
            case R.id.btn_fullscreen:
                setFullScreen(!mIsFullScreen);
                break;
        }
    }

    private void playVideo() {
        if (!isInPlaybackState()) {
            mTargetState = STATE_PLAYING;
            openVideo();
            return;
        }
        if (mMediaPlayer.isPlaying()) {
            pauseVideo();
        } else {
            resumeVideo();
        }
    }

    private void resumeVideo() {
        start();
        mCurrentState = STATE_PLAYING;
        mTargetState = STATE_PLAYING;
        mBtnPlay.setImageResource(R.drawable.ic_pause_circle_outline_white_48dp);
        fadeOutControllers(DEFAULT_TIMEOUT);
    }

    private void pauseVideo() {
        mMediaPlayer.pause();
        mPausePosition = mMediaPlayer.getCurrentPosition();
        mCurrentState = STATE_PAUSED;
        mTargetState = STATE_PAUSED;
        mBtnPlay.setImageResource(R.drawable.ic_play_circle_outline_white_48dp);
        mHandler.removeMessages(SHOW_PROGRESS);
    }

    private void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            mProgressLoading.setVisibility(View.GONE);
        }
        mTargetState = STATE_PLAYING;
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
            mCurrentState != STATE_ERROR &&
            mCurrentState != STATE_IDLE &&
            mCurrentState != STATE_PREPARING);
    }

    protected void openVideo() {
        if (mVideoSource == null
            || mSurfaceView == null
            || mSurfaceHolder == null) {
            return;
        }

        if (mOverlayShouldDisplay && mRawDataState == RAW_DATA_STATE_UNKNOWN) {
            return;
        }


        mProgressLoading.setVisibility(View.VISIBLE);
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);

            if (mHeaders != null) {
                mMediaPlayer.setDataSource(getActivity(), Uri.parse(mVideoSource), mHeaders);
            } else {
                mMediaPlayer.setDataSource(mVideoSource);
            }
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
        } catch (IOException e) {
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            Log.e(TAG, "", e);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mCurrentState = STATE_PREPARED;
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
        if (mSurfaceHolder != null && mVideoWidth != 0 && mVideoHeight != 0) {
            mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
        }
        showController(DEFAULT_TIMEOUT);
        if (mTargetState == STATE_PLAYING) {
            start();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Logger.t(TAG).d("surfaceCreated");
        mSurfaceHolder = holder;
        if (!isInPlaybackState()) {
            openVideo();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("test", "surfaceChanged - w:" + width + "; h:" + height);
        if (mMediaPlayer == null) {
            return;
        }

        mMediaPlayer.setDisplay(holder);

        if (isInPlaybackState() && mSurfaceDestroyed) {
            if (mCurrentState == STATE_PLAYING) {
                Log.e("test", "surfaceChanged - resume");
                resumeVideo();
            } else {
                Log.e("test", "surfaceChanged - seek");
                mMediaPlayer.seekTo(mPausePosition);
                mPausePosition = 0;
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("test", "surfaceDestroyed");
        mSurfaceHolder = null;
        mSurfaceDestroyed = true;
        if (isInPlaybackState() && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPausePosition = mMediaPlayer.getCurrentPosition();
        }
    }

    private void release(final boolean clearTargetState) {
        final MediaPlayer mediaPlayer = mMediaPlayer;
        mMediaPlayer = null;
        mCurrentState = STATE_IDLE;
        if (clearTargetState) {
            mTargetState = STATE_IDLE;
        }
        mVideoWidth = 0;
        mVideoHeight = 0;

        mNonUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    mediaPlayer.reset();
                    mediaPlayer.release();
                }
            }
        });
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mCurrentState = STATE_PLAYBACK_COMPLETED;
        mTargetState = STATE_PLAYBACK_COMPLETED;
        mBtnPlay.setImageResource(R.drawable.ic_play_circle_outline_white_48dp);
        showController(0);
        int duration = mp.getDuration();
        setProgress(duration, duration);
        mHandler.removeMessages(SHOW_PROGRESS);
        onPlayCompletion();
        release(true);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == MediaPlayer.MEDIA_ERROR_IO) {
            Snackbar.make(getView(), "Cannot load video.", Snackbar.LENGTH_SHORT).show();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        getFragmentManager().beginTransaction().remove(VideoPlayFragment.this).commit();
                    } catch (Exception e) {
                        Log.e("test", "", e);
                    }
                }
            }, 1000);
            return true;
        }
        return false;
    }

    void toggleController() {
        if (mProgressLoading.getVisibility() == View.VISIBLE) {
            return;
        }
        mVideoController.setVisibility(mVideoController.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
        mInfoPanel.setVisibility(mInfoPanel.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
    }

    void showController(int timeout) {
        if (mVideoController == null) {
            return;
        }
        mVideoController.setVisibility(View.VISIBLE);
        if (timeout > 0) {
            fadeOutControllers(timeout);
        }
    }

    private void fadeOutControllers(int timeout) {
        mHandler.removeMessages(FADE_OUT);
        mHandler.sendEmptyMessageDelayed(FADE_OUT, timeout);
    }

    private void hideControllers() {
        mVideoController.setVisibility(View.INVISIBLE);
        if (getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            && mCurrentState == STATE_PLAYING) {
            hideSystemUI();
        }
    }

    void showProgress() {
        if (mMediaPlayer == null) {
            return;
        }
        int position = mMediaPlayer.getCurrentPosition();
        int duration = mMediaPlayer.getDuration();
        setProgress(position, duration);
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 20);
        }
    }


    public void setOnProgressListener(OnProgressListener listener) {
        mProgressListener = listener;
    }

    protected abstract void setProgress(int position, int duration);

    protected abstract void displayOverlay(int position);

    protected abstract void onPlayCompletion();

    static class VideoHandler extends Handler {
        VideoPlayFragment thisFragment;

        VideoHandler(VideoPlayFragment fragment) {
            super();
            thisFragment = fragment;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FADE_OUT:
                    thisFragment.hideControllers();
                    break;
                case SHOW_PROGRESS:
                    thisFragment.showProgress();
                    break;
            }
        }
    }

    public interface OnProgressListener {
        void onProgress(int position, int duration);
    }
}
