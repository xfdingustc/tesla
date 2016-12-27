package com.waylens.hachi.utils;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.service.download.DownloadAPI;
import com.waylens.hachi.service.download.DownloadProgressListener;
import com.waylens.hachi.service.download.Downloadable;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.toolbox.ClipUploadUrlRequest;
import com.waylens.hachi.snipe.toolbox.VdbImageRequest;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipPos;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.urls.UploadUrl;
import com.waylens.hachi.ui.entities.LocalMoment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import rx.Observable;
import rx.Subscriber;


/**
 * Created by Xiaofei on 2016/12/22.
 */

public class LocalMomentDownloadHelper {
    private static final String TAG = LocalMomentDownloadHelper.class.getSimpleName();

    public static final int DOWNLOAD_STATUS_GET_VIDEO_URL = 0;
    public static final int DOWNLOAD_STATUS_GET_VIDEO_COVER = 1;
    public static final int DOWNLOAD_STATUS_STORE_VIDEO_COVER = 2;
    public static final int DOWNLOAD_STATUS_UPLOAD_UPLOAD_PROGRESS = 3;

    private static final int VIDIT_RAW_DATA = 1;

    private static final int VIDIT_VIDEO_DATA_LOW = 64;
    private static final int DEFAULT_DATA_TYPE_SD = VdbCommand.Factory.UPLOAD_GET_V1 | VdbCommand.Factory.UPLOAD_GET_RAW;
    private static final int DEFAULT_DATA_TYPE_FULLHD = VdbCommand.Factory.UPLOAD_GET_V0 | VdbCommand.Factory.UPLOAD_GET_RAW;
    private static final int DEFAULT_DATA_TYPE_CLOUD = VIDIT_VIDEO_DATA_LOW | VIDIT_RAW_DATA;


    public static Observable<DownloadLocalMomentStatus> downloadLocalMomentRx(final LocalMoment localMoment) {
        return Observable.create(new Observable.OnSubscribe<DownloadLocalMomentStatus>() {
            @Override
            public void call(Subscriber<? super DownloadLocalMomentStatus> subscriber) {
                try {
                    doDownloadLocalMoment(localMoment, subscriber);
                    subscriber.onCompleted();
                } catch (InterruptedException | ExecutionException | FileNotFoundException e) {
                    subscriber.onError(e);
                }
            }
        });

    }

    private static void doDownloadLocalMoment(LocalMoment localMoment,
                                              Subscriber<? super DownloadLocalMomentStatus> subscriber) throws ExecutionException, InterruptedException, FileNotFoundException {
        VdbRequestQueue requestQueue = VdtCameraManager.getManager().getCurrentCamera().getRequestQueue();
        // Step1:  get playlist info:
        VdbRequestFuture<ClipSet> clipSetRequestFuture = VdbRequestFuture.newFuture();
        ClipSetExRequest request = new ClipSetExRequest(localMoment.playlistId, ClipSetExRequest.FLAG_CLIP_EXTRA | ClipSetExRequest.FLAG_CLIP_DESC, clipSetRequestFuture, clipSetRequestFuture);
        requestQueue.add(request);
        ClipSet playlistClipSet = clipSetRequestFuture.get();
        Logger.t(TAG).d("Play list info got, clip set size:  " + playlistClipSet.getCount());


        // Step2: get upload url info:
        for (int i = 0; i < playlistClipSet.getCount(); i++) {
            Logger.t(TAG).d("Try to get upload url, index: " + i);
            VdbRequestFuture<UploadUrl> uploadUrlRequestFuture = VdbRequestFuture.newFuture();
            Clip clip = playlistClipSet.getClip(i);
            String vin = clip.getVin();
            Bundle parameters = new Bundle();
            parameters.putBoolean(ClipUploadUrlRequest.PARAM_IS_PLAY_LIST, false);
            parameters.putLong(ClipUploadUrlRequest.PARAM_CLIP_TIME_MS, clip.getStartTimeMs());
            parameters.putInt(ClipUploadUrlRequest.PARAM_CLIP_LENGTH_MS, clip.getDurationMs());
            if (localMoment.streamId == 0) {
                parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, DEFAULT_DATA_TYPE_SD);
            } else {
                parameters.putInt(ClipUploadUrlRequest.PARAM_UPLOAD_OPT, DEFAULT_DATA_TYPE_FULLHD);
            }
            ClipUploadUrlRequest uploadUrlRequest = new ClipUploadUrlRequest(clip.cid, parameters, uploadUrlRequestFuture, uploadUrlRequestFuture);
            requestQueue.add(uploadUrlRequest);

            UploadUrl uploadUrl = uploadUrlRequestFuture.get();
            Logger.t(TAG).d("Got clip upload url: " + uploadUrl.url);
            LocalMoment.Segment segment = new LocalMoment.Segment(clip, uploadUrl, DEFAULT_DATA_TYPE_CLOUD);
            localMoment.mSegments.add(segment);
            //checkIfCancelled();
        }

