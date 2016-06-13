package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.FollowJob;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.community.feed.MomentsListAdapter;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.User;
import com.waylens.hachi.ui.settings.AccountActivity;
import com.waylens.hachi.ui.views.RevealBackgroundView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2015/9/21.
 */
public class UserProfileActivity extends BaseActivity {
    private static final String TAG = UserProfileActivity.class.getSimpleName();
    private static final String EXTRA_USER_ID = "user_id";
    private static final String EXTRA_REVEAL_START_LOCATION = "reveal_start_location";
    private String mUserID;
    private MomentsListAdapter mMomentRvAdapter;
    private User mUser;

    private String mReportReason;

    private int[] mStartRevealLocation;


    private ArrayList<Moment> mMomentList;

    @BindView(R.id.reveal_bg)
    RevealBackgroundView mRevealBg;

    @BindView(R.id.profile_content)
    View mProfileContent;

    @BindView(R.id.rvUserMomentList)
    RecyclerView mRvUserMomentList;

    @BindView(R.id.user_profile_root)
    LinearLayout mUserProfileRoot;

    @BindView(R.id.userAvatar)
    CircleImageView civUserAvatar;

    @BindView(R.id.btnFollowersCount)
    TextView mTvFollowersCount;

    @BindView(R.id.btnFollowingCount)
    TextView mTvFollowingCount;

    @BindView(R.id.btnFollow)
    TextView mBtnFollow;

    @OnClick(R.id.btnFollowersCount)
    public void onBtnFollowerCountClicked() {
        FollowListActivity.launch(this, mUserID, true);
    }

    @OnClick(R.id.btnFollowingCount)
    public void onBtnFollowingCountClicked() {
        FollowListActivity.launch(this, mUserID, false);
    }

    @OnClick(R.id.btnFollow)
    public void onBtnFollowClicked() {
        JobManager jobManager = BgJobManager.getManager();
        FollowJob job = new FollowJob(mUserID, !mUser.getIsFollowing());
        jobManager.addJobInBackground(job);
        mUser.setIsFollowing(!mUser.getIsFollowing());
        setFollowButton(mUser.getIsFollowing());
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
        mStartRevealLocation = intent.getIntArrayExtra(EXTRA_REVEAL_START_LOCATION);
        mReportReason = getResources().getStringArray(R.array.report_reason)[0];
        initViews();
    }


    private void initViews() {
        setContentView(R.layout.activity_user_profile);
        setupRevealBackground();

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
                            .negativeText(android.R.string.cancel)
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

    private void setupRevealBackground() {
        mRevealBg.setFillPaintColor(getResources().getColor(R.color.windowBackground));
        mRevealBg.setOnStateChangeListener(new RevealBackgroundView.OnStateChangeListener() {
            @Override
            public void onStateChange(int state) {
                if (RevealBackgroundView.STATE_FINISHED == state) {
                    mProfileContent.setVisibility(View.VISIBLE);
                    mUserProfileRoot.setVisibility(View.VISIBLE);
                    mRvUserMomentList.setVisibility(View.VISIBLE);
                    setupUserProfile();
                    doGetUserList();
                } else {
//            tlUserProfileTabs.setVisibility(View.INVISIBLE);
                    mProfileContent.setVisibility(View.INVISIBLE);
                    mUserProfileRoot.setVisibility(View.INVISIBLE);
                    mRvUserMomentList.setVisibility(View.INVISIBLE);
                }
            }
        });
        mRevealBg.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRevealBg.getViewTreeObserver().removeOnPreDrawListener(this);
                mRevealBg.startFromLocation(mStartRevealLocation);
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

    private void doGetUserList() {
        String requestUrl = Constants.API_FRIENDS + mUserID;
        AuthorizedJsonRequest request = new AuthorizedJsonRequest(requestUrl, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Logger.t(TAG).json(response.toString());
                int followerCount = response.optInt("followers", 0);
                int followingCount = response.optInt("followings", 0);
//                mFollowerUserList = User.parseUserListFromJson(response);
                mTvFollowersCount.setText(getString(R.string.followers) + " " + followerCount);
                mTvFollowingCount.setText(getString(R.string.following) + " " + followingCount);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        mRequestQueue.add(request);
    }


    private void setupUserProfile() {
        final String requestUrl = Constants.API_USERS + "/" + mUserID;
        AuthorizedJsonRequest request = new AuthorizedJsonRequest(requestUrl,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).json(response.toString());
                    Gson gson = new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create();
                    mUser = gson.fromJson(response.toString(), User.class);
                    Logger.t(TAG).d("userInfo: " + mUser.toString());
                    showUserInfo(mUser);
                    setupUserMomentsFeed();
                }
            }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        mRequestQueue.add(request);
    }

    private void showUserInfo(User userInfo) {

        Glide.with(this).load(userInfo.avatarUrl).crossFade().into(civUserAvatar);

        mToolbar.setTitle(userInfo.userName);
        mToolbar.setNavigationIcon(R.drawable.navbar_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        if (isCurrentUser(userInfo)) {
            mBtnFollow.setVisibility(View.GONE);
            mToolbar.getMenu().clear();
            mToolbar.inflateMenu(R.menu.menu_profile_edit);
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.editProfile:
                            AccountActivity.launch(UserProfileActivity.this);
                            break;
                    }
                    return true;
                }
            });
        } else {
            mBtnFollow.setVisibility(View.VISIBLE);
            setupToolbar();
        }

