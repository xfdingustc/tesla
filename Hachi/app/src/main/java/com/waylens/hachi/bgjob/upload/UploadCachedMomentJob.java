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

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;

/**
 * Created by Xiaofei on 2016/9/6.
 */
public class UploadCachedMomentJob extends Job implements IUploadable {
    private static final String TAG = UploadCachedMomentJob.class.getSimpleName();

    private final LocalMoment mLocalMoment;

    private int mState = UPLOAD_STATE_WAITING_FOR_NETWORK_AVAILABLE;

    private int mProgress;

    private int mError;

    public UploadCachedMomentJob(LocalMoment moment) {
        super(new Params(0).requireNetwork().setPersistent(false));
        this.mLocalMoment = moment;
    }

    @Override
    public void onAdded() {
        Logger.t(TAG).d("on added");

    }

    @Override
    public void onRun() throws Throwable {
//        Logger.t(TAG).d("on run " + mLocalMoment.toString());
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
            InitUploadResponse initUploadResponse = new UploadAPI(response.uploadServer.url + "/",
                date,
                authorization)
                .initUploadSync(mLocalMoment.momentID, InitUploadBody.fromLocalMoment(mLocalMoment));

            if (initUploadResponse != null) {
                Logger.t(TAG).d("initUploadResponse: " + initUploadResponse.toString());

                for (LocalMoment.Segment segment : mLocalMoment.mSegments) {
                    URI uri = URI.create(segment.uploadURL.url);
                    File file = new File(uri);

                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream;chartset=UTF-8"), file);

                    UploadDataResponse uploadDataResponse = new UploadAPI(response.uploadServer.url + "/",
                        date,
                        authorization)
                        .uploadChunkSync(requestBody, mLocalMoment.momentID, segment);

                    Logger.t(TAG).d("uploadDataResponse: " + uploadDataResponse);

                }

                File file = new File(mLocalMoment.thumbnailPath);
                RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream;chartset=UTF-8"), file);
                UploadDataResponse uploadDataResponse = new UploadAPI(response.uploadServer.url + "/",
                    date,
                    authorization)
                    .uploadThumbnail(requestBody, mLocalMoment.momentID);

                Logger.t(TAG).d("uploadThmbnailResponse: " + uploadDataResponse);

                new UploadAPI(response.uploadServer.url + "/",
                    date,
                    authorization)
                    .finishUpload(mLocalMoment.momentID);

                Logger.t(TAG).d("upload finished ");

            } else {
                Logger.t(TAG).d("inituploadresponse is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
    public String getJobId() {
        return getId();
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public int getUploadProgress() {
        return mProgress;
    }

    @Override
    public int getUploadError() {
        return mError;
    }

    @Override
    public LocalMoment getLocalMoment() {
        return mLocalMoment;
    }

    @Override
    public void cancelUpload() {

    }
}
