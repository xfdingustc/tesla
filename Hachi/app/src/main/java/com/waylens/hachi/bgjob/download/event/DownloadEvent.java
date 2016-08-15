package com.waylens.hachi.bgjob.download.event;

import com.waylens.hachi.bgjob.download.DownloadJob;

/**
 * Created by Xiaofei on 2016/5/5.
 */
public class DownloadEvent {
    public static final int DOWNLOAD_WHAT_START = 1;
    public static final int DOWNLOAD_WHAT_PROGRESS = 2;
    public static final int DOWNLOAD_WHAT_FINISHED = 3;
    public static final int DOWNLOAD_WHAT_ERROR = 4;
    public static final int DOWNLOAD_WHAT_CANCELLED = 5;

//    public static final int UPLOAD_ERROR_UNKNOWN = 0x100;
//    public static final int UPLOAD_ERROR_LOGIN = 0x101;
//    public static final int UPLOAD_ERROR_CREATE_MOMENT_DESC = 0x102;
//    public static final int UPLOAD_ERROR_UPLOAD_VIDEO = 0x103;
//    public static final int UPLOAD_ERROR_UPLOAD_THUMBNAIL = 0x104;
//    public static final int UPLOAD_ERROR_IO = 0x105;


    private final int mWhat;
    private final DownloadJob mJob;


    public DownloadEvent(int what) {
        this(what, null);
    }


    public DownloadEvent(int what, DownloadJob job) {
        this.mWhat = what;
        this.mJob = job;
    }

    public int getWhat() {
        return mWhat;
    }

    public DownloadJob getJob() {
        return mJob;
    }
}
