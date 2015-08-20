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

    public static final int NO_ACCESS = 24;


    //"the account is already bound"
    public static final int ACCOUNT_ALREADY_BOUND = 26;

    //"username has been registered"
    public static final int USER_NAME_HAS_BEEN_USED = 30; //

    public static final int EMAIL_HAS_BEEN_USED = 31;

    public static int getErrorMessage(int code) {
        return R.string.unknown_error;
    }

    /**
     * Parse Server Error with:
     * 0 -> errorCode,
     * 1 -> resource id of localized error message.
     * @return errorCode and Message resource id.
     */
    public static SparseIntArray parseServerError(VolleyError error) {
        SparseIntArray intArray = new SparseIntArray();
        if (error.networkResponse == null || error.networkResponse.data == null) {
            intArray.put(0, UNKNOWN_ERROR);
            intArray.put(1, R.string.unknown_error);
        } else {
            String errorMessageJson = new String(error.networkResponse.data);
            try {
                JSONObject errorJson = new JSONObject(errorMessageJson);
                Logger.t(TAG).json(errorJson.toString());
                int errorCode = errorJson.getInt("code");
                intArray.put(0, errorCode);
                intArray.put(1, getErrorMessage(errorCode));
            } catch (JSONException e) {
                intArray.put(0, UNKNOWN_ERROR);
                intArray.put(1, R.string.unknown_error);
                Logger.t(TAG).e("", e);
            }
        }
        return intArray;
    }
}
