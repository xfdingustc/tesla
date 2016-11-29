package com.waylens.hachi.bgjob.export.statejobqueue;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.bgjob.upload.HachiAuthorizationHelper;
import com.waylens.hachi.bgjob.upload.event.UploadMomentEvent;
import com.waylens.hachi.jobqueue.Params;
import com.waylens.hachi.rest.IHachiApi;
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
import com.waylens.hachi.utils.StringUtils;
import org.greenrobot.eventbus.EventBus;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;

/**
 * Created by Xiaofei on 2016/9/21.
 */

public class UploadPictureJob extends UploadMomentJob {
    private static final String TAG = UploadPictureJob.class.getSimpleName();
    private final String mTitle;
    private final String mPictureUrl;

    private transient CacheUploadMomentJob.JobCallback mJobCallback;

    private String jobId;

    public UploadPictureJob(String title, String pictureUrl) {
        super(new Params(0).requireNetwork().setGroupId("uploadCacheMoment").setPersistent(true));
        this.mTitle = title;
        this.mPictureUrl = pictureUrl;
        this.jobId = UUID.randomUUID().toString();
    }

    public void setJobCallback(JobCallback callback) {
        this.mJobCallback = callback;
    }

    @Override
    public String getMomentTitle() {
        return mTitle;
    }

    @Override
    public void cancelUpload() {

    }

    @Override
    protected void setUploadState(int state, int progress) {
        super.setUploadState(state, progress);
        if (mJobCallback != null) {
            mJobCallback.updateProgress(this);
        }
    }

    @Override
    public String getId() {
        return jobId;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun(int state) throws Throwable {
        EventBus.getDefault().post(new UploadMomentEvent(UploadMomentEvent.UPLOAD_JOB_ADDED, this));
        IHachiApi hachiApi = HachiService.createHachiApiService();
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

                    setUploadState(UploadMomentJob.UPLOAD_STATE_PROGRESS, progress);
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
                if (mJobCallback != null) {
                    mJobCallback.onSuccess(this);
                }
                EventBus.getDefault().post(new UploadMomentEvent(UploadMomentEvent.UPLOAD_JOB_REMOVED, this));
            } else {
                setUploadState(UPLOAD_STATE_ERROR, UPLOAD_ERROR_MALFORMED_DATA);
            }


            if (jpeg != null) {
                jpeg.delete();
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (mJobCallback != null) {
                mJobCallback.onFailure(this);
            }
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
}
