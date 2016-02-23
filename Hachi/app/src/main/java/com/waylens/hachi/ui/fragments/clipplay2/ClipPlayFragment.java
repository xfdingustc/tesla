package com.waylens.hachi.ui.fragments.clipplay2;

import android.app.DialogFragment;
import android.app.Fragment;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;

import org.ocpsoft.prettytime.format.SimpleTimeFormat;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/2/22.
 */
public class ClipPlayFragment extends DialogFragment implements SurfaceHolder.Callback {
    private static final String TAG = ClipPlayFragment.class.getSimpleName();

    protected Clip mClip;
    private VdtCamera mVdtCamera;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;

    private VdtUriProvider mVdtUriProvider;

    private MediaPlayer mMediaPlayer = new MediaPlayer();

    private SurfaceHolder mSurfaceHolder;
    private Handler mUiHandler;

    private final int STATE_NONE = 0;
    private final int STATE_PREPAREING = 1;
    private final int STATE_PREPARED = 2;
    private final int STATE_PLAYING = 3;
    private final int STATE_PAUSE = 4;

    private int mCurrentState = STATE_NONE;

    private final int PENDING_ACTION_NONE = 0;
    private final int PENDING_ACTION_START = 1;

    private int mPendingAction = PENDING_ACTION_NONE;

    @Bind(R.id.videoView)
    SurfaceView mSurfaceView;

    @Bind(R.id.clipCover)
    ImageView mClipCover;

    @Bind(R.id.progressLoading)
    ProgressBar mProgressLoading;

    @Bind(R.id.btnPlayPause)
    ImageButton mBtnPlayPause;

    @Bind(R.id.playProgress)
    TextView mTvProgress;

    @Bind(R.id.videoProgressBar)
    ProgressBar mPlayProgressBar;

    @OnClick(R.id.btnDismiss)
    public void onBtnDismissClicked() {
        dismiss();
    }

    @OnClick(R.id.btnPlayPause)
    public void onBtnPlayPauseClicked() {
        switch (mCurrentState) {
            case STATE_PREPARED:
            case STATE_PAUSE:
                toggleMediaPlayerStart(true);
                refreshProgressBar();
                break;
            case STATE_PLAYING:
                toggleMediaPlayerStart(false);
                break;
            case STATE_PREPAREING:
                mProgressLoading.setVisibility(View.VISIBLE);
                mPendingAction = PENDING_ACTION_START;
                break;
        }

    }




    public static class Config {
        public static int PROGRESS_BAR_STYLE_SINGLE = 0;
        public int progressBarStyle;
    }

    private Config mConfig;

    public static ClipPlayFragment newInstance(VdtCamera camera, Clip clip, Config config) {
        ClipPlayFragment fragment = new ClipPlayFragment();
        fragment.mVdtCamera = camera;
        fragment.mClip = clip;
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
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.fragment_clip_play, container, false);
        ButterKnife.bind(this, view);
        initViews();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSurfaceView.getHolder().addCallback(this);
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
        mVdtUriProvider = new VdtUriProvider(mVdbRequestQueue);
        mVdtUriProvider.getUri(mClip, new VdtUriProvider.OnUriLoadedListener() {
            @Override
            public void onUriLoaded(Uri uri) {
                Logger.t(TAG).d("Uri: " + uri.toString());
                openVideo(uri);
            }
        });

        mCurrentState = STATE_PREPAREING;

    }

    private void initViews() {
        ClipPos clipPos = new ClipPos(mClip);
        mVdbImageLoader.displayVdbImage(clipPos, mClipCover);
    }

    protected void openVideo(Uri uri) {
        if (mSurfaceView == null || mSurfaceHolder == null) {
            return;
        }

        try {
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Logger.t(TAG).d("Prepare finished!!!");
                    mCurrentState = STATE_PREPARED;
                    if (mPendingAction == PENDING_ACTION_START) {
                        mClipCover.setVisibility(View.GONE);
                        toggleMediaPlayerStart(true);
                    }

                    mPlayProgressBar.setMax(mp.getDuration());
                    refreshProgressBar();
                }
            });
//            mMediaPlayer.setOnCompletionListener(this);
//            mMediaPlayer.setOnErrorListener(this);


            mMediaPlayer.setDataSource(uri.toString());
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
//            mCurrentState = STATE_PREPARING;
        } catch (IOException e) {
//            mCurrentState = STATE_ERROR;
//            mTargetState = STATE_ERROR;
            Logger.t(TAG).e("", e);
        }
    }



    private void toggleMediaPlayerStart(boolean isPlay) {
        if (isPlay == true) {
            mBtnPlayPause.setImageResource(R.drawable.playbar_pause);
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            mProgressLoading.setVisibility(View.GONE);
        } else {
            mBtnPlayPause.setImageResource(R.drawable.playbar_play);
            mMediaPlayer.pause();
            mCurrentState = STATE_PAUSE;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mMediaPlayer.setDisplay(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
    }

    private void refreshProgressBar() {
        int currentPos = mMediaPlayer.getCurrentPosition() / 1000;
        int duration = mMediaPlayer.getDuration() / 1000;

        String timeText = DateUtils.formatElapsedTime(currentPos) + "/" + DateUtils
            .formatElapsedTime(duration);

        mTvProgress.setText(timeText);


        mPlayProgressBar.setProgress(mMediaPlayer.getCurrentPosition());

        if (mMediaPlayer.isPlaying()) {
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshProgressBar();
                }
            }, 500);
        }
    }
}
