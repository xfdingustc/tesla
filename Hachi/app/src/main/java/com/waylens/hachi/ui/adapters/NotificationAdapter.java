package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.bean.Notification;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.community.MomentActivity;
import com.waylens.hachi.ui.views.AvatarView;
import com.waylens.hachi.utils.PrettyTimeUtils;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/10/14.
 */

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static String TAG = NotificationAdapter.class.getSimpleName();

    private List<Notification> mNotificationEvents;

    private final OnListItemClickListener mOnListItemClickListener;

    Context mContext;

    private boolean mHasMore = true;


    public NotificationAdapter(List<Notification> notificationEvents, Context context, OnListItemClickListener listener) {
        mNotificationEvents = notificationEvents;
        mContext = context;
        mOnListItemClickListener = listener;
    }

    public void addNotifications(List<Notification> notificationEvents, boolean isRefresh) {
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
            case Notification.NOTIFICATION_TYPE_COMMENT:
            case Notification.NOTIFICATION_TYPE_LIKE:
            case Notification.NOTIFICATION_TYPE_FOLLOW:
                itemView = LayoutInflater.from(mContext).inflate(R.layout.item_notification, parent, false);
                return new NotificationViewHolder(itemView);
            default:
                break;
        }
        itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
        return new LoadingViewHolder(itemView);
    }

    @Override
    public int getItemCount() {
        if (mNotificationEvents != null) {
            return mNotificationEvents.size() + 1;
        } else {
            return 1;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < mNotificationEvents.size()) {
            //Logger.t(TAG).d("NotificationType" + mNotificationEvents.get(position).mNotificationType);
            return mNotificationEvents.get(position).notificationType;
        } else {
            return -1;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (position < mNotificationEvents.size()) {
            final Notification notificationEvent = mNotificationEvents.get(position);
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
            switch (notificationEvent.notificationType) {
                case Notification.NOTIFICATION_TYPE_COMMENT:
                case Notification.NOTIFICATION_TYPE_LIKE:
                case Notification.NOTIFICATION_TYPE_FOLLOW:
                    onBindCommentViewHolder((NotificationViewHolder) holder, position);
                    break;
                default:
                    break;
            }
            return;
        }
        onBindLoadingViewHolder((LoadingViewHolder) holder, position);
    }

    public void setHasMore(boolean hasMore) {
        mHasMore = hasMore;
        notifyItemChanged(mNotificationEvents.size());
    }

    private void onBindCommentViewHolder(final NotificationViewHolder holder, int position) {
        final Notification notification = mNotificationEvents.get(position);
//        final Comment commentEvent = notification.comment;
/*        if (commentEvent.isRead) {
            holder.commentRootLayout.setAlpha((float) 0.5);
        }*/

        holder.avatarView.loadAvatar(notification.getUserAvatarUrl(), notification.getUser().userName);

        holder.commentUserName.setText(notification.getDescription());
        holder.commentTime.setText(PrettyTimeUtils.getTimeAgo(notification.getCreateTime()));


        if (notification.moment != null) {
            Logger.t(TAG).d("moment: " + notification.moment.toString());
            holder.thumbnailContainer.setVisibility(View.VISIBLE);
            Glide.with(mContext)
                .load(notification.moment.videoThumbnail)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .into(holder.momentThumbnail);
        } else {
            holder.thumbnailContainer.setVisibility(View.GONE);
        }
        holder.commentRootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MomentActivity.launch((BaseActivity) mContext, notification.moment.momentID, notification.moment.videoThumbnail, holder.momentThumbnail);
            }
        });
    }


    private void onBindLoadingViewHolder(LoadingViewHolder holder, int position) {
        if (mHasMore) {
            holder.viewAnimator.setDisplayedChild(0);
        } else {
            holder.viewAnimator.setDisplayedChild(1);
        }
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.avatar_view)
        AvatarView avatarView;

        @BindView(R.id.comment_root_layout)
        LinearLayout commentRootLayout;


        @BindView(R.id.comment_user_name)
        TextView commentUserName;

        @BindView(R.id.comment_time)
        TextView commentTime;

        @BindView(R.id.thumbnail_container)
        View thumbnailContainer;


        @Nullable
        @BindView(R.id.moment_thumbnail)
        ImageView momentThumbnail;

        public NotificationViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }


    public interface OnListItemClickListener {
        void onItemClicked(long eventID);
    }

    public static class LoadingViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.view_animator)
        ViewAnimator viewAnimator;

        @BindView(R.id.tv_no_more)
        TextView textView;

        public LoadingViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            textView.setText(R.string.no_more_notifications);
        }
    }
}