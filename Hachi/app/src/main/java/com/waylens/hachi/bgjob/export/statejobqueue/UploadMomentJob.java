package com.waylens.hachi.bgjob.export.statejobqueue;

import android.content.Context;


import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.bgjob.upload.event.UploadMomentEvent;
import com.waylens.hachi.jobqueue.Params;
import com.waylens.hachi.ui.entities.LocalMoment;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Xiaofei on 2016/9/9.
 */
public abstract class UploadMomentJob extends Job {

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
    public static final int UPLOAD_STATE_WAITING_FOR_NETWORK_AVAILABLE = 12;


    public static final int UPLOAD_ERROR_UNKNOWN = 0x100;
    public static final int UPLOAD_ERROR_LOGIN = 0x101;
    public static final int UPLOAD_ERROR_CREATE_MOMENT_DESC = 0x102;
    public static final int UPLOAD_ERROR_UPLOAD_VIDEO = 0x103;
    public static final int UPLOAD_ERROR_UPLOAD_THUMBNAIL = 0x104;
    public static final int UPLOAD_ERROR_IO = 0x105;
    public static final int UPLOAD_ERROR_MALFORMED_DATA = 0x106;
    public static final int UPLOAD_ERROR_UPLOAD_EXCEED = 0x107;

    protected LocalMoment mLocalMoment;

    protected int mState;
    protected int mProgress;
    protected int mError;
    protected boolean mIsCancel;

    protected UploadMomentJob(Params params) {
        super(params);
    }

    public String getJobId() {
        return getId();
    }

    public int getState() {
        return mState;
    }

    public LocalMoment getLocalMoment() {
        return mLocalMoment;
    }

    public int getUploadProgress() {
        return mProgress;
    }

    public int getUploadError() {
        return mError;
    }

    public String getMomentTitle() {
        return mLocalMoment.title;
    }

    public abstract void cancelUpload();

    protected void setUploadState(int state) {
        setUploadState(state, 0);
    }

    protected void setUploadState(int state, int parameter) {
        mState = state;

        switch (mState) {
            case UPLOAD_STATE_PROGRESS:
                mProgress = parameter;
                break;
            case UPLOAD_STATE_ERROR:
                mError = parameter;
                break;
            default:
                break;
        }

        //UploadManager.getManager().notifyUploadStateChanged(this);
        EventBus.getDefault().post(new UploadMomentEvent(UploadMomentEvent.UPLOAD_JOB_STATE_CHANGED, this));
    }

    public String getStateDescription() {
        Context context = Hachi.getContext();
        switch (mState) {
            case UploadMomentJob.UPLOAD_STATE_GET_URL_INFO:
                return context.getString(R.string.upload_get_url_info);
            case UploadMomentJob.UPLOAD_STATE_GET_VIDEO_COVER:
                return context.getString(R.string.upload_get_video_cover);
            case UploadMomentJob.UPLOAD_STATE_STORE_VIDEO_COVER:
                return context.getString(R.string.upload_store_video_cover);
            case UploadMomentJob.UPLOAD_STATE_CREATE_MOMENT:
                return context.getString(R.string.upload_create_moment);
            case UploadMomentJob.UPLOAD_STATE_LOGIN:
                return context.getString(R.string.upload_login);
            case UploadMomentJob.UPLOAD_STATE_LOGIN_SUCCEED:
                return context.getString(R.string.upload_login_succeed);
            case UploadMomentJob.UPLOAD_STATE_START:
            case UploadMomentJob.UPLOAD_STATE_PROGRESS:
                if (mLocalMoment.cache) {
                    return context.getString(R.string.cache_start);
                } else {
                    return context.getString(R.string.upload_start);
                }
            case UploadMomentJob.UPLOAD_STATE_CANCELLED:
                return context.getString(R.string.upload_cancelled);
            case UploadMomentJob.UPLOAD_STATE_FINISHED:
                return context.getString(R.string.upload_finished);
            case UploadMomentJob.UPLOAD_STATE_ERROR:
                return context.getString(R.string.upload_error);
            case UploadMomentJob.UPLOAD_STATE_WAITING_FOR_NETWORK_AVAILABLE:
                return context.getString(R.string.waiting_for_network);
        }
        return null;
    }

    public String getProgressStatus() {
        Context context = Hachi.getContext();
        if (mLocalMoment.cache) {
            return context.getString(R.string.downloaded_progress, mProgress);
        } else {
            return context.getString(R.string.uploaded_progress, mProgress);

        }
    }

    public String getThumbnail() {
        return mLocalMoment.thumbnailPath;
    }
}
