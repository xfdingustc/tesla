package com.waylens.hachi.ui.community;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.bgjob.upload.HachiAuthorizationHelper;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.IHachiApi;
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



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import rx.Observable;
import rx.Subscriber;

/**
 * Created by Xiaofei on 2017/1/5.
 */

public class PictureUploader {
    private static final String TAG = PictureUploader.class.getSimpleName();
    private final String mPictureUrl;
    private final String mTitle;
    private int mUploadProgress;

    public PictureUploader(String title, String pictureUrl) {
        this.mTitle = title;
        this.mPictureUrl = pictureUrl;
    }

    public Observable<Integer> uploadPictureRx() {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                try {
                    doUploadPicture(subscriber);
                } catch (Exception e) {
                    Observable.error(e);
                }

            }
        });
    }

    private void doUploadPicture(final Subscriber<? super Integer> subscriber) throws IOException, NoSuchAlgorithmException {
        mUploadProgress = 0;
        IHachiApi hachiApi = HachiService.createHachiApiService();
        CreateMomentBody createMomentBody = new CreateMomentBody();
        createMomentBody.title = mTitle;
        createMomentBody.momentType = "PICTURE";
        createMomentBody.accessLevel = "PUBLIC";
        Call<CreateMomentResponse> createMomentResponseCall = hachiApi.createMoment(createMomentBody);


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
                if (Math.abs(progress - mUploadProgress) >= 2) {
                    mUploadProgress = progress;
                    subscriber.onNext(progress);
                }
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
            subscriber.onCompleted();
        } else {
            subscriber.onError(new IOException());
        }


        if (jpeg != null) {
            jpeg.delete();
        }


    }


}
