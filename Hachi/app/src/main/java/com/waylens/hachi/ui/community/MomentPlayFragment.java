package com.waylens.hachi.ui.community;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.orhanobut.logger.Logger;
import com.rest.response.MomentInfo;
import com.rest.response.MomentPlayInfo;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.views.GaugeView;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.ToStringUtils;
import com.waylens.hachi.vdb.rawdata.GpsData;
import com.waylens.hachi.vdb.rawdata.IioData;
import com.waylens.hachi.vdb.rawdata.ObdData;
import com.waylens.hachi.vdb.rawdata.RawDataItem;
import com.xfdingustc.far.FixedAspectRatioFrameLayout;
import com.xfdingustc.mdplaypausebutton.PlayPauseButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class MomentPlayFragment extends BaseFragment implements SurfaceHolder.Callback {
    private static final String TAG = MomentPlayFragment.class.getSimpleName();


    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;


    private final int STATE_IDLE = 0;
    private final int STATE_PREPAREING = 1;
    private final int STATE_PREPARED = 2;
    private final int STATE_PLAYING = 3;
    private final int STATE_PAUSE = 4;


    private static final int DEFAULT_TIMEOUT = 3000;
    private static final long MAX_PROGRESS = 1000L;

    protected static final int RAW_DATA_STATE_UNKNOWN = -1;
    protected static final int RAW_DATA_STATE_READY = 0;
    protected static final int RAW_DATA_STATE_ERROR = 1;


    protected int mRawDataState = RAW_DATA_STATE_UNKNOWN;

    protected boolean mOverlayShouldDisplay = true;

    private MediaPlayer mMediaPlayer = new MediaPlayer();

    private SurfaceHolder mSurfaceHolder;

    private int mCurrentState = STATE_IDLE;


    private VideoHandler mHandler;

    private MomentInfo mMoment;

    private MomentPlayInfo mMomentPlayInfo;

    private List<RawDataTimeInfo> mRawDataTimeInfoList = new ArrayList<>();


    boolean mIsFullScreen;


    int mPausePosition;

    private Timer mTimer;
    private UpdatePlayTimeTask mUpdatePlayTimeTask;


    private RequestQueue mRequestQueue;
//    JSONArray mRawDataUrls;


    private MomentRawDataAdapter mRawDataAdapter = new MomentRawDataAdapter();

    @BindView(R.id.video_thumbnail)
    ImageView mVsCover;

    @BindView(R.id.video_root_container)
    FixedAspectRatioFrameLayout mRootContainer;


    @BindView(R.id.video_controllers)
    FrameLayout mVideoController;

    @BindView(R.id.btn_play_pause)
    PlayPauseButton mBtnPlayPause;

    @BindView(R.id.video_surface)
    SurfaceView mSurfaceView;


    @BindView(R.id.progress_loading)
    ProgressBar mProgressLoading;

    @BindView(R.id.video_seek_bar)
    SeekBar mVideoSeekBar;

    @BindView(R.id.btn_fullscreen)
    ImageView mBtnFullScreen;

    @BindView(R.id.text_video_time)
    TextView mVideoTime;

    @BindView(R.id.text_video_duration)
    TextView mVideoDuration;

    @BindView(R.id.infoPanel)
    LinearLayout mInfoPanel;

    @BindView(R.id.gaugeView)
    GaugeView mGaugeView;

    @OnClick(R.id.btn_play_pause)
    public void onBtnPlayClicked() {
        switch (mCurrentState) {
            case STATE_IDLE:
            case STATE_PREPARED:
                start();
                break;
            case STATE_PAUSE:
                changeState(STATE_PLAYING);
                break;
            case STATE_PLAYING:
                showControllers();
                mBtnPlayPause.setAlpha((float) 1);
                changeState(STATE_PAUSE);
                break;
            case STATE_PREPAREING:
                break;
        }
    }

    @OnClick(R.id.video_surface)
    public void onSurfaceClicked() {
//        onBtnPlayClicked();
        showControllers();
    }

    @OnClick(R.id.btn_fullscreen)
    public void onBtnFullScreenClicked() {
        setFullScreen(!mIsFullScreen);
    }

    @OnClick(R.id.btnShowOverlay)
    public void onBtnShowOverlayClicked() {
        int visibility = mGaugeView.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
        mGaugeView.setVisibility(visibility);
    }

    public static MomentPlayFragment newInstance(MomentInfo moment) {
        MomentPlayFragment fragment = new MomentPlayFragment();
        Bundle args = new Bundle();
        args.putSerializable("moment", moment);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mMoment = (MomentInfo) args.getSerializable("moment");

        mRequestQueue = Volley.newRequestQueue(getActivity());
        mRequestQueue.start();

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);

        getActivity().getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    showControllers();
                }
            }
        });
        getMomentPlayInfo();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_moment_play, container, false);
        ButterKnife.bind(this, view);
        mHandler = new VideoHandler(this);
        initViews();
        return view;
    }

    private void initViews() {
        Glide.with(this).load(mMoment.moment.thumbnail).crossFade().into(mVsCover);
        mBtnPlayPause.toggle(true);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceView.getHolder().addCallback(this);
    }


    @Override
    public void onStart() {
        super.onStart();
        mTimer = new Timer();
        mUpdatePlayTimeTask = new UpdatePlayTimeTask();
        mTimer.schedule(mUpdatePlayTimeTask, 0, 100);

    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
        }
        mTimer.cancel();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mRawDataState = RAW_DATA_STATE_UNKNOWN;
    }


    protected void setSource(String source) {

        mPausePosition = 0;
        openVideo();
    }


    public void setFullScreen(boolean fullScreen) {
        int orientation = getActivity().getRequestedOrientation();

        if (fullScreen || orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            || orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
//            hideSystemUI();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        mIsFullScreen = fullScreen;
        ((BaseActivity) getActivity()).setImmersiveMode(mIsFullScreen);
    }


    private void playVideo() {
        openVideo();
        if (mMediaPlayer.isPlaying()) {
            pauseVideo();
        } else {
            resumeVideo();
        }
    }


    private void resumeVideo() {
        start();
        mBtnPlayPause.toggle(false);

    }

    private void pauseVideo() {
        mMediaPlayer.pause();
        mPausePosition = mMediaPlayer.getCurrentPosition();

        mBtnPlayPause.toggle(true);
        mHandler.removeMessages(SHOW_PROGRESS);
    }

    private void start() {
        openVideo();
        changeState(STATE_PREPAREING);
    }


    private void openVideo() {
        if (mMoment.moment.videoUrl == null || mSurfaceView == null || mSurfaceHolder == null) {
            Logger.t(TAG).d("source: " + mMoment.moment.videoUrl + " surface view: " + mSurfaceView + " surface holder: " + mSurfaceHolder);
            return;
        }

        mProgressLoading.setVisibility(View.VISIBLE);
        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
            } else {
                mMediaPlayer.reset();
            }
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Logger.t(TAG).d("prepared finished");
                    changeState(STATE_PREPARED);
                    changeState(STATE_PLAYING);
                    mBtnPlayPause.setVisibility(View.VISIBLE);
                    showControllers();
                }
            });


            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mVideoSeekBar.setProgress(0);
                    setProgress(0, mMediaPlayer.getDuration());
                    changeState(STATE_IDLE);
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


            mMediaPlayer.setDataSource(mMoment.moment.videoUrl);
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

        } catch (IOException e) {
            Logger.t(TAG).e(e.toString());
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mMediaPlayer == null) {
            return;
        }

        mMediaPlayer.setDisplay(holder);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;

    }


    protected void setProgress(int position, int duration) {
        if (mVideoSeekBar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                mVideoSeekBar.setMax(duration);
                mVideoSeekBar.setProgress(position);
            }
        }
        updateVideoTime(position, duration);
