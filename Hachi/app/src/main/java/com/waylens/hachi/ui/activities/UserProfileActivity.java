package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.MomentsRecyclerAdapter;
import com.waylens.hachi.ui.adapters.UserProfileFeedAdapter;
import com.waylens.hachi.ui.entities.User;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.utils.ImageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2015/9/21.
 */
public class UserProfileActivity extends BaseActivity {
    private static final String TAG = UserProfileActivity.class.getSimpleName();
    private static final String USER_ID = "user_id";
    private String mUserID;
    private MomentsRecyclerAdapter mMomentRvAdapter;
    private User mUser;

    private ArrayList<Moment> mMomentList;

    @Bind(R.id.rvUserMomentList)
    RecyclerView mRvUserMomentList;

    @Bind(R.id.userAvatar)
    CircleImageView mCivUserAvatar;



    @Bind(R.id.btnFollowersCount)
    TextView mBtnFollowersCount;

    @Bind(R.id.btnFollowingCount)
    TextView mBtnFollowingCount;

    @Bind(R.id.btnFollow)
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
        if (mUser.getIsFollowing()) {
            unfollowUser(mUserID);
        } else {
            followUser(mUserID);
        }
    }

    public static void launch(Context context, String userID) {
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.putExtra(USER_ID, userID);
        context.startActivity(intent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mUserID = intent.getStringExtra(USER_ID);
        init();
    }


    @Override
    protected void init() {
        super.init();

        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_user_profile);
        setupUserProfile();

    }



    private void setupUserProfile() {
        final String requestUrl = Constants.API_USERS + "/" + mUserID;
        AuthorizedJsonRequest request = new AuthorizedJsonRequest(requestUrl,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
//                    Logger.t(TAG).json(response.toString());
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
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(userInfo.avatarUrl, mCivUserAvatar, ImageUtils.getAvatarOptions());


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
        } else {
            mBtnFollow.setVisibility(View.VISIBLE);
        }

        mBtnFollowersCount.setText(getString(R.string.followers) + " " + userInfo.getFollowersCount());
        mBtnFollowingCount.setText(getString(R.string.following) + " " + userInfo.getFollowingsCount());

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
                    mMomentRvAdapter = new MomentsRecyclerAdapter(mMomentList, getFragmentManager(), mRequestQueue, getResources());
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
            mBtnFollow.setText(R.string.following);
            mBtnFollow.setTextColor(getResources().getColor(R.color.windowBackgroundDark));
            mBtnFollow.setBackgroundResource(R.color.app_text_color_primary);
        } else {
            mBtnFollow.setText(R.string.add_follow);
            mBtnFollow.setTextColor(getResources().getColor(R.color.app_text_color_primary));
            mBtnFollow.setBackgroundResource(R.drawable.button_with_stroke);
        }
    }

    public void followUser(final String userID) {
        String requestUrl = Constants.API_FRIENDS_FOLLOW;
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put(JsonKey.USER_ID, userID);
            AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST,
                requestUrl, requestBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).d("Follow user: " + userID);
                    setFollowButton(true);
                    mUser.setIsFollowing(true);
                    mBtnFollow.setEnabled(true);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Logger.t(TAG).d(error.toString());
                }
            });
            mRequestQueue.add(request);
           mBtnFollow.setEnabled(false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void unfollowUser(final String userID) {
        String requestUrl = Constants.API_FRIENDS_UNFOLLOW;
        JSONObject requestBody = new JSONObject();

        try {
            requestBody.put(JsonKey.USER_ID, userID);
            AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST,
                requestUrl, requestBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    setFollowButton(false);
                    mUser.setIsFollowing(false);
                    Logger.t(TAG).d("Unfollow user: " + userID);
                    mBtnFollow.setEnabled(true);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Logger.t(TAG).d(error.toString());
                }
            });
            mRequestQueue.add(request);
            mBtnFollow.setEnabled(false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
