package com.waylens.hachi.bgjob.export.download;

import android.media.MediaScannerConnection;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.bgjob.export.ExportableJob;
import com.waylens.hachi.snipe.reative.SnipeApi;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipDownloadInfo;
import com.waylens.hachi.snipe.vdb.ClipPos;
import com.waylens.hachi.utils.ClipDownloadHelper;
import com.waylens.hachi.utils.FileUtils;

import java.util.concurrent.ExecutionException;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by Xiaofei on 2016/5/4.
 */
public class DownloadJob extends ExportableJob {
    private static final String TAG = DownloadJob.class.getSimpleName();
    private int mStreamIndex;
    private ClipDownloadInfo.StreamDownloadInfo mDownloadInfo;
    private int mPlayListId;
    private int mDuration;
    private Clip mClip;
    private Clip.StreamInfo mStreamInfo;

    private transient Object mDownloadFence = new Object();

    private String mDownloadFilePath;


    public DownloadJob(int playListId, int duration, Clip clip, Clip.StreamInfo streamInfo, int streamIndex) {
        super(new Params(0).requireNetwork().setPersistent(false));
        this.mPlayListId = playListId;
        this.mDuration = duration;
        this.mClip = clip;
        this.mStreamInfo = streamInfo;
        this.mStreamIndex = streamIndex;
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

    private void downloadVideoSync() throws InterruptedException, ExecutionException {
        Clip.ID cid = new Clip.ID(mPlayListId, 0, null);
        ClipDownloadInfo clipDownloadInfo = SnipeApi.getClipDownloadInfo(cid, 0, mDuration);
        if (mStreamIndex == Clip.STREAM_MAIN) {
            mDownloadInfo = clipDownloadInfo.main;
        } else {
            mDownloadInfo = clipDownloadInfo.sub;
        }


        ClipDownloadHelper downloadHelper = new ClipDownloadHelper(mStreamInfo, mDownloadInfo);

        mDownloadFilePath = FileUtils.genDownloadFileName(mDownloadInfo.clipDate, mDownloadInfo.clipTimeMs);
        downloadHelper.downloadClipRx(mDownloadFilePath)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Integer>() {
                @Override
                public void onCompleted() {
                    MediaScannerConnection.scanFile(Hachi.getContext(), new String[]{
                        mDownloadFilePath.toString()}, null, null);
                    Logger.t(TAG).d("onExportFinished " + mDownloadFilePath);
                    synchronized (mDownloadFence) {
                        mDownloadFence.notifyAll();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    synchronized (mDownloadFence) {
                        mDownloadFence.notifyAll();
                    }
                }

                @Override
                public void onNext(Integer integer) {
                    notifyProgressChanged(integer);
                }


            });
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