//        displayOverlay(position);

    }


    private void showControllers() {
        if (mVideoController == null) {
            return;
        }
        if (mInfoPanel == null) {
            return;
        }
        mInfoPanel.setVisibility(View.VISIBLE);
        mVideoController.setVisibility(View.VISIBLE);
        mHandler.removeMessages(FADE_OUT);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT), 3000);

    }


    private void hideControllers() {
        mVideoController.setVisibility(View.GONE);
        mInfoPanel.setVisibility(View.GONE);
    }


    void showProgress() {
        if (mMediaPlayer == null) {
            return;
        }
        int position = mMediaPlayer.getCurrentPosition();
        int duration = mMediaPlayer.getDuration();
        setProgress(position, duration);
        mRawDataAdapter.updateCurrentTime(position);
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 20);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateVideoTime(int position, int duration) {
        mVideoTime.setText(DateUtils.formatElapsedTime(position / 1000));
        mVideoDuration.setText(DateUtils.formatElapsedTime(duration / 1000));
    }


    void onLoadRawDataSuccessfully() {
        mRawDataState = RAW_DATA_STATE_READY;
        Logger.t(TAG).d("Raw data load finished");
        mProgressLoading.setVisibility(View.GONE);
        openVideo();
    }


    void onLoadRawDataError(String msg) {
        Logger.t(TAG).d("msg: " + msg);
        mRawDataState = RAW_DATA_STATE_ERROR;
        mProgressLoading.setVisibility(View.GONE);
        openVideo();
    }

    private void getMomentPlayInfo() {
        if (mMoment.moment.id == Moment.INVALID_MOMENT_ID) {
            return;
        }


        mHachi.getMomentPlayInfo(mMoment.moment.id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<MomentPlayInfo>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(MomentPlayInfo momentPlayInfo) {
//                    loadRawData(momentPlayInfo.rawDataUrl.get(0).url);
                    mMomentPlayInfo = momentPlayInfo;

                    calcRawDataTimeInfo();

                    loadRawData(0);
                }
            });


