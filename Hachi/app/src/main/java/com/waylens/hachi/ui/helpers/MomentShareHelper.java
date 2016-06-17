package com.waylens.hachi.ui.helpers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.birbit.android.jobqueue.JobManager;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;

import org.json.JSONObject;


public class MomentShareHelper implements MomentBuilder.OnBuildListener {
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

    MomentBuilder mMomentBuilder;

    Context mContext;

    LocalMoment mLocalMoment;


    public MomentShareHelper(Context context, VdbRequestQueue requestQueue, @NonNull OnShareMomentListener listener) {
        mVdbRequestQueue = requestQueue;
        mShareListener = listener;
        mContext = context;
    }

    public void cancel(boolean cleanListener) {
        if (mMomentBuilder != null) {
            mMomentBuilder.cancel();
        }
        if (cleanListener) {
            mShareListener = null;
        }
    }

    /**
     * Share playList to Waylens cloud.
     * <p/>
     * Please make sure to call cancel() method to terminate the background thread.
     *
     * @param playListID
     * @param title
     * @param tags
     * @param accessLevel
     */
    public void shareMoment(final int playListID, final String title, String descripion, final String[] tags,
                            final String accessLevel, final int audioID, JSONObject gaugeSettings,
                            boolean isFbShare) {
        mMomentBuilder = new MomentBuilder(mContext, mVdbRequestQueue);
        mMomentBuilder.forPlayList(playListID).asMoment(title, descripion, tags, accessLevel, audioID, gaugeSettings, isFbShare).build(this);
    }

    private void uploadData() {

        JobManager jobManager = BgJobManager.getManager();
        UploadMomentJob job = new UploadMomentJob(mLocalMoment);
        jobManager.addJobInBackground(job);
    }



    @Override
    public void onBuildSuccessful(LocalMoment localMoment) {
        mLocalMoment = localMoment;
        uploadData();
        if (mShareListener != null) {
            mShareListener.onUploadStarted();
        }
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

        void onCancelShare();

        void onShareError(int errorCode, int errorResId);



        void onUploadStarted();
    }
}
