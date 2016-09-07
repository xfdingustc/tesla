package com.waylens.hachi.bgjob.timelapse;

import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.cpp.opencv_core;
import com.waylens.hachi.app.DownloadManager;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.bgjob.download.Exportable;
import com.waylens.hachi.bgjob.download.event.DownloadEvent;
import com.waylens.hachi.service.remux.RemuxHelper;
import com.xfdingustc.snipe.VdbRequestFuture;
import com.xfdingustc.snipe.VdbRequestQueue;
import com.xfdingustc.snipe.control.VdtCameraManager;
import com.xfdingustc.snipe.toolbox.VdbImageRequest;
import com.xfdingustc.snipe.vdb.Clip;
import com.xfdingustc.snipe.vdb.ClipPos;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by Xiaofei on 2016/8/10.
 */
public class TimeLapseJob extends Job implements Exportable {
    private static final String TAG = TimeLapseJob.class.getSimpleName();
    private final Clip mClip;
    private final int mSpeed;


    private VdbRequestQueue mVdbRequestQueue;

    private FFmpegFrameRecorder recorder;

    private EventBus mEventBus = EventBus.getDefault();

    private String mOutputFile;

    private DownloadManager mDownloadManager = DownloadManager.getManager();

    private int mExportProgress;


    private BlockingQueue<VdbImage> mVdbImageBitmapQueue = new LinkedBlockingQueue<>();


    public TimeLapseJob(Clip clip, int speed) {
        super(new Params(0).requireNetwork());
        this.mClip = clip;
        this.mSpeed = speed;
    }

    @Override
    public void onAdded() {
        mDownloadManager.addJob(this);
    }

    @Override
    public void onRun() throws Throwable {
        encodeClipThumbnails();
    }

    @Override
    public String getJobId() {
        return getId();
    }

    @Override
    public int getExportProgress() {
        return mExportProgress;
    }

    @Override
    public String getOutputFile() {
        return mOutputFile;
    }

    @Override
    public ClipPos getClipStartPos() {
        return new ClipPos(mClip);
    }

    private void encodeClipThumbnails() throws Throwable {

        mVdbRequestQueue = VdtCameraManager.getManager().getCurrentVdbRequestQueue();

        mEventBus.post(new DownloadEvent(DownloadEvent.DOWNLOAD_WHAT_START));
        new GetVdbImageThread().start();

        mOutputFile = RemuxHelper.genDownloadFileName((int) mClip.getClipDate(), mClip.getStartTimeMs());


        while (true) {
            VdbImage vdbImage = mVdbImageBitmapQueue.take();
            if (vdbImage.thumbnail == null) {
                break;
            }

            if (recorder == null) {
                recorder = new FFmpegFrameRecorder(mOutputFile, vdbImage.thumbnail.getWidth(), vdbImage.thumbnail.getHeight());

                recorder.setFormat("mp4");
                recorder.setFrameRate(mSpeed);// 录像帧率
                recorder.start();
            }


            long time = System.currentTimeMillis();
            opencv_core.IplImage iplImage;
            iplImage = opencv_core.IplImage.create(vdbImage.thumbnail.getWidth(), vdbImage.thumbnail.getHeight(), 8, 4);
            vdbImage.thumbnail.copyPixelsToBuffer(iplImage.getByteBuffer());


            recorder.record(iplImage);
            Log.d(TAG, "encode " + (System.currentTimeMillis() - time));


            Log.d(TAG, "encode one frame encode time: " + vdbImage.pts + " startTime: " + mClip.getStartTimeMs() + " endTime: " + mClip.getEndTimeMs());

            int progress = (int) (((vdbImage.pts - mClip.getStartTimeMs()) * 100) / mClip.getDurationMs());
            if (mExportProgress != progress) {
                mExportProgress = progress;
                mEventBus.post(new DownloadEvent(DownloadEvent.DOWNLOAD_WHAT_PROGRESS, this));
            }


        }


        MediaScannerConnection.scanFile(Hachi.getContext(), new String[]{mOutputFile.toString()}, null, null);
        mEventBus.post(new DownloadEvent(DownloadEvent.DOWNLOAD_WHAT_FINISHED, this));

        recorder.stop();
    }


    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return new RetryConstraint(false);
    }


    private class GetVdbImageThread extends Thread {
        @Override
        public void run() {
            long encodeTime = mClip.getStartTimeMs();
            while (encodeTime < mClip.getEndTimeMs()) {
                VdbRequestFuture<Bitmap> vdbRequestFuture = VdbRequestFuture.newFuture();
                ClipPos clipPos = new ClipPos(mClip, encodeTime);
                VdbImageRequest request = new VdbImageRequest(clipPos, vdbRequestFuture,
                    vdbRequestFuture, 0, 0, ImageView.ScaleType.CENTER_INSIDE, Bitmap.Config.ARGB_8888, null);
                mVdbRequestQueue.add(request);
                try {
                    long time = System.currentTimeMillis();
                    Log.d(TAG, "fetch one frame " + encodeTime);
                    Bitmap thumbnail = vdbRequestFuture.get();
                    mVdbImageBitmapQueue.offer(new VdbImage(thumbnail, encodeTime));
                    Log.d(TAG, "Get one bit: " + (System.currentTimeMillis() - time));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                encodeTime += 1000;

            }

            mVdbImageBitmapQueue.offer(new VdbImage(null, -1));
        }
    }

    private class VdbImage {
        Bitmap thumbnail;
        long pts;

        public VdbImage(Bitmap thumbnail, long pts) {
            this.thumbnail = thumbnail;
            this.pts = pts;
        }
    }


}
