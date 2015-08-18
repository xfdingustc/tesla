package com.waylens.hachi.app;

/**
 * Created by Xiaofei on 2015/8/5.
 */
public class Constants {
    public static final String HOST_URL = "http://ws.waylens.com:9000/";
    public static final String SIGN_UP = "api/users/signup";

    public static final String LOGIN_URL = HOST_URL + "api/users/signin";

    public static final String AUTH_FACEBOOK = HOST_URL + "api/authenticate/facebook?accessToken=";

    /** Link Social account with Waylens account */
    public static final String LINK_ACCOUNT = HOST_URL + "api/users/link_waylens";
}