//        mTvFollowersCount.setText(getString(R.string.followers) + " " + userInfo.getFollowersCount());
//        mTvFollowingCount.setText(getString(R.string.following) + " " + userInfo.getFollowingsCount());

        setFollowButton(userInfo.getIsFollowing());
    }


    public boolean isCurrentUser(User userInfo) {
        SessionManager sessionManager = SessionManager.getInstance();
        String currentUserName = sessionManager.getUserName();
        if (userInfo.userName.equals(currentUserName)) {
            return true;
        }

        return false;
    }

    private void setupUserMomentsFeed() {
        mRvUserMomentList.setLayoutManager(new LinearLayoutManager(this));
        //mMomentRvAdapter = new UserProfileFeedAdapter(this);


        String requestUrl = Constants.API_USERS + "/" + mUserID + "/moments";
        AuthorizedJsonRequest request = new AuthorizedJsonRequest(requestUrl,
            new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {
//                    Logger.t(TAG).json(response.toString());
                    mMomentList = parseMomentArray(response);
//                    mMomentRvAdapter.setMomentList(mMomentList);
                    mMomentRvAdapter = new MomentsListAdapter(UserProfileActivity.this, mMomentList);
                    mMomentRvAdapter.setUserInfo(mUser);
                    mRvUserMomentList.setAdapter(mMomentRvAdapter);
                }
            },
            new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });

        mRequestQueue.add(request);
    }

    private ArrayList<Moment> parseMomentArray(JSONObject response) {
        ArrayList<Moment> moments = new ArrayList<>();
        try {
            JSONArray momentArray = response.getJSONArray(JsonKey.MOMENTS);
            for (int i = 0; i < momentArray.length(); i++) {
                JSONObject momentObject = momentArray.getJSONObject(i);
                Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
                Moment moment = gson.fromJson(momentObject.toString(), Moment.class);
                moments.add(moment);
//                Logger.t(TAG).d("Add one moment: " + moment.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return moments;
    }

    private void setFollowButton(boolean isFollowing) {
        if (isFollowing) {
            mBtnFollow.setText(R.string.unfollow);
            mBtnFollow.setTextColor(getResources().getColor(R.color.windowBackgroundDark));
            mBtnFollow.setBackgroundResource(R.color.app_text_color_primary);
        } else {
            mBtnFollow.setText(R.string.add_follow);
            mBtnFollow.setTextColor(getResources().getColor(R.color.app_text_color_primary));
            mBtnFollow.setBackgroundResource(R.drawable.button_with_stroke);
        }
    }


}
