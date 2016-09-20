package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.lzyzsd.circleprogress.ArcProgress;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.download.DownloadHelper;
import com.waylens.hachi.service.download.DownloadServiceRx;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.utils.HashUtils2;
import com.waylens.hachi.utils.Hex;
import com.xfdingustc.snipe.control.VdtCamera;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import butterknife.BindView;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/8/23.
 */
public class FirmwareUpdateActivity extends BaseActivity {
    private static final String TAG = FirmwareUpdateActivity.class.getSimpleName();
    private static final String EXTRA_FIRMWARE_INFO = "firmwareinfo";

    private FirmwareInfo mFirmwareInfo;

    @BindView(R.id.progress_bar)
    ArcProgress mProgressBar;

    @BindView(R.id.tv_bottom_text)
    TextView mTvBottomText;


    private BroadcastReceiver mDownloadProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int what = intent.getIntExtra(DownloadServiceRx.EVENT_EXTRA_WHAT, -1);
            switch (what) {
                case DownloadServiceRx.EVENT_WHAT_DOWNLOAD_PROGRESS:
                    int progress = intent.getIntExtra(DownloadServiceRx.EVENT_EXTRA_DOWNLOAD_PROGRESS, 0);
                    mProgressBar.setProgress(progress);
                    break;
                case DownloadServiceRx.EVENT_WHAT_DOWNLOAD_FINSHED:
                    final String file = intent.getStringExtra(DownloadServiceRx.EVENT_EXTRA_DOWNLOAD_FILE_PATH);
                    startFirmwareMd5Check(new File(file));

            }


        }
    };


    public static void launch(Activity activity, FirmwareInfo firmwareInfo) {
        Intent intent = new Intent(activity, FirmwareUpdateActivity.class);
        intent.putExtra(EXTRA_FIRMWARE_INFO, firmwareInfo);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(DownloadServiceRx.INTENT_FILTER_DOWNLOAD_INTENT_SERVICE);
        registerReceiver(mDownloadProgressReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiver(mDownloadProgressReceiver);
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
        mFirmwareInfo = (FirmwareInfo) getIntent().getSerializableExtra(EXTRA_FIRMWARE_INFO);
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
//        InetDownloadService.start(this, mFirmwareInfo.getUrl());
        DownloadServiceRx.start(this, mFirmwareInfo.getUrl(), DownloadHelper.getFirmwareDownloadPath());
        mTvBottomText.setText(R.string.download_firmware);
    }


    private void startFirmwareMd5Check(final File file) {
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                subscriber.onNext(0);

                try {
                    final String downloadFileMd5 = Hex.encodeHexString(HashUtils2.encodeMD5(file));
                    if (downloadFileMd5.equals(mFirmwareInfo.getMd5())) {
                        subscriber.onNext(1);
                    } else {
                        subscriber.onNext(-1);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

            }

        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<Integer>() {

                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Integer integer) {
                    switch (integer) {
                        case 0:
                            mTvBottomText.setText(R.string.firmware_check);
                            break;
                        case 1:
                            mTvBottomText.setText(R.string.firmware_correct);
                            doSendFirmware2Camera(file);
                            break;
                        case -1:
                            mTvBottomText.setText(R.string.firmware_corrupt);
                            break;
                    }

                }
            });


    }

    private void doSendFirmware2Camera(final File file) {
        mVdtCamera.sendNewFirmware(mFirmwareInfo.getMd5(), new VdtCamera.OnNewFwVersionListern() {
            @Override
            public void onNewVersion(int response) {
                Logger.t(TAG).d("response: " + response);
                if (response == 1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTvBottomText.setText(R.string.upload_firmware);

                        }
                    });

                    Observable.create(new Observable.OnSubscribe<Integer>() {
                        @Override
                        public void call(Subscriber<? super Integer> subscriber) {
                            FirmwareWriter writer = new FirmwareWriter(file, mVdtCamera, subscriber);
                            writer.start();
                            subscriber.onCompleted();

                        }
                    })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Integer>() {

                            @Override
                            public void onCompleted() {
                                MainActivity.launch(FirmwareUpdateActivity.this);
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onNext(Integer integer) {
                                mProgressBar.setProgress((int)(((long)integer * 100) / file.length()));
                            }
                        });


                } else if (response == 0) {
                    doUpgradeFirmware();
                }
            }
        });

    }

    private void doUpgradeFirmware() {

        mVdtCamera.upgradeFirmware();
    }


}
