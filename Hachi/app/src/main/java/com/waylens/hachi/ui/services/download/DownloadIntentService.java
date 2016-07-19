package com.waylens.hachi.ui.services.download;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.library.vdb.Clip;
import com.waylens.hachi.library.vdb.ClipDownloadInfo;
import com.waylens.hachi.library.vdb.ClipSegment;
import com.waylens.hachi.library.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.DownloadUrlRequest;



public class DownloadIntentService extends IntentService {
    private static final String TAG = DownloadIntentService.class.getSimpleName();


    private static final String ACTION_DOWNLOAD = "com.waylens.hachi.ui.services.action.DOWNLOAD";
    private static ClipSegment mSharedClipSegment;

    private static final String EXTRA_CLIP = "com.waylens.hachi.ui.services.extra.CLIP";

    public static final String INTENT_FILTER_DOWNLOAD_INTENT_SERVICE = "download_intent_service";
    public static final String EVENT_EXTRA_WHAT = "event_what";
    public static final String EVENT_EXTRA_DOWNLOAD_PROGRESS = "download_progress";
    public static final String EVENT_EXTRA_DOWNLOAD_FILE_PATH = "download_file_path";

    public static final int EVENT_WHAT_DOWNLOAD_STARTED = 0;
    public static final int EVENT_WHAT_DOWNLOAD_PROGRESS = 1;
    public static final int EVENT_WHAT_DOWNLOAD_FINSHED = 2;


    private VdbRequestQueue mVdbRequestQueue;
    private ClipSegment mClipSegment;

    private String mDownloadFilePath;


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

    public static void startDownload(Context context, ClipSegment clipSegment) {
        Intent intent = new Intent(context, DownloadIntentService.class);
        intent.setAction(ACTION_DOWNLOAD);
        mSharedClipSegment = clipSegment;
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
                mClipSegment = mSharedClipSegment;
                handleActionDownloadClip(mClipSegment);
            }
        }
    }

    private void init() {
        mVdbRequestQueue = null;//Snipe.newRequestQueue(this, null);
        mVdbRequestQueue.start();
    }

    private void handleActionDownloadClip(ClipSegment clipSegment) {
        requestDownloadUrl(clipSegment);
    }

    private void requestDownloadUrl(final ClipSegment clipSegment) {


        DownloadUrlRequest request = new DownloadUrlRequest(clipSegment, new VdbResponse
            .Listener<ClipDownloadInfo>() {
            @Override
            public void onResponse(ClipDownloadInfo response) {
                Logger.t(TAG).d("on response:!!!!: " + response.main.url);
                Logger.t(TAG).d("on response:!!! poster data size: " + response.posterData.length);

                startDownload(response, 0, clipSegment.getClip().streams[0]);
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


//        HttpRemuxer remuxer = new HttpRemuxer(0);
//        remuxer.setEventListener(new HttpRemuxer.RemuxerEventListener() {
//            @Override
//            public void onEventAsync(HttpRemuxer remuxer, int event, int arg1, int arg2) {
//                switch (event) {
//                    case HttpRemuxer.EVENT_ERROR:
//                        handleRemuxerError(arg1, arg2);
//                        break;
//                    case HttpRemuxer.EVENT_PROGRESS:
//                        handleRemuxerProgress(arg1);
//                        break;
//                    case HttpRemuxer.EVENT_FINISHED:
//                        Logger.t(TAG).d("Event: " + event + " arg1: " + arg1 + " arg2: " + arg2);
//                        handleRemuxerFinished();
//                        break;
//
//                }
//            }
//        });

        // TODO:
        //broadcastInfo(REASON_DOWNLOAD_STARTED, DOWNLOAD_STATE_RUNNING, item, 0, remain);

        int clipDate = params.getClipDate();
        long clipTimeMs = params.getClipTimeMs();
        String outputFile = RemuxHelper.genDownloadFileName(clipDate, clipTimeMs);
        if (outputFile == null) {
            Logger.t(TAG).e("Output File is null");
        } else {
            //item.outputFile = outputFile;
//            remuxer.run(params, outputFile);
            mDownloadFilePath = outputFile;
            Logger.t(TAG).d("remux is running output file is: " + outputFile);
        }
    }

    private void handleRemuxerError(int arg1, int arg2) {

    }

    private void handleRemuxerProgress(int progress) {
        broadcastDownloadProgress(progress);

    }



    private void handleRemuxerFinished() {
        broadcastDownloadFinished();

    }




    private void broadcastDownloadProgress(int progress) {
        Intent intent = new Intent(INTENT_FILTER_DOWNLOAD_INTENT_SERVICE);
        intent.putExtra(EVENT_EXTRA_WHAT, EVENT_WHAT_DOWNLOAD_PROGRESS);
        intent.putExtra(EVENT_EXTRA_DOWNLOAD_PROGRESS, progress);
        sendBroadcast(intent);
    }
    
    private void broadcastDownloadFinished() {
        Intent intent = new Intent(INTENT_FILTER_DOWNLOAD_INTENT_SERVICE);
        intent.putExtra(EVENT_EXTRA_WHAT, EVENT_WHAT_DOWNLOAD_FINSHED);
        intent.putExtra(EVENT_EXTRA_DOWNLOAD_FILE_PATH, mDownloadFilePath);
        sendBroadcast(intent);
    }






}
