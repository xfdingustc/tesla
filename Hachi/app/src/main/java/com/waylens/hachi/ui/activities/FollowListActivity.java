package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.adapters.UserListRvAdapter;
import com.waylens.hachi.ui.entities.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2015/9/23.
 */
public class FollowListActivity extends BaseActivity {
    private static final String TAG = FollowListActivity.class.getSimpleName();

    private static final String USER_ID = "user_id";
    private static final String IS_FOLLOWERS = "is_followers";

    private String mUserId;
    private boolean mIsFollowers;

    private List<User> mUserList;

    private UserListRvAdapter mUserListAdatper;


    @Bind(R.id.rvFollowList)
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
    protected void setupToolbar() {
        super.setupToolbar();
        if (mToolbar != null) {
            String title = mIsFollowers ? getString(R.string.followers) : getString(R.string
                .following);
            mToolbar.setTitle(title);
        }
    }

    private void setupFollowList() {
        mRvFollowList.setLayoutManager(new LinearLayoutManager(this));
        mUserListAdatper = new UserListRvAdapter(this);
        mRvFollowList.setAdapter(mUserListAdatper);

        String requestUrl;
        if (mIsFollowers) {
            requestUrl = Constants.API_FRIENDS + "followers";
        } else {
            requestUrl = Constants.API_FRIENDS + "followings";
        }
        requestUrl += "?u=" + mUserId;
        AuthorizedJsonRequest request = new AuthorizedJsonRequest(requestUrl, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Logger.t(TAG).json(response.toString());
                mUserList = parseUserList(response);
                refreshUserList();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        mRequestQueue.add(request);
    }

    private void refreshUserList() {
        mUserListAdatper.setUserList(mUserList);
    }

    private List<User> parseUserList(JSONObject response) {
        try {
            JSONArray userArray = response.getJSONArray("friends");
            List<User> userList;

            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

            Type type = new TypeToken<ArrayList<User>>() {}.getType();

            userList = gson.fromJson(userArray.toString(), type);


            return userList;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
