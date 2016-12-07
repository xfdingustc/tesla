package com.waylens.hachi.bgjob.export.statejobqueue;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.bgjob.upload.HachiAuthorizationHelper;
import com.waylens.hachi.bgjob.upload.event.UploadMomentEvent;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.jobqueue.Params;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.IHachiApi;
import com.waylens.hachi.rest.body.CreateMomentBody;
import com.waylens.hachi.rest.response.CloudStorageInfo;
import com.waylens.hachi.rest.response.CreateMomentResponse;
import com.waylens.hachi.rest.response.GeoInfoResponse;
import com.waylens.hachi.rest.response.VinQueryResponse;
import com.waylens.hachi.service.download.DownloadAPI;
import com.waylens.hachi.service.download.DownloadProgressListener;
import com.waylens.hachi.service.upload.UploadAPI;
import com.waylens.hachi.service.upload.UploadProgressListener;
import com.waylens.hachi.service.upload.UploadProgressRequestBody;
import com.waylens.hachi.service.upload.rest.body.InitUploadBody;
import com.waylens.hachi.service.upload.rest.response.InitUploadResponse;
import com.waylens.hachi.service.upload.rest.response.UploadDataResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.toolbox.ClipUploadUrlRequest;
import com.waylens.hachi.snipe.toolbox.VdbImageRequest;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipPos;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.urls.UploadUrl;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.utils.FileUtils;
import com.waylens.hachi.utils.StringUtils;
import org.greenrobot.eventbus.EventBus;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import retrofit2.Call;
import retrofit2.Response;


/**
 * Created by lshw on 16/11/22.
 */

public class CacheUploadMomentJob extends UploadMomentJob {
    public static String TAG = CacheUploadMomentJob.class.getSimpleName();

    private static final int VIDIT_RAW_DATA = 1;
    private static final int VIDIT_VIDEO_DATA_LOW = 64;

    private static final int DEFAULT_DATA_TYPE_SD = VdbCommand.Factory.UPLOAD_GET_V1 | VdbCommand.Factory.UPLOAD_GET_RAW;
    private static final int DEFAULT_DATA_TYPE_FULLHD = VdbCommand.Factory.UPLOAD_GET_V0 | VdbCommand.Factory.UPLOAD_GET_RAW;
    private static final int DEFAULT_DATA_TYPE_CLOUD = VIDIT_VIDEO_DATA_LOW | VIDIT_RAW_DATA;

    private transient JobCallback mJobCallback;

    private String jobId;

    private transient VdbRequestQueue mVdbRequestQueue;
    public CacheUploadMomentJob(LocalMoment localMoment) {
        super(new Params(1));
        this.jobId = UUID.randomUUID().toString();
        setSealed(true);
        this.mLocalMoment = localMoment;
    }

    public void setJobCallback(JobCallback callback) {
        this.mJobCallback = callback;
    }

    @Override
    protected void setUploadState(int state, int progress) {
        super.setUploadState(state, progress);
        if (mJobCallback != null) {
            mJobCallback.updateProgress(this);
        }
    }

    @Override
    protected void setUploadState(int state) {
        setUploadState(state, 0);
    }


    @Override
    public String getId() {
        return jobId;
    }

    @Override
    public void onAdded() {
        Logger.t(TAG).d("on Added");
    }

    @Override
    public void onRun(int state) throws Throwable{
        if (state == 0) {
            try {
                cacheMoment();
            } catch (Throwable e) {
                if (mJobCallback != null) {
                    mJobCallback.onFailure(this);
                }
            }
            //change state
            if (mJobCallback != null) {
                mJobCallback.updateJob(this);
            }
        }
        mLocalMoment.cache = false;
        uploadMoment();
        if (mState != UPLOAD_STATE_CANCELLED || mState != UPLOAD_STATE_ERROR) {
            Logger.t(TAG).d("finished");
            setUploadState(UPLOAD_STATE_FINISHED);
        }

        EventBus.getDefault().post(new UploadMomentEvent(UploadMomentEvent.UPLOAD_JOB_REMOVED, this));

    }

