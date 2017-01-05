package com.waylens.hachi.bgjob.export;



import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.waylens.hachi.snipe.vdb.ClipPos;

/**
 * Created by Xiaofei on 2016/8/17.
 */
public abstract class ExportableJob extends Job {
    protected int mDownloadProgress;

    protected String mOutputFile;
    protected OnProgressChangedListener mOnProgressChangedListener;

    protected ExportableJob(Params params) {
        super(params);
    }


    public int getExportProgress() {
        return mDownloadProgress;
    }

    public String getOutputFile() {
        return mOutputFile;
    }

    public abstract ClipPos getClipStartPos();

    public void setOnProgressChangedListener(OnProgressChangedListener listener) {
        this.mOnProgressChangedListener = listener;
    }

    protected void notifyProgressChanged(int progress) {
        if (mDownloadProgress != progress) {
            mDownloadProgress = progress;
            if (mOnProgressChangedListener != null) {
                mOnProgressChangedListener.OnProgressChanged(this);
            }
        }

    }


    public interface OnProgressChangedListener {
        void OnProgressChanged(ExportableJob job);
    }
}
