package com.waylens.hachi.utils;

import android.util.SparseIntArray;

import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Richard on 8/18/15.
 */
public class ServerMessage {

    private static final String TAG = "ServerError";

    public static final int UNKNOWN_ERROR = -1;

    public static final int INVALID_JSON_FORMAT = 1;

    public static final int USER_NAME_PASSWORD_NOT_MATCHED = 20;

    public static final int AUTHENTICATION_FAILED = 23;

    public static final int NO_AUTHORIZED = 24;

    //"the account is already bound"
    public static final int ACCOUNT_ALREADY_BOUND = 26;

    //"username has been registered"
    public static final int USER_NAME_HAS_BEEN_USED = 30; //

    public static final int EMAIL_HAS_BEEN_USED = 31;

    private static SparseIntArray msgResourceIDs = new SparseIntArray();


    static {
        if (msgResourceIDs.size() == 0) {
            msgResourceIDs.put(UNKNOWN_ERROR, R.string.server_msg_unknown_error);
            msgResourceIDs.put(INVALID_JSON_FORMAT, R.string.server_msg_invalid_json_format);
            msgResourceIDs.put(USER_NAME_PASSWORD_NOT_MATCHED, R.string.server_msg_user_pass_error);

            msgResourceIDs.put(AUTHENTICATION_FAILED, R.string.server_msg_authentication_failed);
            msgResourceIDs.put(NO_AUTHORIZED, R.string.server_msg_not_authorized);
            msgResourceIDs.put(ACCOUNT_ALREADY_BOUND, R.string.server_msg_account_already_bound);
            msgResourceIDs.put(USER_NAME_HAS_BEEN_USED, R.string.server_msg_user_name_has_been_used);
            msgResourceIDs.put(EMAIL_HAS_BEEN_USED, R.string.server_msg_email_has_been_used);
            msgResourceIDs.put(UNKNOWN_ERROR, R.string.server_msg_unknown_error);
            msgResourceIDs.put(UNKNOWN_ERROR, R.string.server_msg_unknown_error);
            msgResourceIDs.put(UNKNOWN_ERROR, R.string.server_msg_unknown_error);

        }
    }

    public static int getErrorMessage(int code) {
        int resourceID = msgResourceIDs.get(code);
        if (resourceID == 0) {
            resourceID = R.string.server_msg_unknown_error;
        }
        return resourceID;
    }

    /**
     * Parse Server Error with:
     * 0 -> errorCode,
     * 1 -> resource id of localized error message.
     * @return errorCode and Message resource id.
     */
    public static ErrorMsg parseServerError(VolleyError error) {
        ErrorMsg errorMsg = new ErrorMsg();
        if (error.networkResponse != null && error.networkResponse.data != null) {
            String errorMessageJson = new String(error.networkResponse.data);
            try {
                JSONObject errorJson = new JSONObject(errorMessageJson);
                Logger.t(TAG).json(errorJson.toString());
                int errorCode = errorJson.getInt("code");
                errorMsg.errorCode = errorCode;
                errorMsg.msgResID = getErrorMessage(errorCode);
            } catch (JSONException e) {
                Logger.t(TAG).e("", e);
            }
        }
        return errorMsg;
    }

    public static class ErrorMsg {
        public int errorCode;
        public int msgResID;

        public ErrorMsg() {
            errorCode = UNKNOWN_ERROR;
            msgResID = R.string.server_msg_unknown_error;
        }
    }

}
