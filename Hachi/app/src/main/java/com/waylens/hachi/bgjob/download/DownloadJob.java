package com.waylens.hachi.bgjob.download;

import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.transee.vdb.HttpRemuxer;
import com.waylens.hachi.bgjob.download.event.DownloadEvent;
import com.waylens.hachi.ui.services.download.RemuxHelper;
import com.waylens.hachi.ui.services.download.RemuxerParams;
import com.xfdingustc.snipe.vdb.Clip;
import com.xfdingustc.snipe.vdb.ClipDownloadInfo;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Xiaofei on 2016/5/4.
 */
public class DownloadJob extends Job {
    private static final String TAG = DownloadJob.class.getSimpleName();
    private ClipDownloadInfo.StreamDownloadInfo mDownloadInfo;
    private Clip.StreamInfo mStreamInfo;

    private EventBus mEventBus = EventBus.getDefault();
    private String mDownloadFilePath;


    public DownloadJob(Clip.StreamInfo streamInfo, ClipDownloadInfo.StreamDownloadInfo downloadInfo) {
        super(new Params(0).requireNetwork().setPersistent(false));

        this.mStreamInfo = streamInfo;
        this.mDownloadInfo = downloadInfo;


    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        downloadVideo();
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return new RetryConstraint(false);
    }


    private void downloadVideo() {

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

        startDownloadVideo(params);

    }

    private void startDownloadVideo(RemuxerParams params) {

        Logger.t(TAG).d("start download item " + params.getInputFile());


        HttpRemuxer remuxer = new HttpRemuxer(0);
        remuxer.setEventListener(new HttpRemuxer.RemuxerEventListener() {
            @Override
            public void onEventAsync(HttpRemuxer remuxer, int event, int arg1, int arg2) {
                switch (event) {
                    case HttpRemuxer.EVENT_ERROR:
                        handleRemuxerError(arg1, arg2);
                        break;
                    case HttpRemuxer.EVENT_PROGRESS:
                        handleRemuxerProgress(arg1);
                        break;
                    case HttpRemuxer.EVENT_FINISHED:
                        Logger.t(TAG).d("Event: " + event + " arg1: " + arg1 + " arg2: " + arg2);
                        handleRemuxerFinished();
                        break;

                }
            }
        });

        // TODO:
        //broadcastInfo(REASON_DOWNLOAD_STARTED, DOWNLOAD_STATE_RUNNING, item, 0, remain);

        int clipDate = params.getClipDate();
        long clipTimeMs = params.getClipTimeMs();
        String outputFile = RemuxHelper.genDownloadFileName(clipDate, clipTimeMs);


        Logger.t(TAG).d("outputFile: " + outputFile);
        if (outputFile == null) {
            Logger.t(TAG).e("Output File is null");
        } else {
            //item.outputFile = outputFile;
            mEventBus.post(new DownloadEvent(DownloadEvent.DOWNLOAD_WHAT_START));
            remuxer.run(params, outputFile);
            mDownloadFilePath = outputFile;

            Logger.t(TAG).d("remux is running output file is: " + outputFile);
        }
    }

    private void handleRemuxerError(int arg1, int arg2) {

    }

    private void handleRemuxerProgress(int progress) {
//        broadcastDownloadProgress(progress);
        mEventBus.post(new DownloadEvent(DownloadEvent.DOWNLOAD_WHAT_PROGRESS, progress));
        Logger.t(TAG).d("download progress: " + progress);
    }


    private void handleRemuxerFinished() {
//        broadcastDownloadFinished();
        mEventBus.post(new DownloadEvent(DownloadEvent.DOWNLOAD_WHAT_FINISHED, mDownloadFilePath));
        Logger.t(TAG).d("download started: ");

    }
}
