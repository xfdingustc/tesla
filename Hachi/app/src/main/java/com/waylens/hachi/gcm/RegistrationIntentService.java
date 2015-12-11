/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.waylens.hachi.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String id = instanceID.getId();
            Log.e(TAG, "ID: " + id);
            String token = null ;// instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    //GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]

            sendRegistrationToServer(token);
            subscribeTopics(token);
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            PreferenceUtils.putString(PreferenceUtils.SEND_GCM_TOKEN_SERVER, null);
        }
    }

    /**
     * Persist registration to third-party servers.
     * <p/>
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(final String token) {
        String savedToken = PreferenceUtils.getString(PreferenceUtils.SEND_GCM_TOKEN_SERVER, null);
        if (token == null || token.equals(savedToken)) {
            return;
        }

        RequestQueue requestQueue = VolleyUtil.newVolleyRequestQueue(this);
        JSONObject params = new JSONObject();
        try {
            params.put("deviceType", Constants.DEVICE_TYPE);
            params.put("deviceID", token);
        } catch (JSONException e) {
            Log.e(TAG, "", e);
        }
        requestQueue.add(new AuthorizedJsonRequest(Request.Method.POST, Constants.API_DEVICE_ACTIVATION, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e("test", "response: " + response);
                        String waylensToken = response.optString("token");
                        if (!TextUtils.isEmpty(waylensToken)) {
                            PreferenceUtils.putString(PreferenceUtils.SEND_GCM_TOKEN_SERVER, token);
                            SessionManager.getInstance().refreshToken(waylensToken);
                            Log.e(TAG, "GCM registration is successful.");
                        } else {
                            notifyRegistrationFailure();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        notifyRegistrationFailure();
                    }
                }));
    }

    void notifyRegistrationFailure() {
        PreferenceUtils.putString(PreferenceUtils.SEND_GCM_TOKEN_SERVER, null);
        Log.e(TAG, "GCM registration failed.");
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        for (String topic : TOPICS) {
            GcmPubSub pubSub = GcmPubSub.getInstance(this);
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]

}
