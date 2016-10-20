package com.waylens.hachi.rest.bean;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.View;


import com.waylens.hachi.snipe.utils.ToStringUtils;

import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Richard on 8/26/15.
 */
public class Comment implements Serializable{
    public static final int TYPE_NORMAL = 0;

    public static final int TYPE_LOAD_MORE_INDICATOR = 1;
    public static final int TYPE_LOAD_NO_MORE_COMMENT = 2;

    public static final int UNASSIGNED_ID = -1;

    public long commentID;
    public String content;
    public long createTime;
    public User author;
    public User replyTo;

    public int type;

    public Comment() {
        commentID = UNASSIGNED_ID;
    }

    public SpannableStringBuilder toSpannable() {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int start = ssb.length();
//        ssb.append(author.userName);
//        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        ssb.setSpan(new UserNameSpan(), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (replyTo != null) {
//            ssb.append(" ");
            start = ssb.length();
            ssb.append("@").append(replyTo.userName);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new UserNameSpan(), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append(" ");
        }
        ssb.append(content);
        return ssb;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }

    public static Comment createLoadMoreIndicator() {
        Comment comment = new Comment();
        comment.type = TYPE_LOAD_MORE_INDICATOR;
        return comment;
    }

    public static class UserNameSpan extends ClickableSpan {

        @Override
        public void onClick(View widget) {
            //
        }
    }
}
