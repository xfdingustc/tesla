package com.waylens.hachi.utils;

import com.orhanobut.logger.Logger;
import com.transee.vdb.HttpRemuxer;
import com.waylens.hachi.bgjob.export.download.RemuxerParams;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipDownloadInfo;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by Xiaofei on 2016/11/18.
 */

public class ClipDownloadHelper {
    private static final String TAG = ClipDownloadHelper.class.getSimpleName();
    private final Clip.StreamInfo mStreamInfo;
    private final ClipDownloadInfo.StreamDownloadInfo mDownloadInfo;


    public ClipDownloadHelper(Clip.StreamInfo streamInfo,
                              ClipDownloadInfo.StreamDownloadInfo downloadInfo) {
        this.mStreamInfo = streamInfo;
        this.mDownloadInfo = downloadInfo;

    }

    public Observable<Integer> downloadClipRx(final String outputFile) {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                doDownloadClip(outputFile, subscriber);
            }
        });
    }


    private void doDownloadClip(String outputFile, Subscriber<? super Integer> subscriber) {
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
        startDownloadVideo(outputFile, params, subscriber);
    }

    private void startDownloadVideo(String outputFile, RemuxerParams params, final Subscriber<? super Integer> subscriber) {
        Logger.t(TAG).d("start download item " + params.getInputFile());
        HttpRemuxer remuxer = new HttpRemuxer(0);

        remuxer.setEventListener(new HttpRemuxer.RemuxerEventListener() {
            @Override
            public void onEventAsync(HttpRemuxer remuxer, int event, int arg1, int arg2) {
                if (subscriber == null) {
                    return;
                }
                switch (event) {
                    case HttpRemuxer.EVENT_ERROR:
                        subscriber.onError(new Throwable("download error"));
                        break;
                    case HttpRemuxer.EVENT_PROGRESS:
                        subscriber.onNext(arg1);
                        break;
                    case HttpRemuxer.EVENT_FINISHED:
                        Logger.t(TAG).d("Event: " + event + " arg1: " + arg1 + " arg2: " + arg2);
                        subscriber.onCompleted();
                        break;

                }
            }
        });

        Logger.t(TAG).d("outputFile: " + outputFile);
        if (outputFile == null) {
            Logger.t(TAG).e("Output File is null");
        } else {
            remuxer.run(params, outputFile);
            Logger.t(TAG).d("remux is running output file is: " + outputFile);
        }
    }

}
