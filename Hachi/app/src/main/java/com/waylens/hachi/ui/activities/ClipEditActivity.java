package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.orhanobut.logger.Logger;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.DownloadUrlRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.adapters.ClipFragmentRvAdapter;

import java.io.IOException;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/8/14.
 */
public class ClipEditActivity extends BaseActivity {
    private static final String TAG = ClipEditActivity.class.getSimpleName();

    @Bind(R.id.ivPreviewPicture)
    ImageView mIvPreviewPicture;

    @Bind(R.id.rvClipFragments)
    RecyclerView mRvClipFragments;

    @Bind(R.id.svClipPlayback)
    SurfaceView mSvClipPlayback;

    @Bind(R.id.btnPlay)
    ImageButton mBtnPlay;

    @Bind(R.id.btnDownload)
    ImageButton mBtnDownload;


    private static VdtCamera mSharedCamera;
    private static Clip mSharedClip;

    private VdtCamera mVdtCamera;
    private Clip mClip;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;

    private MediaPlayer mClipPlayer;


    private ClipFragmentRvAdapter mClipFragmentAdapter;

    public static void launch(Context context, VdtCamera vdtCamera, Clip clip) {
        Intent intent = new Intent(context, ClipEditActivity.class);
        mSharedCamera = vdtCamera;
        mSharedClip = clip;
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @OnClick(R.id.btnPlay)
    public void onBtnPlayClicked() {
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTime());

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(mClip, parameters, new VdbResponse.Listener<VdbClient.PlaybackUrl>() {
            @Override
            public void onResponse(VdbClient.PlaybackUrl response) {
                Logger.t(TAG).d("On Response!!!!!!! " + response.url);
                playClip(response);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });

        mVdbRequestQueue.add(request);
    }

    @OnClick(R.id.btnDownload)
    public void onBtnDownloadClicked() {
        downloadClip();
    }

    private void downloadClip() {
        DownloadUrlRequest request = new DownloadUrlRequest(mClip, new VdbResponse.Listener<VdbClient.DownloadInfoEx>() {
            @Override
            public void onResponse(VdbClient.DownloadInfoEx response) {
                Logger.t(TAG).d("on response:!!!!: " + response.main.url);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
    }

    private void playClip(VdbClient.PlaybackUrl response) {
        if (mClipPlayer != null) {
            mClipPlayer.release();
        }
        mClipPlayer = new MediaPlayer();
        mClipPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mClipPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Logger.t(TAG).d("PPPPPPPPPPPPPPPPPPPPPPPPrepared");
                mIvPreviewPicture.setVisibility(View.GONE);
                mBtnPlay.setVisibility(View.GONE);
                mp.setDisplay(mSvClipPlayback.getHolder());
                mp.start();
            }
        });


        try {
            mClipPlayer.setDataSource(response.url);
            mClipPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void init() {
        super.init();
        mVdtCamera = mSharedCamera;
        mClip = mSharedClip;
        mVdbRequestQueue = Snipe.newRequestQueue(this,mVdtCamera.getVdbConnection());
        mVdbImageLoader = new VdbImageLoader(mVdbRequestQueue);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_clip_editor);
        ClipPos clipPos = new ClipPos(mClip, mClip.getStartTime(), ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, mIvPreviewPicture);


        initClipFragmentRecyclerView();
    }

    private void initClipFragmentRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRvClipFragments.setLayoutManager(linearLayoutManager);

        mClipFragmentAdapter = new ClipFragmentRvAdapter(this, mClip, mVdbRequestQueue);
        mRvClipFragments.setAdapter(mClipFragmentAdapter);


    }
}
