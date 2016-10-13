package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.ui.entities.UserDeprecated;

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
    private List<UserDeprecated> mUserList;

    public UserListRvAdapter(Context context) {
        this.mContext = context;
    }

    public void setUserList(List<UserDeprecated> userList) {
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

        UserDeprecated userInfo = mUserList.get(position);

        viewHolder.mTvUserName.setText(userInfo.userName);

        Glide.with(mContext)
            .load(userInfo.avatarUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .placeholder(R.drawable.menu_profile_photo_default)
            .into(viewHolder.mUserAvater);

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
        return mUserList != null ? mUserList.size() : 0;
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
        BgJobHelper.followUser(userID, true);
        setFollowButton(viewHolder, true);
        setUserIsFollowing(viewHolder, true);
    }

    public void unfollowUser(final String userID, final UserListViewHolder viewHolder) {
        BgJobHelper.followUser(userID, false);
        setFollowButton(viewHolder, false);
        setUserIsFollowing(viewHolder, false);
    }

    private void setFollowButton(UserListViewHolder viewHolder, boolean isFollowing) {
        if (isFollowing) {
            viewHolder.mTvFollow.setText(R.string.following);
            viewHolder.mTvFollow.setTextColor(mContext.getResources().getColor(android.R.color.white));
            viewHolder.mTvFollow.setBackgroundResource(R.drawable.round_rectangle_button);
        } else {
            viewHolder.mTvFollow.setText(R.string.add_follow);
            viewHolder.mTvFollow.setTextColor(mContext.getResources().getColor(R.color.style_color_accent));
            viewHolder.mTvFollow.setBackgroundResource(R.drawable.button_with_stroke);
        }
    }

    private void setUserIsFollowing(UserListViewHolder viewHolder, boolean isFollowing) {
        int position = viewHolder.getPosition();
        UserDeprecated user = mUserList.get(position);
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
