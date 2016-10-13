package com.waylens.hachi.rest.response;

import com.waylens.hachi.rest.bean.Comment;

import java.util.List;

/**
 * Created by Xiaofei on 2016/10/13.
 */

public class CommentListResponse {
    public List<Comment> comments;
    public boolean hasMore;
}
