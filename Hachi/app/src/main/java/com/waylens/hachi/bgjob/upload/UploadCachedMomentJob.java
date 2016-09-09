package com.waylens.hachi.bgjob.upload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.CreateMomentBody;
import com.waylens.hachi.rest.response.CreateMomentResponse;
import com.waylens.hachi.service.upload.UploadAPI;
import com.waylens.hachi.service.upload.UploadProgressListener;
import com.waylens.hachi.service.upload.UploadProgressRequestBody;
import com.waylens.hachi.service.upload.rest.body.InitUploadBody;
import com.waylens.hachi.service.upload.rest.response.InitUploadResponse;
import com.waylens.hachi.service.upload.rest.response.UploadDataResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.entities.LocalMoment;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;

/**
 * Created by Xiaofei on 2016/9/6.
 */
public class UploadCachedMomentJob extends UploadMomentJob {
    private static final String TAG = UploadCachedMomentJob.class.getSimpleName();



    public UploadCachedMomentJob(LocalMoment moment) {
        super(new Params(0).requireNetwork().setPersistent(true));
        this.mLocalMoment = moment;
        mLocalMoment.cache = false;
    }

    @Override
    public void onAdded() {
        Logger.t(TAG).d("on added");

    }

    @Override
    public void onRun() throws Throwable {
//        Logger.t(TAG).d("on run " + mLocalMoment.toString());
        mState = UploadMomentJob.UPLOAD_STATE_WAITING_FOR_NETWORK_AVAILABLE;
        EventBus.getDefault().post(new UploadEvent(UploadEvent.UPLOAD_JOB_ADDED, this));
        CreateMomentResponse response;
        while (true) {
            response = getCloudInfo();
            if (response == null) {
                Thread.sleep(10000);
            } else {
                break;
            }
        }
        Logger.t(TAG).d("upload server: " + response.uploadServer.toString());
        mLocalMoment.updateUploadInfo(response);

        try {
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss");
            String date = format.format(System.currentTimeMillis()) + " GMT";
            Logger.t(TAG).d("date: " + date);
            final String authorization = HachiAuthorizationHelper.getAuthoriztion("tscastle.cam2cloud.com:35021",
                SessionManager.getInstance().getUserId() + "/android",
                mLocalMoment.momentID,
                date,
                response.uploadServer.privateKey);

            UploadAPI uploadAPI = new UploadAPI(response.uploadServer.url + "/", date, authorization);

            // Step 1: init upload;
            InitUploadResponse initUploadResponse = uploadAPI.initUploadSync(mLocalMoment.momentID, InitUploadBody.fromLocalMoment(mLocalMoment));

            if (initUploadResponse == null) {
                Logger.t(TAG).d("Failed to init upload");
                return;
            }


            // Step2: upload segments;

            Logger.t(TAG).d("initUploadResponse: " + initUploadResponse.toString());

            final int totalSegments = mLocalMoment.mSegments.size() + 1;

            for (int i = 0; i < mLocalMoment.mSegments.size(); i++) {
                LocalMoment.Segment segment = mLocalMoment.mSegments.get(i);
                final int index = i;

                URI uri = URI.create(segment.uploadURL.url);
                File file = new File(uri);


                UploadProgressRequestBody newRequest = UploadProgressRequestBody.newInstance(file, new UploadProgressListener() {
                    @Override
                    public void update(long bytesWritten, long contentLength, boolean done) {
                        updateUploadProgress(bytesWritten, contentLength, index, totalSegments);
                    }
                });

                UploadDataResponse uploadDataResponse = uploadAPI.uploadChunkSync(newRequest, mLocalMoment.momentID, segment);

                Logger.t(TAG).d("uploadDataResponse: " + uploadDataResponse);

            }

            // Step3: upload thumbnail;
            UploadProgressRequestBody newRequest = UploadProgressRequestBody.newInstance(new File(mLocalMoment.thumbnailPath), new UploadProgressListener() {
                @Override
                public void update(long bytesWritten, long contentLength, boolean done) {
                    updateUploadProgress(bytesWritten, contentLength, totalSegments - 1, totalSegments);
                }
            });
            UploadDataResponse uploadDataResponse = uploadAPI.uploadThumbnail(newRequest, mLocalMoment.momentID);

            Logger.t(TAG).d("uploadThmbnailResponse: " + uploadDataResponse);

            uploadAPI.finishUpload(mLocalMoment.momentID);

            Logger.t(TAG).d("upload finished ");


        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mState != UPLOAD_STATE_CANCELLED || mState != UPLOAD_STATE_ERROR) {
            Logger.t(TAG).d("finished");
            setUploadState(UPLOAD_STATE_FINISHED);
        }

        EventBus.getDefault().post(new UploadEvent(UploadEvent.UPLOAD_JOB_REMOVED, this));

    }


    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return new RetryConstraint(false);
    }

    private CreateMomentResponse getCloudInfo() {

        try {
            HachiApi hachiApi = HachiService.createHachiApiService(10, TimeUnit.SECONDS);
            Logger.t(TAG).d("create");
            CreateMomentBody createMomentBody = new CreateMomentBody(mLocalMoment);
            Call<CreateMomentResponse> createMomentResponseCall = hachiApi.createMoment(createMomentBody);

            CreateMomentResponse response = createMomentResponseCall.execute().body();

            return response;

        } catch (Exception e) {
            Logger.t(TAG).d("failed to fetch moment body");
            return null;
        }
    }


    @Override
    public void cancelUpload() {

    }

    private void updateUploadProgress(long byteWritten, long contentLength, int index, int totalSegment) {
        int progress = (int) ((byteWritten * 100) / contentLength);


        int percentageInThisClip = progress / totalSegment;
        int percentage = index * 100 / totalSegment + percentageInThisClip;
        setUploadState(CacheMomentJob.UPLOAD_STATE_PROGRESS, percentage);
    }



}
