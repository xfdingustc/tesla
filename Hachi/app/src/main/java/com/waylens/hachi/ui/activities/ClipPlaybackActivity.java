package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.orhanobut.logger.Logger;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.PlaybackUrl;

import java.io.IOException;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2015/9/1.
 */
public class ClipPlaybackActivity extends BaseActivity {
    private static final String TAG = ClipPlaybackActivity.class.getSimpleName();

    private VdbRequestQueue mVdbRequestQueue;
    private MediaPlayer mClipPlayer;

    private VdtCamera mVdtCamera;
    private Clip mClip;

    private static VdtCamera mSharedCamera;
    private static Clip mSharedClip;


    @Bind(R.id.svClipPlayback)
    SurfaceView mSvClipPlayback;


    public static void launch(Context context, VdtCamera vdtCamera, Clip clip) {
        Intent intent = new Intent(context, ClipPlaybackActivity.class);
        mSharedCamera = vdtCamera;
        mSharedClip = clip;
        context.startActivity(intent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void onStart() {
        super.onStart();
        preparePlayback();
    }



    @Override
    protected void init() {
        super.init();
        mVdtCamera = mSharedCamera;
        mClip = mSharedClip;
        mVdbRequestQueue = Snipe.newRequestQueue(this, mVdtCamera.getVdbConnection());
        initViews();
    }

    private void initViews() {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_clip_playback);

    }

    private void preparePlayback() {
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTime());

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(mClip, parameters, new VdbResponse.Listener<PlaybackUrl>() {
            @Override
            public void onResponse(PlaybackUrl response) {
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

    private void playClip(PlaybackUrl response) {
        if (mClipPlayer != null) {
            mClipPlayer.release();
        }
        mClipPlayer = new MediaPlayer();
        mClipPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mClipPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Logger.t(TAG).d("Prepared complete!!!");
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
}
