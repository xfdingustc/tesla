package com.waylens.hachi.bgjob.upload;

import android.net.Uri;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.bgjob.export.ExportHelper;
import com.waylens.hachi.service.download.DownloadAPI;
import com.waylens.hachi.service.download.DownloadProgressListener;
import com.waylens.hachi.service.download.Downloadable;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.utils.FileUtils;
import com.waylens.hachi.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Xiaofei on 2016/9/1.
 */
public class MomentUploadCacher {
    private static final String TAG = MomentUploadCacher.class.getSimpleName();
    private final CacheMomentJob mUploadJob;

    public MomentUploadCacher(CacheMomentJob job) {
        this.mUploadJob = job;
    }


    public void cacheMoment(LocalMoment moment) {
        for (int i = 0; i < moment.mSegments.size(); i++) {
            LocalMoment.Segment segment = moment.mSegments.get(i);
            downloadMomentFiles(segment, i, moment.mSegments.size());
        }
    }


    private void downloadMomentFiles(LocalMoment.Segment segment, final int index, final int totalSegments) {
        DownloadProgressListener listener = new DownloadProgressListener() {
            @Override
            public void update(long bytesRead, long contentLength, boolean done) {
                Downloadable downloadable = new Downloadable();
                downloadable.setTotalFileSize(contentLength);
                downloadable.setCurrentFileSize(bytesRead);
                int progress = (int) ((bytesRead * 100) / contentLength);
                downloadable.setProgress(progress);

                int percentageInThisClip = progress / totalSegments;
                int percentage = index * 100 / totalSegments + percentageInThisClip;
                mUploadJob.setUploadState(CacheMomentJob.UPLOAD_STATE_PROGRESS, percentage);

            }
        };
        String file = ExportHelper.getMomentCachePath() + StringUtils.getFileName(segment.uploadURL.url);
        File outputFile = new File(file);
        Logger.t(TAG).d("output file: " + outputFile);
        String baseUrl = StringUtils.getHostName(segment.uploadURL.url);

        try {
            InputStream inputStream = new DownloadAPI(baseUrl, listener).downloadFileSync(segment.uploadURL.url);
            if (inputStream != null) {
                FileUtils.writeFile(inputStream, outputFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        segment.uploadURL.url = Uri.fromFile(outputFile).toString();


    }
}