        subscriber.onNext(new DownloadLocalMomentStatus(DOWNLOAD_STATUS_GET_VIDEO_URL));

        // Step3: get videoThumbnail:
        Logger.t(TAG).d("Try to get videoThumbnail");
        final Clip firstClip = playlistClipSet.getClip(0);
        ClipPos clipPos = new ClipPos(firstClip.getVdbId(), firstClip.cid, firstClip.getClipDate(), firstClip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
        VdbRequestFuture<Bitmap> thumbnailRequestFuture = VdbRequestFuture.newFuture();
        VdbImageRequest imageRequest = new VdbImageRequest(clipPos, thumbnailRequestFuture, thumbnailRequestFuture);
        requestQueue.add(imageRequest);
        Bitmap thumbnail = thumbnailRequestFuture.get();
        Logger.t(TAG).d("Got videoThumbnail");
        subscriber.onNext(new DownloadLocalMomentStatus(DOWNLOAD_STATUS_GET_VIDEO_COVER));
//        checkIfCancelled();

        // Step4: Store videoThumbnail:
        File cacheDir = Hachi.getContext().getExternalCacheDir();
        File file = new File(cacheDir, "t" + firstClip.getStartTimeMs() + ".jpg");
        FileOutputStream fos = new FileOutputStream(file);
        thumbnail.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, fos);
        localMoment.thumbnailPath = file.getAbsolutePath();
        Logger.t(TAG).d("Saved videoThumbnail: " + localMoment.thumbnailPath);
        subscriber.onNext(new DownloadLocalMomentStatus(DOWNLOAD_STATUS_STORE_VIDEO_COVER));

        //checkIfCancelled();

        doCacheMoment(localMoment, subscriber);
        //MomentUploadCacher cacher = new MomentUploadCacher(this);
        //cacher.cacheMoment(mLocalMoment);
        //BgJobHelper.uploadCachedMoment(mLocalMoment);
    }

    private static void doCacheMoment(LocalMoment localMoment, Subscriber<? super DownloadLocalMomentStatus> subscriber) {
        for (int i = 0; i < localMoment.mSegments.size(); i++) {
            LocalMoment.Segment segment = localMoment.mSegments.get(i);
            downloadMomentFiles(segment, i, localMoment.mSegments.size(), subscriber);
        }

    }

    private static void downloadMomentFiles(LocalMoment.Segment segment, final int index, final int totalSegments, final Subscriber<? super DownloadLocalMomentStatus> subscriber) {
        DownloadProgressListener listener = new DownloadProgressListener() {
            @Override
            public void update(long bytesRead, long contentLength, boolean done) {
/*                Downloadable downloadable = new Downloadable();
                downloadable.setTotalFileSize(contentLength);
                downloadable.setCurrentFileSize(bytesRead);
                downloadable.setProgress(progress);*/

                int progress = (int) ((bytesRead * 100) / contentLength);
                int percentageInThisClip = progress / totalSegments;
                int percentage = index * 100 / totalSegments + percentageInThisClip;
                subscriber.onNext(new DownloadLocalMomentStatus(DOWNLOAD_STATUS_UPLOAD_UPLOAD_PROGRESS, percentage));
            }
        };

        String file = FileUtils.genMomentCacheFileName(StringUtils.getFileName(segment.uploadURL.url));
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


    public static class DownloadLocalMomentStatus {



        public int status;
        public int progress;


        public DownloadLocalMomentStatus(int status) {
            this(status, 0);
        }

        public DownloadLocalMomentStatus(int status, int progress) {
            this.status = status;
            this.progress = progress;
        }

    }
}
