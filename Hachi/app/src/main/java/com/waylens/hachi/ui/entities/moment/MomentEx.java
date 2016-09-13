package com.waylens.hachi.ui.entities.moment;

import com.waylens.hachi.ui.entities.Comment;
import com.waylens.hachi.ui.entities.User;

import java.util.List;

/**
 * Created by Xiaofei on 2016/9/13.
 */
public class MomentEx {
    public MomentAbstract moment;

    public User owner;

    public User lastLike;

    public List<Comment> lastComments;
}
