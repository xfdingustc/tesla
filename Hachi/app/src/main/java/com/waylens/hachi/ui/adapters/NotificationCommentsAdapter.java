package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.utils.ImageUtils;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Richard on 9/6/15.
 */
public class NotificationCommentsAdapter extends RecyclerView.Adapter<NotificationCommentsAdapter.NotificationCommentVH> {

    private ArrayList<Notification> mNotifications;

    public NotificationCommentsAdapter(ArrayList<Notification> notifications) {
        mNotifications = notifications;
    }

    public void setNotifications(ArrayList<Notification> notifications) {
        mNotifications = notifications;
        notifyDataSetChanged();
    }

    @Override
    public NotificationCommentVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_comment, parent, false);
        return new NotificationCommentVH(itemView);
    }

    @Override
    public int getItemCount() {
        if (mNotifications != null) {
            return mNotifications.size();
        } else {
            return 0;
        }
    }

    @Override
    public void onBindViewHolder(NotificationCommentVH holder, int position) {
        Notification notification = mNotifications.get(position);
        ImageLoader.getInstance().displayImage(notification.thumbnail, holder.momentThumbnail, ImageUtils.getVideoOptions());
    }

    public static class NotificationCommentVH extends RecyclerView.ViewHolder {

        @Bind(R.id.moment_thumbnail)
        ImageView momentThumbnail;

        public NotificationCommentVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}