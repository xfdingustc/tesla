package com.waylens.hachi.bgjob.upload;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;
import com.waylens.hachi.jobqueue.Params;
import com.waylens.hachi.jobqueue.RetryConstraint;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.CreateMomentBody;
import com.waylens.hachi.rest.body.FinishUploadBody;
import com.waylens.hachi.rest.response.CreateMomentResponse;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.service.upload.UploadAPI;
import com.waylens.hachi.service.upload.UploadProgressListener;
import com.waylens.hachi.service.upload.UploadProgressRequestBody;
import com.waylens.hachi.service.upload.rest.response.UploadDataResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.utils.HashUtils;
import com.waylens.hachi.utils.Hex;
import com.waylens.hachi.utils.PictureUtils;
import com.waylens.hachi.utils.StringUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;

/**
 * Created by Xiaofei on 2016/9/21.
 */

public class PictureUploadJob extends UploadMomentJob {
    private static final String TAG = PictureUploadJob.class.getSimpleName();
    private final String mTitle;
    private final String mPictureUrl;

    public PictureUploadJob(String title, String pictureUrl) {
        super(new Params(0).requireNetwork().setGroupId("uploadCacheMoment").setPersistent(true));
        this.mTitle = title;
        this.mPictureUrl = pictureUrl;
    }

    @Override
    public String getMomentTitle() {
        return mTitle;
    }

    @Override
    public void cancelUpload() {

    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        EventBus.getDefault().post(new UploadEvent(UploadEvent.UPLOAD_JOB_ADDED, this));
        HachiApi hachiApi = HachiService.createHachiApiService();
        CreateMomentBody createMomentBody = new CreateMomentBody();
        createMomentBody.title = mTitle;
        createMomentBody.momentType = "PICTURE";
        createMomentBody.accessLevel = "PUBLIC";
        Call<CreateMomentResponse> createMomentResponseCall = hachiApi.createMoment(createMomentBody);

        try {
            CreateMomentResponse response = createMomentResponseCall.execute().body();
            Logger.t(TAG).d("response: " + response.uploadServer.toString());

            File cacheDir = Hachi.getContext().getExternalCacheDir();
            File jpeg = new File(cacheDir, StringUtils.getFileName(mPictureUrl) + ".jpg");

            FileOutputStream out = new FileOutputStream(jpeg);
            //Bitmap originBitmap = PictureUtils.extractPicture(mPictureUrl);
            Bitmap originBitmap = BitmapFactory.decodeFile(mPictureUrl);
            if (originBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                out.flush();
                out.close();
            }

            String fileSha1 = Hex.encodeHexString(HashUtils.encodeSHA1(jpeg));

            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss", Locale.US);
            String date = format.format(System.currentTimeMillis()) + " GMT";
            String server = StringUtils.getHostNameWithoutPrefix(response.uploadServer.url);

            final String authorization = HachiAuthorizationHelper.getAuthoriztion(server,
                SessionManager.getInstance().getUserId() + "/android",
                response.momentID,
                fileSha1,
                "upload_picture",
                date,
                response.uploadServer.privateKey);


            UploadAPI uploadAPI = new UploadAPI(response.uploadServer.url + "/", date, authorization, -1);

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), jpeg);
            UploadProgressRequestBody progressRequestBody = new UploadProgressRequestBody(requestBody, new UploadProgressListener() {
                @Override
                public void update(long bytesWritten, long contentLength, boolean done) {
                    int progress = (int) ((bytesWritten * 100) / contentLength);

                    setUploadState(CacheMomentJob.UPLOAD_STATE_PROGRESS, progress);
                }
            });

            UploadDataResponse uploadData = uploadAPI.uploadPictureSync(progressRequestBody, response.momentID, fileSha1);

            Logger.t(TAG).d("response: " + uploadData);

            if (uploadData.result == 2) {
                FinishUploadBody uploadBody = new FinishUploadBody();
                uploadBody.momentID = response.momentID;
                uploadBody.pictureNum = 1;
                Call<SimpleBoolResponse> finishUploadResponse = hachiApi.finishUploadPictureMoment(uploadBody);
                finishUploadResponse.execute();
                EventBus.getDefault().post(new UploadEvent(UploadEvent.UPLOAD_JOB_REMOVED, this));
            } else {
                setUploadState(UPLOAD_STATE_ERROR, UPLOAD_ERROR_MALFORMED_DATA);
            }


            if (jpeg != null) {
                jpeg.delete();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public String getProgressStatus() {
        Context context = Hachi.getContext();
        return context.getString(R.string.uploaded_progress, mProgress);
    }

    @Override
    public String getThumbnail() {
        return mPictureUrl;
    }

    @Override
    public String getStateDescription() {
        Context context = Hachi.getContext();
        return context.getString(R.string.upload_start);
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
