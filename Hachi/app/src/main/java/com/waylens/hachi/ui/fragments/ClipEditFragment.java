package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipExtentGetRequest;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlExRequest;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RemoteClip;
import com.waylens.hachi.views.VideoTrimmer;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Richard on 12/7/15.
 */
public class ClipEditFragment extends Fragment implements MediaPlayer.OnPreparedListener,
        SurfaceHolder.Callback, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnErrorListener {
    private static final String TAG = "ClipEditFragment";

    protected static final String REQUEST_TAG = "RETRIEVE_CLIP_DATA";

    private static final int MAX_EXTENSION = 1000 * 30;

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

    protected int mRawDataState = RAW_DATA_STATE_UNKNOWN;

    @Bind(R.id.video_trimmer)
    VideoTrimmer mVideoTrimmer;

    @Bind(R.id.video_cover)
    ImageView videoCover;

    @Bind(R.id.btn_play)
    ImageView mBtnPlay;

    @Bind(R.id.video_surface)
    SurfaceView mSurfaceView;

    @Bind(R.id.progress_loading)
    ProgressBar mProgressLoading;

    @Bind(R.id.control_panel)
    View mControlPanel;

    SurfaceHolder mSurfaceHolder;

    Clip mClip;
    Clip mOriginalClip;

    VdbImageLoader mImageLoader;

    MediaPlayer mMediaPlayer;

    long mSeekToPosition;

    OnActionListener mOnActionListener;
    int mPosition;
    VdbRequestQueue mVdbRequestQueue;
    String mVideoSource;
    int mPausePosition;
    int mVideoWidth;
    int mVideoHeight;
    boolean mSurfaceDestroyed;
    Handler mHandler;
    long mInitPosition;
    PlaybackUrl mPlaybackUrl;
    ClipExtent mClipExtent;

    long mOldClipStartTimeMs;
    long mOldClipEndTimeMs;

    public static ClipEditFragment newInstance(Clip clip, int position, OnActionListener lifecycleListener) {
        Bundle args = new Bundle();

        ClipEditFragment fragment = new ClipEditFragment();
        fragment.mClip = clip;
        fragment.mOnActionListener = lifecycleListener;
        fragment.mPosition = position;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVdbRequestQueue = Snipe.newRequestQueue();
        mImageLoader = new VdbImageLoader(mVdbRequestQueue);
        mHandler = new VideoHandler(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_clip, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ClipPos clipPos = new ClipPos(mClip, mClip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
        mImageLoader.displayVdbImage(clipPos, videoCover);
        mSurfaceView.getHolder().addCallback(this);

    }

    @Override
    public void onStart() {
        super.onStart();
        if (mOnActionListener != null) {
            mOnActionListener.onFragmentStart(mPosition);
        }
        getClipExtent();
    }

    @Override
    public void onStop() {
        super.onStop();
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
        mOnActionListener = null;
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    @OnClick(R.id.video_cover)
    public void stopEditing() {
        if (mOnActionListener != null) {
            mOnActionListener.onStopEditing(mPosition);
        }
    }

    @OnClick(R.id.btn_play)
    public void playVideo() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                pauseVideo();
            } else {
                resumeVideo();
            }
        } else {
            mBtnPlay.setVisibility(View.INVISIBLE);
            mControlPanel.setVisibility(View.INVISIBLE);
            mProgressLoading.setVisibility(View.VISIBLE);
            mRawDataState = RAW_DATA_STATE_ERROR;
            loadPlayURL();
        }
    }

    @OnClick(R.id.video_surface)
    void clickSurface() {
        if (mBtnPlay.getVisibility() != View.VISIBLE) {
            showController(0);
        } else {
            if (mOnActionListener != null) {
                mOnActionListener.onStopEditing(mPosition);
            }
        }
    }

    private void getClipExtent() {
        mVdbRequestQueue.add(new ClipExtentGetRequest(mClip, new VdbResponse.Listener<ClipExtent>() {
            @Override
            public void onResponse(ClipExtent clipExtent) {
                if (clipExtent != null) {
                    mClipExtent = clipExtent;
                    mOriginalClip = new RemoteClip(mClipExtent.originalCid.type,
                            mClipExtent.originalCid.subType,
                            mClipExtent.originalCid.extra,
                            mClip.clipDate,
                            (int) (mClipExtent.maxClipEndTimeMs - mClipExtent.minClipStartTimeMs));
                    initVideoTrimmer();
                }
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e("test", "", error);
            }
        }));
    }

    void initVideoTrimmer() {
        if (mVideoTrimmer == null) {
            return;
        }
        long minValue = mClipExtent.clipStartTimeMs - MAX_EXTENSION;
        if (minValue < mClipExtent.minClipStartTimeMs) {
            minValue = mClipExtent.minClipStartTimeMs;
        }
        long maxValue = mClipExtent.clipEndTimeMs + MAX_EXTENSION;
        if (maxValue > mClipExtent.maxClipEndTimeMs) {
            maxValue = mClipExtent.maxClipEndTimeMs;
        }

        mVideoTrimmer.setBackgroundClip(mImageLoader, mClip, minValue, maxValue, ViewUtils.dp2px(64, getResources()));
        mVideoTrimmer.setInitRangeValues(minValue, maxValue);

        mOldClipStartTimeMs = mClipExtent.clipStartTimeMs;
        mOldClipEndTimeMs = mClipExtent.clipEndTimeMs;

        mVideoTrimmer.setLeftValue(mClipExtent.clipStartTimeMs);
        mVideoTrimmer.setRightValue(mClipExtent.clipEndTimeMs);

        mVideoTrimmer.setOnChangeListener(new VideoTrimmer.OnTrimmerChangeListener() {
            @Override
            public void onStartTrackingTouch(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag) {
                if (mOnActionListener != null) {
                    mOnActionListener.onStartDragging();
                }
                if (isInPlaybackState() && mMediaPlayer.isPlaying()) {
                    pauseVideo();
                }
            }

            @Override
            public void onProgressChanged(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag, long start, long end, long progress) {
                if (videoCover != null) {
                    videoCover.setVisibility(View.VISIBLE);
                    ClipPos clipPos = new ClipPos(mClip.getVdbId(), mClip.realCid, mClip.clipDate, progress, ClipPos.TYPE_POSTER, false);
                    mImageLoader.displayVdbImage(clipPos, videoCover, true, false);
                }
                mSeekToPosition = progress;
            }

            @Override
            public void onStopTrackingTouch(VideoTrimmer trimmer) {
                if (mOnActionListener != null) {
                    mOnActionListener.onStopDragging();
                }
                if (mOldClipStartTimeMs != trimmer.getLeftValue() || mOldClipEndTimeMs != trimmer.getRightValue()) {
                    mOldClipStartTimeMs = trimmer.getLeftValue();
                    mOldClipEndTimeMs = trimmer.getRightValue();
                    loadPlayURL();
                    return;
                }
                if (isInPlaybackState()) {
                    seekTo((int) mSeekToPosition);
                }
            }
        });

    }

    void loadPlayURL() {
        if (mProgressLoading.getVisibility() != View.VISIBLE) {
            mProgressLoading.setVisibility(View.VISIBLE);
        }
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mOldClipStartTimeMs);
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_CLIP_LENGTH_MS, (int) (mOldClipEndTimeMs - mOldClipStartTimeMs));

        ClipPlaybackUrlExRequest request = new ClipPlaybackUrlExRequest(mClip.realCid, parameters, new VdbResponse.Listener<PlaybackUrl>() {
            @Override
            public void onResponse(PlaybackUrl playbackUrl) {
                mPlaybackUrl = playbackUrl;
                setSource(playbackUrl.url);
                mProgressLoading.setVisibility(View.INVISIBLE);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                mProgressLoading.setVisibility(View.INVISIBLE);
                Log.e("test", "", error);
            }
        });

        mVdbRequestQueue.add(request.setTag(REQUEST_TAG));
    }

    protected void setSource(String source) {
        mVideoSource = source;
        mTargetState = STATE_PLAYING;
        mPausePosition = 0;
        openVideo();
    }

    protected void openVideo() {
        if (mVideoSource == null
                || mSurfaceView == null
                || mSurfaceHolder == null
                || mRawDataState == RAW_DATA_STATE_UNKNOWN) {
            return;
        }
        mProgressLoading.setVisibility(View.VISIBLE);
        release(false);
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setOnErrorListener(this);
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
        mProgressLoading.setVisibility(View.INVISIBLE);
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
        if (mSurfaceHolder != null && mVideoWidth != 0 && mVideoHeight != 0) {
            mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
        }
        int duration = mMediaPlayer.getDuration();
        int position = mMediaPlayer.getCurrentPosition();
        Log.e("test", "init pos: " + position + "; duration: " + duration);
        int seekToPosition = (int) mSeekToPosition;
        if (mSeekToPosition != 0) {
            seekTo(seekToPosition);
        }
        if (mTargetState == STATE_PLAYING) {
            start();
        }
    }

    private void start() {
        if (isInPlaybackState()) {
            videoCover.setVisibility(View.INVISIBLE);
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mBtnPlay.setImageResource(R.drawable.ic_pause_circle_outline_white_48dp);
        mTargetState = STATE_PLAYING;
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        showController(DEFAULT_TIMEOUT);
    }

    private void resumeVideo() {
        start();
        mCurrentState = STATE_PLAYING;
        mTargetState = STATE_PLAYING;
        mBtnPlay.setImageResource(R.drawable.ic_pause_circle_outline_white_48dp);
        fadeOutControllers(DEFAULT_TIMEOUT);
    }

    public void seekTo(int msec) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(msec - (int) mPlaybackUrl.realTimeMs);
            mSeekToPosition = 0;
        } else {
            mSeekToPosition = msec;
        }
    }

    private void pauseVideo() {
        mMediaPlayer.pause();
        mPausePosition = mMediaPlayer.getCurrentPosition();
        mCurrentState = STATE_PAUSED;
        mTargetState = STATE_PAUSED;
        mHandler.removeMessages(SHOW_PROGRESS);
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
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

    void showController(int timeout) {
        mBtnPlay.setVisibility(View.VISIBLE);
        if (timeout > 0) {
            fadeOutControllers(timeout);
        }
    }

    void fadeOutControllers(int timeout) {
        mHandler.removeMessages(FADE_OUT);
        mHandler.sendEmptyMessageDelayed(FADE_OUT, timeout);
    }

    void hideControllers() {
        mControlPanel.setVisibility(View.INVISIBLE);
        mBtnPlay.setVisibility(View.INVISIBLE);
        if (getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                && mCurrentState == STATE_PLAYING) {
            hideSystemUI();
        }
    }

    void hideSystemUI() {
        int newUiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
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
    public void onCompletion(MediaPlayer mp) {
        mCurrentState = STATE_PLAYBACK_COMPLETED;
        mTargetState = STATE_PLAYBACK_COMPLETED;
        mBtnPlay.setImageResource(R.drawable.ic_refresh_white_48dp);
        showController(0);
        mHandler.removeMessages(SHOW_PROGRESS);
        onPlayCompletion();
        release(true);
    }

    protected void displayOverlay(int position) {

    }

    protected void onPlayCompletion() {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == MediaPlayer.MEDIA_ERROR_IO) {
            Snackbar.make(getView(), "Cannot load video.", Snackbar.LENGTH_SHORT).show();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        //TODO
                    } catch (Exception e) {
                        Log.e("test", "", e);
                    }
                }
            }, 1000);
            return true;
        }
        return false;
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
        if (mPlaybackUrl.realTimeMs != 0
                && mInitPosition == 0
                && position != 0
                && Math.abs(mPlaybackUrl.realTimeMs - position) < 200) {
            mInitPosition = mPlaybackUrl.realTimeMs;
            Log.e("test", "setProgress - deviation: " + Math.abs(mPlaybackUrl.realTimeMs - position));
        }
        //Log.e("test", "setProgress - duration: " + duration + "; position: " + position + "; real: "
        //        + mPlaybackUrl.realTimeMs + "; duration2: " + mPlaybackUrl.lengthMs);

        if (mVideoTrimmer != null) {
            if (duration > 0) {
                //long pos = position - mInitPosition + mInitPosition;
                //Log.e("test", "setProgress - position: " + position + "; real: "
                //        + mPlaybackUrl.realTimeMs + "; duration2: " + mPlaybackUrl.lengthMs);
                if (mInitPosition == 0) {
                    mVideoTrimmer.setProgress(position + mPlaybackUrl.realTimeMs);
                }
                mVideoTrimmer.setProgress(position);
            }
            //int percent = mMediaPlayer.getBufferPercentage();
            //mProgress.setSecondaryProgress(percent * 10);
        }

        displayOverlay(position);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        start();
    }

    public interface OnActionListener {
        void onFragmentStart(int listPosition);

        void onStopEditing(int position);

        void onStartDragging();

        void onStopDragging();
    }

    static class VideoHandler extends Handler {
        ClipEditFragment thisFragment;

        VideoHandler(ClipEditFragment fragment) {
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
