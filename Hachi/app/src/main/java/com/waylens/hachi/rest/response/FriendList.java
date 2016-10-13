package com.waylens.hachi.rest.response;

import com.waylens.hachi.ui.entities.UserDeprecated;

import java.util.List;

/**
 * Created by Xiaofei on 2016/9/14.
 */
public class FriendList {
    public List<UserDeprecated> friends;
    public int nextCursor;
}
