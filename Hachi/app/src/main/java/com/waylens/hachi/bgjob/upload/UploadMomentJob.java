package com.waylens.hachi.bgjob.upload;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.app.UploadManager;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.library.vdb.Clip;
import com.waylens.hachi.library.vdb.ClipPos;
import com.waylens.hachi.library.vdb.ClipSet;
import com.waylens.hachi.library.vdb.urls.UploadUrl;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.CreateMomentBody;
import com.waylens.hachi.rest.response.CreateMomentResponse;
import com.waylens.hachi.library.snipe.VdbCommand;
import com.waylens.hachi.library.snipe.VdbRequestFuture;
import com.waylens.hachi.library.snipe.VdbRequestQueue;
import com.waylens.hachi.library.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.library.snipe.toolbox.ClipUploadUrlRequest;
import com.waylens.hachi.library.snipe.toolbox.VdbImageRequest;
import com.waylens.hachi.ui.entities.LocalMoment;


import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;

import com.waylens.hachi.library.crs_svr.CrsCommand;
import retrofit2.Call;

/**
 * Created by Xiaofei on 2016/5/2.
 */
public class UploadMomentJob extends Job {
    private static final String TAG = UploadMomentJob.class.getSimpleName();

    private static final int DEFAULT_DATA_TYPE_CAM = VdbCommand.Factory.UPLOAD_GET_V1 | VdbCommand.Factory.UPLOAD_GET_RAW;
    private static final int DEFAULT_DATA_TYPE_CLOUD = CrsCommand.VIDIT_VIDEO_DATA_LOW | CrsCommand.VIDIT_RAW_DATA;

    public static final int UPLOAD_STATE_NONE = 0;
    public static final int UPLOAD_STATE_GET_URL_INFO = 1;
    public static final int UPLOAD_STATE_GET_VIDEO_COVER = 2;
    public static final int UPLOAD_STATE_STORE_VIDEO_COVER = 3;
    public static final int UPLOAD_STATE_CREATE_MOMENT = 4;
    public static final int UPLOAD_STATE_LOGIN = 5;
    public static final int UPLOAD_STATE_LOGIN_SUCCEED = 6;
    public static final int UPLOAD_STATE_START = 7;
    public static final int UPLOAD_STATE_PROGRESS = 8;
    public static final int UPLOAD_STATE_FINISHED = 9;
    public static final int UPLOAD_STATE_ERROR = 10;
    public static final int UPLOAD_STATE_CANCELLED = 11;


    public static final int UPLOAD_ERROR_UNKNOWN = 0x100;
    public static final int UPLOAD_ERROR_LOGIN = 0x101;
    public static final int UPLOAD_ERROR_CREATE_MOMENT_DESC = 0x102;
    public static final int UPLOAD_ERROR_UPLOAD_VIDEO = 0x103;
    public static final int UPLOAD_ERROR_UPLOAD_THUMBNAIL = 0x104;
    public static final int UPLOAD_ERROR_IO = 0x105;

    private final LocalMoment mLocalMoment;

    private VdbRequestQueue mVdbRequestQueue;


    private int mUploadState;
    private int mUploadProgress;
    private int mUploadError;

    public UploadMomentJob(LocalMoment moment) {
        super(new Params(0).requireNetwork().setPersistent(false));
        this.mLocalMoment = moment;

    }


    @Override
    public void onAdded() {
        Logger.t(TAG).d("on Added");
        UploadManager.getManager().addJob(this);
    }