//        String url = Constants.API_MOMENT_PLAY + mMoment.moment.id;
//        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
//            @Override
//            public void onResponse(JSONObject response) {
//                Logger.t(TAG).json(response.toString());
////                mRawDataUrls = response.optJSONArray("rawDataUrl");
////                loadRawData(0);
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
//                onLoadRawDataError("ErrorCode: " + errorInfo.errorCode);
//            }
//        }).setTag(TAG));
        mProgressLoading.setVisibility(View.VISIBLE);
    }

    private void calcRawDataTimeInfo() {
        int offset = 0;
//        Logger.t(TAG).d("rawDataUrl size: " + mMomentPlayInfo.rawDataUrl.size());
        for (int i = 0; i <= mMomentPlayInfo.rawDataUrl.size(); i++) {
            MomentPlayInfo.RawDataUrl rawDataUrl = mMomentPlayInfo.rawDataUrl.get(i);
            RawDataTimeInfo timeInfo = new RawDataTimeInfo();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            try {
                Date date = dateFormat.parse(rawDataUrl.captureTime);
                timeInfo.captureTime = date.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            timeInfo.offset = offset;
            offset += rawDataUrl.duration;

            mRawDataTimeInfoList.add(timeInfo);
//            Logger.t(TAG).d("add time info: " + timeInfo.toString());
        }
    }


    private void loadRawData(final int index) {
        if (mMomentPlayInfo.rawDataUrl == null || index >= mMomentPlayInfo.rawDataUrl.size()) {
            onLoadRawDataSuccessfully();
            return;
        }

//        HachiApi hachiApi = HachiService.createHachiApiServiceNoBaseUrl();
//        Call<RawDataResponse> rawDataResponseCall = hachiApi.getRawData(mMomentPlayInfo.rawDataUrl.get(index).url);
//        rawDataResponseCall.enqueue(new Callback<RawDataResponse>() {
//            @Override
//            public void onResponse(Call<RawDataResponse> call, retrofit2.Response<RawDataResponse> response) {
//                RawDataResponse rawDataResponse = response.body();
//                for (Long captureTime : rawDataResponse.acc.captureTime) {
//                    Logger.t(TAG).d("capture time: " + captureTime);
//                }
//
//                for (RawDataResponse.AccRawData.Acceleration acceleration : rawDataResponse.acc.acceleration) {
//                    Logger.t(TAG).d(acceleration.toString());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<RawDataResponse> call, Throwable t) {
//                Logger.t(TAG).d("Failed");
//            }
//        });


        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(mMomentPlayInfo.rawDataUrl.get(index).url)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
//                    Logger.t(TAG).json(response.toString());
                    Logger.t(TAG).d("finish loading index: " + index);
                    if (parseRawData(index, response)) {
                        int nextIndex = index + 1;
                        if (nextIndex < mMomentPlayInfo.rawDataUrl.size()) {
                            loadRawData(nextIndex);
                        } else {
                            onLoadRawDataSuccessfully();
                        }
                    } else {
                        onLoadRawDataError("Load Raw data error");
                    }
                }
            })
            .errorListener(new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                    onLoadRawDataError("ErrorCode: " + errorInfo.errorCode);
                }
            })
            .build();
        request.setTag(TAG);
        mRequestQueue.add(request);


    }

    private boolean parseRawData(int index, JSONObject response) {
        int offset = mRawDataTimeInfoList.get(index).offset;

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
                    mRawDataAdapter.addObdData(
                        captureTime.getLong(i) + offset,
                        speed.getInt(i),
                        rpm.getInt(i),
                        temperature.getInt(i),
                        tp.getInt(i),
                        imp.getInt(i),
                        bp.getInt(i),
                        bhp.getInt(i));

                }
            }
            JSONObject acc = response.optJSONObject("acc");
            if (acc != null) {
                JSONArray captureTime = acc.getJSONArray("captureTime");
                JSONArray acceleration = acc.getJSONArray("acceleration");
                for (int i = 0; i < captureTime.length(); i++) {
                    JSONObject accObj = acceleration.getJSONObject(i);
                    mRawDataAdapter.addAccData(
                        captureTime.getLong(i) + offset,
                        accObj.getInt("accelX"), accObj.getInt("accelY"), accObj.getInt("accelZ"),
                        accObj.getInt("gyroX"), accObj.getInt("gyroY"), accObj.getInt("gyroZ"),
                        accObj.getInt("magnX"), accObj.getInt("magnY"), accObj.getInt("magnZ"),
                        accObj.getInt("eulerHeading"), accObj.getInt("eulerRoll"), accObj.getInt("eulerPitch"),
                        accObj.getInt("quaternionW"), accObj.getInt("quaternionX"), accObj.getInt("quaternionY"), accObj.getInt("quaternionZ"),
                        accObj.getInt("pressure")
                    );
                }
            }
            JSONObject gps = response.optJSONObject("gps");
            if (gps != null) {
                JSONArray captureTime = gps.getJSONArray("captureTime");
                JSONArray coordinates = gps.getJSONObject("coordinate").getJSONArray("coordinates");

                for (int i = 0; i < captureTime.length(); i++) {
                    JSONArray coordinateObj = coordinates.getJSONArray(i);
                    mRawDataAdapter.addGpsData(
                        captureTime.getLong(i) + offset,
                        coordinateObj.getDouble(0),
                        coordinateObj.getDouble(1),
                        coordinateObj.getDouble(2)
                    );
                }
            }

            return true;
        } catch (JSONException e) {
            Logger.t(TAG).d(e.toString());
            return false;
        }
    }


    private class MomentRawDataAdapter {
        List<RawDataItem> mOBDData = new ArrayList<>();
        List<RawDataItem> mAccData = new ArrayList<>();
        List<RawDataItem> mGPSData = new ArrayList<>();

        private int mObdDataIndex = 0;
        private int mAccDataIndex = 0;
        private int mGpsDataIndex = 0;

        public void reset() {
            mObdDataIndex = 0;
            mAccDataIndex = 0;
            mGpsDataIndex = 0;
        }


        public void addObdData(long captureTime, int speed, int rpm, int temperature, int tp, int imp, int bp, int bhp) {
            RawDataItem item = new RawDataItem(RawDataItem.DATA_TYPE_OBD, captureTime);

            ObdData data = new ObdData(speed, temperature, rpm);
            item.data = data;
            mOBDData.add(item);
        }

        public void addAccData(long captureTime, int accX, int accY, int accZ,
                               int gyroX, int gyroY, int gyroZ,
                               int magnX, int magnY, int magnZ,
                               int eulerHeading, int eulerRoll, int eulerPitch,
                               int quaternionW, int quaternionX, int quaternionY, int quaternionZ,
                               int pressure) {
            RawDataItem item = new RawDataItem(RawDataItem.DATA_TYPE_IIO, captureTime);
            IioData data = new IioData();

            data.accX = accX;
            data.accY = accY;
            data.accZ = accZ;
            data.euler_roll = eulerRoll;
            data.euler_pitch = eulerPitch;

            item.data = data;
            mAccData.add(item);
        }

        public void addGpsData(long captureTime, double longitude, double latitude, double altitude) {
            RawDataItem item = new RawDataItem(RawDataItem.DATA_TYPE_GPS, captureTime);
            GpsData data = new GpsData();
            data.coord.lat = latitude;
            data.coord.lat_orig = latitude;
            data.coord.lng = longitude;
            data.coord.lng_orig = longitude;
            data.altitude = altitude;
            item.data = data;

            mGPSData.add(item);
        }

        public void updateCurrentTime(int currentTime) {
            if (checkIfUpdated(mAccData, 0, currentTime)) {
                mAccDataIndex++;
            }
            if (checkIfUpdated(mGPSData, 0, currentTime)) {
                mGpsDataIndex++;
            }
            if (checkIfUpdated(mOBDData, 0, currentTime)) {
                mObdDataIndex++;
            }
        }

        private boolean checkIfUpdated(List<RawDataItem> list, int fromPosition, int currentTime) {
            int index = fromPosition;
            if (index >= list.size()) {
                return false;
            }

            for (int i = 1; i < list.size(); i++) {
                RawDataItem item = list.get(i);
                if (currentTime < item.getPtsMs()) {
//                    Logger.t(TAG).d("found rawdata: " + i + " currentTime: " + currentTime + " itempts: " + item.getPtsMs());
                    RawDataItem updateItem = new RawDataItem(list.get(i - 1));
                    long startTime = getRawDataIndex(currentTime);
                    updateItem.setPtsMs(startTime + updateItem.getPtsMs());
                    mGaugeView.updateRawDateItem(updateItem);
                    return true;
                }
            }




            return false;
        }

        private long getRawDataIndex(int currentTime) {
            for (int i = 1; i < mRawDataTimeInfoList.size(); i++) {
                if (currentTime < mRawDataTimeInfoList.get(i).offset) {
                    return mRawDataTimeInfoList.get(i - 1).captureTime;
                }
            }
            return mRawDataTimeInfoList.get(0).captureTime;
        }


    }

    private void changeState(int targetState) {
//        Logger.t(TAG).d("target state: " + targetState);
        switch (targetState) {
            case STATE_IDLE:
                mVsCover.setVisibility(View.VISIBLE);
                mProgressLoading.setVisibility(View.INVISIBLE);
                mBtnPlayPause.toggle(true);
//                mBtnPlayPause.setImageResource(R.drawable.playbar_play);
                break;
            case STATE_PREPAREING:
                mVsCover.setVisibility(View.VISIBLE);
                mProgressLoading.setVisibility(View.VISIBLE);
                break;
            case STATE_PREPARED:
                mProgressLoading.setVisibility(View.GONE);
                break;
            case STATE_PLAYING:
                mVsCover.setVisibility(View.GONE);
                mBtnPlayPause.toggle(false);
                startPlayer();
                mProgressLoading.setVisibility(View.GONE);
                break;
            case STATE_PAUSE:
                mBtnPlayPause.toggle(true);
                pausePlayer();
                break;

        }
        mCurrentState = targetState;
    }

    private void startPlayer() {
        mMediaPlayer.start();

    }

    private void pausePlayer() {
        mMediaPlayer.pause();

    }

    private void stopPlayer() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }

    }


    public class RawDataTimeInfo {
        private long captureTime;

        private int offset;

        @Override
        public String toString() {
            return ToStringUtils.getString(this);
        }
    }

    public class UpdatePlayTimeTask extends TimerTask {

        @Override
        public void run() {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                refreshProgressBar();

            }
        }

        private void refreshProgressBar() {
            if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
                return;
            }
            final int currentPos = mMediaPlayer.getCurrentPosition();

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setProgress(currentPos, mMediaPlayer.getDuration());
                    //
                    mRawDataAdapter.updateCurrentTime(currentPos);
                }
            });
        }
    }

    private static class VideoHandler extends Handler {
        private WeakReference<MomentPlayFragment> mFragmentRef;

        VideoHandler(MomentPlayFragment fragment) {
            super();
            mFragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            MomentPlayFragment fragment = mFragmentRef.get();
            if (fragment == null) {
                return;
            }
            switch (msg.what) {
                case FADE_OUT:
                    fragment.hideControllers();
                    break;
                case SHOW_PROGRESS:
                    fragment.showProgress();
                    break;
            }
        }
    }


}
