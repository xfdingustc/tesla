package com.waylens.hachi.utils;

import android.util.Log;
import android.util.SparseIntArray;

import com.android.volley.NoConnectionError;
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

    public static final int ERROR_NO_CONNECTION = -2;

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

    public static final int EMAIL_NOT_EXIST = 32;
    public static final int EXCEED_MAX_RETRIES = 39;
    public static final int EMAIL_TOO_FREQUENT = 40;
    public static final int VERIFICATION_CODE_HAS_EXPIRED = 41;
    public static final int VERIFICATION_CODE_INCORRECT = 42;

    private static SparseIntArray msgResourceIDs = new SparseIntArray();


    static {
        if (msgResourceIDs.size() == 0) {
            msgResourceIDs.put(ERROR_NO_CONNECTION, R.string.error_no_connection);
            msgResourceIDs.put(UNKNOWN_ERROR, R.string.server_msg_unknown_error);
            msgResourceIDs.put(INVALID_JSON_FORMAT, R.string.server_msg_invalid_json_format);
            msgResourceIDs.put(USER_NAME_PASSWORD_NOT_MATCHED, R.string.server_msg_user_pass_error);

            msgResourceIDs.put(AUTHENTICATION_FAILED, R.string.server_msg_authentication_failed);
            msgResourceIDs.put(NO_AUTHORIZED, R.string.server_msg_not_authorized);
            msgResourceIDs.put(ACCOUNT_ALREADY_BOUND, R.string.server_msg_account_already_bound);
            msgResourceIDs.put(USER_NAME_HAS_BEEN_USED, R.string.server_msg_user_name_has_been_used);
            msgResourceIDs.put(EMAIL_HAS_BEEN_USED, R.string.server_msg_email_has_been_used);
            msgResourceIDs.put(EMAIL_NOT_EXIST, R.string.server_msg_email_not_existed);
            msgResourceIDs.put(EXCEED_MAX_RETRIES, R.string.server_msg_exceed_max_retries);
            msgResourceIDs.put(EMAIL_TOO_FREQUENT, R.string.server_msg_email_too_frequent);
            msgResourceIDs.put(VERIFICATION_CODE_HAS_EXPIRED, R.string.server_msg_code_has_expired);
            msgResourceIDs.put(VERIFICATION_CODE_INCORRECT, R.string.server_msg_code_incorrect);
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
                Logger.t(TAG).d("Server msg: " + errorMessageJson);
                JSONObject errorJson = new JSONObject(errorMessageJson);
                Logger.t(TAG).json(errorJson.toString());
                int errorCode = errorJson.getInt("code");
                errorMsg.errorCode = errorCode;
                errorMsg.msgResID = getErrorMessage(errorCode);
            } catch (JSONException e) {
                Logger.t(TAG).e("", e);
            }
        } else if (error instanceof NoConnectionError) {
            errorMsg.errorCode = ERROR_NO_CONNECTION;
            errorMsg.msgResID = getErrorMessage(ERROR_NO_CONNECTION);
        } else {
            Logger.t(TAG).e(error, "");
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
