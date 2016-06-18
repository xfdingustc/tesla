package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.entities.LikeEvent;
import com.waylens.hachi.utils.ImageUtils;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;


import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Richard on 9/6/15.
 */

public class NotificationLikesAdapter extends RecyclerView.Adapter<NotificationLikesAdapter.NotificationLikeVH> {

    private ArrayList<LikeEvent> mLikeEvents;

    PrettyTime mPrettyTime;

    Resources mResources;

    public NotificationLikesAdapter(ArrayList<LikeEvent> likeEvents, Resources resources) {
        mLikeEvents = likeEvents;
        mPrettyTime = new PrettyTime();
        mResources = resources;
    }

    public void addNotifications(ArrayList<LikeEvent> likeEvents, boolean isRefresh) {
        if (isRefresh) {
            mLikeEvents = likeEvents;
            notifyDataSetChanged();
        } else {
            if (mLikeEvents == null) {
                mLikeEvents = new ArrayList<>();
            }
            int start = mLikeEvents.size();
            mLikeEvents.addAll(likeEvents);
            notifyItemRangeInserted(start, likeEvents.size());
        }
    }

    @Override
    public NotificationLikeVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_like, parent, false);
        return new NotificationLikeVH(itemView);
    }

    @Override
    public int getItemCount() {
        if (mLikeEvents != null) {
            return mLikeEvents.size();
        } else {
            return 0;
        }
    }

    @Override
    public void onBindViewHolder(NotificationLikeVH holder, int position) {
        LikeEvent likeEvent = mLikeEvents.get(position);
        Context context = holder.likeUserAvatar.getContext();
        Glide.with(context)
            .load(likeEvent.liker.avatarUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(holder.likeUserAvatar);


        holder.likeUserName.setText(mResources.getString(R.string.like_your_post, likeEvent.liker.userName));
        holder.likeTime.setText(mPrettyTime.formatUnrounded(new Date(likeEvent.createTime)));
        if (!TextUtils.isEmpty(likeEvent.title)) {
            holder.momentTitle.setText(likeEvent.title);
        }

        Glide.with(context)
            .load(likeEvent.thumbnail)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(holder.momentThumbnail);
    }

    public static class NotificationLikeVH extends RecyclerView.ViewHolder {

        @BindView(R.id.like_user_avatar)
        CircleImageView likeUserAvatar;

        @BindView(R.id.like_user_name)
        TextView likeUserName;

        @BindView(R.id.like_time)
        TextView likeTime;

        @BindView(R.id.moment_title)
        TextView momentTitle;

        @BindView(R.id.moment_thumbnail)
        ImageView momentThumbnail;

        public NotificationLikeVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}