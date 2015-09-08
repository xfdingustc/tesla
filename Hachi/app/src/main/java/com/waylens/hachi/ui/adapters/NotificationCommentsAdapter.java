package com.waylens.hachi.ui.adapters;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.entities.CommentEvent;
import com.waylens.hachi.utils.ImageUtils;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Richard on 9/6/15.
 */

public class NotificationCommentsAdapter extends RecyclerView.Adapter<NotificationCommentsAdapter.NotificationCommentVH> {

    private ArrayList<CommentEvent> mCommentEvents;

    PrettyTime mPrettyTime;

    public NotificationCommentsAdapter(ArrayList<CommentEvent> commentEvents) {
        mCommentEvents = commentEvents;
        mPrettyTime = new PrettyTime();
    }

    public void addNotifications(ArrayList<CommentEvent> commentEvents, boolean isRefresh) {
        if (isRefresh) {
            mCommentEvents = commentEvents;
            notifyDataSetChanged();
        } else {
            if (mCommentEvents == null) {
                mCommentEvents = new ArrayList<>();
            }
            int start = mCommentEvents.size();
            mCommentEvents.addAll(commentEvents);
            notifyItemRangeInserted(start, commentEvents.size());
        }
    }

    @Override
    public NotificationCommentVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_comment, parent, false);
        return new NotificationCommentVH(itemView);
    }

    @Override
    public int getItemCount() {
        if (mCommentEvents != null) {
            return mCommentEvents.size();
        } else {
            return 0;
        }
    }

    @Override
    public void onBindViewHolder(NotificationCommentVH holder, int position) {
        CommentEvent commentEvent = mCommentEvents.get(position);
        ImageLoader.getInstance().displayImage(commentEvent.author.avatarUrl, holder.commentUserAvatar, ImageUtils.getAvatarOptions());
        holder.commentUserName.setText(commentEvent.author.userName);
        holder.commentTime.setText(mPrettyTime.formatUnrounded(new Date(commentEvent.createTime)));
        holder.commentContent.setText(commentEvent.commentToSpannable());
        if (!TextUtils.isEmpty(commentEvent.title)) {
            holder.momentTitle.setText(commentEvent.title);
        }
        ImageLoader.getInstance().displayImage(commentEvent.thumbnail, holder.momentThumbnail, ImageUtils.getVideoOptions());
    }

    public static class NotificationCommentVH extends RecyclerView.ViewHolder {

        @Bind(R.id.comment_user_avatar)
        CircleImageView commentUserAvatar;

        @Bind(R.id.comment_user_name)
        TextView commentUserName;

        @Bind(R.id.comment_time)
        TextView commentTime;

        @Bind(R.id.comment_content)
        TextView commentContent;

        @Bind(R.id.moment_title)
        TextView momentTitle;

        @Bind(R.id.moment_thumbnail)
        ImageView momentThumbnail;

        public NotificationCommentVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}