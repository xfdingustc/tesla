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




    public static final String API_RESET_PASSWORD_MAIL = HOST_URL + "api/users/send_passwordreset_mail?e=";

    public static final String API_RESET_PASSWORD = HOST_URL + "api/users/reset_password";

    public static final String API_AUTH_FACEBOOK = HOST_URL + "api/authenticate/facebook?accessToken=";


    public static final String API_REPORT = HOST_URL + "api/reports";

    public static final String API_BLOCK = HOST_URL + "api/friends/block";

    public static final String API_COUNTRY = HOST_URL + "api/region/countries";



    public static final String API_MAKER = HOST_URL + "api/vehicle/makers";

    public static final String API_MODEL = HOST_URL + "api/vehicle/models";

    public static final String API_MODEL_YEAR = HOST_URL + "api/vehicle/years";

    public static final String API_USER_VEHICLE = HOST_URL + "api/users/vehicle";


    public static final String PARAM_SORT_UPLOAD_TIME = "uploadtime_desc";



    public static final String API_COMMENTS = HOST_URL + "api/comments";


    public static final String API_COMMENTS_QUERY_STRING = "?m=%s&cursor=%s&count=%s";

    public static final String API_DEVICE_ACTIVATION = HOST_URL + "api/devices/login";



    public static final String API_NOTIFICATIONS_COMMENTS = HOST_URL + "api/events/comments";

    public static final String API_NOTIFICATIONS_LIKES = HOST_URL + "api/events/likes";

    public static final String API_NOTIFICATIONS_FOLLOWS = HOST_URL + "api/events/follows";

    public static final String API_QS_COMMON = "?cursor=%s&count=%s";

    public static final String API_COMMENTS_MARK_READ = HOST_URL + "api/events/mark_read";


    public enum EventType {FOLLOW_USER, COMMENT_MOMENT, LIKE_MOMENT, REFER_USER, PUBLISH_NEWS}
}
