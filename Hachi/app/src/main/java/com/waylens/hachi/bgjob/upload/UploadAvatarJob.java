package com.waylens.hachi.bgjob.upload;

import android.support.annotation.Nullable;


import com.orhanobut.logger.Logger;
import com.waylens.hachi.bgjob.upload.event.UploadAvatarEvent;
import com.waylens.hachi.jobqueue.Job;
import com.waylens.hachi.jobqueue.Params;
import com.waylens.hachi.jobqueue.RetryConstraint;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.service.upload.UploadAPI;
import com.waylens.hachi.service.upload.rest.response.UploadDataResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.utils.HashUtils;
import com.waylens.hachi.utils.Hex;
import com.waylens.hachi.utils.StringUtils;
import com.waylens.hachi.utils.rxjava.RxBus;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Xiaofei on 2016/4/27.
 */
public class UploadAvatarJob extends Job {
    private static final String TAG = UploadAvatarJob.class.getSimpleName();

    private final String file;

    public UploadAvatarJob(String file) {
        super(new Params(0).requireNetwork().setPersistent(false));
        this.file = file;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        Logger.t(TAG).d("on Run file: " + file);


        try {
            HachiApi hachiApi = HachiService.createHachiApiService();
            UploadServer uploadServer = hachiApi.getAvatarUploadServer().execute().body().uploadServer;

            RxBus.getDefault().post(new UploadAvatarEvent(UploadAvatarEvent.UPLOAD_WHAT_START));
            Logger.t(TAG).d("get upload server: " + uploadServer.toString());


            String fileSha1 = Hex.encodeHexString(HashUtils.encodeSHA1(new File(file)));

            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss", Locale.US);
            String date = format.format(System.currentTimeMillis()) + " GMT";
            String server = StringUtils.getHostNameWithoutPrefix(uploadServer.url);

            final String authorization = HachiAuthorizationHelper.getAuthoriztion(server,
                SessionManager.getInstance().getUserId() + "/android",
                fileSha1,
                "upload_avatar",
                date,
                uploadServer.privateKey);
            RxBus.getDefault().post(new UploadAvatarEvent(UploadAvatarEvent.UPLOAD_WHAT_START));

            UploadAPI uploadAPI = new UploadAPI(uploadServer.url + "/", date, authorization, -1);

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), new File(file));

            UploadDataResponse response = uploadAPI.uploadAvatarSync(requestBody, fileSha1);

            Logger.t(TAG).d("response: " + response);


        } catch (Exception e) {
            e.printStackTrace();
        }

        RxBus.getDefault().post(new UploadAvatarEvent(UploadAvatarEvent.UPLOAD_WHAT_FINISHED));
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }


    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return new RetryConstraint(false);
    }


}
