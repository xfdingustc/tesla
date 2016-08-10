package com.waylens.hachi.bgjob.timelapse;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.cpp.opencv_core;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.ui.services.download.RemuxHelper;
import com.waylens.mediatranscoder.engine.surfaces.InputSurface;
import com.xfdingustc.snipe.VdbRequestFuture;
import com.xfdingustc.snipe.VdbRequestQueue;
import com.xfdingustc.snipe.toolbox.VdbImageRequest;
import com.xfdingustc.snipe.vdb.Clip;
import com.xfdingustc.snipe.vdb.ClipPos;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Created by Xiaofei on 2016/8/10.
 */
public class TimeLapseJob extends Job {
    private static final String TAG = TimeLapseJob.class.getSimpleName();
    private final Clip mClip;
    private final int mSpeed;


    private VdbRequestQueue mVdbRequestQueue;

    private FFmpegFrameRecorder recorder;

    private EventBus mEventBus = EventBus.getDefault();


    public TimeLapseJob(Clip clip, int speed) {
        super(new Params(0).requireNetwork());
        this.mClip = clip;
        this.mSpeed = speed;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        encodeClipThumbnails();
    }

    private void encodeClipThumbnails() throws Throwable {

        mVdbRequestQueue = VdtCameraManager.getManager().getCurrentVdbRequestQueue();
        mEventBus.post(new TimelapseEvent(TimelapseEvent.EVENT_START));

        String outputFile = RemuxHelper.genDownloadFileName((int) mClip.getClipDate(), mClip.getStartTimeMs());

        long encodeTime = mClip.getStartTimeMs();
        while (encodeTime < mClip.getEndTimeMs()) {
            VdbRequestFuture<Bitmap> vdbRequestFuture = VdbRequestFuture.newFuture();
            ClipPos clipPos = new ClipPos(mClip, encodeTime);
            VdbImageRequest request = new VdbImageRequest(clipPos, vdbRequestFuture, vdbRequestFuture, 0, 0, ImageView.ScaleType.CENTER_INSIDE, Bitmap.Config.ARGB_8888, null);
            mVdbRequestQueue.add(request);
            long time = System.currentTimeMillis();
            Bitmap thumbnail = vdbRequestFuture.get();
            Log.d(TAG, "Get one bit: " + (System.currentTimeMillis() - time) + " " + thumbnail.getWidth()  + " X " + thumbnail.getHeight());

//            mDecoderOutputSurfaceWrapper.getTransformMatrix(mSTMatrix);

            if (recorder == null) {
                recorder = new FFmpegFrameRecorder(outputFile, thumbnail.getWidth(), thumbnail.getHeight());
                recorder.setFormat("mp4");
                recorder.setFrameRate(mSpeed);// 录像帧率
                recorder.start();
            }

            time = System.currentTimeMillis();
            opencv_core.IplImage iplImage;
            iplImage = opencv_core.IplImage.create(thumbnail.getWidth(), thumbnail.getHeight(), 8, 4);
            thumbnail.copyPixelsToBuffer(iplImage.getByteBuffer());

            recorder.record(iplImage);
            Log.d(TAG, "encode " + (System.currentTimeMillis() - time));

            Log.d(TAG, "encode one frame encode time: " + encodeTime + " startTime: " + mClip.getStartTimeMs() + " endTime: " + mClip.getEndTimeMs());

            int progress = (int) (((encodeTime - mClip.getStartTimeMs()) * 100) / mClip.getDurationMs());
            mEventBus.post(new TimelapseEvent(TimelapseEvent.EVENT_PROGRESS, progress));

            encodeTime += 1000;

        }

        mEventBus.post(new TimelapseEvent(TimelapseEvent.EVENT_END, outputFile));

        Log.d("test", "录制完成....");
        recorder.stop();
    }





    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return new RetryConstraint(false);
    }



}
