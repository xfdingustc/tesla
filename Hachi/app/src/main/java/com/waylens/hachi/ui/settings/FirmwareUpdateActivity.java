package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.lzyzsd.circleprogress.ArcProgress;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.camera.VdtCamera;
import com.waylens.hachi.rest.bean.Firmware;
import com.waylens.hachi.service.download.DownloadAPI;
import com.waylens.hachi.service.download.DownloadProgressListener;
import com.waylens.hachi.service.download.Downloadable;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.utils.FileUtils;
import com.waylens.hachi.utils.HashUtils;
import com.waylens.hachi.utils.Hex;
import com.waylens.hachi.utils.StringUtils;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;


import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/8/23.
 */
public class FirmwareUpdateActivity extends BaseActivity {
    private static final String TAG = FirmwareUpdateActivity.class.getSimpleName();
    private static final String EXTRA_FIRMWARE_INFO = "firmwareinfo";

    private static final int UPGRADE_STATE_NONE = 0;
    private static final int UPGRADE_STATE_DOWNLOADED = 1;
    private static final int UPGRADE_STATE_UPLOADED = 2;

    private Firmware mFirmware;

    private int mUpgradeState = UPGRADE_STATE_NONE;

    private File mDownloadFirmware;

    @BindView(R.id.progress_bar)
    ArcProgress mProgressBar;

    @BindView(R.id.tv_bottom_text)
    TextView mTvBottomText;

    @BindView(R.id.btn_retry)
    Button btnRetry;

    @OnClick(R.id.btn_retry)
    public void onBtnRetryClicked() {
        if (mUpgradeState == UPGRADE_STATE_NONE) {
            doDownloadFirmware();
        } else if (mUpgradeState == UPGRADE_STATE_DOWNLOADED) {
            doSendFirmware2Camera();
        }
    }


    public static void launch(Activity activity, Firmware firmware) {
        Intent intent = new Intent(activity, FirmwareUpdateActivity.class);
        intent.putExtra(EXTRA_FIRMWARE_INFO, firmware);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    public void onBackPressed() {
        new MaterialDialog.Builder(this)
            .title(R.string.cancel_firmware_upgrade)
            .positiveText(R.string.leave_anyway)
            .negativeText(R.string.continuee)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    FirmwareUpdateActivity.super.onBackPressed();
                }
            })
            .show();
    }

    @Override
    protected void init() {
        super.init();
        mFirmware = (Firmware) getIntent().getSerializableExtra(EXTRA_FIRMWARE_INFO);
        initViews();
        doDownloadFirmware();
    }

    private void initViews() {
        setContentView(R.layout.activity_firmware_update);
        setupToolbar();
        mProgressBar.setMax(100);
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.firmware_upgrade);
    }

    private void doDownloadFirmware() {
        btnRetry.setVisibility(View.GONE);
        Logger.t(TAG).d(mFirmware.url);
        mTvBottomText.setText(R.string.download_firmware);
        DownloadProgressListener listener = new DownloadProgressListener() {
            @Override
            public void update(long bytesRead, long contentLength, boolean done) {
                Downloadable downloadable = new Downloadable();
                downloadable.setTotalFileSize(contentLength);
                downloadable.setCurrentFileSize(bytesRead);
                final int progress = (int) ((bytesRead * 100) / contentLength);
                downloadable.setProgress(progress);
//                sendNotification(downloadable);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setProgress(progress);
                    }
                });

            }
        };

        mDownloadFirmware = new File(FileUtils.getFirmwareDownloadPath());
        Logger.t(TAG).d("output file: " + mDownloadFirmware);
        String baseUrl = StringUtils.getHostName(mFirmware.url);

        new DownloadAPI(baseUrl, listener)
            .downloadFile(mFirmware.url, mDownloadFirmware, new Subscriber() {
                @Override
                public void onCompleted() {
//                    downloadCompleted();
                    startFirmwareMd5Check(mDownloadFirmware);
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
//                    downloadError();
                    mTvBottomText.setText(R.string.download_firmware_error);
                    btnRetry.setVisibility(View.VISIBLE);
                    Logger.t(TAG).d("Download error: " + e.getMessage());
                }

                @Override
                public void onNext(Object o) {

                }
            });
    }


    private void startFirmwareMd5Check(final File file) {
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                subscriber.onNext(0);
                try {
                    final String downloadFileMd5 = Hex.encodeHexString(HashUtils.encodeMD5(file));
                    if (downloadFileMd5.equals(mFirmware.md5)) {
                        subscriber.onNext(1);
                    } else {
                        subscriber.onNext(-1);
                    }
                } catch (IOException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

            }

        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<Integer>() {
                @Override
                public void onNext(Integer integer) {
                    switch (integer) {
                        case 0:
                            mTvBottomText.setText(R.string.firmware_check);
                            break;
                        case 1:
                            mTvBottomText.setText(R.string.firmware_correct);
                            mUpgradeState = UPGRADE_STATE_DOWNLOADED;
                            doSendFirmware2Camera();
                            break;
                        case -1:
                            mTvBottomText.setText(R.string.firmware_corrupt);
                            btnRetry.setVisibility(View.VISIBLE);
                            break;
                    }
                }
            });
    }

    private void doSendFirmware2Camera() {
        btnRetry.setVisibility(View.GONE);
        mVdtCamera.sendNewFirmware(mFirmware.md5, new VdtCamera.OnNewFwVersionListern() {
            @Override
            public void onNewVersion(final int response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleFwVersionRetCode(response);
                    }
                });


            }
        });

    }

    private void handleFwVersionRetCode(int response) {
        Logger.t(TAG).d("response: " + response);
        switch (response) {
            case 1:
                startUploadFirmware();
                break;
            case 0:
                doUpgradeFirmware();
                break;
            case -1:
                mTvBottomText.setText(R.string.upload_failed);
                btnRetry.setVisibility(View.VISIBLE);
                break;
        }

    }

    private void startUploadFirmware() {
        mTvBottomText.setText(R.string.upload_firmware);
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                FirmwareWriter writer = new FirmwareWriter(mDownloadFirmware, mVdtCamera, subscriber);
                writer.start();
                subscriber.onCompleted();

            }
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<Integer>() {
                @Override
                public void onNext(Integer integer) {
                    mProgressBar.setProgress((int) (((long) integer * 100) / mDownloadFirmware.length()));
                }

                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    super.onError(e);
                    mTvBottomText.setText(R.string.upload_failed);
                    btnRetry.setVisibility(View.VISIBLE);
                }
            });
    }

    private void doUpgradeFirmware() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .title(R.string.firmware_upgrade)
            .content(R.string.camera_will_disconnected)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    mVdtCamera.upgradeFirmware();
                    MainActivity.launch(FirmwareUpdateActivity.this);
                }
            }).show();

    }


}
