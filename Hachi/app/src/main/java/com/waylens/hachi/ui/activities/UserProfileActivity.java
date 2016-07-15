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
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.FollowJob;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.FollowInfo;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.community.feed.MomentsListAdapter;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.settings.AccountActivity;
import com.waylens.hachi.ui.views.RecyclerViewExt;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Created by Xiaofei on 2015/9/21.
 */
public class UserProfileActivity extends BaseActivity {
    private static final String TAG = UserProfileActivity.class.getSimpleName();
    private static final String EXTRA_USER_ID = "user_id";
    private static final String EXTRA_REVEAL_START_LOCATION = "reveal_start_location";
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


    @BindView(R.id.userAvatar)
    CircleImageView civUserAvatar;

    @BindView(R.id.btnFollowersCount)
    TextView mTvFollowersCount;


    @BindView(R.id.btnFollow)
    TextView mBtnFollow;

    @BindView(R.id.user_name)
    TextView mUserName;

    @BindView(R.id.btn_account_setting)
    ImageButton mBtnAccountSetting;

    @OnClick(R.id.btnFollowersCount)
    public void onBtnFollowerCountClicked() {
        FollowListActivity.launch(this, mUserID, true);
    }


    @OnClick(R.id.btn_account_setting)
    public void onBtnAccountSettingClicked() {
        AccountActivity.launch(UserProfileActivity.this);
    }

    @OnClick(R.id.btnFollow)
    public void onBtnFollowClicked() {
        JobManager jobManager = BgJobManager.getManager();
        FollowJob job = new FollowJob(mUserID, !mFollowInfo.isMyFollowing);
        jobManager.addJobInBackground(job);
        mFollowInfo.isMyFollowing = !mFollowInfo.isMyFollowing;
        if (!mFollowInfo.isMyFollowing) {
            mFollowInfo.followers--;
        } else {
            mFollowInfo.followers++;
        }
        updateFollowInfo();
    }

    public static void launch(Activity activity, String userID, View startView) {
        Intent intent = new Intent(activity, UserProfileActivity.class);
        int[] startLocation = new int[2];
        startView.getLocationOnScreen(startLocation);
        startLocation[0] += startView.getWidth() / 2;
        intent.putExtra(EXTRA_USER_ID, userID);
        intent.putExtra(EXTRA_REVEAL_START_LOCATION, startLocation);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
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
        mRvUserMomentList.setVisibility(View.VISIBLE);
        mMomentRvAdapter = new MomentsListAdapter(this);
        mRvUserMomentList.setAdapter(mMomentRvAdapter);
        mRvUserMomentList.setOnLoadMoreListener(new RecyclerViewExt.OnLoadMoreListener() {
            @Override
            public void loadMore() {
                loadUserMoment(mCurrentCursor, true);
            }
        });
        setupUserProfile();
        doGetFollowInfo();
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
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
                                    mReportReason = getResources().getStringArray(R.array.report_reason)[which];
                                    return true;
                                }
                            })
                            .positiveText(R.string.report)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    doReportComment();
                                }
                            })
                            .show();

                        break;
                    case R.id.block:
                        doBlockUser();
                        break;
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

    private void doReportComment() {
        String url = Constants.API_REPORT;
        final JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("userId", mUserID);
            requestBody.put("reason", mReportReason);

            Logger.t(TAG).json(requestBody.toString());
            AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST, url, requestBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).json(response.toString());
                    Snackbar.make(mRvUserMomentList, "Report comment successfully", Snackbar.LENGTH_LONG).show();
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

    private void doGetFollowInfo() {

        Call<FollowInfo> followInfoCall = mHachiApi.getFollowInfo(mUserID);
        followInfoCall.enqueue(new Callback<FollowInfo>() {
            @Override
            public void onResponse(Call<FollowInfo> call, retrofit2.Response<FollowInfo> response) {
                mFollowInfo = response.body();
                if (mFollowInfo != null) {
                    updateFollowInfo();
                }
            }


            @Override
            public void onFailure(Call<FollowInfo> call, Throwable t) {

            }
        });
    }

    private void updateFollowInfo() {
        mTvFollowersCount.setText("" + mFollowInfo.followers + " " + getString(R.string.followers));
        if (mFollowInfo.isMyFollowing) {
            mBtnFollow.setText(R.string.followed);
            mBtnFollow.setTextColor(getResources().getColor(R.color.app_text_color_disabled));
        } else {
            mBtnFollow.setText(R.string.follow);
        }

    }


    private void setupUserProfile() {
        Logger.t(TAG).d("userId: " + mUserID);
        Call<UserInfo> userInfoCall = mHachiApi.getUserInfo(mUserID);
        userInfoCall.enqueue(new Callback<UserInfo>() {
            @Override
            public void onResponse(Call<UserInfo> call, retrofit2.Response<UserInfo> response) {
                Logger.t(TAG).d("userInfo: " + response.raw().toString());
                mUserInfo = response.body();
                if (mUserInfo != null) {
                    updateUserInfo();
                    loadUserMoment(mCurrentCursor, false);
                }
            }

            @Override
            public void onFailure(Call<UserInfo> call, Throwable t) {
                Logger.t(TAG).e(t.getMessage());
            }
        });
    }

    private void updateUserInfo() {

        Glide.with(this)
            .load(mUserInfo.avatarUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.menu_profile_photo_default)
            .dontAnimate()
            .into(civUserAvatar);

        mToolbar.setTitle(mUserInfo.displayName);
        mUserName.setText(mUserInfo.displayName);
        mToolbar.setNavigationIcon(R.drawable.navbar_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        if (isCurrentUser(mUserInfo.userName)) {
            mBtnAccountSetting.setVisibility(View.VISIBLE);
            mBtnFollow.setVisibility(View.GONE);
            mToolbar.getMenu().clear();
        } else {
            mBtnFollow.setVisibility(View.VISIBLE);
            setupToolbar();
            mBtnAccountSetting.setVisibility(View.GONE);
        }
    }


    public boolean isCurrentUser(String userName) {
        SessionManager sessionManager = SessionManager.getInstance();
        String currentUserName = sessionManager.getUserName();
        if (userName.equals(currentUserName)) {
            return true;
        }

        return false;
    }

    private void loadUserMoment(int cursor, final boolean isRefresh) {
        mRvUserMomentList.setLayoutManager(new LinearLayoutManager(this));


        String requestUrl = Constants.API_USERS + "/" + mUserID + "/moments?cursor=" + cursor;

        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(requestUrl)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    List<Moment> momentList = Moment.parseMomentArray(response);
                    for (Moment moment : momentList) {
                        moment.owner.userID = mUserID;
                        moment.owner.avatarUrl = mUserInfo.avatarUrl;
                    }
//                    mMomentRvAdapter.setMomentList(mMomentList);
                    mCurrentCursor += momentList.size();
                    mMomentRvAdapter.setMoments(momentList);
                    if (isRefresh) {
                        mMomentRvAdapter.setMoments(momentList);
                    } else {
                        mMomentRvAdapter.addMoments(momentList);
                    }

                    mRvUserMomentList.setIsLoadingMore(false);
                    if (!response.optBoolean("hasMore")) {
                        mRvUserMomentList.setEnableLoadMore(false);
                        mMomentRvAdapter.setHasMore(false);
                    } else {
                        mMomentRvAdapter.setHasMore(true);
                    }
                }
            })
            .errorListener(new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }).build();


        mRequestQueue.add(request);
    }


}
