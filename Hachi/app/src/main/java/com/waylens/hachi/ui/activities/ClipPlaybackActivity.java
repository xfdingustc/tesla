package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Xml;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
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
import com.waylens.hachi.snipe.toolbox.DownloadRawDataBlockRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.views.DashboardView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

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

    private static VdtCamera mSharedCamera;
    private static Clip mSharedClip;


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
        startPlayback();
    }




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
        //preparePlayback();

    }




    @Override
    protected void init() {
        super.init();
        mVdtCamera = mSharedCamera;
        mClip = mSharedClip;
        mVdbRequestQueue = Snipe.newRequestQueue(this, mVdtCamera.getVdbConnection());
        mVdbImageLoader = new VdbImageLoader(mVdbRequestQueue);
        initViews();
    }

    private void initViews() {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_clip_playback);
        ClipPos clipPos = new ClipPos(mClip, mClip.getStartTime(), ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, mIvBackground);



    }

    private void startPlayback() {
        Bundle parameter = new Bundle();
        parameter.putLong(DownloadRawDataBlockRequest.PARAMETER_CLIP_TIME_MS, 0);
        parameter.putInt(DownloadRawDataBlockRequest.PARAMETER_LENGTH_MS, mClip.clipLengthMs);
        parameter.putInt(DownloadRawDataBlockRequest.PARAMETER_DATA_TYPE, VdbClient.RAW_DATA_ODB);
        DownloadRawDataBlockRequest request = new DownloadRawDataBlockRequest(mClip, parameter,
            new VdbResponse.Listener<RawDataBlock.DownloadRawDataBlock>() {
                @Override
                public void onResponse(RawDataBlock.DownloadRawDataBlock response) {
                    Logger.t(TAG).d("GGGGGGGGGGet response: " + response.header.mDataType);
                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
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
