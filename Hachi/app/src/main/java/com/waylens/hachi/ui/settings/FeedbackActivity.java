package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.birbit.android.jobqueue.JobManager;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.BuildConfig;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.ReportJob;
import com.waylens.hachi.rest.body.ReportFeedbackBody;
import com.waylens.hachi.ui.activities.BaseActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by laina on 16/9/26.
 */

public class FeedbackActivity extends BaseActivity {
    public static final String TAG = FeedbackActivity.class.getSimpleName();

    @BindView(R.id.feedback_content)
    EditText mFeedbackContent;

    @BindView(R.id.btn_send)
    Button mBtnSend;

    @BindView(R.id.with_log)
    CheckBox mCbWithLog;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, FeedbackActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_feedback);
        setupToolbar();
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(mFeedbackContent.getText())) {
                    Snackbar.make(mFeedbackContent, getResources().getString(R.string.feedback_empty), Snackbar.LENGTH_SHORT).show();
                } else {
                    doReportFeedback();
                    Snackbar.make(mFeedbackContent, getResources().getString(R.string.feedback_success), Snackbar.LENGTH_SHORT).show();
                    Observable.timer(1, TimeUnit.SECONDS).subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long aLong) {
                            finish();
                        }
                    });

                }
            }
        });
    }


    private void doReportFeedback() {
        final JobManager jobManager = BgJobManager.getManager();
        final ReportFeedbackBody reportFeedbackBody = new ReportFeedbackBody();
        reportFeedbackBody.reason = getResources().getStringArray(R.array.report_reason)[4];
        reportFeedbackBody.detail = mFeedbackContent.getText().toString();
        reportFeedbackBody.deviceHW = Build.MANUFACTURER + Build.MODEL;
        reportFeedbackBody.deviceOS = "android" + Build.VERSION.RELEASE;
        reportFeedbackBody.waylensApp = BuildConfig.VERSION_NAME;
        Logger.t(TAG).d(mFeedbackContent.getText().toString());
        if (mVdtCamera != null) {
            reportFeedbackBody.cameraSSID = mVdtCamera.getSSID();
            reportFeedbackBody.cameraHW = mVdtCamera.getHardwareName();
            reportFeedbackBody.cameraFW = mVdtCamera.getBspFirmware();
        }
        if (mCbWithLog.isChecked()) {
            Observable.create(new Observable.OnSubscribe<StringBuffer>() {
                @Override
                public void call(Subscriber<? super StringBuffer> subscriber) {
                    StringBuffer stringBuffer = getLog();
                    subscriber.onNext(stringBuffer);
                }
            }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Action1<StringBuffer>() {
                    @Override
                    public void call(StringBuffer stringBuffer) {
                        if (stringBuffer != null) {
                            Logger.t(TAG).d(stringBuffer.toString());
                            reportFeedbackBody.log = stringBuffer.toString();
                        }
                        ReportJob job = new ReportJob(reportFeedbackBody, ReportJob.REPORT_TYPE_FEEDBACK);
                        jobManager.addJobInBackground(job);
                    }
                });
        }
    }

    private StringBuffer getLog() {
        Logger.t(TAG).d("log", "log start");
        String shellCmd = "logcat -d TAG:W";
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        BufferedReader reader = null;
        StringBuffer stringBuffer = new StringBuffer();
        try {
            process = runtime.exec(shellCmd);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains(String.valueOf(android.os.Process.myPid()))) {
                    stringBuffer.append(line);
                    stringBuffer.append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuffer;
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();

        getToolbar().setNavigationIcon(R.drawable.ic_arrow_back);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getToolbar().setTitle(getResources().getString(R.string.feedback));
    }
}
