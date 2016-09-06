package com.waylens.hachi.bgjob.upload;

import com.waylens.hachi.ui.entities.LocalMoment;

/**
 * Created by Xiaofei on 2016/9/6.
 */
public interface IUploadable {

    int UPLOAD_STATE_NONE = 0;
    int UPLOAD_STATE_GET_URL_INFO = 1;
    int UPLOAD_STATE_GET_VIDEO_COVER = 2;
    int UPLOAD_STATE_STORE_VIDEO_COVER = 3;
    int UPLOAD_STATE_CREATE_MOMENT = 4;
    int UPLOAD_STATE_LOGIN = 5;
    int UPLOAD_STATE_LOGIN_SUCCEED = 6;
    int UPLOAD_STATE_START = 7;
    int UPLOAD_STATE_PROGRESS = 8;
    int UPLOAD_STATE_FINISHED = 9;
    int UPLOAD_STATE_ERROR = 10;
    int UPLOAD_STATE_CANCELLED = 11;
    int UPLOAD_STATE_WAITING_FOR_NETWORK_AVAILABLE = 12;


    int UPLOAD_ERROR_UNKNOWN = 0x100;
    int UPLOAD_ERROR_LOGIN = 0x101;
    int UPLOAD_ERROR_CREATE_MOMENT_DESC = 0x102;
    int UPLOAD_ERROR_UPLOAD_VIDEO = 0x103;
    int UPLOAD_ERROR_UPLOAD_THUMBNAIL = 0x104;
    int UPLOAD_ERROR_IO = 0x105;

    String getJobId();

    int getState();

    int getUploadProgress();

    int getUploadError();

    LocalMoment getLocalMoment();

    void cancelUpload();
}