    private void cacheMoment() throws Throwable{
        Logger.t(TAG).d("on Run, playlistId: " + mLocalMoment.playlistId);
        mVdbRequestQueue = VdtCameraManager.getManager().getCurrentCamera().getRequestQueue();
        // Step1:  get playlist info:
        VdbRequestFuture<ClipSet> clipSetRequestFuture = VdbRequestFuture.newFuture();
        ClipSetExRequest request = new ClipSetExRequest(mLocalMoment.playlistId, ClipSetExRequest.FLAG_CLIP_EXTRA | ClipSetExRequest.FLAG_CLIP_DESC, clipSetRequestFuture, clipSetRequestFuture);
        mVdbRequestQueue.add(request);
        ClipSet playlistClipSet = clipSetRequestFuture.get();
        Logger.t(TAG).d("Play list info got, clip set size:  " + playlistClipSet.getCount());

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
            if (mLocalMoment.streamId == 0) {
                parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, DEFAULT_DATA_TYPE_SD);
            } else {
                parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, DEFAULT_DATA_TYPE_FULLHD);
            }
            ClipUploadUrlRequest uploadUrlRequest = new ClipUploadUrlRequest(clip.cid, parameters, uploadUrlRequestFuture, uploadUrlRequestFuture);
            mVdbRequestQueue.add(uploadUrlRequest);

            UploadUrl uploadUrl = uploadUrlRequestFuture.get();
            Logger.t(TAG).d("Got clip upload url: " + uploadUrl.url);
            LocalMoment.Segment segment = new LocalMoment.Segment(clip, uploadUrl, DEFAULT_DATA_TYPE_CLOUD);
            mLocalMoment.mSegments.add(segment);

        }
        setUploadState(UPLOAD_STATE_GET_URL_INFO);

        // Step3: get videoThumbnail:
        Logger.t(TAG).d("Try to get videoThumbnail");
        final Clip firstClip = playlistClipSet.getClip(0);
        ClipPos clipPos = new ClipPos(firstClip.getVdbId(), firstClip.cid, firstClip.getClipDate(), firstClip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
        VdbRequestFuture<Bitmap> thumbnailRequestFuture = VdbRequestFuture.newFuture();
        VdbImageRequest imageRequest = new VdbImageRequest(clipPos, thumbnailRequestFuture, thumbnailRequestFuture);
        mVdbRequestQueue.add(imageRequest);
        Bitmap thumbnail = thumbnailRequestFuture.get();
        Logger.t(TAG).d("Got videoThumbnail");
        setUploadState(UPLOAD_STATE_GET_VIDEO_COVER);

        // Step4: Store videoThumbnail:
        File cacheDir = Hachi.getContext().getExternalCacheDir();
        File file = new File(cacheDir, "t" + firstClip.getStartTimeMs() + ".jpg");
        FileOutputStream fos = new FileOutputStream(file);
        thumbnail.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, fos);
        mLocalMoment.thumbnailPath = file.getAbsolutePath();
        Logger.t(TAG).d("Saved videoThumbnail: " + mLocalMoment.thumbnailPath);
        setUploadState(UPLOAD_STATE_STORE_VIDEO_COVER);

        for (int i = 0; i < mLocalMoment.mSegments.size(); i++) {
            LocalMoment.Segment segment = mLocalMoment.mSegments.get(i);
            downloadMomentFiles(segment, i, mLocalMoment.mSegments.size());
        }
    }


    private void downloadMomentFiles(LocalMoment.Segment segment, final int index, final int totalSegments) {
        DownloadProgressListener listener = new DownloadProgressListener() {
            @Override
            public void update(long bytesRead, long contentLength, boolean done) {
                int progress;
                progress = (int) ((bytesRead * 100) / contentLength);
                int percentageInThisClip = progress / totalSegments;
                int percentage = index * 100 / totalSegments + percentageInThisClip;
                setUploadState(CacheUploadMomentJob.UPLOAD_STATE_PROGRESS, percentage);

            }
        };

        String file = FileUtils.genMomentCacheFileName(StringUtils.getFileName(segment.uploadURL.url));
        File outputFile = new File(file);
        Logger.t(TAG).d("output file: " + outputFile);
        String baseUrl = StringUtils.getHostName(segment.uploadURL.url);

        try {
            InputStream inputStream = new DownloadAPI(baseUrl, listener).downloadFileSync(segment.uploadURL.url);
            if (inputStream != null) {
                FileUtils.writeFile(inputStream, outputFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        segment.uploadURL.url = Uri.fromFile(outputFile).toString();
    }

    private void uploadMoment() throws Throwable {
        while (true) {
            boolean cloudAvailable = checkCloudStorageAvailable();
            if (cloudAvailable) {
                break;
            } else {
                Thread.sleep(10000);
            }
        }

        IHachiApi hachiApi = HachiService.createHachiApiService();
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
            Logger.t(TAG).d("cloud info = " + response.toString());
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
//            Logger.t(TAG).d("finished " + mState);
            setUploadState(UPLOAD_STATE_FINISHED);
            if (mJobCallback != null) {
                mJobCallback.onSuccess(this);
            }
            for (LocalMoment.Segment segment : mLocalMoment.mSegments) {
                URI uri = URI.create(segment.uploadURL.url);
                File file = new File(uri);
                file.delete();
            }
            File file = new File(mLocalMoment.thumbnailPath);
            file.delete();
            EventBus.getDefault().post(new UploadMomentEvent(UploadMomentEvent.UPLOAD_JOB_REMOVED, this));
            fireNotification(getApplicationContext().getString(R.string.upload_success));
            Toast.makeText(Hachi.getContext(), R.string.upload_success, Toast.LENGTH_LONG).show();
        } else {
            fireNotification(getApplicationContext().getString(R.string.upload_failed));
            Toast.makeText(Hachi.getContext(), R.string.upload_failed, Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkCloudStorageAvailable() {
        try {
            IHachiApi hachiApi = HachiService.createHachiApiService(10, TimeUnit.SECONDS);
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

    private CreateMomentResponse getCloudInfo() throws IOException {

        IHachiApi hachiApi = HachiService.createHachiApiService(10, TimeUnit.SECONDS);
        CreateMomentBody createMomentBody = new CreateMomentBody(mLocalMoment);
        Call<CreateMomentResponse> createMomentResponseCall = hachiApi.createMoment(createMomentBody);
        CreateMomentResponse response = createMomentResponseCall.execute().body();
        return response;

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

    private void updateUploadProgress(long byteWritten, long contentLength, int index, int totalSegment) {
        int progress = (int) ((byteWritten * 100) / contentLength);


        int percentageInThisClip = progress / totalSegment;
        int percentage = index * 100 / totalSegment + percentageInThisClip;
        setUploadState(UPLOAD_STATE_PROGRESS, percentage);
    }

    @Override
    public void cancelUpload() {

    }
}
