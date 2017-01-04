package com.waylens.hachi.uploadqueue;

import android.content.Context;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.bgjob.upload.HachiAuthorizationHelper;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.IHachiApi;
import com.waylens.hachi.rest.body.CreateMomentBody;
import com.waylens.hachi.rest.response.CloudStorageInfo;
import com.waylens.hachi.rest.response.CreateMomentResponse;
import com.waylens.hachi.rest.response.GeoInfoResponse;
import com.waylens.hachi.rest.response.VinQueryResponse;
import com.waylens.hachi.service.upload.UploadAPI;
import com.waylens.hachi.service.upload.UploadProgressListener;
import com.waylens.hachi.service.upload.UploadProgressRequestBody;
import com.waylens.hachi.service.upload.rest.body.InitUploadBody;
import com.waylens.hachi.service.upload.rest.response.InitUploadResponse;
import com.waylens.hachi.service.upload.rest.response.UploadDataResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.uploadqueue.db.UploadQueueDBAdapter;
import com.waylens.hachi.uploadqueue.interfaces.UploadResponseListener;
import com.waylens.hachi.uploadqueue.model.UploadError;
import com.waylens.hachi.uploadqueue.model.UploadRequest;
import com.waylens.hachi.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by Xiaofei on 2016/12/30.
 */

public class UploadQueueUploader extends Thread {
    private static final String TAG = UploadQueueUploader.class.getSimpleName();
    private final Context mContext;
    private final UploadRequest mRequest;
    private final UploadResponseListener mReponseListener;

    private UploadError uploadError = UploadError.UNABLE_TO_UPLOAD_FILE;

    private boolean mResult = false;

    private boolean mStopUploading = false;

    public UploadQueueUploader(Context context, UploadRequest request, UploadResponseListener listener) {
        this.mContext = context;
        this.mRequest = request;
        this.mReponseListener = listener;
        request.currentError = UploadError.NO_ERROR;
    }


    @Override
    public void run() {
        super.run();
        onPreExecute();

        if (!UploadManager.isConfiguredNetworkAvailable(mContext)) {
            uploadError = UploadError.NETWORK_WEAK;
            mResult = false;
        }

        mRequest.setUploading(true);


        LocalMoment localMoment = mRequest.getLocalMoment();

        try {
            boolean cloudAvailabe = checkCloudStorageAvailable(localMoment);
            if (cloudAvailabe) {
                doUploadLocalMoment(localMoment);
            }
        } catch (IOException e) {
            e.printStackTrace();
            mResult = false;
            uploadError = UploadError.CONNECTION_TIMEOUT;
        } catch (InterruptedException e) {
            e.printStackTrace();
            mResult = false;
        }

        onPostExecute(mResult, localMoment);
    }

    public void cancel() {
        mStopUploading = true;

    }


    private boolean checkCloudStorageAvailable(LocalMoment localMoment) throws IOException, InterruptedException {
        checkIfStopped();
        IHachiApi hachiApi = HachiService.createHachiApiService(10, TimeUnit.SECONDS);
        Call<CloudStorageInfo> createMomentResponseCall = hachiApi.getCloudStorageInfo();
        CloudStorageInfo cloudStorageInfo = createMomentResponseCall.execute().body();
        int clipLength = 0;
        for (LocalMoment.Segment segment : localMoment.mSegments) {
            clipLength += segment.uploadURL.lengthMs;
        }

        Logger.t(TAG).d("used: " + cloudStorageInfo.current.durationUsed + "total: " + cloudStorageInfo.current.plan.durationQuota);
        if (cloudStorageInfo.current.durationUsed + clipLength > cloudStorageInfo.current.plan.durationQuota) {
            uploadError = UploadError.CLOUD_STORAGE_NOT_AVAILABLE;
            return false;
        }

        return true;

    }

