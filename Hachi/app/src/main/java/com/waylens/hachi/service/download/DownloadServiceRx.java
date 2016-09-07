package com.waylens.hachi.service.download;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.utils.StringUtils;

import java.io.File;

import rx.Subscriber;


/**
 * Created by Xiaofei on 2016/9/2.
 */
public class DownloadServiceRx extends IntentService {
    private static final String TAG = DownloadServiceRx.class.getSimpleName();

    public static final String DOWNLOAD_URL = "url";
    public static final String OUTPUT_PATH = "output_path";

    public static final String INTENT_FILTER_DOWNLOAD_INTENT_SERVICE = "inet_download_intent_service";
    public static final String EVENT_EXTRA_WHAT = "event_what";
    public static final String EVENT_EXTRA_DOWNLOAD_PROGRESS = "download_progress";
    public static final String EVENT_EXTRA_DOWNLOAD_FILE_PATH = "download_file_path";

    public static final int EVENT_WHAT_DOWNLOAD_STARTED = 0;
    public static final int EVENT_WHAT_DOWNLOAD_PROGRESS = 1;
    public static final int EVENT_WHAT_DOWNLOAD_FINSHED = 2;

    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;

    private String mDownloadUrl;
    private String mOutputFilePath;

    public static void start(Context context, String url, String outputFile) {
        Intent intent = new Intent(context, DownloadServiceRx.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(DOWNLOAD_URL, url);
        intent.putExtra(OUTPUT_PATH, outputFile);
        context.startService(intent);
    }

    public DownloadServiceRx() {
        super("DownloadServiceRx");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mDownloadUrl = intent.getStringExtra(DOWNLOAD_URL);
        mOutputFilePath = intent.getStringExtra(OUTPUT_PATH);
        Logger.t(TAG).d("On start Download Rx service, url: " + mDownloadUrl);
        mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationBuilder = new NotificationCompat.Builder(this)
            .setSmallIcon(R.mipmap.ic_notification)
            .setContentTitle(getString(R.string.download))
            .setContentText(getString(R.string.downloading))
            .setAutoCancel(true);

        mNotificationManager.notify(0, mNotificationBuilder.build());
        download();
    }

    private void download() {
        DownloadProgressListener listener = new DownloadProgressListener() {
            @Override
            public void update(long bytesRead, long contentLength, boolean done) {
                Downloadable downloadable = new Downloadable();
                downloadable.setTotalFileSize(contentLength);
                downloadable.setCurrentFileSize(bytesRead);
                int progress  = (int) ((bytesRead * 100) / contentLength);
                downloadable.setProgress(progress);
                sendNotification(downloadable);
            }
        };

        File outputFile = new File(mOutputFilePath);
        Logger.t(TAG).d("output file: " + outputFile);
        String baseUrl = StringUtils.getHostName(mDownloadUrl);

        new DownloadAPI(baseUrl, listener)
            .downloadFile(mDownloadUrl, outputFile, new Subscriber() {
                @Override
                public void onCompleted() {
                    downloadCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    downloadError();
                    Logger.t(TAG).d("Download error: " + e.getMessage());
                }

                @Override
                public void onNext(Object o) {

                }
            });
    }

    private void downloadCompleted() {
        Intent intent = new Intent(INTENT_FILTER_DOWNLOAD_INTENT_SERVICE);
        intent.putExtra(EVENT_EXTRA_WHAT, EVENT_WHAT_DOWNLOAD_FINSHED);
        intent.putExtra(EVENT_EXTRA_DOWNLOAD_FILE_PATH, mOutputFilePath.toString());
        sendBroadcast(intent);
    }

    private void downloadError() {

    }

    private void sendNotification(Downloadable downloadable) {
//        sendIntent(downloadable);
        mNotificationBuilder.setProgress(100, downloadable.getProgress(), false);
        mNotificationBuilder.setContentText(
            StringUtils.getDataSize(downloadable.getCurrentFileSize()) + "/" +
                StringUtils.getDataSize(downloadable.getTotalFileSize()));
        mNotificationManager.notify(0, mNotificationBuilder.build());
        Intent intent = new Intent(INTENT_FILTER_DOWNLOAD_INTENT_SERVICE);
        intent.putExtra(EVENT_EXTRA_WHAT, EVENT_WHAT_DOWNLOAD_PROGRESS);
        intent.putExtra(EVENT_EXTRA_DOWNLOAD_PROGRESS, downloadable.getProgress());
        sendBroadcast(intent);
    }
}
