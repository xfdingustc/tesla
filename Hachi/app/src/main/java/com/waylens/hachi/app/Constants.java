package com.waylens.hachi.app;

import com.waylens.hachi.utils.PreferenceUtils;

/**
 * Created by Xiaofei on 2015/8/5.
 */
public class Constants {

    public static final String DEVICE_TYPE = "ANDROID";


    public static final String HOST_URL = getHostUrl();

    public static String getHostUrl() {
        return PreferenceUtils.getString("server", "https://agent.waylens.com/");
    }

//    public static final String HOST_URL = "http://192.168.20.97:9000/";

    public static final String API_AUTH_FACEBOOK = HOST_URL + "api/authenticate/facebook?accessToken=";


    public static final String API_REPORT = HOST_URL + "api/reports";

    public static final String API_BLOCK = HOST_URL + "api/friends/block";

    public static final String PARAM_SORT_UPLOAD_TIME = "uploadtime_desc";

    public static final String API_DEVICE_ACTIVATION = HOST_URL + "api/devices/login";

    public static final String DEFAULT_AVATAR= "https://d3dxhfn6er5hd4.cloudfront.net/avatar/default.png";




    public static final String API_COMMENTS_MARK_READ = HOST_URL + "api/events/mark_read";


    public enum EventType {FOLLOW_USER, COMMENT_MOMENT, LIKE_MOMENT, REFER_USER, PUBLISH_NEWS}
}
