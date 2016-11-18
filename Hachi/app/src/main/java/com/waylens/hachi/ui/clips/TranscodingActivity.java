package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.transition.TransitionManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipDownloadInfo;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.views.gauge.GaugeView;
import com.waylens.hachi.utils.ClipDownloadHelper;
import com.waylens.hachi.utils.FileUtils;
import com.waylens.mediatranscoder.MediaTranscoder;
import com.waylens.mediatranscoder.format.MediaFormatStrategyPresets;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/11/18.
 */

public class TranscodingActivity extends BaseActivity {
    private static final String TAG = TranscodingActivity.class.getSimpleName();
    private static final String EXTRA_STREAM_INFO = "stream.info";
    private static final String EXTRA_STREAM_DOWNLOAD_INFO = "stream.download.info";

    private Clip.StreamInfo mStreamInfo;
    private ClipDownloadInfo.StreamDownloadInfo mStreamDownloadInfo;

    private String mDownloadFile;

    public static void launch(Activity activity, Clip.StreamInfo streamInfo, ClipDownloadInfo.StreamDownloadInfo downloadInfo) {
        Intent intent = new Intent(activity, TranscodingActivity.class);
        intent.putExtra(EXTRA_STREAM_INFO, streamInfo);
        intent.putExtra(EXTRA_STREAM_DOWNLOAD_INFO, downloadInfo);
        activity.startActivity(intent);
    }

    @BindView(R.id.tv_status)
    TextView tvStatus;

    @BindView(R.id.progress_bar)
    ProgressBar mProgressBar;

    @BindView(R.id.gauge_view)
    GaugeView mGaugeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mStreamInfo = (Clip.StreamInfo)getIntent().getSerializableExtra(EXTRA_STREAM_INFO);
        mStreamDownloadInfo = (ClipDownloadInfo.StreamDownloadInfo)getIntent().getSerializableExtra(EXTRA_STREAM_DOWNLOAD_INFO);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_transcoding);
        mGaugeView.showGauge(true, true);

        ClipDownloadHelper downloadHelper = new ClipDownloadHelper(mStreamInfo, mStreamDownloadInfo, new ClipDownloadHelper.OnExportListener() {
            @Override
            public void onExportError(int arg1, int arg2) {

            }

            @Override
            public void onExportProgress(int progress) {
                mProgressBar.setProgress(progress);
            }

            @Override
            public void onExportFinished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startTranscoding();
                    }
                });

            }
        });

        mDownloadFile = downloadHelper.downloadVideo();
        tvStatus.setText("Downloading");

    }

    private void startTranscoding() {
        tvStatus.setText("Transcoding");
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
        MediaTranscoder.getInstance().transcodeVideo(fileDescriptor, outputFile, MediaFormatStrategyPresets.createAndroid720pStrategy(), new MediaTranscoder.Listener() {
            @Override
            public void onTranscodeProgress(double progress, long currentTimeMs) {
                mProgressBar.setProgress((int)(progress * 100));
            }

            @Override
            public void onTranscodeCompleted() {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(outputFile)), "video/mp4");
                startActivity(intent);
            }

            @Override
            public void onTranscodeFailed(Exception exception) {

            }
        }, mGaugeView);
    }
}
