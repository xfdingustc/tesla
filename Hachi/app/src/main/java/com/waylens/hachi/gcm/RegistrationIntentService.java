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

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;


import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.DeviceLoginBody;
import com.waylens.hachi.rest.response.AuthorizeResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.PushUtils;
import com.waylens.hachi.utils.VolleyUtil;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class RegistrationIntentService extends IntentService {
    private static final String TAG = RegistrationIntentService.class.getSimpleName();
    private static final String[] TOPICS = {"global"};

    public static void launch(Activity activity) {
        if (SessionManager.getInstance().isLoggedIn() && PushUtils.checkGooglePlayServices(activity)) {
            Intent intent = new Intent(activity, RegistrationIntentService.class);
            activity.startService(intent);
        }
    }

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
            Logger.t(TAG).d("in Registration Intent Service!");
            InstanceID instanceID = InstanceID.getInstance(this);
            String id = instanceID.getId();
            Log.e(TAG, "ID: " + id);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]

            sendRegistrationToServer(token);
            subscribeTopics(token);
        } catch (Exception e) {
            Logger.t(TAG).d("Failed to complete token refresh", e);
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
        Logger.t(TAG).d("send registration to server!");
        Logger.t(TAG).d(token);
        String savedToken = PreferenceUtils.getString(PreferenceUtils.SEND_GCM_TOKEN_SERVER, null);
        if (token == null || token.equals(savedToken)) {
            return;
        }


        HachiService.createHachiApiService().deviceLoginRx(new DeviceLoginBody(Constants.DEVICE_TYPE, token))
            .subscribe(new SimpleSubscribe<AuthorizeResponse>() {
                @Override
                public void onNext(AuthorizeResponse authorizeResponse) {
                    String waylensToken = authorizeResponse.token;
                    if (!TextUtils.isEmpty(waylensToken)) {
                        PreferenceUtils.putString(PreferenceUtils.SEND_GCM_TOKEN_SERVER, token);
                        SessionManager.getInstance().setToken(waylensToken);
                        Logger.t(TAG).d("GCM registration is successful.");
                    } else {
                        notifyRegistrationFailure();
                        Logger.t(TAG).d("GCM registration fails.");
                    }
                }

                @Override
                public void onError(Throwable e) {
                    super.onError(e);
                    Logger.t(TAG).d("GCM registration fails.");
                    notifyRegistrationFailure();
                }
            });
    }

    void notifyRegistrationFailure() {
        PreferenceUtils.putString(PreferenceUtils.SEND_GCM_TOKEN_SERVER, null);
        Logger.t(TAG).e("GCM registration failed.");
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
