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



    public static final String PARAM_SORT_UPLOAD_TIME = "uploadtime_desc";


    public static final String DEFAULT_AVATAR= "https://d3dxhfn6er5hd4.cloudfront.net/avatar/default.png";






    public enum EventType {FOLLOW_USER, COMMENT_MOMENT, LIKE_MOMENT, REFER_USER, PUBLISH_NEWS}
}
