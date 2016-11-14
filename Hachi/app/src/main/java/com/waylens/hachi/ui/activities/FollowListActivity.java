package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.rest.IHachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.FriendList;
import com.waylens.hachi.ui.adapters.UserListRvAdapter;
import com.waylens.hachi.utils.ServerErrorHelper;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;


import butterknife.BindView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * Created by Xiaofei on 2015/9/23.
 */
public class FollowListActivity extends BaseActivity {
    private static final String TAG = FollowListActivity.class.getSimpleName();

    private static final String USER_ID = "user_id";
    private static final String IS_FOLLOWERS = "is_followers";

    private String mUserId;
    private boolean mIsFollowers;


    private UserListRvAdapter mUserListAdatper;


    @BindView(R.id.view_animator)
    ViewAnimator viewAnimator;

    @BindView(R.id.rvFollowList)
    RecyclerView mRvFollowList;

    public static void launch(Context context, String userId, boolean isFollowers) {
        Intent intent = new Intent(context, FollowListActivity.class);
        intent.putExtra(USER_ID, userId);
        intent.putExtra(IS_FOLLOWERS, isFollowers);
        context.startActivity(intent);
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
        mUserId = intent.getStringExtra(USER_ID);
        mIsFollowers = intent.getBooleanExtra(IS_FOLLOWERS, false);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_followlist);
        setupToolbar();
        setupFollowList();
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        if (mToolbar != null) {
            String title = mIsFollowers ? getString(R.string.followers) : getString(R.string
                .following);
            mToolbar.setTitle(title);
            mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    private void setupFollowList() {
        mRvFollowList.setLayoutManager(new LinearLayoutManager(this));
        mUserListAdatper = new UserListRvAdapter(this);
        mRvFollowList.setAdapter(mUserListAdatper);

        String follow;
        if (mIsFollowers) {
            follow = "followers";
        } else {
            follow = "followings";
        }
        IHachiApi hachiApi = HachiService.createHachiApiService();
        hachiApi.getFriendListRx(follow, mUserId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<FriendList>() {
                @Override
                public void onNext(FriendList friendList) {
                    mUserListAdatper.setUserList(friendList.friends);
                    viewAnimator.setDisplayedChild(1);
                }

                @Override
                public void onError(Throwable e) {
                    ServerErrorHelper.showErrorMessage(mToolbar, e);
                }
            });
    }


}
