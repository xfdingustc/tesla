package com.waylens.hachi.ui.entities;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

import org.json.JSONObject;

/**
 * Created by Richard on 9/6/15.
 */
public class CommentEvent extends NotificationEvent {

    public long commentID;
    public String content;
    public BasicUserInfo author;
    public BasicUserInfo replyTo;
    public long createTime;


    CommentEvent(JSONObject jsonObject) {
        super(jsonObject);
    }

    public static CommentEvent fromJson(JSONObject jsonObject) {
        CommentEvent commentEvent = new CommentEvent(jsonObject);
        JSONObject jsonComment = jsonObject.optJSONObject("comment");
        commentEvent.commentID = jsonComment.optLong("commentID");
        commentEvent.content = jsonComment.optString("content");
        commentEvent.author = BasicUserInfo.fromJson(jsonComment.optJSONObject("author"));
        commentEvent.replyTo = BasicUserInfo.fromJson(jsonComment.optJSONObject("replyTo"));
        commentEvent.createTime = jsonComment.optLong("createTime");
        return commentEvent;
    }

    public Spannable commentToSpannable() {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int start = 0;
        if (replyTo != null) {
            start = ssb.length();
            ssb.append("@").append(replyTo.userName);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new Comment.UserNameSpan(), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        ssb.append(" ").append(content);
        return ssb;
    }

}
