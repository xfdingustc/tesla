package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.orhanobut.logger.Logger;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.views.dashboard2.DashboardLayout;

import java.io.IOException;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/9/1.
 */
public class ClipPlaybackActivity extends BaseActivity {
    private static final String TAG = ClipPlaybackActivity.class.getSimpleName();

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;
    private MediaPlayer mClipPlayer;

    private Clip mClip;

    private static Clip mSharedClip;


    //private DashboardView.Adapter mDashboardViewAdapter;


    @Bind(R.id.svClipPlayback)
    SurfaceView mSvClipPlayback;

    @Bind(R.id.ivBackgroundPicture)
    ImageView mIvBackground;

    @Bind(R.id.dashboard)
    DashboardLayout mDashboardView;

    @Bind(R.id.btnPlay)
    ImageButton mBtnPlay;

    @OnClick(R.id.btnPlay)
    public void onBtnPlayClicked() {
        mBtnPlay.setVisibility(View.GONE);
        loadRawData();
    }


    public static void launch(Context context, Clip clip) {
        Intent intent = new Intent(context, ClipPlaybackActivity.class);
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
        //preparePlayback();

    }


    @Override
    protected void init() {
        super.init();
        mClip = mSharedClip;
        mVdbRequestQueue = Snipe.newRequestQueue(this);
        mVdbImageLoader = new VdbImageLoader(mVdbRequestQueue);


        initViews();

    }

    private void initViews() {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_clip_playback);
        ClipPos clipPos = new ClipPos(mClip, mClip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, mIvBackground);

        //mDashboardViewAdapter = new DashboardView.Adapter();
        //mDashboardView.setAdapter(mDashboardViewAdapter);

    }

    private void loadRawData() {

        ClipFragment clipFragment = new ClipFragment(mClip);

        RawDataBlockRequest accRequest = new RawDataBlockRequest(clipFragment, RawDataBlock.RAW_DATA_ACC,
            new VdbResponse.Listener<RawDataBlock>() {
                @Override
                public void onResponse(RawDataBlock response) {
                    Logger.t(TAG).d("Get ACC data block");
                    //mDashboardViewAdapter.setAccDataBlock(response);
                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(accRequest);

        RawDataBlockRequest obdRequest = new RawDataBlockRequest(clipFragment, RawDataBlock.RAW_DATA_ODB,
            new VdbResponse.Listener<RawDataBlock>() {
                @Override
                public void onResponse(RawDataBlock response) {
                    Logger.t(TAG).d("Get Obd data block");
                    //mDashboardViewAdapter.setObdDataBlock(response);
                    startPlayback();
                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(obdRequest);
    }

    private void startPlayback() {
        startUpdateDashboardViewThread();
    }

    private void startUpdateDashboardViewThread() {
        final long startPlayTime = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    final long currentPlayTime = System.currentTimeMillis() - startPlayTime;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //mDashboardView.update(currentPlayTime);
                        }
                    });

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }



    private void preparePlayback() {
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTimeMs());

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

        mClipPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Logger.t(TAG).d("Buffer update, percent: " + percent);
            }
        });

        mClipPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Logger.t(TAG).d("what: " + what + " extra: " + extra);
                return false;
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
