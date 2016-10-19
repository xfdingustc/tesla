package com.waylens.hachi.bgjob.upload;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.CreateMomentBody;
import com.waylens.hachi.rest.response.GeoInfoResponse;
import com.waylens.hachi.rest.response.VinQueryResponse;
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
import java.util.Locale;
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
        HachiApi hachiApi = HachiService.createHachiApiService();
        if (mLocalMoment.withCarInfo && mLocalMoment.vin != null && mLocalMoment.mVehicleMaker == null) {

            Call<VinQueryResponse> vinQueryResponseCall = hachiApi.queryByVin(mLocalMoment.vin);
            Response<VinQueryResponse> response = vinQueryResponseCall.execute();
            if (response.isSuccessful()) {
                VinQueryResponse vinQueryResponse = response.body();
                Logger.t(TAG).d(response.code() + response.message());
                if (vinQueryResponse != null) {
                    mLocalMoment.mVehicleMaker = vinQueryResponse.makerName;
                    mLocalMoment.mVehicleModel = vinQueryResponse.modelName;
                    mLocalMoment.mVehicleYear = vinQueryResponse.year;
                    Logger.t(TAG).d("vin query response:" + vinQueryResponse.makerName + vinQueryResponse.modelName + vinQueryResponse.year);
                }
            }
        }

        if (mLocalMoment.withGeoTag && mLocalMoment.geoInfo.country == null) {
            Call<GeoInfoResponse> geoInfoResponseCall = hachiApi.getGeoInfo(mLocalMoment.geoInfo.longitude, mLocalMoment.geoInfo.latitude);
            Response<GeoInfoResponse> response = geoInfoResponseCall.execute();
            if (response.isSuccessful()) {
                mLocalMoment.geoInfo.city = response.body().city;
                mLocalMoment.geoInfo.country = response.body().country;
                mLocalMoment.geoInfo.region = response.body().region;
            }
        }

        try {

            CreateMomentResponse response = getCloudInfo();
            Logger.t(TAG).d("upload server: " + response.uploadServer.toString());
            mLocalMoment.updateUploadInfo(response);
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss", Locale.US);
            String date = format.format(System.currentTimeMillis()) + " GMT";

            String server = StringUtils.getHostNameWithoutPrefix(response.uploadServer.url);
            Logger.t(TAG).d("server: " + server);

            final String authorization = HachiAuthorizationHelper.getAuthoriztion(server,
                SessionManager.getInstance().getUserId() + "/android",
                mLocalMoment.momentID,
                date,
                response.uploadServer.privateKey);

            UploadAPI uploadAPI = new UploadAPI(response.uploadServer.url + "/", date, authorization, -1);

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

            // Step3: upload videoThumbnail;
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
            setUploadState(UPLOAD_STATE_ERROR, UPLOAD_ERROR_MALFORMED_DATA);
        }

        if (mState != UPLOAD_STATE_CANCELLED && mState != UPLOAD_STATE_ERROR) {
            Logger.t(TAG).d("finished " + mState);
            setUploadState(UPLOAD_STATE_FINISHED);

            for (LocalMoment.Segment segment : mLocalMoment.mSegments) {
                URI uri = URI.create(segment.uploadURL.url);
                File file = new File(uri);
                file.delete();
            }

            File file = new File(mLocalMoment.thumbnailPath);
            file.delete();
            EventBus.getDefault().post(new UploadEvent(UploadEvent.UPLOAD_JOB_REMOVED, this));
            fireNotification(getApplicationContext().getString(R.string.upload_success));
            Toast.makeText(Hachi.getContext(), R.string.upload_success, Toast.LENGTH_LONG).show();
        } else {
            fireNotification(getApplicationContext().getString(R.string.upload_failed));
            Toast.makeText(Hachi.getContext(), R.string.upload_failed, Toast.LENGTH_LONG).show();
        }


    }


    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return new RetryConstraint(false);
    }

    private CreateMomentResponse getCloudInfo() throws IOException {

        HachiApi hachiApi = HachiService.createHachiApiService(10, TimeUnit.SECONDS);
        CreateMomentBody createMomentBody = new CreateMomentBody(mLocalMoment);
        Call<CreateMomentResponse> createMomentResponseCall = hachiApi.createMoment(createMomentBody);

        CreateMomentResponse response = createMomentResponseCall.execute().body();

        return response;

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

    private void fireNotification(String msg) {
        Context context = getApplicationContext();
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap largeBitmap =  BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_app);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_notification)
                .setColor(context.getResources().getColor(R.color.material_deep_orange_500))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(msg)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setLargeIcon(largeBitmap);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(1, notificationBuilder.build());
    }


}
