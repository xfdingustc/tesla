package com.waylens.hachi.bgjob.upload;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.ui.entities.LocalMoment;

/**
 * Created by Xiaofei on 2016/9/1.
 */
public class MomentUploadCacher {
    private static final String TAG = MomentUploadCacher.class.getSimpleName();
    private final UploadMomentJob mUploadMomentJob;

    public MomentUploadCacher(UploadMomentJob job) {
        this.mUploadMomentJob = job;
    }


    public void cacheMoment(LocalMoment moment) {
        for (LocalMoment.Segment segment : moment.mSegments) {
            Logger.t(TAG).d("segment upload url: " + segment.uploadURL.url);
        }
    }
}
