package com.waylens.hachi.utils;

import com.orhanobut.logger.Logger;
import com.transee.vdb.HttpRemuxer;
import com.waylens.hachi.bgjob.export.download.RemuxerParams;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipDownloadInfo;

/**
 * Created by Xiaofei on 2016/11/18.
 */

public class ClipDownloadHelper {
    private static final String TAG = ClipDownloadHelper.class.getSimpleName();
    private final Clip.StreamInfo mStreamInfo;
    private final ClipDownloadInfo.StreamDownloadInfo mDownloadInfo;
    private final OnExportListener mListener;

    public ClipDownloadHelper(Clip.StreamInfo streamInfo,
                              ClipDownloadInfo.StreamDownloadInfo downloadInfo, OnExportListener listener) {
        this.mStreamInfo = streamInfo;
        this.mDownloadInfo = downloadInfo;
        this.mListener = listener;
    }


    public String downloadVideo() {
        RemuxerParams params = new RemuxerParams();
        // clip params
        params.setClipDate(mDownloadInfo.clipDate);
        params.setClipTimeMs(mDownloadInfo.clipTimeMs);
        params.setClipLength(mDownloadInfo.lengthMs);
        params.setDurationMs(mDownloadInfo.lengthMs);
        // stream info
        params.setStreamVersion(mStreamInfo.version);
        params.setVideoCoding(mStreamInfo.video_coding);
        params.setVideoFrameRate(mStreamInfo.video_framerate);
        params.setVideoWidth(mStreamInfo.video_width);
        params.setVideoHeight(mStreamInfo.video_height);
        params.setAudioCoding(mStreamInfo.audio_coding);
        params.setAudioNumChannels(mStreamInfo.audio_num_channels);
        params.setAudioSamplingFreq(mStreamInfo.audio_sampling_freq);
        // download params
        params.setInputFile(mDownloadInfo.url + ",0,-1;");
        params.setInputMime("ts");
        params.setOutputFormat("mp4");
//        params.setPosterData(downloadOptions.clipDownloadInfo.posterData);
        params.setGpsData(null);
        params.setAccData(null);
        params.setObdData(null);
        params.setAudioFormat("mp3");
        // add to queue
        //   RemuxHelper.remux(this, params);
        return startDownloadVideo(params);
    }

    private String startDownloadVideo(RemuxerParams params) {

        Logger.t(TAG).d("start download item " + params.getInputFile());


        HttpRemuxer remuxer = new HttpRemuxer(0);

        remuxer.setEventListener(new HttpRemuxer.RemuxerEventListener() {
            @Override
            public void onEventAsync(HttpRemuxer remuxer, int event, int arg1, int arg2) {
                if (mListener == null) {
                    return;
                }
                switch (event) {
                    case HttpRemuxer.EVENT_ERROR:
                        mListener.onExportError(arg1, arg2);
                        break;
                    case HttpRemuxer.EVENT_PROGRESS:
                        mListener.onExportProgress(arg1);
                        break;
                    case HttpRemuxer.EVENT_FINISHED:
                        Logger.t(TAG).d("Event: " + event + " arg1: " + arg1 + " arg2: " + arg2);
                        mListener.onExportFinished();
                        break;

                }
            }
        });

        int clipDate = params.getClipDate();
        long clipTimeMs = params.getClipTimeMs();
        String outputFile = FileUtils.genDownloadFileName(clipDate, clipTimeMs);


        Logger.t(TAG).d("outputFile: " + outputFile);
        if (outputFile == null) {
            Logger.t(TAG).e("Output File is null");
        } else {
            remuxer.run(params, outputFile);
            Logger.t(TAG).d("remux is running output file is: " + outputFile);
        }

        return outputFile;
    }

    public interface OnExportListener {
        void onExportError(int arg1, int arg2);

        void onExportProgress(int progress);

        void onExportFinished();
    }
}
