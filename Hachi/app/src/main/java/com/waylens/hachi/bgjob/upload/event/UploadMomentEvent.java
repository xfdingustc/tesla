package com.waylens.hachi.bgjob.upload.event;

import com.waylens.hachi.bgjob.export.statejobqueue.UploadMomentJob;

/**
 * Created by Xiaofei on 2016/4/28.
 */
public class UploadMomentEvent {

    public static final int UPLOAD_JOB_ADDED = 0;
    public static final int UPLOAD_JOB_STATE_CHANGED = 1;
    public static final int UPLOAD_JOB_REMOVED = 2;


    private final int mWhat;
    private final UploadMomentJob mUploadable;


    public UploadMomentEvent(int what) {
        this(what, null);
    }

    public UploadMomentEvent(int what, UploadMomentJob job) {
        this.mWhat = what;
        this.mUploadable = job;
    }

    public int getWhat() {
        return mWhat;
    }

    public UploadMomentJob getUploadable() {
        return mUploadable;
    }
}
