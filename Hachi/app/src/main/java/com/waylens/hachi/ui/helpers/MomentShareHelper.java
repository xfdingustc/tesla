package com.waylens.hachi.ui.helpers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.utils.DataUploaderV2;

/**
 * MomentShareHelper
 * Created by Richard on 1/5/16.
 */
public class MomentShareHelper implements DataUploaderV2.OnUploadListener,
        MomentBuilder.OnBuildListener {
    private static final String TAG = "MomentShareHelper";

    public static final int ERROR_LOGIN = -1000;
    public static final int ERROR_CREATE_MOMENT_DESC = -1001;
    public static final int ERROR_UPLOAD_VIDEO = -1002;
    public static final int ERROR_UPLOAD_THUMBNAIL = -1003;
    public static final int ERROR_IO = -1004;

    public static final int ERROR_GET_CLIP_SET = -2001;
    public static final int ERROR_GET_UPLOAD_URL = -2002;
    public static final int ERROR_GET_THUMBNAIL = -2003;
    public static final int ERROR_CACHE_THUMBNAIL = -2004;
    public static final int ERROR_CREATE_MOMENT = -2005;

    VdbRequestQueue mVdbRequestQueue;
    OnShareMomentListener mShareListener;

    volatile Thread mUploadThread;
    volatile DataUploaderV2 uploaderV2;
    volatile boolean isCancelled;

    MomentBuilder mMomentBuilder;

    Handler mHandler;

    Context mContext;

    LocalMoment mLocalMoment;


    public MomentShareHelper(Context context, @NonNull OnShareMomentListener listener) {
        mVdbRequestQueue = null;//Snipe.newRequestQueue();
        mShareListener = listener;
        mHandler = new Handler(Looper.getMainLooper());
        mContext = context;
    }

    public void cancel(boolean cleanListener) {
        isCancelled = true;
        if (mMomentBuilder != null) {
            mMomentBuilder.cancel();
        }
        if (mUploadThread != null && mUploadThread.isAlive() && uploaderV2 != null) {
            uploaderV2.cancel();
        }
        if (cleanListener) {
            mShareListener = null;
        }
    }

    /**
     * Share playList to Waylens cloud.
     * <p>
     * Please make sure to call cancel() method to terminate the background thread.
     *
     * @param playListID
     * @param title
     * @param tags
     * @param accessLevel
     */
    public void shareMoment(final int playListID,
                            final String title,
                            final String[] tags,
                            final String accessLevel,
                            final int audioID,
                            String gaugeSettings) {
        mMomentBuilder = new MomentBuilder(mContext, mVdbRequestQueue);
        mMomentBuilder.forPlayList(playListID)
                .asMoment(title, tags, accessLevel, audioID, gaugeSettings)
                .build(this);
    }

    void uploadData() {
        mUploadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isCancelled) {
                        if (mShareListener != null) {
                            mShareListener.onCancelShare();
                        }
                        return;
                    }
                    uploaderV2 = new DataUploaderV2();
                    uploaderV2.upload(mLocalMoment, MomentShareHelper.this);
                } catch (Exception e) {
                    Logger.t(TAG).e(e, "");
                    if (mShareListener != null) {
                        mShareListener.onShareError(ERROR_IO, 0);
                    }
                }
                uploaderV2 = null;
                mUploadThread = null;
            }
        }, "share-moment-thread");
        mUploadThread.start();
    }

    @Override
    public void onUploadSuccessful() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mShareListener != null) {
                    mShareListener.onShareSuccessful(mLocalMoment);
                }
            }
        });
    }

    @Override
    public void onUploadProgress(final int percentage) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mShareListener != null) {
                    mShareListener.onUploadProgress(percentage);
                }
            }
        });
    }

    @Override
    public void onUploadError(final int errorCode, final int extraCode) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mShareListener != null) {
                    mShareListener.onShareError(errorCode, 0);
                }
            }
        });
    }

    @Override
    public void onCancelUpload() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mShareListener != null) {
                    mShareListener.onCancelShare();
                }
            }
        });
    }

    @Override
    public void onBuildSuccessful(LocalMoment localMoment) {
        mLocalMoment = localMoment;
        uploadData();
    }

    @Override
    public void onBuildError(int errorCode, int messageResID) {
        if (mShareListener != null) {
            mShareListener.onShareError(errorCode, messageResID);
        }
    }

    @Override
    public void onCancelBuild() {
        if (mShareListener != null) {
            mShareListener.onCancelShare();
        }
    }


    public interface OnShareMomentListener {
        void onShareSuccessful(LocalMoment localMoment);

        void onCancelShare();

        void onShareError(int errorCode, int errorResId);

        void onUploadProgress(int uploadPercentage);
    }
}
