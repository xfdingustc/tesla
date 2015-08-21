package com.waylens.hachi.app;

/**
 * Created by Xiaofei on 2015/8/5.
 */
public class Constants {
    public static final String HOST_URL = "http://ws.waylens.com:9000/";
    public static final String API_SIGN_UP = HOST_URL + "api/users/signup";

    public static final String API_LOGIN = HOST_URL + "api/users/signin";

    public static final String API_AUTH_FACEBOOK = HOST_URL + "api/authenticate/facebook?accessToken=";

    /** Link Social account with Waylens account */
    public static final String API_LINK_ACCOUNT = HOST_URL + "api/users/link_waylens";

    public static final String API_CHECK_EMAIL = HOST_URL + "api/users/check_id?key=email&id=";

    public static final String API_CHECK_USER_NAME = HOST_URL + "api/users/check_id?key=username&id=";

    public static final String API_START_UPLOAD_AVATAR = HOST_URL + "api/users/start_upload_avatar";
}
