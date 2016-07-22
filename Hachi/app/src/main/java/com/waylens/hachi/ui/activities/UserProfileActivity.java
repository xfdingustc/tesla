package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.birbit.android.jobqueue.JobManager;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.ReportJob;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.ReportUserBody;
import com.waylens.hachi.rest.response.FollowInfo;
import com.waylens.hachi.rest.response.MomentListResponse;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.ui.community.feed.IMomentListAdapterHeaderView;
import com.waylens.hachi.ui.community.feed.MomentsListAdapter;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.User;
import com.waylens.hachi.ui.user.UserProfileHeaderView;
import com.waylens.hachi.ui.views.RecyclerViewExt;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2015/9/21.
 */
public class UserProfileActivity extends BaseActivity {
    private static final String TAG = UserProfileActivity.class.getSimpleName();
    private static final String EXTRA_USER_ID = "user_id";

    private String mUserID;
    private MomentsListAdapter mMomentRvAdapter;

    private UserInfo mUserInfo;

    private String mReportReason;

    private HachiApi mHachiApi = HachiService.createHachiApiService();


//    private ArrayList<Moment> mMomentList;

    private FollowInfo mFollowInfo;

    private int mCurrentCursor;


    @BindView(R.id.rvUserMomentList)
    RecyclerViewExt mRvUserMomentList;


    public static void launch(Activity activity, String userID) {
        Intent intent = new Intent(activity, UserProfileActivity.class);
        intent.putExtra(EXTRA_USER_ID, userID);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);

    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mUserID = intent.getStringExtra(EXTRA_USER_ID);
        mReportReason = getResources().getStringArray(R.array.report_reason)[0];
        initViews();
    }


    private void initViews() {
        setContentView(R.layout.activity_user_profile);
        setupToolbar();
        mRvUserMomentList.setVisibility(View.VISIBLE);
        mMomentRvAdapter = new MomentsListAdapter(this);

        mRvUserMomentList.setAdapter(mMomentRvAdapter);
        mRvUserMomentList.setOnLoadMoreListener(new RecyclerViewExt.OnLoadMoreListener() {
            @Override
            public void loadMore() {
                loadUserMoment(mCurrentCursor, false);
            }
        });

        fetchUserProfile();


        mRvUserMomentList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void fetchUserProfile() {
        Observable.create(new Observable.OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                Call<UserInfo> userInfoCall = mHachiApi.getUserInfo(mUserID);
                try {
                    mUserInfo = userInfoCall.execute().body();
                } catch (IOException e) {
                    subscriber.onError(e);
                }


                Call<FollowInfo> followInfoCall = mHachiApi.getFollowInfo(mUserID);
                try {
                    mFollowInfo = followInfoCall.execute().body();
                } catch (IOException e) {
                    subscriber.onError(e);
                }

                subscriber.onNext(0);

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
                    e.printStackTrace();
                }

                @Override
                public void onNext(Integer integer) {
                    mToolbar.setTitle(mUserInfo.displayName);

                    setupUserProfileHeaderView();

                    loadUserMoment(mCurrentCursor, true);
                }


            });
    }

    private void setupUserProfileHeaderView() {
        IMomentListAdapterHeaderView headerView = new UserProfileHeaderView(this, mUserID, mUserInfo, mFollowInfo);
        mMomentRvAdapter.setHeaderView(headerView);
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().getMenu().clear();
        getToolbar().inflateMenu(R.menu.menu_user_profile);
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.report:
                        MaterialDialog dialog = new MaterialDialog.Builder(UserProfileActivity.this)
                            .title(R.string.report)
                            .items(R.array.report_reason)
                            .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                    return true;
                                }
                            })
                            .positiveText(R.string.report)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    int index = dialog.getSelectedIndex();
                                    mReportReason = getResources().getStringArray(R.array.report_reason)[index];
                                    Logger.t(TAG).d("report reason:" + mReportReason + "index:" + index);
                                    doReportUser();
                                }
                            })
                            .show();

                        break;
//                    case R.id.block:
//                        doBlockUser();
//                        break;
                }
                return true;
            }
        });
    }

    private void doBlockUser() {
        String url = Constants.API_BLOCK;
        JSONObject requestBody = new JSONObject();

        try {
            requestBody.put("userID", mUserID);
            Logger.t(TAG).json(requestBody.toString());
            AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST, url, requestBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).json(response.toString());
                    Snackbar.make(mRvUserMomentList, "Block user", Snackbar.LENGTH_LONG).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Logger.t(TAG).d(error.toString());
                }
            });
            mRequestQueue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void doReportUser() {
        JobManager jobManager = BgJobManager.getManager();
        ReportUserBody reportUserBody = new ReportUserBody();
        reportUserBody.userID = mUserID;
        reportUserBody.reason = mReportReason;
        reportUserBody.detail = "";
        Logger.t(TAG).d(mReportReason);

        ReportJob job = new ReportJob(reportUserBody, ReportJob.REPORT_TYPE_USER);
        jobManager.addJobInBackground(job);


    }


    private void loadUserMoment(int cursor, final boolean isRefresh) {

        Call<MomentListResponse> momentListResponseCall = mHachiApi.getUserMoments(mUserID, cursor);
        momentListResponseCall.enqueue(new Callback<MomentListResponse>() {
            @Override
            public void onResponse(Call<MomentListResponse> call, retrofit2.Response<MomentListResponse> response) {
                MomentListResponse momentListResponse = response.body();
                for (Moment moment : momentListResponse.moments) {
                    moment.owner = new User();
                    moment.owner.userID = mUserID;
                    moment.owner.avatarUrl = mUserInfo.avatarUrl;
                    Logger.t(TAG).d(moment.toString());
                }

                mCurrentCursor += momentListResponse.moments.size();
                if (isRefresh) {
                    mMomentRvAdapter.setMoments(momentListResponse.moments);
                } else {
                    mMomentRvAdapter.addMoments(momentListResponse.moments);
                }

                mRvUserMomentList.setIsLoadingMore(false);
                if (!momentListResponse.hasMore) {
                    mRvUserMomentList.setEnableLoadMore(false);
                    mMomentRvAdapter.setHasMore(false);
                } else {
                    mMomentRvAdapter.setHasMore(true);
                }
            }

            @Override
            public void onFailure(Call<MomentListResponse> call, Throwable t) {

            }
        });

    }


}
