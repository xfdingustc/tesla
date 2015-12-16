package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.annotations.SpriteFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;
import com.orhanobut.logger.Logger;
import com.transee.common.GPSRawData;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipExtentGetRequest;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlExRequest;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.ui.views.OnViewDragListener;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;
import com.waylens.hachi.vdb.RemoteClip;
import com.waylens.hachi.views.DragLayout;
import com.waylens.hachi.views.GaugeView;
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

    protected static final int RAW_DATA_STATE_UNKNOWN = 0;
    protected static final int RAW_DATA_STATE_READY = 1;
    protected static final int RAW_DATA_STATE_ERROR = 2;

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

    @Bind(R.id.drag_container)
    DragLayout mDragLayout;

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

    SparseArray<RawDataBlock> mTypedRawData = new SparseArray<>();
    SparseIntArray mTypedState = new SparseIntArray();
    SparseIntArray mTypedPosition = new SparseIntArray();
    GaugeView mObdView;
    MapView mMapView;

    private MarkerOptions mMarkerOptions;
    private PolylineOptions mPolylineOptions;
    private boolean mIsReplay;

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
        mControlPanel.setVisibility(View.INVISIBLE);
        mDragLayout.setOnViewDragListener(new OnViewDragListener() {
            @Override
            public void onStartDragging() {
                if (mOnActionListener != null) {
                    mOnActionListener.onStartDragging();
                }
            }

            @Override
            public void onStopDragging() {
                if (mOnActionListener != null) {
                    mOnActionListener.onStopDragging();
                }
            }
        });
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    release(true);
                }
            }).start();

        }
        mVideoSource = null;
        mTypedRawData.clear();
    }

    @Override
    public void onDestroyView() {
        mOnActionListener = null;
        mVdbRequestQueue.cancelAll(REQUEST_TAG);
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
        if (isInPlaybackState() && !mMediaPlayer.isPlaying()) {
            resumeVideo();
        } else {
            loadClipInfo();
        }
    }

    boolean isRawDataReady() {
        return mTypedState.get(RawDataBlock.RAW_DATA_ODB) == RAW_DATA_STATE_READY
                && mTypedState.get(RawDataBlock.RAW_DATA_ACC) == RAW_DATA_STATE_READY
                && mTypedState.get(RawDataBlock.RAW_DATA_GPS) == RAW_DATA_STATE_READY;
    }

    @OnClick(R.id.video_surface)
    void clickSurface() {
        if (isInPlaybackState() && mMediaPlayer.isPlaying()) {
            pauseVideo();
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
                mTypedPosition.clear();
            }

            @Override
            public void onStopTrackingTouch(VideoTrimmer trimmer) {
                if (mOnActionListener != null) {
                    mOnActionListener.onStopDragging();
                }
                if (mOldClipStartTimeMs != trimmer.getLeftValue() || mOldClipEndTimeMs != trimmer.getRightValue()) {
                    mOldClipStartTimeMs = trimmer.getLeftValue();
                    mOldClipEndTimeMs = trimmer.getRightValue();
                    loadClipInfo();
                    return;
                }
                if (isInPlaybackState()) {
                    seekTo((int) mSeekToPosition);
                } else {
                    mBtnPlay.setImageResource(R.drawable.ic_play_circle_outline_white_48dp);
                    mBtnPlay.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    void loadClipInfo() {
        mProgressLoading.setVisibility(View.VISIBLE);
        hideControllers();
        if (!isRawDataReady()) {
            loadRawData();
        } else {
            loadPlayURL();
        }
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
        mTargetState = STATE_PLAYING;
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        hideControllers();
    }

    private void resumeVideo() {
        start();
        mCurrentState = STATE_PLAYING;
        mTargetState = STATE_PLAYING;
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
        showController(0);
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
        //TODO mControlPanel.setVisibility(View.VISIBLE);
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
        showController(0);
        mHandler.removeMessages(SHOW_PROGRESS);
        onPlayCompletion();
        release(true);
    }

    protected void displayOverlay(int position) {
        if (mObdView == null) {
            return;
        }

        RawDataItem obd = getRawData(RawDataBlock.RAW_DATA_ODB, position);
        if (obd != null && obd.object != null) {
            mObdView.setSpeed(((OBDData) obd.object).speed);
            mObdView.setTargetValue(((OBDData) obd.object).rpm / 1000.0f);
        } else {
            Logger.t(TAG).e("Position: " + position + "; mOBDPosition: " + mTypedPosition
                    .get(RawDataBlock.RAW_DATA_ODB));
        }

        RawDataItem gps = getRawData(RawDataBlock.RAW_DATA_GPS, position);
        if (gps != null) {
            GPSRawData gpsRawData = (GPSRawData) gps.object;
            mMarkerOptions.getMarker().remove();
            LatLng point = new LatLng(gpsRawData.coord.lat_orig, gpsRawData.coord.lng_orig);
            mMarkerOptions.position(point);
            mMapView.addMarker(mMarkerOptions);
            mMapView.setCenterCoordinate(point);
            mMapView.setDirection(-gpsRawData.track);
        }
    }

    RawDataItem getRawData(int dataType, int position) {
        RawDataBlock raw = mTypedRawData.get(dataType);
        int pos = mTypedPosition.get(dataType);
        RawDataItem rawDataItem = null;
        while (pos < raw.dataSize.length) {
            RawDataItem tmp = raw.getRawDataItem(pos);
            long timeOffsetMs = raw.timeOffsetMs[pos] + raw.header.mRequestedTimeMs;
            if (timeOffsetMs == position) {
                rawDataItem = tmp;
                mTypedPosition.put(dataType, pos);
                break;
            } else if (timeOffsetMs < position) {
                rawDataItem = tmp;
                mTypedPosition.put(dataType, pos);
                pos++;
            } else if (timeOffsetMs > position) {
                break;
            }
        }
        return rawDataItem;
    }

    protected void onPlayCompletion() {
        mTypedPosition.clear();
        mIsReplay = true;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e("test", "MediaPlayer Error: " + what + "; extra: " + extra);
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
                //Log.e("test", "setProgress - position: " + position + "; real: "
                //        + mPlaybackUrl.realTimeMs + "; duration2: " + mPlaybackUrl.lengthMs);
                if (mInitPosition == 0) {
                    position = position + (int) mPlaybackUrl.realTimeMs;
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

    void loadRawData() {
        mProgressLoading.setVisibility(View.VISIBLE);
        mTypedRawData.clear();
        mTypedState.clear();
        mTypedPosition.clear();

        if (mTypedState.get(RawDataBlock.RAW_DATA_ODB) != RAW_DATA_STATE_READY) {
            loadRawData(RawDataBlock.RAW_DATA_ODB);
        }

        if (mTypedState.get(RawDataBlock.RAW_DATA_ACC) != RAW_DATA_STATE_READY) {
            loadRawData(RawDataBlock.RAW_DATA_ACC);
        }
        if (mTypedState.get(RawDataBlock.RAW_DATA_GPS) != RAW_DATA_STATE_READY) {
            loadRawData(RawDataBlock.RAW_DATA_GPS);
        }
    }

    void loadRawData(final int dataType) {
        if (mClip == null || mVdbRequestQueue == null) {
            mRawDataState = RAW_DATA_STATE_ERROR;
            return;
        }

        Logger.t(TAG).d("DataType[1]: " + dataType);

        Bundle params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, dataType);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, (long) mVideoTrimmer.getMinValue());
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, (int) mVideoTrimmer.getMaxValue());

        RawDataBlockRequest obdRequest = new RawDataBlockRequest(mClip.cid, params,
                new VdbResponse.Listener<RawDataBlock>() {
                    @Override
                    public void onResponse(RawDataBlock response) {
                        Logger.t(TAG).d("resoponse datatype: " + dataType);
                        mTypedRawData.put(dataType, response);
                        mTypedState.put(dataType, RAW_DATA_STATE_READY);
                        onLoadRawDataFinished();
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        mTypedState.put(dataType, RAW_DATA_STATE_ERROR);
                        onLoadRawDataFinished();
                        Logger.t(TAG).d("error response:");
                    }
                });
        mVdbRequestQueue.add(obdRequest.setTag(REQUEST_TAG));
    }

    void onLoadRawDataFinished() {
        if (mTypedState.get(RawDataBlock.RAW_DATA_ODB) == RAW_DATA_STATE_UNKNOWN
                || mTypedState.get(RawDataBlock.RAW_DATA_ACC) == RAW_DATA_STATE_UNKNOWN
                || mTypedState.get(RawDataBlock.RAW_DATA_GPS) == RAW_DATA_STATE_UNKNOWN) {
            return;
        }
        mRawDataState = RAW_DATA_STATE_READY;
        loadPlayURL();

        if (mTypedRawData.get(RawDataBlock.RAW_DATA_ODB) != null && mObdView == null) {
            mObdView = new GaugeView(getActivity());
            int defaultSize = ViewUtils.dp2px(64, getResources());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(defaultSize, defaultSize);
            params.gravity = Gravity.BOTTOM | Gravity.END;
            mDragLayout.addView(mObdView, params);
        }

        if (mTypedRawData.get(RawDataBlock.RAW_DATA_GPS) != null && mMapView == null) {
            initMapView();
        }
    }

    private void initMapView() {
        mMapView = new MapView(getActivity(), Constants.MAP_BOX_ACCESS_TOKEN);
        mMapView.setStyleUrl(Style.DARK);
        mMapView.setZoomLevel(14);
        mMapView.setLogoVisibility(View.GONE);
        mMapView.setCompassEnabled(false);
        mMapView.onCreate(null);
        GPSRawData firstGPS = (GPSRawData) mTypedRawData.get(RawDataBlock.RAW_DATA_GPS).getRawDataItem(0).object;
        SpriteFactory spriteFactory = new SpriteFactory(mMapView);
        LatLng firstPoint = new LatLng(firstGPS.coord.lat_orig, firstGPS.coord.lng_orig);
        mMarkerOptions = new MarkerOptions().position(firstPoint)
                .icon(spriteFactory.fromResource(R.drawable.map_car_inner_red_triangle));
        mMapView.addMarker(mMarkerOptions);
        mPolylineOptions = new PolylineOptions().color(Color.rgb(252, 219, 12)).width(3).add(firstPoint);
        mMapView.setCenterCoordinate(firstPoint);
        mMapView.setDirection(firstGPS.track);
        int defaultSize = ViewUtils.dp2px(96, getResources());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(defaultSize, defaultSize);
        mDragLayout.addView(mMapView, params);
        buildFullPath();
    }

    void buildFullPath() {
        RawDataBlock raw = mTypedRawData.get(RawDataBlock.RAW_DATA_GPS);
        for (int i = 0; i < raw.dataSize.length; i++) {
            RawDataItem item = raw.getRawDataItem(i);
            GPSRawData gpsRawData = (GPSRawData) item.object;
            LatLng point = new LatLng(gpsRawData.coord.lat_orig, gpsRawData.coord.lng_orig);
            mPolylineOptions.add(point);
        }
        mMapView.addPolyline(mPolylineOptions);
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
