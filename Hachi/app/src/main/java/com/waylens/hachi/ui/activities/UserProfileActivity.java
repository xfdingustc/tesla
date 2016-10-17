package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.FollowJob;
import com.waylens.hachi.bgjob.social.ReportJob;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.MomentAmount;
import com.waylens.hachi.rest.bean.User;
import com.waylens.hachi.rest.body.ReportUserBody;
import com.waylens.hachi.rest.response.FollowInfo;
import com.waylens.hachi.rest.response.MomentListResponse;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.community.feed.MomentsListAdapter;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.entities.moment.MomentEx;
import com.waylens.hachi.ui.settings.ProfileSettingActivity;
import com.waylens.hachi.ui.views.AvatarView;
import com.waylens.hachi.ui.views.RecyclerViewExt;
import com.waylens.hachi.utils.TransitionHelper;
import com.xfdingustc.rxutils.library.SimpleSubscribe;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.functions.Func3;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2015/9/21.
 */
public class UserProfileActivity extends BaseActivity {
    private static final String TAG = UserProfileActivity.class.getSimpleName();
    private static final String EXTRA_USER = "user";
//    private static final String EXTRA_USER_ID = "user_id";
//    private static final String EXTRA_USER_AVATAR = "user_avatar";

//    private String mUserID;
    private MomentsListAdapter mMomentRvAdapter;

    private String mReportReason;

    private UserProfileZip mUserInfoEx;

    private User mUser;

    private int mCurrentCursor;


    @BindView(R.id.rvUserMomentList)
    RecyclerViewExt mRvUserMomentList;


//    public static void launch(Activity activity, String userID, View transitionView) {
//        Intent intent = new Intent(activity, UserProfileActivity.class);
//        intent.putExtra(EXTRA_USER_ID, userID);
//        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        final Pair<View, String>[] pairs = TransitionHelper.createSafeTransitionParticipants(activity,
//            false, new Pair<>(transitionView, activity.getString(R.string.trans_avatar)));
//        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pairs);
//        ActivityCompat.startActivity(activity, intent, options.toBundle());
//    }

    public static void launch(Activity activity, User user, View transitionView) {
        Intent intent = new Intent(activity, UserProfileActivity.class);
        intent.putExtra(EXTRA_USER, user);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final Pair<View, String>[] pairs = TransitionHelper.createSafeTransitionParticipants(activity,
            false, new Pair<>(transitionView, activity.getString(R.string.trans_avatar)));
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pairs);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @BindView(R.id.userAvatar)
    AvatarView userAvatar;

    @BindView(R.id.btnFollowersCount)
    TextView mTvFollowersCount;

    @BindView(R.id.following_count)
    TextView followingCount;

    @BindView(R.id.follow)
    Button mBtnFollow;

    @BindView(R.id.user_name)
    TextView mUserName;


    @BindView(R.id.moment_count)
    TextView momentCount;


    @OnClick(R.id.ll_followers)
    public void onBtnFollowerCountClicked() {
        FollowListActivity.launch(this, mUser.userID, true);
    }

    @OnClick(R.id.ll_following)
    public void onFollowingCountClicked() {
        FollowListActivity.launch(this, mUser.userID, false);
    }


    @OnClick(R.id.follow)
    public void onBtnFollowClicked() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            AuthorizeActivity.launch(this);
            return;
        }

        if (SessionManager.getInstance().isCurrentUser(mUserInfoEx.userInfo.userName)) {
            ProfileSettingActivity.launch(this);
            return;
        }

        if (!SessionManager.checkUserVerified(this)) {
            return;
        }
        JobManager jobManager = BgJobManager.getManager();
        FollowJob job = new FollowJob(mUser.userID, !mUserInfoEx.followInfo.isMyFollowing);
        jobManager.addJobInBackground(job);
        mUserInfoEx.followInfo.isMyFollowing = !mUserInfoEx.followInfo.isMyFollowing;
        if (!mUserInfoEx.followInfo.isMyFollowing) {
            mUserInfoEx.followInfo.followers--;
        } else {
            mUserInfoEx.followInfo.followers++;
        }
        updateFollowInfo();
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
        mUser = (User)intent.getSerializableExtra(EXTRA_USER);
        mReportReason = getResources().getStringArray(R.array.report_reason)[0];
        initViews();
    }


    private void initViews() {
        setContentView(R.layout.activity_user_profile);
//        setupToolbar();
        mRvUserMomentList.setVisibility(View.VISIBLE);
        mMomentRvAdapter = new MomentsListAdapter(this);

        mRvUserMomentList.setAdapter(mMomentRvAdapter);


        fetchUserProfile();

        if (mUser != null) {
            userAvatar.loadAvatar(mUser);
        }


        mRvUserMomentList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void fetchUserProfile() {
        Observable<UserInfo> userInfoObservable = HachiService.createHachiApiService().getUserInfoRx(mUser.userID);

        Observable<FollowInfo> followInfoObservable = HachiService.createHachiApiService().getFollowInfoRx(mUser.userID);

        Observable<MomentAmount> getMomentAmount = HachiService.createHachiApiService().getUserMomentAmountRx(mUser.userID);

//        Observable<MomentListResponse> momentListResponseObservable = mHachiApi.getUserMomentsRx(mUserID, mCurrentCursor);

        Observable.zip(userInfoObservable, followInfoObservable, getMomentAmount, new Func3<UserInfo, FollowInfo, MomentAmount, UserProfileZip>() {

            @Override
            public UserProfileZip call(UserInfo userInfo, FollowInfo followInfo, MomentAmount amount) {
                UserProfileZip userInfoEx = new UserProfileZip();
                userInfoEx.userInfo = userInfo;
                userInfoEx.followInfo = followInfo;
                userInfoEx.amount = amount;
//                userInfoEx.momentList = momentListResponse;
                return userInfoEx;
            }
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<UserProfileZip>() {
                @Override
                public void onNext(UserProfileZip userProfileZip) {
                    mUserInfoEx = userProfileZip;
                    Logger.t(TAG).d("get user info ex");
                    setupUserProfileHeaderView();

                    loadUserMoment(mCurrentCursor, true);
                }
            });
    }

    private void setupUserProfileHeaderView() {
        userAvatar.loadAvatar(mUserInfoEx.userInfo.avatarUrl, mUserInfoEx.userInfo.userName);

        mUserName.setText(mUserInfoEx.userInfo.displayName);
        momentCount.setText(String.valueOf(mUserInfoEx.amount.amount));

        if (SessionManager.getInstance().isCurrentUser(mUserInfoEx.userInfo.userName)) {
            mBtnFollow.setText(R.string.edit_profile);
        }

        updateFollowInfo();
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().getMenu().clear();
        getToolbar().inflateMenu(R.menu.menu_user_profile);
        if (mUser.userID.equals(SessionManager.getInstance().getUserId())) {
            getToolbar().getMenu().removeItem(R.id.report);
        }
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.report:
                        if (!SessionManager.getInstance().isLoggedIn()) {
                            AuthorizeActivity.launch(UserProfileActivity.this);
                            return true;
                        }
                        if (!SessionManager.checkUserVerified(UserProfileActivity.this)) {
                            return true;
                        }

                        DialogHelper.showReportUserDialog(UserProfileActivity.this, new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                int index = dialog.getSelectedIndex();
                                mReportReason = getResources().getStringArray(R.array.report_reason)[index];
                                Logger.t(TAG).d("report reason:" + mReportReason + "index:" + index);
                                doReportUser();
                            }
                        });
                        break;
                }
                return true;
            }
        });
    }