    private void doUploadLocalMoment(LocalMoment localMoment) throws IOException, InterruptedException {
        Logger.t(TAG).d("do upload local moment");
        checkIfStopped();
        UploadQueueDBAdapter dbAdapter = UploadQueueDBAdapter.getInstance();
        IHachiApi hachiApi = HachiService.createHachiApiService();
        if (localMoment.withCarInfo && localMoment.vin != null && localMoment.mVehicleMaker == null) {
            Call<VinQueryResponse> vinQueryResponseCall = hachiApi.queryByVin(localMoment.vin);
            Response<VinQueryResponse> response = vinQueryResponseCall.execute();
            if (response.isSuccessful()) {
                VinQueryResponse vinQueryResponse = response.body();
                Logger.t(TAG).d(response.code() + response.message());
                if (vinQueryResponse != null) {
                    localMoment.mVehicleMaker = vinQueryResponse.makerName;
                    localMoment.mVehicleModel = vinQueryResponse.modelName;
                    localMoment.mVehicleYear = vinQueryResponse.year;
                    Logger.t(TAG).d("vin query response:" + vinQueryResponse.makerName + vinQueryResponse.modelName + vinQueryResponse.year);
                    dbAdapter.updateRequest(mRequest);
                }
            }
        }

        checkIfStopped();
        if (localMoment.withGeoTag && localMoment.geoInfo.country == null) {
            Call<GeoInfoResponse> geoInfoResponseCall = hachiApi.getGeoInfo(localMoment.geoInfo.longitude, localMoment.geoInfo.latitude);
            Response<GeoInfoResponse> response = geoInfoResponseCall.execute();
            if (response.isSuccessful()) {
                localMoment.geoInfo.city = response.body().city;
                localMoment.geoInfo.country = response.body().country;
                localMoment.geoInfo.region = response.body().region;
                dbAdapter.updateRequest(mRequest);
            }
        }

        checkIfStopped();
        if (localMoment.cloudInfo == null) {
            CreateMomentResponse response = createMoment(localMoment);
            Logger.t(TAG).d("upload server: " + response.uploadServer.toString());
            localMoment.updateUploadInfo(response);
            dbAdapter.updateRequest(mRequest);
        }

        Logger.t(TAG).d("cloud info: " + localMoment.cloudInfo.toString());
        Logger.t(TAG).d("momentid: " + localMoment.momentID);

        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss", Locale.US);
        String date = format.format(System.currentTimeMillis()) + " GMT";

        String server = StringUtils.getHostNameWithoutPrefix(localMoment.cloudInfo.url);
        Logger.t(TAG).d("server: " + server);

        checkIfStopped();
        final String authorization = HachiAuthorizationHelper.getAuthoriztion(server,
            SessionManager.getInstance().getUserId() + "/android",
            localMoment.momentID,
            date,
            localMoment.cloudInfo.privateKey);

        UploadAPI uploadAPI = new UploadAPI(localMoment.cloudInfo.url + "/", date, authorization, -1);


        checkIfStopped();
        // Step 1: init upload;
        InitUploadResponse initUploadResponse = uploadAPI.initUploadSync(localMoment.momentID, InitUploadBody.fromLocalMoment(localMoment));

        if (initUploadResponse == null) {
            Logger.t(TAG).d("Failed to init upload");
            return;
        }


        // Step2: upload segments;

        Logger.t(TAG).d("initUploadResponse: " + initUploadResponse.toString());

        final int totalSegments = localMoment.mSegments.size();

        for (int i = 0; i < localMoment.mSegments.size(); i++) {
            LocalMoment.Segment segment = localMoment.mSegments.get(i);
            final int index = i;

            URI uri = URI.create(segment.uploadURL.url);
            File file = new File(uri);

            checkIfStopped();
            UploadProgressRequestBody newRequest = UploadProgressRequestBody.newInstance(file, new UploadProgressListener() {
                @Override
                public void update(long bytesWritten, long contentLength, boolean done) {
                    updateUploadProgress(bytesWritten, contentLength, index, totalSegments);
                }
            });

            UploadDataResponse uploadDataResponse = uploadAPI.uploadChunkSync(newRequest, localMoment.momentID, segment);

            Logger.t(TAG).d("uploadDataResponse: " + uploadDataResponse);
        }

        // Step3: upload videoThumbnail;
        checkIfStopped();
        UploadProgressRequestBody newRequest = UploadProgressRequestBody.newInstance(new File(localMoment.thumbnailPath), new UploadProgressListener() {
            @Override
            public void update(long bytesWritten, long contentLength, boolean done) {
                updateUploadProgress(bytesWritten, contentLength, totalSegments, totalSegments);
            }
        });
        UploadDataResponse uploadDataResponse = uploadAPI.uploadThumbnail(newRequest, localMoment.momentID);

        Logger.t(TAG).d("uploadThmbnailResponse: " + uploadDataResponse);

        uploadAPI.finishUpload(localMoment.momentID);

        Logger.t(TAG).d("upload finished ");

/*
        if (mState != UPLOAD_STATE_CANCELLED && mState != UPLOAD_STATE_ERROR) {
//            Logger.t(TAG).d("finished " + mState);
            setUploadState(UPLOAD_STATE_FINISHED);


        } */
        mResult = true;

    }

    private void checkIfStopped() throws InterruptedException {
        if (mStopUploading) {
            mReponseListener.onComplete(mRequest.getKey());
            throw new InterruptedException();
        }
    }

    private void updateUploadProgress(long byteWritten, long contentLength, int index, int totalSegment) {
        if (index < totalSegment) {
            int progress = (int) ((byteWritten * 100) / contentLength);
            int percentageInThisClip = progress / totalSegment;
            int percentage = index * 100 / totalSegment + percentageInThisClip;
            percentage = percentage * 9 / 10;
            mReponseListener.updateProgress(mRequest.getKey(), percentage);
        } else {
            int progress = (int) ((byteWritten * 10) / contentLength);
            mReponseListener.updateProgress(mRequest.getKey(), progress + 90);
        }

    }

    private CreateMomentResponse createMoment(LocalMoment localMoment) throws IOException {

        IHachiApi hachiApi = HachiService.createHachiApiService(10, TimeUnit.SECONDS);
        CreateMomentBody createMomentBody = new CreateMomentBody(localMoment);
        Call<CreateMomentResponse> createMomentResponseCall = hachiApi.createMoment(createMomentBody);

        CreateMomentResponse response = createMomentResponseCall.execute().body();

        return response;

    }

    private void onPreExecute() {
        uploadError = UploadError.UNABLE_TO_UPLOAD_FILE;
        if (mReponseListener != null) {
            mReponseListener.onUploadStart(mRequest.getKey());
        }
    }

    private void onPostExecute(boolean uploadComplete, LocalMoment localMoment) {
        mRequest.setUploading(false);

        if (uploadComplete || mStopUploading) {
            mReponseListener.onComplete(mRequest.getKey());
            for (LocalMoment.Segment segment : localMoment.mSegments) {
                URI uri = URI.create(segment.uploadURL.url);
                File file = new File(uri);
                file.delete();
            }

            File file = new File(localMoment.thumbnailPath);
            file.delete();
        } else {
            mReponseListener.onError(mRequest.getKey(), uploadError);
        }
    }
}
