package com.waylens.hachi.utils;

import com.waylens.hachi.R;

/**
 * Created by Richard on 8/18/15.
 */
public class ServerMessage {

    //"the account is already bound"
    public static final int ACCOUNT_ALREADY_BOUND = 26;

    //"username has been registered"
    public static final int USER_NAME_BEEN_USED = 30; //

    public static int getErrorMessage(int code) {
        return R.string.unknown_error;
    }
}
