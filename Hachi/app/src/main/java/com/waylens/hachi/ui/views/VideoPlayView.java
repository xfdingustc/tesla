package com.waylens.hachi.ui.views;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.waylens.hachi.R;
import com.xfdingustc.far.FixedAspectRatioFrameLayout;

/**
 * VideoPlayView can be used to play videos.
 * <p>
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
    Handler mUIHandler;

    View mBtnPlay;
    View mLoadingIcon;
    public ImageView videoCover;

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
        mUIHandler = new Handler(Looper.getMainLooper());
        mNonUIThread = new HandlerThread(TAG);
        mNonUIThread.start();
        mNonUIHandler = new Handler(mNonUIThread.getLooper());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    public void setSource(String source) {
        setSource(source, STATE_IDLE);
    }

    public void setSource(String source, int targetState) {
        mVideoPath = source;
        mSeekWhenPrepared = 0;
        switch (targetState) {
            case STATE_IDLE:
                showController(0);
                mTargetState = STATE_IDLE;
                mCurrentState = STATE_IDLE;
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
                pause();
                break;
        }
    }

    protected void onClickPlayButton() {
        playVideo();
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

    void release(boolean clearTargetState) {
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
    }

    void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
        showController(0);
    }

    void seekTo(int msec) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    boolean isInPlaybackState() {
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
                showController(0);
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
                    showController(0);
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
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                mBtnPlay.setVisibility(VISIBLE);
            }
        });
    }

    int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    //MediaPlayer.OnCompletionListener
    @Override
    public void onCompletion(MediaPlayer mp) {
        release(true);
        showController(0);
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

    }


}
