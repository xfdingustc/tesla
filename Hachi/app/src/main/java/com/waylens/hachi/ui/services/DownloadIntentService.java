package com.waylens.hachi.ui.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.orhanobut.logger.Logger;
import com.transee.vdb.HttpRemuxer;
import com.transee.vdb.RemuxHelper;
import com.transee.vdb.RemuxerParams;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.DownloadUrlRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipDownloadInfo;
import com.waylens.hachi.vdb.RawDataBlock;


public class DownloadIntentService extends IntentService {
    private static final String TAG = DownloadIntentService.class.getSimpleName();

    private static final String ACTION_DOWNLOAD = "com.waylens.hachi.ui.services.action.DOWNLOAD";
    private static Clip mSharedClip;

    private static final String EXTRA_CLIP = "com.waylens.hachi.ui.services.extra.CLIP";

    private VdbRequestQueue mVdbRequestQueue;
    private Clip mClip;


    private static class DownloadOptions {
        ClipDownloadInfo clipDownloadInfo;
        int stream;
        Clip.StreamInfo clipStreamInfo;
        RawDataBlock.DownloadRawDataBlock mAccData;
        RawDataBlock.DownloadRawDataBlock mGpsData;
        RawDataBlock.DownloadRawDataBlock mObdData;
    }

    private DownloadOptions mDownloadOptions;


    public DownloadIntentService() {
        super("DownloadIntentService");
    }

    public static void startDownload(Context context, Clip clip) {
        Intent intent = new Intent(context, DownloadIntentService.class);
        intent.setAction(ACTION_DOWNLOAD);
        mSharedClip = clip;
        context.startService(intent);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOAD.equals(action)) {
                mClip = mSharedClip;
                handleActionDownloadClip(mClip);
            }
        }
    }

    private void init() {
        mVdbRequestQueue = Snipe.newRequestQueue(this);
        mVdbRequestQueue.start();
    }

    private void handleActionDownloadClip(Clip clip) {
        requestDownloadUrl(clip);
    }

    private void requestDownloadUrl(final Clip clip) {
        DownloadUrlRequest request = new DownloadUrlRequest(clip, new VdbResponse.Listener<ClipDownloadInfo>() {
            @Override
            public void onResponse(ClipDownloadInfo response) {
                Logger.t(TAG).d("on response:!!!!: " + response.main.url);
                Logger.t(TAG).d("on response:!!! poster data size: " + response.posterData.length);

                startDownload(response, 0, clip.streams[0]);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
    }

    private void startDownload(ClipDownloadInfo clipDownloadInfo, int stream,
                               Clip.StreamInfo streamInfo) {
        mDownloadOptions = new DownloadOptions();
        mDownloadOptions.clipDownloadInfo = clipDownloadInfo;
        mDownloadOptions.stream = stream;
        mDownloadOptions.clipStreamInfo = streamInfo;

        downloadVideo(mDownloadOptions);

    }


    private void downloadVideo(DownloadOptions downloadOptions) {
        ClipDownloadInfo.StreamDownloadInfo downloadInfo =
            downloadOptions.stream == 0 ? downloadOptions.clipDownloadInfo.main : downloadOptions.clipDownloadInfo.sub;

        RemuxerParams params = new RemuxerParams();
        // clip params
        params.setClipDate(downloadInfo.clipDate);
        params.setClipTimeMs(downloadInfo.clipTimeMs);
        params.setClipLength(downloadInfo.lengthMs);
        params.setDurationMs(downloadInfo.lengthMs);
        // stream info
        params.setStreamVersion(downloadOptions.clipStreamInfo.version);
        params.setVideoCoding(downloadOptions.clipStreamInfo.video_coding);
        params.setVideoFrameRate(downloadOptions.clipStreamInfo.video_framerate);
        params.setVideoWidth(downloadOptions.clipStreamInfo.video_width);
        params.setVideoHeight(downloadOptions.clipStreamInfo.video_height);
        params.setAudioCoding(downloadOptions.clipStreamInfo.audio_coding);
        params.setAudioNumChannels(downloadOptions.clipStreamInfo.audio_num_channels);
        params.setAudioSamplingFreq(downloadOptions.clipStreamInfo.audio_sampling_freq);
        // download params
        params.setInputFile(downloadInfo.url + ",0,-1;");
        params.setInputMime("ts");
        params.setOutputFormat("mp4");
        params.setPosterData(downloadOptions.clipDownloadInfo.posterData);
        params.setGpsData(downloadOptions.mGpsData != null ? downloadOptions.mGpsData.ack_data : null);
        params.setAccData(downloadOptions.mAccData != null ? downloadOptions.mAccData.ack_data : null);
        params.setObdData(downloadOptions.mObdData != null ? downloadOptions.mObdData.ack_data : null);
        params.setAudioFormat("mp3");
        // add to queue
        //   RemuxHelper.remux(this, params);

        startDownloadVideo(params);

    }

    private void startDownloadVideo(RemuxerParams params) {

        Logger.t(TAG).d("start download item " + params.getInputFile());

        HttpRemuxer remuxer = new HttpRemuxer(0) {
            @Override
            public void onEventAsync(HttpRemuxer remuxer, int event, int arg1, int arg2) {
                //processRemuxerEvent(remuxer, event, arg1, arg2);
                Logger.t(TAG).d("Event: " + event + " arg1: " + arg1 + " arg2: " + arg2);
            }
        };

        //DownloadService.DownloadItem item = notif.item;
        //int remain = mWorkQueue.initDownload(remuxer, item);

        // TODO:
        //broadcastInfo(REASON_DOWNLOAD_STARTED, DOWNLOAD_STATE_RUNNING, item, 0, remain);


        Logger.t(TAG).d("run remuxer");

        //RemuxerParams params = item.params;
        int clipDate = params.getClipDate();
        long clipTimeMs = params.getClipTimeMs();
        String outputFile = RemuxHelper.genDownloadFileName(clipDate, clipTimeMs);
        if (outputFile == null) {
            Logger.t(TAG).e("Output File is null");
        } else {
            //item.outputFile = outputFile;
            remuxer.run(params, outputFile);
            Logger.t(TAG).d("remux is running output file is: " + outputFile);
        }
    }


}
