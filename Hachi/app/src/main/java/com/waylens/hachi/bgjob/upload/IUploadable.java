package com.waylens.hachi.bgjob.upload;

import com.waylens.hachi.ui.entities.LocalMoment;

/**
 * Created by Xiaofei on 2016/9/6.
 */
public interface IUploadable {

    public static final int UPLOAD_STATE_NONE = 0;
    public static final int UPLOAD_STATE_GET_URL_INFO = 1;
    public static final int UPLOAD_STATE_GET_VIDEO_COVER = 2;
    public static final int UPLOAD_STATE_STORE_VIDEO_COVER = 3;
    public static final int UPLOAD_STATE_CREATE_MOMENT = 4;
    public static final int UPLOAD_STATE_LOGIN = 5;
    public static final int UPLOAD_STATE_LOGIN_SUCCEED = 6;
    public static final int UPLOAD_STATE_START = 7;
    public static final int UPLOAD_STATE_PROGRESS = 8;
    public static final int UPLOAD_STATE_FINISHED = 9;
    public static final int UPLOAD_STATE_ERROR = 10;
    public static final int UPLOAD_STATE_CANCELLED = 11;


    public static final int UPLOAD_ERROR_UNKNOWN = 0x100;
    public static final int UPLOAD_ERROR_LOGIN = 0x101;
    public static final int UPLOAD_ERROR_CREATE_MOMENT_DESC = 0x102;
    public static final int UPLOAD_ERROR_UPLOAD_VIDEO = 0x103;
    public static final int UPLOAD_ERROR_UPLOAD_THUMBNAIL = 0x104;
    public static final int UPLOAD_ERROR_IO = 0x105;

    String getJobId();

    int getState();

    int getUploadProgress();

    int getUploadError();

    LocalMoment getLocalMoment();

    void cancelUpload();
}
