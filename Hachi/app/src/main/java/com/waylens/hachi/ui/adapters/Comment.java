package com.waylens.hachi.ui.adapters;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.View;

import org.json.JSONObject;

/**
 * Created by Richard on 8/26/15.
 */
public class Comment {
    public static final int UNASSIGNED_ID = -1;

    public long commentID;
    public String content;
    public long createTime;
    public BasicUserInfo author;
    public BasicUserInfo replyTo;

    public Comment() {
        commentID = UNASSIGNED_ID;
    }

    public static Comment fromJson(JSONObject jsonObject) {
        Comment comment = new Comment();
        comment.commentID = jsonObject.optLong("commentID");
        comment.content = jsonObject.optString("content");
        comment.createTime = jsonObject.optLong("createTime");
        comment.author = BasicUserInfo.fromJson(jsonObject.optJSONObject("author"));
        comment.replyTo = BasicUserInfo.fromJson(jsonObject.optJSONObject("replyTo"));
        return comment;
    }

    public SpannableStringBuilder toSpannable() {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int start = ssb.length();
        ssb.append(author.userName);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new UserNameSpan(), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (replyTo != null) {
            ssb.append(" ");
            start = ssb.length();
            ssb.append("@").append(replyTo.userName);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new UserNameSpan(), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        ssb.append(" ").append(content);
        return ssb;
    }

    static class UserNameSpan extends ClickableSpan {

        @Override
        public void onClick(View widget) {
            //
        }
    }
}
