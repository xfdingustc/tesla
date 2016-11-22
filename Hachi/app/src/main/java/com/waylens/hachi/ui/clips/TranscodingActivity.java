package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.DrmInitData;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.jobqueue.scheduling.Scheduler;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipDownloadInfo;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetManager;
import com.waylens.hachi.snipe.vdb.ClipSetPos;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.clips.player.ClipRawDataAdapter;
import com.waylens.hachi.ui.clips.player.RawDataLoader;
import com.waylens.hachi.utils.ClipDownloadHelper;
import com.waylens.hachi.utils.FileUtils;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;
import com.waylens.hachi.view.gauge.GaugeView;
import com.waylens.mediatranscoder.MediaTranscoder;
import com.waylens.mediatranscoder.engine.OverlayProvider;
import com.waylens.mediatranscoder.format.MediaFormatStrategy;
import com.waylens.mediatranscoder.format.MediaFormatStrategyPresets;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;

import butterknife.BindView;
import is.arontibo.library.ElasticDownloadView;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/11/18.
 */

public class TranscodingActivity extends BaseActivity {
    private static final String TAG = TranscodingActivity.class.getSimpleName();
    private static final String EXTRA_PLAYLIST_ID = "playlist.id";
    private static final String EXTRA_STREAM_INFO = "stream.info";
    private static final String EXTRA_STREAM_DOWNLOAD_INFO = "stream.download.info";

    private int mPlaylistId;
    private Clip.StreamInfo mStreamInfo;
    private ClipDownloadInfo.StreamDownloadInfo mStreamDownloadInfo;

    private String mDownloadFile;

    private RawDataLoader mRawDataLoader;
    private ClipRawDataAdapter mAdapter;

    private OverlayProvider mOverlayProvider;

    public static void launch(Activity activity, int playListId, Clip.StreamInfo streamInfo, ClipDownloadInfo.StreamDownloadInfo downloadInfo) {
        Intent intent = new Intent(activity, TranscodingActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playListId);
        intent.putExtra(EXTRA_STREAM_INFO, streamInfo);
        intent.putExtra(EXTRA_STREAM_DOWNLOAD_INFO, downloadInfo);
        activity.startActivity(intent);
    }


    @BindView(R.id.gauge_view)
    GaugeView mGaugeView;


    @BindView(R.id.elastic_download_view)
    ElasticDownloadView mDownloadView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mPlaylistId = getIntent().getIntExtra(EXTRA_PLAYLIST_ID, -1);
        mStreamInfo = (Clip.StreamInfo) getIntent().getSerializableExtra(EXTRA_STREAM_INFO);
        mStreamDownloadInfo = (ClipDownloadInfo.StreamDownloadInfo) getIntent().getSerializableExtra(EXTRA_STREAM_DOWNLOAD_INFO);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_transcoding);
        mGaugeView.showGauge(true, true);

        mDownloadView.startIntro();

        mRawDataLoader = new RawDataLoader(mPlaylistId);
        mRawDataLoader.loadRawDataRx()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe() {
                @Override
                public void onNext(Object o) {

                }

                @Override
                public void onCompleted() {
                    super.onCompleted();
                    mAdapter = new ClipRawDataAdapter(getClipSet());
                    mAdapter.setRawDataLoader(mRawDataLoader);
                    mGaugeView.setAdapter(mAdapter);
                    downloadClipEsData();

                }
            });


    }

    private void downloadClipEsData() {
        ClipDownloadHelper downloadHelper = new ClipDownloadHelper(mStreamInfo, mStreamDownloadInfo);
        mDownloadFile = FileUtils.genDownloadFileName(mStreamDownloadInfo.clipDate, mStreamDownloadInfo.clipTimeMs);
        downloadHelper.downloadClipRx(mDownloadFile)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Integer>() {
                @Override
                public void onCompleted() {
                    startTranscoding();
                }

                @Override
                public void onError(Throwable e) {
                    mDownloadView.fail();
                }

                @Override
                public void onNext(Integer integer) {
                    mDownloadView.setProgress((float) integer / 2);
                }
            });

    }

    private void startTranscoding() {
        Uri fileUri = Uri.fromFile(new File(mDownloadFile));
        ContentResolver resolver = getContentResolver();
        final ParcelFileDescriptor parcelFileDescriptor;

        try {
            parcelFileDescriptor = resolver.openFileDescriptor(fileUri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        final String outputFile = FileUtils.genDownloadFileName(0, 0);
        mOverlayProvider = new ClipRawDataOverlayProvider();
        MediaTranscoder.getInstance().transcodeVideoRx(fileDescriptor, outputFile, MediaFormatStrategyPresets.createAndroid720pStrategy(), mOverlayProvider)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<MediaTranscoder.TranscodeProgress>() {
                @Override
                public void onCompleted() {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(new File(outputFile)), "video/mp4");
                    startActivity(intent);
                }

                @Override
                public void onError(Throwable e) {
                    mDownloadView.fail();
                }

                @Override
                public void onNext(MediaTranscoder.TranscodeProgress transcodeProgress) {
                    mDownloadView.setProgress(((float) transcodeProgress.progress * 100) / 2 + 50.0f);
                }
            });

    }

    private ClipSet getClipSet() {
        return ClipSetManager.getManager().getClipSet(mPlaylistId);
    }

    private class ClipRawDataOverlayProvider implements OverlayProvider {

        @Override
        public Bitmap updateTexImage(final long pts) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ClipSetPos clipSetPos = getClipSet().getClipSetPosByTimeOffset(pts / 1000000);
                    mAdapter.setClipSetPos(clipSetPos);

                }
            });
            return getBitmapFromView(mGaugeView);
        }
    }


    public static Bitmap getBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        // Draw background
        Drawable bgDrawable = v.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(c);
        }

        // Draw view to canvas
        v.draw(c);
        return b;
    }
}
