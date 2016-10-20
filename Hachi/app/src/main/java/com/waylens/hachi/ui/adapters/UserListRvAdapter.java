package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.BgJobHelper;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.entities.UserDeprecated;
import com.waylens.hachi.ui.views.AvatarView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/9/23.
 */
public class UserListRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
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
        final UserListViewHolder viewHolder = (UserListViewHolder) holder;

        UserDeprecated userInfo = mUserList.get(position);

        viewHolder.mTvUserName.setText(userInfo.userName);

        viewHolder.mUserAvater.loadAvatar(userInfo.avatarUrl, userInfo.userName);

        if (SessionManager.getInstance().isCurrentUserId(userInfo.userID)) {
            viewHolder.mTvFollow.setVisibility(View.GONE);
        } else {
            viewHolder.mTvFollow.setVisibility(View.VISIBLE);
            if (userInfo.getIsFollowing()) {
                setFollowButton(viewHolder, true);
            } else {
                setFollowButton(viewHolder, false);
            }
        }
        viewHolder.mTvFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = viewHolder.getAdapterPosition();
                String userID = mUserList.get(position).userID;
                if (mUserList.get(position).getIsFollowing()) {
                    unfollowUser(userID, viewHolder);
                } else {
                    followUser(userID, viewHolder);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUserList != null ? mUserList.size() : 0;
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
        Logger.t(TAG).d("set follow button: " + isFollowing);
        if (isFollowing) {
//            viewHolder.mTvFollow.setText(R.string.following);
//            viewHolder.mTvFollow.setTextColor(mContext.getResources().getColor(android.R.color.white));
            viewHolder.mTvFollow.setBackgroundResource(R.drawable.round_rectangle_button);
            viewHolder.mTvFollow.setActivated(false);
        } else {
//            viewHolder.mTvFollow.setText(R.string.add_follow);
//            viewHolder.mTvFollow.setTextColor(mContext.getResources().getColor(R.color.style_color_accent));
            viewHolder.mTvFollow.setBackgroundResource(R.drawable.button_with_stroke);
            viewHolder.mTvFollow.setActivated(true);
        }
    }

    private void setUserIsFollowing(UserListViewHolder viewHolder, boolean isFollowing) {
        int position = viewHolder.getPosition();
        UserDeprecated user = mUserList.get(position);
        user.setIsFollowing(isFollowing);
    }

    public class UserListViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.userAvatar)
        AvatarView mUserAvater;

        @BindView(R.id.tvUserName)
        TextView mTvUserName;

        @BindView(R.id.follow)
        ImageButton mTvFollow;

        public UserListViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
