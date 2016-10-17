package com.waylens.hachi.ui.user;

import android.app.Activity;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.social.FollowJob;
import com.waylens.hachi.rest.response.FollowInfo;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.FollowListActivity;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.community.feed.IMomentListAdapterHeaderView;
import com.waylens.hachi.ui.settings.AccountActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/7/21.
 */
public class UserProfileHeaderView implements IMomentListAdapterHeaderView {
    private static final String TAG = UserProfileHeaderView.class.getSimpleName();
    private final String mUserId;
    private final UserInfo mUserInfo;
    private final FollowInfo mFollowInfo;
    private final Activity mActivity;

    public UserProfileHeaderView(Activity activity, String userId, UserInfo userInfo, FollowInfo followInfo) {
        this.mActivity = activity;
        this.mUserId = userId;
        this.mUserInfo = userInfo;
        this.mFollowInfo = followInfo;
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.headerview_user_profile, parent, false);
        return new UserProfileHeaderViewHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
        final UserProfileHeaderViewHolder holder = (UserProfileHeaderViewHolder) viewHolder;

        Glide.with(mActivity)
            .load(mUserInfo.avatarUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.menu_profile_photo_default)
            .dontAnimate()
            .into(holder.civUserAvatar);


        holder.mUserName.setText(mUserInfo.displayName);

        if (SessionManager.getInstance().isCurrentUser(mUserInfo.userName)) {
            holder.mBtnAccountSetting.setVisibility(View.VISIBLE);
            holder.mBtnFollow.setVisibility(View.GONE);
        } else {
            holder.mBtnFollow.setVisibility(View.VISIBLE);

            holder.mBtnAccountSetting.setVisibility(View.GONE);
        }

        updateFollowInfo(holder);
    }


    private void updateFollowInfo(UserProfileHeaderViewHolder viewHolder) {
        Resources resources = Hachi.getContext().getResources();
        viewHolder.mTvFollowersCount.setText(Integer.toString(mFollowInfo.followers));
        viewHolder.followingCount.setText(Integer.toString(mFollowInfo.followings));
        if (mFollowInfo.isMyFollowing) {
            viewHolder.mBtnFollow.setText(R.string.followed);
            viewHolder.mBtnFollow.setTextColor(resources.getColor(R.color.app_text_color_disabled));
        } else {
            viewHolder.mBtnFollow.setText(R.string.follow);
            viewHolder.mBtnFollow.setTextColor(resources.getColor(R.color.style_color_accent));
        }

    }


    public class UserProfileHeaderViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.userAvatar)
        ImageView civUserAvatar;

        @BindView(R.id.btnFollowersCount)
        TextView mTvFollowersCount;

        @BindView(R.id.following_count)
        TextView followingCount;


        @BindView(R.id.btnFollow)
        TextView mBtnFollow;

        @BindView(R.id.user_name)
        TextView mUserName;

        @BindView(R.id.btn_account_setting)
        ImageButton mBtnAccountSetting;


        @OnClick(R.id.btnFollowersCount)
        public void onBtnFollowerCountClicked() {
            FollowListActivity.launch(mActivity, mUserId, true);
        }

        @OnClick(R.id.btn_account_setting)
        public void onBtnAccountSettingClicked() {
            AccountActivity.launch(mActivity);
        }

        @OnClick(R.id.btnFollow)
        public void onBtnFollowClicked() {
            if (!SessionManager.getInstance().isLoggedIn()) {
                AuthorizeActivity.launch(mActivity);
                return;
            }
            if (!SessionManager.checkUserVerified(mActivity)) {
                return;
            }
            JobManager jobManager = BgJobManager.getManager();
            FollowJob job = new FollowJob(mUserId, !mFollowInfo.isMyFollowing);
            jobManager.addJobInBackground(job);
            mFollowInfo.isMyFollowing = !mFollowInfo.isMyFollowing;
            if (!mFollowInfo.isMyFollowing) {
                mFollowInfo.followers--;
            } else {
                mFollowInfo.followers++;
            }
            updateFollowInfo(this);
        }


        public UserProfileHeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }


    }
}
