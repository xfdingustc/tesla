package com.waylens.hachi.ui.helpers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.waylens.hachi.utils.PreferenceUtils;

import java.lang.ref.WeakReference;

/**
 * Created by Richard on 2/2/16.
 */
public class DownloadHelper {

    private static final String TAG = "DownloadHelper";
    public static final String DOWNLOAD_ID = "DownloadHelper_id";

    DownloadManager downloadManager = null;

    WeakReference<Context> mContextReference;

    OnDownloadListener mDownloadListener;

    Downloadable mDownloadable;

    public DownloadHelper(Context context, OnDownloadListener listener) {
        mContextReference = new WeakReference<Context>(context);
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        mDownloadListener = listener;
    }

    public void download(Downloadable downloadable) {
        if (downloadManager == null || mContextReference == null || mContextReference.get() == null) {
            return;
        }
        mDownloadable = downloadable;
        DownloadManager.Request request;
        try {
            request = downloadable.getDownloadRequest(mContextReference.get());
            long downloadID = downloadManager.enqueue(request);
            PreferenceUtils.putLong(DOWNLOAD_ID, downloadID);
        } catch (Exception e) {
            Log.e(TAG, "", e);
            if (mDownloadListener != null) {
                mDownloadListener.onError(downloadable);
            }
        }
    }

    void queryDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        long downloadId = PreferenceUtils.getLong(DOWNLOAD_ID, 0);
        query.setFilterById(downloadId);
        Cursor c = downloadManager.query(query);
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                case DownloadManager.STATUS_PAUSED:
                    log("STATUS_PAUSED");
                case DownloadManager.STATUS_PENDING:
                    log("STATUS_PENDING");
                case DownloadManager.STATUS_RUNNING:
                    log("STATUS_RUNNING");
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    log("File is downloaded.");
                    String downloadedFile = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                    PreferenceUtils.remove(DOWNLOAD_ID);
                    if (mDownloadListener != null) {
                        mDownloadListener.onSuccess(mDownloadable, downloadedFile);
                    }
                    break;
                case DownloadManager.STATUS_FAILED:
                    log("STATUS_FAILED");
                    downloadManager.remove(downloadId);
                    PreferenceUtils.remove(DOWNLOAD_ID);
                    if (mDownloadListener != null) {
                        mDownloadListener.onError(mDownloadable);
                    }
                    break;
            }
        }
    }

    void log(String msg) {
        Log.e(TAG, msg);
    }

    public final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            queryDownloadStatus();
        }
    };

    public interface Downloadable {
        DownloadManager.Request getDownloadRequest(Context context);
    }

    public interface OnDownloadListener {
        void onSuccess(Downloadable downloadable, String filePath);

        void onError(Downloadable downloadable);
    }
}
