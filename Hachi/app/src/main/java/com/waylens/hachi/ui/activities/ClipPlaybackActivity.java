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
import com.waylens.hachi.vdb.AccData;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;
import com.waylens.hachi.views.dashboard.DashboardView;

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

    private VdtCamera mVdtCamera;
    private Clip mClip;

    private static Clip mSharedClip;

    private RawDataBlock mAccDataBlock;
    private RawDataBlock mObdDataBlock;
    private RawDataBlock mGpsDataBlock;


    @Bind(R.id.svClipPlayback)
    SurfaceView mSvClipPlayback;

    @Bind(R.id.ivBackgroundPicture)
    ImageView mIvBackground;

    @Bind(R.id.dashboard)
    DashboardView mDashboardView;

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


    }

    private void loadRawData() {
        long clipTimeMs = 0;
        int lengthMs = mClip.getDurationMs();

        ClipFragment clipFragment = new ClipFragment(mClip);

        RawDataBlockRequest accRequest = new RawDataBlockRequest(clipFragment, RawDataBlock.RAW_DATA_ACC,
            new VdbResponse.Listener<RawDataBlock>() {
                @Override
                public void onResponse(RawDataBlock response) {
                    mAccDataBlock = response;

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
                    mObdDataBlock = response;
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
        startAccRenderThread();
        startObdRenderThread();
    }

    private void startAccRenderThread() {
        final long startPlayTime = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {

                for (int i = 0; i < mAccDataBlock.header.mNumItems; ) {
                    RawDataItem accItem = mAccDataBlock.getRawDataItem(i);
                    long currentPlayTime = System.currentTimeMillis() - startPlayTime;
                    if (accItem.clipTimeMs < currentPlayTime) {
                        final AccData accData = (AccData) accItem.object;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDashboardView.setRawData(DashboardView.GFORCE_LEFT, (float) accData.accX * 5);
                                mDashboardView.setRawData(DashboardView.GFORCE_RIGHT, (float) accData.accY * 5);
                            }
                        });

                        i++;
                    } else {
                        try {
                            Thread.sleep((accItem.clipTimeMs - currentPlayTime) / 2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }


    private void startObdRenderThread() {
        final long startPlayTime = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {

                for (int i = 0; i < mObdDataBlock.header.mNumItems; ) {
                    RawDataItem obdItem = mObdDataBlock.getRawDataItem(i);
                    long currentPlayTime = System.currentTimeMillis() - startPlayTime;
                    if (obdItem.clipTimeMs < currentPlayTime) {
                        final OBDData obdData = (OBDData) obdItem.object;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDashboardView.setRawData(DashboardView.RPM, (float) obdData.rpm / 1000);
                                mDashboardView.setRawData(DashboardView.MPH, obdData.speed);
                            }
                        });

                        i++;
                    } else {
                        try {
                            Thread.sleep((obdItem.clipTimeMs - currentPlayTime) / 2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
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
