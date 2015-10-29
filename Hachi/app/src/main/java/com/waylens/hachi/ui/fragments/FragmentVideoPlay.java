package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
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

import com.waylens.hachi.R;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Video Player
 * Created by Richard on 10/26/15.
 */
public class FragmentVideoPlay extends Fragment implements View.OnClickListener,
        MediaPlayer.OnPreparedListener, SurfaceHolder.Callback, MediaPlayer.OnCompletionListener {

    private static final String TAG = "FragmentVideoPlay";

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

    private static final int DEFAULT_TIMEOUT = 1000;
    private static final long MAX_PROGRESS = 1000L;

    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    @Bind(R.id.root_container)
    FrameLayout mRootContainer;

    @Bind(R.id.waylens_video_container)
    FrameLayout mVideoContainer;

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

    private SurfaceHolder mSurfaceHolder;

    private MediaPlayer mMediaPlayer;

    private VideoHandler mHandler;

    private String mVideoSource;

    private boolean mIsFullScreen;

    private ViewGroup mRootView;

    static String VIDEO_URL = "http://192.168.2.3:8085/clip/0/1644c89e/1/0-1800799-0-0.m3u8";
    private int mPausePosition;
    private int mVideoWidth;
    private int mVideoHeight;
    private boolean mSurfaceDestroyed;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new VideoHandler(this);
        VIDEO_URL = "http://192.168.2.3:8085/clip/1/55e6f08e/1/904904-38038-0-0.m3u8";
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
    }

    @Override
    public void onStart() {
        super.onStart();
        setSource(VIDEO_URL);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
        mHandler.removeMessages(FADE_OUT);
        mHandler.removeMessages(SHOW_PROGRESS);
        release(true);
    }

    public void setSource(String source) {
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
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mRootView.removeView(mVideoContainer);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mRootContainer.addView(mVideoContainer, params);
            mBtnFullScreen.setImageResource(R.drawable.ic_fullscreen_white_36dp);
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

    private void openVideo() {
        if (mVideoSource == null
                || mSurfaceView == null
                || mSurfaceHolder == null) {
            return;
        }
        mProgressLoading.setVisibility(View.VISIBLE);
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setDataSource(mVideoSource);
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
        if (mTargetState == STATE_PLAYING) {
            start();
        }
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
    }

    void toggleController() {
        mVideoController.setVisibility(mVideoController.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
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
        mVideoController.setVisibility(View.GONE);
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

    private void setProgress() {
        if (mMediaPlayer == null) {
            return;
        }
        int position = mMediaPlayer.getCurrentPosition();
        int duration = mMediaPlayer.getDuration();
        if (mProgressBar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = MAX_PROGRESS * position / duration;
                mProgressBar.setProgress((int) pos);
            }
            //int percent = mMediaPlayer.getBufferPercentage();
            //mProgress.setSecondaryProgress(percent * 10);
        }
    }

    static class VideoHandler extends Handler {
        FragmentVideoPlay thisFragment;

        VideoHandler(FragmentVideoPlay fragment) {
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
