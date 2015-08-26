package com.waylens.hachi.ui.adapters;

import org.json.JSONObject;

/**
 * Created by Richard on 8/26/15.
 */
public class Comment {
    public long commentID;
    public String content;
    public long createTime;
    public BasicUserInfo author;
    public BasicUserInfo replyTo;

    public static Comment fromJson(JSONObject jsonObject) {
        Comment comment = new Comment();
        comment.commentID = jsonObject.optLong("commentID");
        comment.content = jsonObject.optString("content");
        comment.createTime = jsonObject.optLong("createTime");
        comment.author = BasicUserInfo.fromJson(jsonObject.optJSONObject("author"));
        comment.replyTo = BasicUserInfo.fromJson(jsonObject.optJSONObject("replyTo"));
        return comment;
    }
}
