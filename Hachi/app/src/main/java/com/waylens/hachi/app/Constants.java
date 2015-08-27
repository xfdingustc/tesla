package com.waylens.hachi.app;

/**
 * Created by Xiaofei on 2015/8/5.
 */
public class Constants {
    public static final String DEVELOPER_KEY = "AIzaSyBuPLSINMBz163ecxJQRySpcev4jfI_BJg";

    public static final String HOST_URL = "http://ws.waylens.com:9000/";
    public static final String API_SIGN_UP = HOST_URL + "api/users/signup";

    public static final String API_LOGIN = HOST_URL + "api/users/signin";

    public static final String API_AUTH_FACEBOOK = HOST_URL + "api/authenticate/facebook?accessToken=";

    /** Link Social account with Waylens account */
    public static final String API_LINK_ACCOUNT = HOST_URL + "api/users/link_waylens";

    public static final String API_CHECK_EMAIL = HOST_URL + "api/users/check_id?key=email&id=";

    public static final String API_CHECK_USER_NAME = HOST_URL + "api/users/check_id?key=username&id=";

    public static final String API_START_UPLOAD_AVATAR = HOST_URL + "api/users/start_upload_avatar";

    public static final String API_MOMENTS = HOST_URL + "api/moments?order=uploadtime_desc";

    public static final String API_MOMENT_PLAY = HOST_URL + "api/moments/play/";

    public static final String PARAM_SORT_UPLOAD_TIME = "uploadtime_desc";

    public static final String PARAM_SORT_LIKE_COUNT = "likescount_desc";

    public static final String API_MOMENT_LIKE = HOST_URL + "api/likes";

    public static final String  API_COMMENTS = HOST_URL + "api/comments";

    public static final String API_COMMENTS_QUERY_STRING = "?m=%s&cursor=%s&count=%s";
}