    @Override
    public void onRun() throws Throwable {
        Logger.t(TAG).d("on Run, playlistId: " + mLocalMoment.playlistId);
        mVdbRequestQueue = VdtCameraManager.getManager().getCurrentCamera().getRequestQueue();
        // Step1:  get playlist info:
        VdbRequestFuture<ClipSet> clipSetRequestFuture = VdbRequestFuture.newFuture();
        ClipSetExRequest request = new ClipSetExRequest(mLocalMoment.playlistId, ClipSetExRequest.FLAG_CLIP_EXTRA, clipSetRequestFuture, clipSetRequestFuture);
        mVdbRequestQueue.add(request);
        ClipSet playlistClipSet = clipSetRequestFuture.get();


        Logger.t(TAG).d("Play list info got, clip set size:  " + playlistClipSet.getCount());

        // Step2: get upload url info:
        for (int i = 0; i < playlistClipSet.getCount(); i++) {
            Logger.t(TAG).d("Try to get upload url, index: " + i);
            VdbRequestFuture<UploadUrl> uploadUrlRequestFuture = VdbRequestFuture.newFuture();
            Clip clip = playlistClipSet.getClip(i);
            Bundle parameters = new Bundle();
            parameters.putBoolean(ClipUploadUrlRequest.PARAM_IS_PLAY_LIST, false);
            parameters.putLong(ClipUploadUrlRequest.PARAM_CLIP_TIME_MS, clip.getStartTimeMs());
            parameters.putInt(ClipUploadUrlRequest.PARAM_CLIP_LENGTH_MS, clip.getDurationMs());
            parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, DEFAULT_DATA_TYPE_CAM);
            ClipUploadUrlRequest uploadUrlRequest = new ClipUploadUrlRequest(clip.cid, parameters, uploadUrlRequestFuture, uploadUrlRequestFuture);
            mVdbRequestQueue.add(uploadUrlRequest);

            UploadUrl uploadUrl = uploadUrlRequestFuture.get();
            Logger.t(TAG).d("Got clip upload url: " + uploadUrl.url);
            LocalMoment.Segment segment = new LocalMoment.Segment(clip, uploadUrl, DEFAULT_DATA_TYPE_CLOUD);
            mLocalMoment.mSegments.add(segment);

        }
        setUploadState(UPLOAD_STATE_GET_URL_INFO);

        // Step3: get thumbnail:
        Logger.t(TAG).d("Try to get thumbnail");
        final Clip firstClip = playlistClipSet.getClip(0);
        ClipPos clipPos = new ClipPos(firstClip.getVdbId(), firstClip.cid, firstClip.getClipDate(), firstClip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
        VdbRequestFuture<Bitmap> thumbnailRequestFuture = VdbRequestFuture.newFuture();
        VdbImageRequest imageRequest = new VdbImageRequest(clipPos, thumbnailRequestFuture, thumbnailRequestFuture);
        mVdbRequestQueue.add(imageRequest);
        Bitmap thumbnail = thumbnailRequestFuture.get();
        Logger.t(TAG).d("Got thumbnail");
        setUploadState(UPLOAD_STATE_GET_VIDEO_COVER);

        // Step4: Store thumbnail:
        File cacheDir = Hachi.getContext().getExternalCacheDir();
        File file = new File(cacheDir, "t" + firstClip.getStartTimeMs() + ".jpg");
        FileOutputStream fos = new FileOutputStream(file);
        thumbnail.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, fos);
        mLocalMoment.thumbnailPath = file.getAbsolutePath();
        Logger.t(TAG).d("Saved thumbnail: " + mLocalMoment.thumbnailPath);
        setUploadState(UPLOAD_STATE_STORE_VIDEO_COVER);


        // Step5: create moment in server:
        HachiApi hachiApi = HachiService.createHachiApiService();
        CreateMomentBody createMomentBody = new CreateMomentBody(mLocalMoment);
        Call<CreateMomentResponse> createMomentResponseCall = hachiApi.createMoment(createMomentBody);
        CreateMomentResponse response = createMomentResponseCall.execute().body();

        Logger.t(TAG).d("Get moment response: " + response.toString());
        mLocalMoment.updateUploadInfo(response);
        setUploadState(UPLOAD_STATE_CREATE_MOMENT);


        MomentUploader uploader = new MomentUploader(this);
//        mCloudInfo = new CloudInfo("52.74.236.46", 35020, "qwertyuiopasdfgh");
        uploader.upload(mLocalMoment);

        setUploadState(UPLOAD_STATE_FINISHED);
//        UploadManager.getManager().removeJob(this);
    }


    public void setUploadState(int state) {
        setUploadState(state, 0);
    }

    public void setUploadState(int state, int parameter) {
        mUploadState = state;

        switch (mUploadState) {
            case UPLOAD_STATE_PROGRESS:
                mUploadProgress = parameter;
                break;
            case UPLOAD_STATE_ERROR:
                mUploadError = parameter;
                break;
            default:
                break;
        }

//            UploadManager.getManager().notifyUploadStateChanged(this);
        EventBus.getDefault().post(new UploadEvent(UploadEvent.UPLOAD_JOB_STATE_CHANGED, this));
    }

    public int getState() {
        return mUploadState;
    }

    public int getUploadProgress() {
        return mUploadProgress;
    }

    public int getUploadError() {
        return mUploadError;
    }

    public LocalMoment getLocalMoment() {
        return mLocalMoment;
    }


    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }


    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return new RetryConstraint(false);
    }
}