//    private void doBlockUser() {
//        String url = Constants.API_BLOCK;
//        JSONObject requestBody = new JSONObject();
//
//        try {
//            requestBody.put("userID", mUserID);
//            Logger.t(TAG).json(requestBody.toString());
//            AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST, url, requestBody, new Response.Listener<JSONObject>() {
//                @Override
//                public void onResponse(JSONObject response) {
//                    Logger.t(TAG).json(response.toString());
//                    Snackbar.make(mRvUserMomentList, "Block user", Snackbar.LENGTH_LONG).show();
//                }
//            }, new Response.ErrorListener() {
//                @Override
//                public void onErrorResponse(VolleyError error) {
//                    Logger.t(TAG).d(error.toString());
//                }
//            });
//            mRequestQueue.add(request);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

    private void doReportUser() {
        JobManager jobManager = BgJobManager.getManager();
        ReportUserBody reportUserBody = new ReportUserBody();
        reportUserBody.userID = mUser.userID;
        reportUserBody.reason = mReportReason;
        reportUserBody.detail = "";
        Logger.t(TAG).d(mReportReason);

        ReportJob job = new ReportJob(reportUserBody, ReportJob.REPORT_TYPE_USER);
        jobManager.addJobInBackground(job);
    }


    private void loadUserMoment(int cursor, final boolean isRefresh) {
        Logger.t(TAG).d("load user moment, cursor: " + cursor);
        HachiService.createHachiApiService().getUserMomentsRx(mUser.userID, cursor)
            .map(new Func1<MomentListResponse, MomentListResponse>() {
                @Override
                public MomentListResponse call(MomentListResponse momentInfo) {
                    for (MomentEx moment : momentInfo.moments) {
                        moment.owner = new User();
                        moment.owner.userID = mUser.userID;
                        moment.owner.avatarUrl = mUserInfoEx.userInfo.avatarUrl;
                        moment.owner.userName = mUserInfoEx.userInfo.userName;
                    }
                    return momentInfo;
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<MomentListResponse>() {
                @Override
                public void onNext(MomentListResponse momentListResponse) {
                    onLoadUserMoment(momentListResponse, isRefresh);

                }
            });


    }

    private void onLoadUserMoment(MomentListResponse momentListResponse, boolean isRefresh) {
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
            mRvUserMomentList.setOnLoadMoreListener(new RecyclerViewExt.OnLoadMoreListener() {
                @Override
                public void loadMore() {
                    loadUserMoment(mCurrentCursor, false);
                }
            });
        }
    }

    private void updateFollowInfo() {
        mTvFollowersCount.setText(Integer.toString(mUserInfoEx.followInfo.followers));
        followingCount.setText(Integer.toString(mUserInfoEx.followInfo.followings));
        if (!SessionManager.getInstance().isCurrentUser(mUserInfoEx.userInfo.userName)) {
            if (mUserInfoEx.followInfo.isMyFollowing) {
                mBtnFollow.setText(R.string.followed);
                mBtnFollow.setActivated(true);
            } else {
                mBtnFollow.setText(R.string.follow);
                mBtnFollow.setActivated(false);
            }
        }

    }

    public class UserProfileZip {
        UserInfo userInfo;
        FollowInfo followInfo;
        MomentAmount amount;
    }


}
