package com.waylens.hachi.bgjob.upload;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;
import com.waylens.hachi.library.crs_svr.CrsCommand;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.CreateMomentBody;
import com.waylens.hachi.rest.response.CreateMomentResponse;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.xfdingustc.snipe.VdbCommand;
import com.xfdingustc.snipe.VdbRequestFuture;
import com.xfdingustc.snipe.VdbRequestQueue;
import com.xfdingustc.snipe.control.VdtCameraManager;
import com.xfdingustc.snipe.toolbox.ClipSetExRequest;
import com.xfdingustc.snipe.toolbox.ClipUploadUrlRequest;
import com.xfdingustc.snipe.toolbox.VdbImageRequest;
import com.xfdingustc.snipe.vdb.Clip;
import com.xfdingustc.snipe.vdb.ClipPos;
import com.xfdingustc.snipe.vdb.ClipSet;
import com.xfdingustc.snipe.vdb.urls.UploadUrl;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CancellationException;

import retrofit2.Call;

/**
 * Created by Xiaofei on 2016/5/2.
 */
public class CacheMomentJob extends UploadMomentJob {
    private static final String TAG = CacheMomentJob.class.getSimpleName();

    private static final int DEFAULT_DATA_TYPE_CAM = VdbCommand.Factory.UPLOAD_GET_V1 | VdbCommand.Factory.UPLOAD_GET_RAW;
    private static final int DEFAULT_DATA_TYPE_CLOUD = CrsCommand.VIDIT_VIDEO_DATA_LOW | CrsCommand.VIDIT_RAW_DATA;


    private VdbRequestQueue mVdbRequestQueue;

    private MomentUploader mUploader = null;


    public CacheMomentJob(LocalMoment moment) {
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
        ClipSetExRequest request = new ClipSetExRequest(mLocalMoment.playlistId, ClipSetExRequest.FLAG_CLIP_EXTRA | ClipSetExRequest.FLAG_CLIP_DESC, clipSetRequestFuture, clipSetRequestFuture);
        mVdbRequestQueue.add(request);
        ClipSet playlistClipSet = clipSetRequestFuture.get();


        Logger.t(TAG).d("Play list info got, clip set size:  " + playlistClipSet.getCount());

        checkIfCancelled();

/*        if (playlistClipSet.getCount() == 1) {
            Clip clip = playlistClipSet.getClip(0);
            if ((clip.typeRace & Clip.TYPE_RACE) > 0) {
                //get race Timing points
            }
        }*/

        // Step2: get upload url info:
        for (int i = 0; i < playlistClipSet.getCount(); i++) {
            Logger.t(TAG).d("Try to get upload url, index: " + i);
            VdbRequestFuture<UploadUrl> uploadUrlRequestFuture = VdbRequestFuture.newFuture();
            Clip clip = playlistClipSet.getClip(i);
            String vin = clip.getVin();
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
            checkIfCancelled();

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
        checkIfCancelled();

        // Step4: Store thumbnail:
        File cacheDir = Hachi.getContext().getExternalCacheDir();
        File file = new File(cacheDir, "t" + firstClip.getStartTimeMs() + ".jpg");
        FileOutputStream fos = new FileOutputStream(file);
        thumbnail.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, fos);
        mLocalMoment.thumbnailPath = file.getAbsolutePath();
        Logger.t(TAG).d("Saved thumbnail: " + mLocalMoment.thumbnailPath);
        setUploadState(UPLOAD_STATE_STORE_VIDEO_COVER);
        checkIfCancelled();


        if (!mLocalMoment.cache) {
            // Step5: create moment in server:
            HachiApi hachiApi = HachiService.createHachiApiService();
            CreateMomentBody createMomentBody = new CreateMomentBody(mLocalMoment);
            Call<CreateMomentResponse> createMomentResponseCall = hachiApi.createMoment(createMomentBody);
            CreateMomentResponse response = createMomentResponseCall.execute().body();

            Logger.t(TAG).d("Get moment response: ");
            mLocalMoment.updateUploadInfo(response);
            setUploadState(UPLOAD_STATE_CREATE_MOMENT);
            checkIfCancelled();
            mUploader = new MomentUploader(this);
            mUploader.upload(mLocalMoment);
            Logger.t(TAG).d("updatestate: " + mState);
        } else {
            MomentUploadCacher cacher = new MomentUploadCacher(this);
            cacher.cacheMoment(mLocalMoment);
            BgJobHelper.uploadCachedMoment(mLocalMoment);
        }

        if (mState != UPLOAD_STATE_CANCELLED || mState != UPLOAD_STATE_ERROR) {
            Logger.t(TAG).d("finished");
            setUploadState(UPLOAD_STATE_FINISHED);
        }

        EventBus.getDefault().post(new UploadEvent(UploadEvent.UPLOAD_JOB_REMOVED, this));


    }

    private void checkIfCancelled() {
        if (mIsCancel == true) {
            setUploadState(UPLOAD_STATE_CANCELLED);
            throw new CancellationException("Job cancelled");
        }
    }


    @Override
    public void cancelUpload() {
        this.mIsCancel = true;
        if (mUploader != null) {
            mUploader.cancel();
        }

    }


    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
        Logger.t(TAG).d("one cancel: " + throwable.toString());
    }


    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return new RetryConstraint(false);
    }
}
