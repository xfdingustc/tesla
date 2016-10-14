package com.waylens.hachi.rest.response;

import com.waylens.hachi.rest.bean.Notification;

import java.util.List;

/**
 * Created by Xiaofei on 2016/10/14.
 */

public class NotificationResponse {
    public List<Notification> notifications;
    public long nextCursor;
    public int unreadCount;
}
