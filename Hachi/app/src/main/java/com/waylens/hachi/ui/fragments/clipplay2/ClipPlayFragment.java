package com.waylens.hachi.ui.fragments.clipplay2;

import android.app.Fragment;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/2/22.
 */
public class ClipPlayFragment extends Fragment implements SurfaceHolder.Callback {
    private static final String TAG = ClipPlayFragment.class.getSimpleName();

    @Bind(R.id.videoView)
    SurfaceView mSurfaceView;

    @Bind(R.id.clipCover)
    ImageView mClipCover;


    @OnClick(R.id.btnPlayPause)
    public void onBtnPlayPauseClicked() {
//        mVideoView.setVideoURI();
//        mVideoView.
        mVdtUriProvider.getUri(mClip, new VdtUriProvider.OnUriLoadedListener() {

            @Override
            public void onUriLoaded(Uri uri) {
                Logger.t(TAG).d("Uri: " + uri.toString());
                openVideo(uri);
                //mVideoView.setVideoURI(uri);
//                mVideoView.start();
            }
        });
    }

    protected Clip mClip;
    private VdtCamera mVdtCamera;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;

    private VdtUriProvider mVdtUriProvider;

    private MediaPlayer mMediaPlayer = new MediaPlayer();

    private SurfaceHolder mSurfaceHolder;



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

    private void init() {
        mVdbRequestQueue = Snipe.newRequestQueue(getActivity(), mVdtCamera);
        mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
        mVdtUriProvider = new VdtUriProvider(mVdbRequestQueue);

    }

    private void initViews() {
        ClipPos clipPos = new ClipPos(mClip);
        mVdbImageLoader.displayVdbImage(clipPos, mClipCover);
    }

    protected void openVideo(Uri uri) {
        if (mSurfaceView == null
            || mSurfaceHolder == null) {
            return;
        }


//        mProgressLoading.setVisibility(View.VISIBLE);
        try {
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Logger.t(TAG).d("Prepare finished!!!");
                    mClipCover.setVisibility(View.GONE);
                    mMediaPlayer.start();
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
//                            mProgressLoading.setVisibility(View.VISIBLE);
                            break;
                        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
//                            mProgressLoading.setVisibility(View.GONE);
                            break;
                    }
                    return false;
                }
            });
//            mCurrentState = STATE_PREPARING;
        } catch (IOException e) {
//            mCurrentState = STATE_ERROR;
//            mTargetState = STATE_ERROR;
            Log.e(TAG, "", e);
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
}
