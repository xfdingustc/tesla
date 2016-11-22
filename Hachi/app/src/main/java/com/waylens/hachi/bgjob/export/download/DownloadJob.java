package com.waylens.hachi.bgjob.export.download;

import android.media.MediaScannerConnection;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.bgjob.export.ExportableJob;
import com.waylens.hachi.jobqueue.Params;
import com.waylens.hachi.jobqueue.RetryConstraint;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipDownloadInfo;
import com.waylens.hachi.snipe.vdb.ClipPos;
import com.waylens.hachi.utils.ClipDownloadHelper;

/**
 * Created by Xiaofei on 2016/5/4.
 */
public class DownloadJob extends ExportableJob {
    private static final String TAG = DownloadJob.class.getSimpleName();
    private boolean mWithOverlay;
    private ClipDownloadInfo.StreamDownloadInfo mDownloadInfo;
    private Clip mClip;
    private Clip.StreamInfo mStreamInfo;

    private transient Object mDownloadFence = new Object();

    private String mDownloadFilePath;


    public DownloadJob(Clip clip, Clip.StreamInfo streamInfo, ClipDownloadInfo.StreamDownloadInfo downloadInfo, boolean withOverlay) {
        super(new Params(0).requireNetwork().setPersistent(false));
        this.mClip = clip;
        this.mStreamInfo = streamInfo;
        this.mDownloadInfo = downloadInfo;
        this.mWithOverlay = withOverlay;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        downloadVideoSync();
    }


    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return new RetryConstraint(false);
    }

    private void downloadVideoSync() throws InterruptedException {
        ClipDownloadHelper downloadHelper = new ClipDownloadHelper(mStreamInfo, mDownloadInfo);
        mDownloadFilePath = downloadHelper.downloadVideo(new ClipDownloadHelper.OnExportListener() {
            @Override
            public synchronized void onExportError(int arg1, int arg2) {
                synchronized (mDownloadFence) {
                    mDownloadFence.notifyAll();
                }
            }

            @Override
            public void onExportProgress(int progress) {
                notifyProgressChanged(progress);
            }

            @Override
            public void onExportFinished() {
                MediaScannerConnection.scanFile(Hachi.getContext(), new String[]{
                    mDownloadFilePath.toString()}, null, null);
                Logger.t(TAG).d("onExportFinished " + mDownloadFilePath);
                synchronized (mDownloadFence) {
                    mDownloadFence.notifyAll();
                }
            }
        });
//        wait();
        synchronized (mDownloadFence) {
            mDownloadFence.wait();
        }
        Logger.t(TAG).d("download finished " + mDownloadFilePath);
    }


    @Override
    public int getExportProgress() {
        return mDownloadProgress;
    }

    @Override
    public String getOutputFile() {
        return mDownloadFilePath;
    }

    @Override
    public ClipPos getClipStartPos() {
        return new ClipPos(mClip);
    }
}
