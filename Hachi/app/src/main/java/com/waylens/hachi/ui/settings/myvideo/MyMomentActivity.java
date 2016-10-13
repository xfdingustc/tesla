package com.waylens.hachi.ui.settings.myvideo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ViewAnimator;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.bgjob.upload.UploadManager;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.MomentListResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.entities.Moment;
import com.xfdingustc.rxutils.library.SimpleSubscribe;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.util.List;

import butterknife.BindView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/6/17.
 */
public class MyMomentActivity extends BaseActivity implements UploadManager.OnUploadJobStateChangeListener {
    private static final String TAG = MyMomentActivity.class.getSimpleName();

    private MomentItemAdapter mVideoItemAdapter;



    private static final int VIEW_ANIMATOR_LOADING_PROGRESS = 0;
    private static final int VIEW_ANIMATOR_MOMENT_LIST = 1;
    private static final int VIEW_ANIMATOR_EMPTY_VIEW = 2;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, MyMomentActivity.class);
        activity.startActivity(intent);
    }


    @BindView(R.id.moment_list)
    RecyclerView mRvMomentList;

    @BindView(R.id.view_animator)
    ViewAnimator mViewAnimator;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

//    @Override
//    public void finish() {
//        if (UploadManager.getManager().getUploadingJobCount() > 0) {
//            MaterialDialog dialog = new MaterialDialog.Builder(MyMomentActivity.this)
//                .content(R.string.exit_video_upload_confirm)
//                .negativeText(R.string.stay)
//                .positiveText(R.string.leave_anyway)
//                .onPositive(new MaterialDialog.SingleButtonCallback() {
//                    @Override
//                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                        MyMomentActivity.super.finish();
//                    }
//                })
//                .show();
//        } else {
//            MyMomentActivity.super.finish();
//        }
//    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    @Override
    public void onUploadJobStateChanged(UploadMomentJob job, int index) {
        if (job.getState() == UploadMomentJob.UPLOAD_STATE_FINISHED) {
            loadUserMoment(0, false);
        }
    }

    @Override
    public void onUploadJobAdded() {

    }

    @Override
    public void onUploadJobRemoved() {
    }

    private void initViews() {
        setContentView(R.layout.activity_upload);
        setupToolbar();
        setupMyVideoList();
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.my_moments);

        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    private void setupMyVideoList() {


        mRvMomentList.setLayoutManager(new LinearLayoutManager(this));
        mVideoItemAdapter = new MomentItemAdapter(this);
        mRvMomentList.setAdapter(mVideoItemAdapter);


        UploadManager.getManager().addOnUploadJobStateChangedListener(this);
        loadUserMoment(0, false);
    }


    private void loadUserMoment(int cursor, final boolean isRefresh) {
        SessionManager sessionManager = SessionManager.getInstance();
        HachiService.createHachiApiService().getUserMomentsRx(sessionManager.getUserId(), cursor)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<MomentListResponse>() {
                @Override
                public void onNext(MomentListResponse momentListResponse) {
                    if (momentListResponse.moments.size() > 0) {
                        mViewAnimator.setDisplayedChild(VIEW_ANIMATOR_MOMENT_LIST);
                    } else {
                        mViewAnimator.setDisplayedChild(VIEW_ANIMATOR_EMPTY_VIEW);
                    }
                    mVideoItemAdapter.setUploadedMomentList(momentListResponse.moments);
                }
            });
    }
}
