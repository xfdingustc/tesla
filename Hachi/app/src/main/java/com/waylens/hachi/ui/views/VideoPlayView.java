package com.waylens.hachi.ui.views;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.views.dashboard.DashboardLayout;
import com.xfdingustc.far.FixedAspectRatioFrameLayout;

/**
 * VideoPlayView can be used to play videos.
 * <p/>
 * Created by Richard on 1/11/16.
 */
public class VideoPlayView extends FixedAspectRatioFrameLayout implements
        View.OnClickListener,
        SurfaceHolder.Callback,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnVideoSizeChangedListener,
        MediaPlayer.OnSeekCompleteListener {
    private static final String TAG = "VideoPlayView";

    private static final int STATE_ERROR = -1;
    public static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    //Raw data states
    protected static final int RAW_DATA_STATE_READY = 0;
    protected static final int RAW_DATA_STATE_UNKNOWN = -1;
    protected static final int RAW_DATA_STATE_ERROR = -2;

    protected int mRawDataState = RAW_DATA_STATE_UNKNOWN;

    int mCurrentState = STATE_IDLE;
    int mTargetState = STATE_IDLE;

    String mVideoPath;
    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    MediaPlayer mMediaPlayer;
    int mVideoWidth;
    int mVideoHeight;
    int mSurfaceWidth;
    int mSurfaceHeight;
    int mSeekWhenPrepared;

    HandlerThread mNonUIThread;
    Handler mNonUIHandler;

    View mBtnPlay;
    View mLoadingIcon;
    public ImageView videoCover;

    protected DashboardLayout mOverlayLayout;

    OnProgressListener mOnProgressListener;
    protected VideoHandler mUIHandler;

    public VideoPlayView(Context context) {
        this(context, null, 0);
    }

    public VideoPlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews(context);
    }

    private void initViews(Context context) {
        setClipChildren(false);
        View.inflate(context, R.layout.video_play_view, this);
        mVideoWidth = 0;
        mVideoHeight = 0;
        mSurfaceView = (SurfaceView) findViewById(R.id.video_surface);
        mSurfaceView.getHolder().addCallback(this);
        mSurfaceView.setOnClickListener(this);
        mBtnPlay = findViewById(R.id.btn_play);
        mBtnPlay.setOnClickListener(this);
        mLoadingIcon = findViewById(R.id.progress_loading);
        videoCover = (ImageView) findViewById(R.id.video_cover);
        mOverlayLayout = (DashboardLayout) findViewById(R.id.overlayLayout);
        mUIHandler = new VideoHandler(this);
        mNonUIThread = new HandlerThread(TAG);
        mNonUIThread.start();
        mNonUIHandler = new Handler(mNonUIThread.getLooper());
    }

    public void setSource(String source) {
        setSource(source, STATE_IDLE);
    }

    public void setSource(String source, int targetState) {
        mVideoPath = source;
        switch (targetState) {
            case STATE_IDLE:
                mTargetState = STATE_IDLE;
                mCurrentState = STATE_IDLE;
                mUIHandler.sendEmptyMessage(SHOW_CONTROLLERS);
                break;
            case STATE_PREPARED:
            case STATE_PLAYING:
                mTargetState = targetState;
                openVideo();
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:
                onClickPlayButton();
                break;
            case R.id.video_surface:
                onClickSurface();
                break;
        }
    }

    protected void onClickPlayButton() {
        playVideo();
    }

    void onClickSurface() {
        if (isPlaying()) {
            pause();
        } else {
            cleanup();
            callOnClick();
        }
    }

    protected void cleanup() {
        mBtnPlay.setVisibility(VISIBLE);
        release(true);
    }

    void playVideo() {
        mBtnPlay.setVisibility(INVISIBLE);
        videoCover.setVisibility(INVISIBLE);
        if (mCurrentState == STATE_IDLE) {
            mTargetState = STATE_PLAYING;
            openVideo();
        } else if (mCurrentState == STATE_PREPARED
                || mCurrentState == STATE_PAUSED) {
            start();
        }
    }

    void openVideo() {
        if (mVideoPath == null || mSurfaceHolder == null || mTargetState == STATE_IDLE) {
            return;
        }
        mLoadingIcon.setVisibility(VISIBLE);
        release(false);

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnInfoListener(this);
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setDataSource(mVideoPath);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
        } catch (Exception ex) {
            Log.w(TAG, "Unable to open content: " + mVideoPath, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    public void release(boolean clearTargetState) {
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

    void start() {
        if (isInPlaybackState()) {
            mBtnPlay.setVisibility(INVISIBLE);
            videoCover.setVisibility(INVISIBLE);
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
        mUIHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

    public void pause() {
        if (isPlaying()) {
            mMediaPlayer.pause();
            mCurrentState = STATE_PAUSED;
        }
        mTargetState = STATE_PAUSED;
        mUIHandler.sendEmptyMessage(SHOW_CONTROLLERS);
    }

    public void seekTo(int msec) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    public boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    /* ==== SurfaceHolder.Callback ==== */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG, "surfaceCreated");
        mSurfaceHolder = holder;
        if (isInPlaybackState()) {
            mMediaPlayer.setDisplay(mSurfaceHolder);
        } else {
            openVideo();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(TAG, "surfaceChanged");
        mSurfaceWidth = width;
        mSurfaceHeight = height;

        if (mMediaPlayer != null && (mVideoWidth == width && mVideoHeight == height)) {
            Log.e(TAG, "surfaceChanged - 1");
            if (mSeekWhenPrepared != 0) {
                seekTo(mSeekWhenPrepared);
            }
            if (mTargetState == STATE_PLAYING) {
                Log.e(TAG, "surfaceChanged - 3");
                start();
            } else {
                Log.e(TAG, "surfaceChanged - 2");
                mUIHandler.sendEmptyMessage(SHOW_CONTROLLERS);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(TAG, "surfaceDestroyed");
        if (isInPlaybackState()) {
            mMediaPlayer.pause();
            mSurfaceHolder = null;
        }
    }

    /* ==== MediaPlayer.OnErrorListener ==== */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    // MediaPlayer.OnPreparedListener
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.e(TAG, "onPrepared");
        mLoadingIcon.setVisibility(GONE);
        mCurrentState = STATE_PREPARED;
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();

        int seekToPosition = mSeekWhenPrepared;
        if (seekToPosition != 0) {
            seekTo(seekToPosition);
        }
        if (mVideoWidth != 0 && mVideoHeight != 0 && mSurfaceHolder != null) {
            mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
            if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                if (mTargetState == STATE_PLAYING) {
                    start();
                } else if (!isPlaying()) {
                    mUIHandler.sendEmptyMessage(SHOW_CONTROLLERS);
                }
            }
        } else {
            if (mTargetState == STATE_PLAYING) {
                start();
            }
        }
    }

    void showController(int timeOut) {
        if (mBtnPlay == null) {
            return;
        }
        mBtnPlay.setVisibility(VISIBLE);
    }

    int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    //MediaPlayer.OnCompletionListener
    @Override
    public void onCompletion(MediaPlayer mp) {
        int duration = mMediaPlayer.getDuration();
        if (mOnProgressListener != null) {
            mOnProgressListener.onProgress(duration, duration);
        }
        release(true);
        mUIHandler.sendEmptyMessage(SHOW_CONTROLLERS);
    }

    // MediaPlayer.OnInfoListener
    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START == what) {
            Log.e(TAG, "MEDIA_INFO_VIDEO_RENDERING_START - start play");
        }
        if (MediaPlayer.MEDIA_INFO_BUFFERING_START == what) {
            Log.e(TAG, "MEDIA_INFO_BUFFERING_START");
        }
        if (MediaPlayer.MEDIA_INFO_BUFFERING_END == what) {
            Log.e(TAG, "MEDIA_INFO_BUFFERING_END - play");
        }
        return false;
    }

    //MediaPlayer.OnBufferingUpdateListener
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.e(TAG, "onBufferingUpdate - percent: " + percent);
    }

    //MediaPlayer.OnVideoSizeChangedListener
    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
        if (mVideoWidth != 0 && mVideoHeight != 0 && mSurfaceHolder != null) {
            mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
            requestLayout();
        }
    }

    //MediaPlayer.OnSeekCompleteListener
    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.e(TAG, "onSeekComplete");
    }

    void hideControllers() {
        //
    }

    protected void updateInternalProgress(int position, int duration) {
        //
    }

    void showProgress() {
        if (!isInPlaybackState()) {
            return;
        }
        ;
        int position = mMediaPlayer.getCurrentPosition();
        int duration = mMediaPlayer.getDuration();
        if (mOnProgressListener != null) {
            mOnProgressListener.onProgress(position, duration);
        }
        updateInternalProgress(position, duration);
        if (isPlaying()) {
            mUIHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 20);
        }
    }

    public void setOnProgressListener(OnProgressListener listener) {
        mOnProgressListener = listener;
    }

    static final int FADE_OUT = 1;
    static final int SHOW_PROGRESS = 2;
    static final int SHOW_CONTROLLERS = 3;

    static class VideoHandler extends Handler {
        VideoPlayView mVideoPlayView;

        VideoHandler(VideoPlayView videoPlayView) {
            super();
            mVideoPlayView = videoPlayView;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FADE_OUT:
                    mVideoPlayView.hideControllers();
                    break;
                case SHOW_PROGRESS:
                    mVideoPlayView.showProgress();
                    break;
                case SHOW_CONTROLLERS:
                    mVideoPlayView.showController(0);
            }
        }
    }

    public interface OnProgressListener {
        void onProgress(int position, int duration);
    }
}
