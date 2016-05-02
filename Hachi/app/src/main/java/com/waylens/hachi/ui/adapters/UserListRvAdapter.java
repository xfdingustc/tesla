package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.ui.entities.User;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2015/9/23.
 */
public class UserListRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    private static final String TAG = UserListRvAdapter.class.getSimpleName();

    private final Context mContext;
    private List<User> mUserList;
    private RequestQueue mRequestQueue;

    public UserListRvAdapter(Context context) {
        this.mContext = context;
        this.mRequestQueue = VolleyUtil.newVolleyRequestQueue(context);
        mRequestQueue.start();
    }

    public void setUserList(List<User> userList) {
        this.mUserList = userList;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_user_list, parent, false);
        return new UserListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        UserListViewHolder viewHolder = (UserListViewHolder) holder;

        User userInfo = mUserList.get(position);

        viewHolder.mTvUserName.setText(userInfo.userName);

        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(userInfo.avatarUrl, viewHolder.mUserAvater, ImageUtils.getAvatarOptions());

        if (userInfo.getIsFollowing()) {
            setFollowButton(viewHolder, true);
        } else {
            setFollowButton(viewHolder, false);

        }
        viewHolder.mTvFollow.setTag(viewHolder);
        viewHolder.mTvFollow.setOnClickListener(this);
    }

    @Override
    public int getItemCount() {
        if (mUserList != null) {
            return mUserList.size();
        }

        return 0;
    }

    @Override
    public void onClick(View v) {
        UserListViewHolder viewHolder = (UserListViewHolder) v.getTag();
        switch (v.getId()) {
            case R.id.btnFollow:
                int position = viewHolder.getPosition();
                String userID = mUserList.get(position).userID;
                if (mUserList.get(position).getIsFollowing()) {
                    unfollowUser(userID, viewHolder);
                } else {
                    followUser(userID, viewHolder);
                }
                break;
        }
    }

    public void followUser(final String userID, final UserListViewHolder viewHolder) {
        String requestUrl = Constants.API_FRIENDS_FOLLOW;
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put(JsonKey.USER_ID, userID);
            AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST,
                requestUrl, requestBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).d("Follow user: " + userID);
                    setFollowButton(viewHolder, true);
                    setUserIsFollowing(viewHolder, true);
                    viewHolder.mTvFollow.setEnabled(true);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Logger.t(TAG).d(error.toString());
                }
            });
            mRequestQueue.add(request);
            viewHolder.mTvFollow.setEnabled(false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void unfollowUser(final String userID, final UserListViewHolder viewHolder) {
        String requestUrl = Constants.API_FRIENDS_UNFOLLOW;
        JSONObject requestBody = new JSONObject();

        try {
            requestBody.put(JsonKey.USER_ID, userID);
            AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST,
                requestUrl, requestBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    setFollowButton(viewHolder, false);
                    setUserIsFollowing(viewHolder, false);
                    Logger.t(TAG).d("Unfollow user: " + userID);
                    viewHolder.mTvFollow.setEnabled(true);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Logger.t(TAG).d(error.toString());
                }
            });
            mRequestQueue.add(request);
            viewHolder.mTvFollow.setEnabled(false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void setFollowButton(UserListViewHolder viewHolder, boolean isFollowing) {
        if (isFollowing) {
            viewHolder.mTvFollow.setText(R.string.following);
            viewHolder.mTvFollow.setTextColor(mContext.getResources().getColor(R.color.windowBackground));
            viewHolder.mTvFollow.setBackgroundResource(R.color.app_text_color_primary);
        } else {
            viewHolder.mTvFollow.setText(R.string.add_follow);
            viewHolder.mTvFollow.setTextColor(mContext.getResources().getColor(R.color.app_text_color_primary));
            viewHolder.mTvFollow.setBackgroundResource(R.drawable.button_with_stroke);
        }
    }

    private void setUserIsFollowing(UserListViewHolder viewHolder, boolean isFollowing) {
        int position = viewHolder.getPosition();
        User user = mUserList.get(position);
        user.setIsFollowing(isFollowing);
    }

    public class UserListViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.userAvatar)
        CircleImageView mUserAvater;

        @BindView(R.id.tvUserName)
        TextView mTvUserName;

        @BindView(R.id.btnFollow)
        TextView mTvFollow;

        public UserListViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
