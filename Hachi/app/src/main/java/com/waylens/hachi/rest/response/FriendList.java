package com.waylens.hachi.rest.response;

import com.waylens.hachi.ui.entities.User;

import java.util.List;

/**
 * Created by Xiaofei on 2016/9/14.
 */
public class FriendList {
    public List<User> friends;
    public int nextCursor;
}
