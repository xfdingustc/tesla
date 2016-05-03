package com.waylens.hachi.ui.fragments;

import android.annotation.SuppressLint;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.views.DragLayout;
import com.waylens.hachi.ui.views.OnViewDragListener;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;
import com.xfdingustc.far.FixedAspectRatioFrameLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MomentPlayFragment extends Fragment implements View.OnClickListener, SurfaceHolder.Callback {
    private static final String TAG = MomentPlayFragment.class.getSimpleName();
    private static final String REQUEST_TAG = "RETRIEVE_RAW_DATA";

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

    protected boolean mOverlayShouldDisplay = true;

    public static MomentPlayFragment fullScreenPlayer;

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

    long mMomentID = Moment.INVALID_MOMENT_ID;
    RequestQueue mRequestQueue;
    JSONArray mRawDataUrls;

    private FrameLayout.LayoutParams mPortraitInfoPanelParems;

    @BindView(R.id.video_root_container)
    FixedAspectRatioFrameLayout mRootContainer;

    @BindView(R.id.waylens_video_container)
    DragLayout mVideoContainer;

    @BindView(R.id.video_controllers)
    FrameLayout mVideoController;

    @BindView(R.id.btn_play)
    ImageView mBtnPlay;

    @BindView(R.id.video_surface)
    SurfaceView mSurfaceView;

    @BindView(R.id.progress_loading)
    ProgressBar mProgressLoading;

    @BindView(R.id.progress_bar)
    ProgressBar mProgressBar;

    @BindView(R.id.btn_fullscreen)
    ImageView mBtnFullScreen;

    @BindView(R.id.text_video_time)
    TextView mVideoTime;

    @BindView(R.id.infoPanel)
    LinearLayout mInfoPanel;

    @OnClick(R.id.btn_play)
    public void onBtnPlayClicked() {
        playVideo();
    }

    public static MomentPlayFragment newInstance(Moment moment, OnViewDragListener listener) {
        Bundle args = new Bundle();
        MomentPlayFragment fragment = new MomentPlayFragment();
        fragment.setArguments(args);
        fragment.mMomentID = moment.id;
        fragment.setSource(moment.videoURL);

        fragment.mDragListener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
        mRequestQueue.start();
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
//        mVideoContainer.setOnClickListener(this);

        mSurfaceView.getHolder().addCallback(this);
//        mBtnFullScreen.setOnClickListener(this);
        mProgressBar.setMax((int) MAX_PROGRESS);
        mVideoController.setVisibility(View.INVISIBLE);
        if (mDragListener != null) {
            mVideoContainer.setOnViewDragListener(mDragListener);
        }


    }

    @Override
    public void onStart() {
        super.onStart();
        if (mRawDataState != RAW_DATA_STATE_READY) {
            readRawURL();
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
        mNonUIThread.quitSafely();
    }

    @Override
    public void onDestroyView() {
        mRequestQueue.cancelAll(REQUEST_TAG);
        super.onDestroyView();
        if (fullScreenPlayer == this) {
            fullScreenPlayer = null;
        }
        mDragListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRawDataState = RAW_DATA_STATE_UNKNOWN;
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


    public void setFullScreen(boolean fullScreen) {
        int orientation = getActivity().getRequestedOrientation();

        if (fullScreen || orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            || orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            hideSystemUI();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//            mPortraitParams = (FrameLayout.LayoutParams) mDashboardLayout.getLayoutParams();
            mPortraitInfoPanelParems = (FrameLayout.LayoutParams) mInfoPanel.getLayoutParams();

            mRootContainer.removeView(mVideoContainer);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mRootView.addView(mVideoContainer, params);
            mBtnFullScreen.setImageResource(R.drawable.screen_narrow);


            mRootContainer.removeView(mInfoPanel);
            mRootView.addView(mInfoPanel, mPortraitInfoPanelParems);

            fullScreenPlayer = this;
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mRootView.removeView(mVideoContainer);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mRootContainer.addView(mVideoContainer, params);
            mBtnFullScreen.setImageResource(R.drawable.screen_full);
            fullScreenPlayer = null;


            mRootView.removeView(mInfoPanel);
            mRootContainer.addView(mInfoPanel, mPortraitInfoPanelParems);
        }
        mIsFullScreen = fullScreen;
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
        mBtnPlay.setImageResource(R.drawable.playbar_pause);
        fadeOutControllers(DEFAULT_TIMEOUT);
    }

    void pauseVideo() {
        mMediaPlayer.pause();
        mPausePosition = mMediaPlayer.getCurrentPosition();
        mCurrentState = STATE_PAUSED;
        mTargetState = STATE_PAUSED;
        mBtnPlay.setImageResource(R.drawable.playbar_play);
        mHandler.removeMessages(SHOW_PROGRESS);
    }

    void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            mProgressLoading.setVisibility(View.GONE);
        }
        mTargetState = STATE_PLAYING;
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
    }


    boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
            mCurrentState != STATE_ERROR &&
            mCurrentState != STATE_IDLE &&
            mCurrentState != STATE_PREPARING);
    }

    protected void openVideo() {
        if (mVideoSource == null || mSurfaceView == null || mSurfaceHolder == null) {
            return;
        }

        if (mOverlayShouldDisplay && mRawDataState == RAW_DATA_STATE_UNKNOWN) {
            return;
        }

        mProgressLoading.setVisibility(View.VISIBLE);
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
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
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    mTargetState = STATE_PLAYBACK_COMPLETED;
                    mBtnPlay.setImageResource(R.drawable.playbar_play);
                    showController(0);
                    int duration = mp.getDuration();
                    setProgress(duration, duration);
                    mHandler.removeMessages(SHOW_PROGRESS);
//        onPlayCompletion();
                    release(true);
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == MediaPlayer.MEDIA_ERROR_IO) {
                        Snackbar.make(getView(), "Cannot load video.", Snackbar.LENGTH_SHORT).show();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    getFragmentManager().beginTransaction().remove(MomentPlayFragment.this).commit();
                                } catch (Exception e) {
                                    Logger.t(TAG).d("", e);
                                }
                            }
                        }, 1000);
                        return true;
                    }
                    return false;
                }
            });

            if (mHeaders != null) {
                mMediaPlayer.setDataSource(getActivity(), Uri.parse(mVideoSource), mHeaders);
            } else {
                mMediaPlayer.setDataSource(mVideoSource);
            }
            mMediaPlayer.setDisplay(mSurfaceHolder);
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
            mCurrentState = STATE_PREPARING;
        } catch (IOException e) {
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            Log.e(TAG, "", e);
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
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("test", "surfaceDestroyed");
        mSurfaceHolder = null;
        mSurfaceDestroyed = true;
        if (isInPlaybackState() && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPausePosition = mMediaPlayer.getCurrentPosition();
        }
    }

    void release(final boolean clearTargetState) {
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


    protected void setProgress(int position, int duration) {
        if (mProgressBar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = MAX_PROGRESS * position / duration;
                mProgressBar.setProgress((int) pos);
            }
        }
        updateVideoTime(position, duration);
//        displayOverlay(position);
        if (mProgressListener != null) {
            mProgressListener.onProgress(position, duration);
        }
    }

    void toggleController() {
        if (mProgressLoading.getVisibility() == View.VISIBLE) {
            return;
        }
        int visibitliy = mVideoController.getVisibility() == View.VISIBLE ? View.INVISIBLE : View
            .VISIBLE;
        mVideoController.setVisibility(visibitliy);
        mInfoPanel.setVisibility(visibitliy);
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

    @SuppressLint("SetTextI18n")
    private void updateVideoTime(int position, int duration) {
        mVideoTime.setText(DateUtils.formatElapsedTime(position / 1000) + " / " + DateUtils.formatElapsedTime(duration / 1000));
    }


    void onLoadRawDataSuccessfully() {
        mRawDataState = RAW_DATA_STATE_READY;
        mProgressLoading.setVisibility(View.GONE);
//        mDashboardLayout.setAdapter(mRawDataAdapter);
        openVideo();
    }


    void onLoadRawDataError(String msg) {
        Log.e("test", "msg: " + msg);
        mRawDataState = RAW_DATA_STATE_ERROR;
        mProgressLoading.setVisibility(View.GONE);
        openVideo();
    }

    void readRawURL() {
        if (mMomentID == Moment.INVALID_MOMENT_ID) {
            return;
        }
        String url = Constants.API_MOMENT_PLAY + mMomentID;
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                mRawDataUrls = response.optJSONArray("rawDataUrl");
                loadRawData(0);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                onLoadRawDataError("ErrorCode: " + errorInfo.errorCode);
            }
        }).setTag(REQUEST_TAG));
        mProgressLoading.setVisibility(View.VISIBLE);
    }

    void loadRawData(final int index) {
        if (mRawDataUrls == null || index >= mRawDataUrls.length()) {
            onLoadRawDataSuccessfully();
            return;
        }

        try {
            JSONObject jsonObject = mRawDataUrls.getJSONObject(index);
            String url = jsonObject.getString("url");
            mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (parseRawData(response)) {
                            int nextIndex = index + 1;
                            if (nextIndex < mRawDataUrls.length()) {
                                loadRawData(nextIndex);
                            } else {
                                onLoadRawDataSuccessfully();
                            }
                        } else {
                            onLoadRawDataError("Load Raw data error");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                        onLoadRawDataError("ErrorCode: " + errorInfo.errorCode);
                    }
                }).setTag(REQUEST_TAG));
        } catch (JSONException e) {
            Log.e("test", "", e);
        }
    }

    boolean parseRawData(JSONObject response) {
        try {
            JSONObject obd = response.optJSONObject("obd");
            if (obd != null) {
                JSONArray captureTime = obd.getJSONArray("captureTime");
                JSONArray speed = obd.getJSONArray("speed");
                JSONArray rpm = obd.getJSONArray("rpm");
                JSONArray temperature = obd.getJSONArray("temperature");
                JSONArray tp = obd.getJSONArray("tp");
                JSONArray imp = obd.getJSONArray("imp");
                JSONArray bp = obd.getJSONArray("bp");
                JSONArray bhp = obd.getJSONArray("bhp");
                for (int i = 0; i < captureTime.length(); i++) {
//                    mRawDataAdapter.addObdData(
//                        captureTime.getLong(i),
//                        speed.getInt(i),
//                        rpm.getInt(i),
//                        temperature.getInt(i),
//                        tp.getInt(i),
//                        imp.getInt(i),
//                        bp.getInt(i),
//                        bhp.getInt(i));

                }
            }
            JSONObject acc = response.optJSONObject("acc");
            if (acc != null) {
                JSONArray captureTime = acc.getJSONArray("captureTime");
                JSONArray acceleration = acc.getJSONArray("acceleration");
                for (int i = 0; i < captureTime.length(); i++) {
                    JSONObject accObj = acceleration.getJSONObject(i);
//                    mRawDataAdapter.addAccData(
//                        captureTime.getLong(i),
//                        accObj.getInt("accelX"), accObj.getInt("accelY"), accObj.getInt("accelZ"),
//                        accObj.getInt("gyroX"), accObj.getInt("gyroY"), accObj.getInt("gyroZ"),
//                        accObj.getInt("magnX"), accObj.getInt("magnY"), accObj.getInt("magnZ"),
//                        accObj.getInt("eulerHeading"), accObj.getInt("eulerRoll"), accObj.getInt("eulerPitch"),
//                        accObj.getInt("quaternionW"), accObj.getInt("quaternionX"), accObj.getInt("quaternionY"), accObj.getInt("quaternionZ"),
//                        accObj.getInt("pressure")
//                    );
                }
            }
            JSONObject gps = response.optJSONObject("gps");
            if (gps != null) {
                JSONArray captureTime = gps.getJSONArray("captureTime");
                JSONArray coordinates = gps.getJSONObject("coordinate").getJSONArray("coordinates");

                for (int i = 0; i < captureTime.length(); i++) {
                    JSONArray coordinateObj = coordinates.getJSONArray(i);
//                    mRawDataAdapter.addGpsData(
//                        captureTime.getLong(i),
//                        coordinateObj.getDouble(0),
//                        coordinateObj.getDouble(1),
//                        coordinateObj.getDouble(2)
//                    );
                }
            }

            return true;
        } catch (JSONException e) {
            Log.e("test", "", e);
            return false;
        }
    }

    public void setOnProgressListener(OnProgressListener listener) {
        mProgressListener = listener;
    }


    static class VideoHandler extends Handler {
        MomentPlayFragment thisFragment;

        VideoHandler(MomentPlayFragment fragment) {
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

//    private static class MomentRawDataAdapter extends RawDataAdapter {
//        private static final String TAG = MomentRawDataAdapter.class.getSimpleName();
//        List<RawDataItem> mOBDData = new ArrayList<>();
//        List<RawDataItem> mAccData = new ArrayList<>();
//        List<RawDataItem> mGPSData = new ArrayList<>();
//
//        private int mObdDataIndex = 0;
//        private int mAccDataIndex = 0;
//        private int mGpsDataIndex = 0;
//
//        public void reset() {
//            mObdDataIndex = 0;
//            mAccDataIndex = 0;
//            mGpsDataIndex = 0;
//        }
//
//
//        public void addObdData(long captureTime, int speed, int rpm, int temperature, int tp, int imp, int bp, int bhp) {
//            RawDataItem item = new RawDataItem(RawDataItem.DATA_TYPE_OBD, captureTime);
//
//            RawDataItem.OBDData data = new RawDataItem.OBDData(speed, temperature, rpm);
//            item.data = data;
//            mOBDData.add(item);
//        }
//
//        public void addAccData(long captureTime, int accX, int accY, int accZ,
//                               int gyroX, int gyroY, int gyroZ,
//                               int magnX, int magnY, int magnZ,
//                               int eulerHeading, int eulerRoll, int eulerPitch,
//                               int quaternionW, int quaternionX, int quaternionY, int quaternionZ,
//                               int pressure) {
//            RawDataItem item = new RawDataItem(RawDataItem.DATA_TYPE_IIO, captureTime);
//            RawDataItem.AccData data = new RawDataItem.AccData();
//
//            data.accX = accX;
//            data.accY = accY;
//            data.accZ = accZ;
//            data.euler_roll = eulerRoll;
//            data.euler_pitch = eulerPitch;
//
//            item.data = data;
//            mAccData.add(item);
//        }
//
//        public void addGpsData(long captureTime, double longitude, double latitude, double altitude) {
//            RawDataItem item = new RawDataItem(RawDataItem.DATA_TYPE_GPS, captureTime);
//            RawDataItem.GpsData data = new RawDataItem.GpsData();
//            data.coord.lat = latitude;
//            data.coord.lat_orig = latitude;
//            data.coord.lng = longitude;
//            data.coord.lng_orig = longitude;
//            data.altitude = altitude;
//            item.data = data;
//
//            mGPSData.add(item);
//        }
//
//        public void updateCurrentTime(int currentTime) {
//            if (checkIfUpdated(mAccData, mAccDataIndex, currentTime)) {
//                mAccDataIndex++;
//            }
//            if (checkIfUpdated(mGPSData, mGpsDataIndex, currentTime)) {
//                mGpsDataIndex++;
//            }
//            if (checkIfUpdated(mOBDData, mObdDataIndex, currentTime)) {
//                mObdDataIndex++;
//            }
//        }
//
//        private boolean checkIfUpdated(List<RawDataItem> list, int fromPosition, int currentTime) {
//            int index = fromPosition;
//            if (index >= list.size()) {
//                return false;
//            }
//
//            RawDataItem item = list.get(index);
//            if (item.getPtsMs() < currentTime) {
//                fromPosition++;
//                notifyDataSetChanged(item);
//                return true;
//            }
//
//            return false;
//        }
//
//
//    }
}
