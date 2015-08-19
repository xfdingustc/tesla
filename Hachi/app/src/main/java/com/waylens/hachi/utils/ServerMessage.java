package com.waylens.hachi.utils;

import com.waylens.hachi.R;

/**
 * Created by Richard on 8/18/15.
 */
public class ServerMessage {

    public static final int INVALID_JSON_FORMAT = 1;

    public static final int USER_NAME_PASSWORD_NOT_MATCHED = 20;

    public static final int AUTHENTICATION_FAILED = 23;

    public static final int NO_ACCESS = 24;


    //"the account is already bound"
    public static final int ACCOUNT_ALREADY_BOUND = 26;

    //"username has been registered"
    public static final int USER_NAME_HAS_BEEN_USED = 30; //

    public static final int EMAIL_HAS_BEEN_USED = 31;

    public static int getErrorMessage(int code) {
        return R.string.unknown_error;
    }
}
