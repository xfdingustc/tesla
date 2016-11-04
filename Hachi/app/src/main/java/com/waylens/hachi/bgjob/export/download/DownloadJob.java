package com.waylens.hachi.bgjob.export.download;

import android.media.MediaScannerConnection;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.transee.vdb.HttpRemuxer;
import com.waylens.hachi.bgjob.export.ExportHelper;
import com.waylens.hachi.bgjob.export.ExportManager;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.bgjob.export.ExportableJob;
import com.waylens.hachi.jobqueue.Params;
import com.waylens.hachi.jobqueue.RetryConstraint;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipDownloadInfo;
import com.waylens.hachi.snipe.vdb.ClipPos;

/**
 * Created by Xiaofei on 2016/5/4.
 */
public class DownloadJob extends ExportableJob {
    private static final String TAG = DownloadJob.class.getSimpleName();
    private ClipDownloadInfo.StreamDownloadInfo mDownloadInfo;
    private Clip mClip;
    private Clip.StreamInfo mStreamInfo;


    private String mDownloadFilePath;


    private ExportManager mDownloadManager = ExportManager.getManager();


    public DownloadJob(Clip clip, Clip.StreamInfo streamInfo, ClipDownloadInfo.StreamDownloadInfo downloadInfo) {
        super(new Params(0).requireNetwork().setPersistent(false));
        this.mClip = clip;
        this.mStreamInfo = streamInfo;
        this.mDownloadInfo = downloadInfo;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public synchronized void onRun() throws Throwable {
        try {
            downloadVideo();
            wait();
        } catch (Throwable e) {
            e.printStackTrace();
        }
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

        int clipDate = params.getClipDate();
        long clipTimeMs = params.getClipTimeMs();
        String outputFile = ExportHelper.genDownloadFileName(clipDate, clipTimeMs);


        Logger.t(TAG).d("outputFile: " + outputFile);
        if (outputFile == null) {
            Logger.t(TAG).e("Output File is null");
        } else {
            //item.outputFile = outputFile;
//            mEventBus.post(new DownloadEvent(DownloadEvent.EXPORT_WHAT_START));
            remuxer.run(params, outputFile);
            mDownloadFilePath = outputFile;

            Logger.t(TAG).d("remux is running output file is: " + outputFile);
        }
    }

    private synchronized void handleRemuxerError(int arg1, int arg2) {
        notifyAll();
    }

    private void handleRemuxerProgress(int progress) {

        notifyProgressChanged(progress);
    }


    private synchronized void handleRemuxerFinished() {
        MediaScannerConnection.scanFile(Hachi.getContext(), new String[]{
            mDownloadFilePath.toString()}, null, null);
        Logger.t(TAG).d("download finished " + mDownloadFilePath);
        notifyAll();
    }

    @Override
    public int getExportProgress() {
        return mDownloadProgress;
    }

    @Override
    public String getOutputFile() {
        return mDownloadFilePath;
    }

    @Override
    public ClipPos getClipStartPos() {
        return new ClipPos(mClip);
    }
}
