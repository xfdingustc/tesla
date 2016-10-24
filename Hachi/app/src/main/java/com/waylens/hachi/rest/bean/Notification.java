package com.waylens.hachi.rest.bean;

import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.session.SessionManager;

/**
 * Created by Xiaofei on 2016/10/14.
 */

public class Notification {
    public static final int NOTIFICATION_TYPE_COMMENT = 0;
    public static final int NOTIFICATION_TYPE_FOLLOW = 1;
    public static final int NOTIFICATION_TYPE_LIKE = 2;
    public static final int NOTIFICATION_TYPE_SHARE = 3;
    public MomentSimple moment;
    public Comment comment;
    public Like like;
    public Share share;
    public Follow follow;
    public long eventID;
    public boolean isRead;
    public int notificationType;

    public long getCreateTime() {
        switch (notificationType) {
            case NOTIFICATION_TYPE_COMMENT:
                return comment.createTime;
            case NOTIFICATION_TYPE_FOLLOW:
                return follow.createTime;
            case NOTIFICATION_TYPE_LIKE:
                return like.createTime;
            case NOTIFICATION_TYPE_SHARE:
                return share.createTime;
        }
        return 0;
    }

    public String getUserAvatarUrl() {
        switch (notificationType) {
            case NOTIFICATION_TYPE_COMMENT:
                return comment.author.avatarUrl;
            case NOTIFICATION_TYPE_FOLLOW:
                return follow.user.avatarUrl;
            case NOTIFICATION_TYPE_LIKE:
                return like.user.avatarUrl;
            case NOTIFICATION_TYPE_SHARE:
                return SessionManager.getInstance().getAvatarUrl();
        }
        return null;
    }

    public String getDescription() {
        switch (notificationType) {
            case NOTIFICATION_TYPE_COMMENT:
                return comment.author.userName + " " + Hachi.getContext().getResources().getString(R.string.made_comment) + " ";
            case NOTIFICATION_TYPE_LIKE:
                return like.user.userName + " " + Hachi.getContext().getResources().getString(R.string.like_your_post);
            case NOTIFICATION_TYPE_FOLLOW:
                return follow.user.userName + " " + Hachi.getContext().getResources().getString(R.string.start_follow);
            case NOTIFICATION_TYPE_SHARE:
                if (share.status.equals("POST_COMPLETED")) {
                    return String.format(Hachi.getContext().getResources().getString(R.string.share_social_media_success), "", share.provider);
                } else {
                    return String.format(Hachi.getContext().getResources().getString(R.string.share_social_media_failed), "", share.provider);
                }

            default:
                break;
        }
        return null;
    }

    public String getUserName() {
        switch (notificationType) {
            case NOTIFICATION_TYPE_COMMENT:
                return comment.author.userName;
            case NOTIFICATION_TYPE_LIKE:
                return like.user.userName;
            case NOTIFICATION_TYPE_FOLLOW:
                return follow.user.userName;
            case NOTIFICATION_TYPE_SHARE:
                return SessionManager.getInstance().getUserName();
        }
        return null;
    }
}
