package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.TextView;

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
import com.waylens.hachi.ui.adapters.UserProfileFeedAdapter;
import com.waylens.hachi.ui.entities.BasicUserInfo;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.utils.ImageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2015/9/21.
 */
public class UserProfileActivity extends BaseActivity {
    private static final String TAG = UserProfileActivity.class.getSimpleName();
    private static final String USER_ID = "user_id";
    private String mUserID;
    private UserProfileFeedAdapter mMomentRvAdapter;

    private List<Moment> mMomentList;

    @Bind(R.id.rvUserMomentList)
    RecyclerView mRvUserMomentList;

    @Bind(R.id.userAvatar)
    CircleImageView mCivUserAvatar;

    @Bind(R.id.userName)
    TextView mTvUserName;

    @Bind(R.id.tvFollowersCount)
    Button mTvFollowersCount;

    @Bind(R.id.tvFollowingCount)
    Button mTvFollowingCount;

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
        setupUserMomentsFeed();

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
                    BasicUserInfo userInfo = gson.fromJson(response.toString(), BasicUserInfo.class);
                    Logger.t(TAG).d("userInfo: " + userInfo.toString());
                    showUserInfo(userInfo);
                }
            }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        mRequestQueue.add(request);
    }

    private void showUserInfo(BasicUserInfo userInfo) {
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(userInfo.avatarUrl, mCivUserAvatar, ImageUtils.getAvatarOptions());

        mTvUserName.setText(userInfo.userName);

        mTvFollowersCount.setText(getString(R.string.followers) + " " + userInfo.getFollowersCount());
        mTvFollowingCount.setText(getString(R.string.following) + " " + userInfo.getFollowingsCount());
    }

    private void setupUserMomentsFeed() {
        mRvUserMomentList.setLayoutManager(new LinearLayoutManager(this));
        mMomentRvAdapter = new UserProfileFeedAdapter(this);
        mRvUserMomentList.setAdapter(mMomentRvAdapter);


        String requestUrl = Constants.API_USERS + "/" + mUserID + "/moments";
        AuthorizedJsonRequest request = new AuthorizedJsonRequest(requestUrl,
            new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {
                    mMomentList = parseMomentArray(response);
                    mMomentRvAdapter.setMomentList(mMomentList);
                }
            },
            new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });

        mRequestQueue.add(request);
    }

    private List<Moment> parseMomentArray(JSONObject response) {
        List<Moment> moments = new ArrayList<>();
        try {
            JSONArray momentArray = response.getJSONArray(JsonKey.MOMENTS);
            for (int i = 0; i < momentArray.length(); i++) {
                JSONObject momentObject = momentArray.getJSONObject(i);
                Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
                Moment moment = gson.fromJson(momentObject.toString(), Moment.class);
                moments.add(moment);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return moments;
    }
}
