package com.waylens.hachi.ui.adapters;

import android.app.Notification;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.entities.CommentEvent;
import com.waylens.hachi.ui.entities.FollowEvent;
import com.waylens.hachi.ui.entities.LikeEvent;
import com.waylens.hachi.ui.entities.NotificationEvent;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by lshw on 16/8/3.
 */
public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static String TAG = NotificationAdapter.class.getSimpleName();

    private ArrayList<NotificationEvent> mNotificationEvents;

    private final OnListItemClickListener mOnListItemClickListener;

    PrettyTime mPrettyTime;

    Context mContext;


    public NotificationAdapter(ArrayList<NotificationEvent> notificationEvents, Context context, OnListItemClickListener listener) {
        mNotificationEvents = notificationEvents;
        mPrettyTime = new PrettyTime();
        mContext = context;
        mOnListItemClickListener = listener;
    }

    public void addNotifications(ArrayList<NotificationEvent> notificationEvents, boolean isRefresh) {
        if (isRefresh) {
            mNotificationEvents = notificationEvents;
            //Logger.t(TAG).d("mNotification List size = " + mNotificationEvents.size());
            notifyDataSetChanged();
        } else {
            if (mNotificationEvents == null) {
                mNotificationEvents = new ArrayList<>();
            }
            int start = mNotificationEvents.size();
            int length = notificationEvents.size();
            mNotificationEvents.addAll(notificationEvents);
            Logger.t(TAG).d("mNotification List size = " + mNotificationEvents.size());
            notifyItemRangeInserted(start, length);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = null;
        //Logger.t(TAG).d("view T");
        switch (viewType) {
            case NotificationEvent.NOTIFICATION_TYPE_COMMENT:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_comment, parent, false);
                return new NotificationCommentVH(itemView);
            case NotificationEvent.NOTIFICATION_TYPE_LIKE:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_like, parent, false);
                return new NotificationLikeVH(itemView);
            case NotificationEvent.NOTIFICATION_TYPE_FOLLOW:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_follow, parent, false);
                return new NotificationFollowVH(itemView);
            default:
                break;
        }
        return null;
    }

    @Override
    public int getItemCount() {
        if (mNotificationEvents != null) {
            return mNotificationEvents.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < mNotificationEvents.size()) {
            //Logger.t(TAG).d("NotificationType" + mNotificationEvents.get(position).mNotificationType);
            return mNotificationEvents.get(position).mNotificationType;
        } else {
            return -1;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final NotificationEvent notificationEvent = mNotificationEvents.get(position);
        final long eventID = notificationEvent.eventID;
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!notificationEvent.isRead) {
                    notificationEvent.isRead = true;
                    notifyItemChanged(position);
                }
                if (mOnListItemClickListener != null) {
                    mOnListItemClickListener.onItemClicked(eventID);
                }
            }
        });
        switch (notificationEvent.mNotificationType) {
            case NotificationEvent.NOTIFICATION_TYPE_COMMENT:
                onBindCommentViewHolder((NotificationCommentVH)holder, position);
                break;
            case NotificationEvent.NOTIFICATION_TYPE_LIKE:
                onBindLikeViewHolder((NotificationLikeVH)holder, position);
                break;
            case NotificationEvent.NOTIFICATION_TYPE_FOLLOW:
                onBindFollowViewHolder((NotificationFollowVH)holder, position);
                break;
            default:
                break;
        }
    }

    private void onBindCommentViewHolder(final NotificationCommentVH holder, int position) {
        final CommentEvent commentEvent = (CommentEvent) mNotificationEvents.get(position);
/*        if (commentEvent.isRead) {
            holder.commentRootLayout.setAlpha((float) 0.5);
        }*/
        Context context = holder.commentUserAvatar.getContext();
        Glide.with(context)
                .load(commentEvent.author.avatarUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .into(holder.commentUserAvatar);
        holder.commentUserName.setText(commentEvent.author.userName + " " + mContext.getResources().getString(R.string.made_comment) + " ");
        holder.commentTime.setText(mPrettyTime.formatUnrounded(new Date(commentEvent.createTime)));
        if (!TextUtils.isEmpty(commentEvent.title)) {
            holder.commentUserName.append(": " + commentEvent.title + ".");
        }

        Logger.t(TAG).d(commentEvent.thumbnail);

        Glide.with(mContext)
                .load(commentEvent.thumbnail)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .into(holder.momentThumbnail);
        holder.commentRootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MomentActivity.launch((BaseActivity) mContext, commentEvent.momentID, commentEvent.thumbnail, holder.momentThumbnail);
            }
        });
    }

    private void onBindLikeViewHolder(final NotificationLikeVH holder, int position) {
        final LikeEvent likeEvent = (LikeEvent) mNotificationEvents.get(position);
/*        if (likeEvent.isRead) {
            holder.likeRootLayout.setAlpha((float) 0.5);
        }*/
        Context context = holder.likeUserAvatar.getContext();
        Glide.with(context)
                .load(likeEvent.liker.avatarUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .into(holder.likeUserAvatar);

        holder.likeUserName.setText(likeEvent.liker.userName + " " + mContext.getResources().getString(R.string.like_your_post));
        holder.likeTime.setText(mPrettyTime.formatUnrounded(new Date(likeEvent.createTime)));
        if (!TextUtils.isEmpty(likeEvent.title)) {
            holder.likeUserName.append(": " + likeEvent.title + ".");
        }

        Logger.t(TAG).d(likeEvent.thumbnail);
        Glide.with(mContext)
                .load(likeEvent.thumbnail)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .into(holder.momentThumbnail);

        holder.likeRootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MomentActivity.launch((BaseActivity) mContext, likeEvent.momentID, likeEvent.thumbnail, holder.momentThumbnail);
            }
        });
    }

    private void onBindFollowViewHolder(NotificationFollowVH holder, int position) {
        final FollowEvent followEvent = (FollowEvent) mNotificationEvents.get(position);
/*        if (followEvent.isRead) {
            holder.followRootLayout.setAlpha((float) 0.5);
        }*/
        Context context = holder.followUserAvatar.getContext();
        Glide.with(context)
                .load(followEvent.follower.avatarUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .into(holder.followUserAvatar);

        holder.followUserName.setText(followEvent.follower.userName + " " + mContext.getResources().getString(R.string.start_follow));
        holder.followTime.setText(mPrettyTime.formatUnrounded(new Date(followEvent.createTime)));

        holder.followRootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserProfileActivity.launch((BaseActivity) mContext, followEvent.follower.userID);
            }
        });
    }

    public static class NotificationCommentVH extends RecyclerView.ViewHolder {

        @BindView(R.id.comment_root_layout)
        LinearLayout commentRootLayout;

        @BindView(R.id.comment_user_avatar)
        CircleImageView commentUserAvatar;

        @BindView(R.id.comment_user_name)
        TextView commentUserName;

        @BindView(R.id.comment_time)
        TextView commentTime;



        @BindView(R.id.moment_thumbnail)
        ImageView momentThumbnail;

        public NotificationCommentVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public static class NotificationLikeVH extends RecyclerView.ViewHolder {

        @BindView(R.id.like_root_layout)
        LinearLayout likeRootLayout;

        @BindView(R.id.like_user_avatar)
        CircleImageView likeUserAvatar;

        @BindView(R.id.like_user_name)
        TextView likeUserName;

        @BindView(R.id.like_time)
        TextView likeTime;

        @BindView(R.id.moment_thumbnail)
        ImageView momentThumbnail;

        public NotificationLikeVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public static class NotificationFollowVH extends RecyclerView.ViewHolder {

        @BindView(R.id.follow_root_layout)
        LinearLayout followRootLayout;

        @BindView(R.id.follow_user_avatar)
        CircleImageView followUserAvatar;

        @BindView(R.id.follow_user_name)
        TextView followUserName;

        @BindView(R.id.follow_time)
        TextView followTime;

        public NotificationFollowVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface OnListItemClickListener {
        void onItemClicked(long eventID);
    }
}