package com.waylens.hachi.bgjob.upload.event;

import com.waylens.hachi.bgjob.upload.IUploadable;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;

/**
 * Created by Xiaofei on 2016/4/28.
 */
public class UploadEvent {
//    public static final int UPLOAD_WHAT_LOGIN = 0;
//    public static final int UPLOAD_WHAT_LOGIN_SUCCEED = 1;
//    public static final int UPLOAD_WHAT_START = 1;
//    public static final int UPLOAD_WHAT_PROGRESS = 2;
//    public static final int UPLOAD_WHAT_FINISHED = 3;
//    public static final int UPLOAD_WHAT_ERROR = 4;
//    public static final int UPLOAD_WHAT_CANCELLED = 5;
//
//    public static final int UPLOAD_ERROR_UNKNOWN = 0x100;
//    public static final int UPLOAD_ERROR_LOGIN = 0x101;
//    public static final int UPLOAD_ERROR_CREATE_MOMENT_DESC = 0x102;
//    public static final int UPLOAD_ERROR_UPLOAD_VIDEO = 0x103;
//    public static final int UPLOAD_ERROR_UPLOAD_THUMBNAIL = 0x104;
//    public static final int UPLOAD_ERROR_IO = 0x105;


    public static final int UPLOAD_JOB_ADDED = 0;
    public static final int UPLOAD_JOB_STATE_CHANGED = 1;
    public static final int UPLOAD_JOB_REMOVED = 2;


    private final int mWhat;
    private final IUploadable mUploadable;


    public UploadEvent(int what) {
        this(what, null);
    }

    public UploadEvent(int what, IUploadable job) {
        this.mWhat = what;
        this.mUploadable = job;
    }

    public int getWhat() {
        return mWhat;
    }

    public IUploadable getUploadable() {
        return mUploadable;
    }
}
