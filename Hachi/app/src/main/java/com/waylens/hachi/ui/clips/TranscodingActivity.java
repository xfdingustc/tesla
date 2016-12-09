package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipDownloadInfo;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetManager;
import com.waylens.hachi.snipe.vdb.ClipSetPos;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.clips.player.ClipRawDataAdapter;
import com.waylens.hachi.ui.clips.player.RawDataLoader;
import com.waylens.hachi.utils.BitmapUtils;
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
import butterknife.OnClick;
import it.michelelacorte.elasticprogressbar.ElasticDownloadView;
import it.michelelacorte.elasticprogressbar.OptionView;
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
    private static final String EXTRA_STREAM_INDEX = "stream.index";
    private static final String EXTRA_QUALITY_INDEX = "quality.index";

    private static final int TRANS_STATE_ERROR = -1;
    private static final int TRANS_STATE_PREPARING = 0;
    private static final int TRANS_STATE_DOWNLOAD_CLIP = 1;
    private static final int TRANS_STATE_TRANCODING = 2;
    private static final int TRANS_STATE_FINISHED = 3;

    private int mPlaylistId;
    private Clip.StreamInfo mStreamInfo;
    private int mStreamIndex;
    private ClipDownloadInfo.StreamDownloadInfo mStreamDownloadInfo;

    private String mDownloadFile;
    private String mOutputFile;

    private RawDataLoader mRawDataLoader;
    private ClipRawDataAdapter mAdapter;
    private int mQualityIndex;

    private OverlayProvider mOverlayProvider;

    private int mState = TRANS_STATE_PREPARING;

    public static void launch(Activity activity, int playListId, Clip.StreamInfo streamInfo, int streamIndex, int qualityIndex) {
        Intent intent = new Intent(activity, TranscodingActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playListId);
        intent.putExtra(EXTRA_STREAM_INFO, streamInfo);
        intent.putExtra(EXTRA_STREAM_INDEX, streamIndex);
        intent.putExtra(EXTRA_QUALITY_INDEX, qualityIndex);
        activity.startActivity(intent);
    }

    @BindView(R.id.export_status)
    TextView exportStatus;

    @BindView(R.id.container)
    ViewGroup container;


    @BindView(R.id.gauge_view)
    GaugeView mGaugeView;


    @BindView(R.id.elastic_download_view)
    ElasticDownloadView mDownloadView;

    @OnClick(R.id.btn_close)
    public void onBtnCloseClicked() {
        onBackPressed();
    }




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    public void onBackPressed() {
        if (mState != TRANS_STATE_FINISHED && mState != TRANS_STATE_ERROR) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content(R.string.exit_exporting_confirm)
                .positiveText(R.string.exit)
                .negativeText(R.string.stay)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (mDownloadFile != null) {
                            Logger.t(TAG).d("Delete temp file: " + mDownloadFile);
                            File file = new File(mDownloadFile);
                            file.delete();
                        }
                        if (mState == TRANS_STATE_TRANCODING) {
                            Logger.t(TAG).d("Delete output file: " + mOutputFile);
                            File file = new File(mOutputFile);
                            file.delete();
                            MediaTranscoder.getInstance().cancel();
                        }
                        TranscodingActivity.super.onBackPressed();
                    }
                }).show();
        } else {
            super.onBackPressed();
        }

    }

    @Override
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mPlaylistId = intent.getIntExtra(EXTRA_PLAYLIST_ID, -1);
        mStreamInfo = (Clip.StreamInfo) intent.getSerializableExtra(EXTRA_STREAM_INFO);
        mStreamIndex = intent.getIntExtra(EXTRA_STREAM_INDEX, Clip.STREAM_SUB);
        mQualityIndex = intent.getIntExtra(EXTRA_QUALITY_INDEX, 0);
        initViews();
    }

    private void initViews() {
        OptionView.noBackground = true;
        OptionView.setColorProgressBarInProgress(R.color.style_color_accent);
        OptionView.setColorCloud(R.color.style_color_accent);
        OptionView.setColorProgressBarText(R.color.white);
        OptionView.setColorSuccess(R.color.white);
        OptionView.setColorFail(R.color.white);
        setContentView(R.layout.activity_transcoding);
        mGaugeView.setGaugeMode(GaugeView.MODE_MOMENT);
        mGaugeView.initGaugeViewBySetting();
        mGaugeView.showGauge(true, true);
        int totalLengthMs = getClipSet().getTotalLengthMs();
        mGaugeView.setTail(totalLengthMs - 1000);
        mDownloadView.startIntro();

        Clip.ID cid = new Clip.ID(mPlaylistId, 0, null);
        SnipeApiRx.getClipDownloadInfoRx(cid, 0, getClipSet().getTotalLengthMs())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<ClipDownloadInfo>() {
                @Override
                public void onNext(ClipDownloadInfo clipDownloadInfo) {
                    if (mStreamIndex == Clip.STREAM_MAIN) {
                        mStreamDownloadInfo = clipDownloadInfo.main;
                    } else {
                        mStreamDownloadInfo = clipDownloadInfo.sub;
                    }
                    loadRawData();
                }
            });


    }

    private void loadRawData() {
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
        mState = TRANS_STATE_DOWNLOAD_CLIP;
        exportStatus.setText(R.string.exporting);
        ClipDownloadHelper downloadHelper = new ClipDownloadHelper(mStreamInfo, mStreamDownloadInfo);
        mDownloadFile = FileUtils.genDownloadFileName(mStreamDownloadInfo.clipDate, mStreamDownloadInfo.clipTimeMs) + ".mp4";
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
                    onTranscodeError(e);
                }

                @Override
                public void onNext(Integer integer) {
                    mDownloadView.setProgress((float) integer * 2 / 3);
                }
            });

    }


    private void startTranscoding() {
        mState = TRANS_STATE_TRANCODING;
        exportStatus.setText(R.string.transcoding);
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
        mOutputFile = FileUtils.genDownloadFileName(mStreamDownloadInfo.clipDate, mStreamDownloadInfo.clipTimeMs);
        mOverlayProvider = new ClipRawDataOverlayProvider();



        MediaTranscoder.getInstance().transcodeVideoRx(fileDescriptor, mOutputFile, getMediaFormatStrategy(), mOverlayProvider)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<MediaTranscoder.TranscodeProgress>() {
                @Override
                public void onCompleted() {
                    onTranscodeFinished();
                }

                @Override
                public void onError(Throwable e) {
                    onTranscodeError(e);
                }

                @Override
                public void onNext(MediaTranscoder.TranscodeProgress transcodeProgress) {
                    mDownloadView.setProgress(((float) transcodeProgress.progress * 100) / 3 + 66.66f);
                }
            });

    }

    private void onTranscodeFinished() {
        mState = TRANS_STATE_FINISHED;
        mDownloadView.success();
        deleteTempFile();
        FinishedActivity.launch(this, mOutputFile);

    }

    private void onTranscodeError(Throwable e) {
        mDownloadView.fail();
        deleteTempFile();
        mState = TRANS_STATE_ERROR;
    }

    private void deleteTempFile() {
        File file = new File(mDownloadFile);
        if (file.exists()) {
            file.delete();
        }
    }

    private ClipSet getClipSet() {
        return ClipSetManager.getManager().getClipSet(mPlaylistId);
    }

    private class ClipRawDataOverlayProvider implements OverlayProvider {

        @Override
        public Bitmap updateTexImage(final long ptsNs) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int currentPlayTimeMs = (int) (ptsNs / 1000000);
                    mGaugeView.setPlayTime(currentPlayTimeMs);
                    ClipSetPos clipSetPos = getClipSet().getClipSetPosByTimeOffset(currentPlayTimeMs);
                    mAdapter.setClipSetPos(clipSetPos);

                }
            });
            return BitmapUtils.getBitmapFromView(mGaugeView);
        }
    }

    private MediaFormatStrategy getMediaFormatStrategy() {
        switch (mQualityIndex) {
            case 1:
                return MediaFormatStrategyPresets.createAndroid720pStrategy();
            case 2:
                return MediaFormatStrategyPresets.createAndroid1080pStrategy();

            default:
                return MediaFormatStrategyPresets.createAndroid360pStrategy();
        }
    }


}
