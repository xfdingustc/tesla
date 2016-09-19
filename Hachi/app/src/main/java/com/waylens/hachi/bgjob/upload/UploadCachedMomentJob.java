package com.waylens.hachi.bgjob.upload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.CreateMomentBody;
import com.waylens.hachi.rest.body.VinQueryResponse;
import com.waylens.hachi.rest.response.CloudStorageInfo;
import com.waylens.hachi.rest.response.CreateMomentResponse;
import com.waylens.hachi.service.upload.UploadAPI;
import com.waylens.hachi.service.upload.UploadProgressListener;
import com.waylens.hachi.service.upload.UploadProgressRequestBody;
import com.waylens.hachi.service.upload.rest.body.InitUploadBody;
import com.waylens.hachi.service.upload.rest.response.InitUploadResponse;
import com.waylens.hachi.service.upload.rest.response.UploadDataResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.utils.StringUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by Xiaofei on 2016/9/6.
 */
public class UploadCachedMomentJob extends UploadMomentJob {
    private static final String TAG = UploadCachedMomentJob.class.getSimpleName();


    public UploadCachedMomentJob(LocalMoment moment) {
        super(new Params(0).requireNetwork().setGroupId("uploadCacheMoment").setPersistent(true));
        this.mLocalMoment = moment;
        mLocalMoment.cache = false;

    }


    @Override
    public void onAdded() {
        Logger.t(TAG).d("on added");

    }


    @Override
    public void onRun() throws Throwable {
        Logger.t(TAG).d("on run " + getId());

        EventBus.getDefault().post(new UploadEvent(UploadEvent.UPLOAD_JOB_ADDED, this));
        mState = UploadMomentJob.UPLOAD_STATE_WAITING_FOR_NETWORK_AVAILABLE;


        while (true) {
            boolean cloudAvailabe = checkCloudStorageAvailable();
            if (cloudAvailabe) {
                break;
            } else {
                Thread.sleep(10000);
            }
        }

        if (mLocalMoment.mVehicleMaker == null && mLocalMoment.vin != null) {
            HachiApi hachiApi = HachiService.createHachiApiService();
            Call<VinQueryResponse> vinQueryResponseCall = hachiApi.queryByVin(mLocalMoment.vin);
            Response<VinQueryResponse> response = vinQueryResponseCall.execute();
            VinQueryResponse vinQueryResponse = response.body();
            Logger.t(TAG).d(response.code() + response.message());
            if (vinQueryResponse != null) {
                mLocalMoment.mVehicleMaker = vinQueryResponse.makerName;
                mLocalMoment.mVehicleModel = vinQueryResponse.modelName;
                mLocalMoment.mVehicleYear = vinQueryResponse.year;
                Logger.t(TAG).d("vin query response:" + vinQueryResponse.makerName + vinQueryResponse.modelName + vinQueryResponse.year);
            }
        }

        try {

            CreateMomentResponse response = getCloudInfo();
            Logger.t(TAG).d("upload server: " + response.uploadServer.toString());
            mLocalMoment.updateUploadInfo(response);
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss");
            String date = format.format(System.currentTimeMillis()) + " GMT";

            String server = StringUtils.getHostNameWithoutPrefix(response.uploadServer.url);
            Logger.t(TAG).d("server: " + server);

            final String authorization = HachiAuthorizationHelper.getAuthoriztion(server,
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

            for (LocalMoment.Segment segment : mLocalMoment.mSegments) {
                URI uri = URI.create(segment.uploadURL.url);
                File file = new File(uri);
                file.delete();
            }

            File file = new File(mLocalMoment.thumbnailPath);
            file.delete();
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
            CreateMomentBody createMomentBody = new CreateMomentBody(mLocalMoment);
            Call<CreateMomentResponse> createMomentResponseCall = hachiApi.createMoment(createMomentBody);

            CreateMomentResponse response = createMomentResponseCall.execute().body();

            return response;

        } catch (Exception e) {
            Logger.t(TAG).d("failed to fetch moment body");
            return null;
        }
    }


    private boolean checkCloudStorageAvailable() {
        try {
            HachiApi hachiApi = HachiService.createHachiApiService(10, TimeUnit.SECONDS);
            Call<CloudStorageInfo> createMomentResponseCall = hachiApi.getCloudStorageInfo();
            CloudStorageInfo cloudStorageInfo = createMomentResponseCall.execute().body();
            int clipLength = 0;
            for (LocalMoment.Segment segment : mLocalMoment.mSegments) {
                clipLength += segment.uploadURL.lengthMs;
            }

            Logger.t(TAG).d("used: " + cloudStorageInfo.current.durationUsed + "total: " + cloudStorageInfo.current.plan.durationQuota);
            if (cloudStorageInfo.current.durationUsed + clipLength > cloudStorageInfo.current.plan.durationQuota) {
                mError = UPLOAD_ERROR_UPLOAD_EXCEED;
                return false;
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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
