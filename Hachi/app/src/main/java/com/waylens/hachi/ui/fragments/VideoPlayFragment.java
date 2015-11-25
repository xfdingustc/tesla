package com.waylens.hachi.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.views.DragLayout;

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
        MediaPlayer.OnErrorListener {

    private static final String TAG = "VideoPlayFragment";

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
    private static final long MAX_PROGRESS = 1000L;

    protected static final int RAW_DATA_STATE_UNKNOWN = -1;
    protected static final int RAW_DATA_STATE_READY = 0;
    protected static final int RAW_DATA_STATE_ERROR = 1;

    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    protected int mRawDataState =  RAW_DATA_STATE_UNKNOWN;

    public static VideoPlayFragment fullScreenPlayer;

    @Bind(R.id.root_container)
    FrameLayout mRootContainer;

    @Bind(R.id.waylens_video_container)
    DragLayout mVideoContainer;

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

    protected DragLayout.OnViewDragListener mDragListener;

    private SurfaceHolder mSurfaceHolder;

    private MediaPlayer mMediaPlayer;

    private VideoHandler mHandler;

    private String mVideoSource;

    private boolean mIsFullScreen;

    private ViewGroup mRootView;

    private int mPausePosition;
    private int mVideoWidth;
    private int mVideoHeight;
    private boolean mSurfaceDestroyed;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new VideoHandler(this);
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
    public void onStop() {
        super.onStop();
        mHandler.removeMessages(FADE_OUT);
        mHandler.removeMessages(SHOW_PROGRESS);
        if (mMediaPlayer != null) {
            release(true);
        }
        mVideoSource = null;
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
        mVideoSource = source;
        mTargetState = STATE_PLAYING;
        mPausePosition = 0;
        openVideo();
    }

    public void setFullScreen(boolean fullScreen) {
        int orientation = getActivity().getRequestedOrientation();
        if (fullScreen || orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                || orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            hideSystemUI();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mRootContainer.removeView(mVideoContainer);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mRootView.addView(mVideoContainer, params);
            mBtnFullScreen.setImageResource(R.drawable.ic_fullscreen_exit_white_36dp);
            fullScreenPlayer = this;
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mRootView.removeView(mVideoContainer);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mRootContainer.addView(mVideoContainer, params);
            mBtnFullScreen.setImageResource(R.drawable.ic_fullscreen_white_36dp);
            fullScreenPlayer = null;
        }
        mIsFullScreen = fullScreen;
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
        mHandler.removeMessages(SHOW_PROGRESS);
    }

    private void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
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
                || mSurfaceHolder == null
                || mRawDataState == RAW_DATA_STATE_UNKNOWN) {
            return;
        }
        mProgressLoading.setVisibility(View.VISIBLE);
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            //mMediaPlayer.setDataSource(mVideoSource);
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Cookie", "user=richard;key1=value1");
            mMediaPlayer.setDataSource(null, Uri.parse(mVideoSource), headers);
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

        mProgressLoading.setVisibility(View.GONE);
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
        if (mSurfaceHolder != null && mVideoWidth != 0 && mVideoHeight != 0) {
            mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
        }
        showController(DEFAULT_TIMEOUT);
        int duration = mMediaPlayer.getDuration();
        int position = mMediaPlayer.getCurrentPosition();
        updateVideoTime(position, duration);

        if (mTargetState == STATE_PLAYING) {
            start();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateVideoTime(int position, int duration) {
        mVideoTime.setText(DateUtils.formatElapsedTime(position / 1000) + " / " + DateUtils.formatElapsedTime(duration / 1000));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e("test", "surfaceCreated");
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
            mSurfaceDestroyed = false;
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

    private void release(boolean clearTargetState) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (clearTargetState) {
                mTargetState = STATE_IDLE;
            }
        }
        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mCurrentState = STATE_PLAYBACK_COMPLETED;
        mTargetState = STATE_PLAYBACK_COMPLETED;
        mBtnPlay.setImageResource(R.drawable.ic_refresh_white_48dp);
        showController(0);
        mHandler.removeMessages(SHOW_PROGRESS);
        onPlayCompletion();
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
    }

    void showController(int timeout) {
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
        setProgress();
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 20);
        }
    }

    protected void setProgress() {
        if (mMediaPlayer == null) {
            return;
        }
        int position = mMediaPlayer.getCurrentPosition();
        int duration = mMediaPlayer.getDuration();
        //Log.e("test", "duration: " + duration + "; position: " + position);

        if (mProgressBar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = MAX_PROGRESS * position / duration;
                mProgressBar.setProgress((int) pos);
            }
            //int percent = mMediaPlayer.getBufferPercentage();
            //mProgress.setSecondaryProgress(percent * 10);
        }
        updateVideoTime(position, duration);
        displayOverlay(position);
    }

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
}
