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
import com.transee.vdb.RemuxHelper;
import com.transee.vdb.RemuxerParams;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.DownloadUrlRequest;
import com.waylens.hachi.ui.adapters.ClipFragmentRvAdapter;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.DownloadInfoEx;
import com.waylens.hachi.vdb.PlaybackUrl;

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

    private ClipFragmentRvAdapter mClipFragmentAdapter;
    private DownloadState mDownloadState;

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
        ClipPlaybackActivity.launch(this, mVdtCamera, mClip);
    }

    @OnClick(R.id.btnDownload)
    public void onBtnDownloadClicked() {
        downloadClip();
    }

    private void downloadClip() {
        DownloadUrlRequest request = new DownloadUrlRequest(mClip, new VdbResponse.Listener<DownloadInfoEx>() {
            @Override
            public void onResponse(DownloadInfoEx response) {
                Logger.t(TAG).d("on response:!!!!: " + response.main.url);
                startDownload(response, 0, mClip.streams[0]);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
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

    private static class DownloadState {
        DownloadInfoEx downloadInfo;
        int stream;
        Clip.StreamInfo si;
        VdbClient.DownloadRawDataBlock mAccData;
        VdbClient.DownloadRawDataBlock mGpsData;
        VdbClient.DownloadRawDataBlock mObdData;
    }

    private void startDownload(DownloadInfoEx downloadInfo, int stream, Clip.StreamInfo si) {
        mDownloadState = new DownloadState();
        mDownloadState.downloadInfo = downloadInfo;
        mDownloadState.stream = stream;
        mDownloadState.si = si;

        downloadVideo(mDownloadState);

    }


    private void downloadVideo(DownloadState ds) {
        DownloadInfoEx.DownloadStreamInfo dsi = ds.stream == 0 ? ds.downloadInfo.main : ds.downloadInfo.sub;

        RemuxerParams params = new RemuxerParams();
        // clip params
        params.setClipDate(dsi.clipDate); // TODO
        params.setClipTimeMs(dsi.clipTimeMs); // TODO
        params.setClipLength(dsi.lengthMs);
        // stream info
        params.setStreamVersion(ds.si.version);
        params.setVideoCoding(ds.si.video_coding);
        params.setVideoFrameRate(ds.si.video_framerate);
        params.setVideoWidth(ds.si.video_width);
        params.setVideoHeight(ds.si.video_height);
        params.setAudioCoding(ds.si.audio_coding);
        params.setAudioNumChannels(ds.si.audio_num_channels);
        params.setAudioSamplingFreq(ds.si.audio_sampling_freq);
        // download params
        params.setInputFile(dsi.url + ",0,-1;");
        params.setInputMime("ts");
        params.setOutputFormat("mp4");
        params.setPosterData(ds.downloadInfo.posterData);
        params.setGpsData(ds.mGpsData != null ? ds.mGpsData.ack_data : null);
        params.setAccData(ds.mAccData != null ? ds.mAccData.ack_data : null);
        params.setObdData(ds.mObdData != null ? ds.mObdData.ack_data : null);
        params.setDurationMs(dsi.lengthMs);
        //params.setDisableAudio(mEditor.bMuteAudio);
        //params.setAudioFileName(mEditor.audioFileName);
        params.setAudioFormat("mp3");
        // add to queue
        RemuxHelper.remux(this, params);

    }

}
