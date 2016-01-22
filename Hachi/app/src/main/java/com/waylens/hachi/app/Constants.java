package com.waylens.hachi.app;

/**
 * Created by Xiaofei on 2015/8/5.
 */
public class Constants {
    public static final String DEVICE_TYPE = "ANDROID";

    public static final String DEVELOPER_KEY = "AIzaSyBuPLSINMBz163ecxJQRySpcev4jfI_BJg";

    public static final String API_WEATHER = "http://api.worldweatheronline.com/free/v2/weather.ashx?format=json&num_of_days=1&tp=12&key=e081e88edf6ffe4bcd0d12f34b26e&q=%s";

    public static final String HOST_URL = "http://ws.waylens.com:9000/";

    //public static final String HOST_URL = "http://192.168.20.97:9000/";

    public static final String API_SIGN_UP = HOST_URL + "api/users/signup";

    public static final String API_LOGIN = HOST_URL + "api/users/signin";

    public static final String API_AUTH_FACEBOOK = HOST_URL + "api/authenticate/facebook?accessToken=";

    /**
     * Link Social account with Waylens account
     */
    public static final String API_LINK_ACCOUNT = HOST_URL + "api/users/link_waylens";

    public static final String API_CHECK_EMAIL = HOST_URL + "api/users/check_id?key=email&id=";

    public static final String API_CHECK_USER_NAME = HOST_URL + "api/users/check_id?key=username&id=";

    public static final String API_START_UPLOAD_AVATAR = HOST_URL + "api/users/start_upload_avatar";

    public static final String API_MOMENTS = HOST_URL + "api/moments";

    public static final String API_MOMENTS_ME = HOST_URL + "api/users/me/moments";

    public static final String API_QS_MOMENTS = "?order=%s&cursor=%d&count=%d";

    public static final String API_MOMENT_PLAY = HOST_URL + "api/moments/play/";

    public static final String PARAM_MOMENTS = "moments";

    public static final String PARAM_SORT_UPLOAD_TIME = "uploadtime_desc";

    public static final String PARAM_SORT_LIKE_COUNT = "likescount_desc";

    public static final String API_MOMENT_LIKE = HOST_URL + "api/likes";

    public static final String API_COMMENTS = HOST_URL + "api/comments";

    public static final String API_COMMENTS_QUERY_STRING = "?m=%s&cursor=%s&count=%s";

    public static final String API_DEVICE_ACTIVATION = HOST_URL + "api/devices/login";

    public static final String API_DEVICE_DEACTIVATION = HOST_URL + "api/devices/logout";

    public static final String API_NOTIFICATIONS_COMMENTS = HOST_URL + "api/events/comments";

    public static final String API_NOTIFICATIONS_LIKES = HOST_URL + "api/events/likes";

    public static final String API_QS_COMMON = "?cursor=%s&count=%s";

    public static final String API_COMMENTS_MARK_READ = HOST_URL + "api/events/mark_read";

    public static final String API_USERS = HOST_URL + "api/users";

    public static final String API_FRIENDS = HOST_URL + "api/friends/";

    public static final String API_LIKES = HOST_URL + "api/likes";

    public static final String API_FRIENDS_FOLLOW = HOST_URL + "api/friends/follow";

    public static final String API_FRIENDS_UNFOLLOW = HOST_URL + "api/friends/unfollow";

    public static final String MAP_BOX_ACCESS_TOKEN = "sk.eyJ1IjoibGlhbmd5eCIsImEiOiJjaWduYjFmajIwMGEyNjFtMzAyZ2xkNTN6In0.k1oa9ynRfAHp_3ka68JJ_w";

    public enum EventType {FOLLOW_USER, COMMENT_MOMENT, LIKE_MOMENT, REFER_USER, PUBLISH_NEWS}
}
