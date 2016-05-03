package com.waylens.hachi.ui.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.widget.DatePicker;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Xiaofei on 2016/5/3.
 */
public class AccountSettingPreferenceFragment extends PreferenceFragment {
    private static final String TAG = AccountSettingPreferenceFragment.class.getSimpleName();
    private Preference mEmail;
    private Preference mChangePassword;
    private Preference mUserName;
    private Preference mBirthday;
    private Preference mGender;
    private Preference mRegion;

    private SessionManager mSessionManager = SessionManager.getInstance();
    private RequestQueue mRequestQueue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_account_setting);
        mRequestQueue = Volley.newRequestQueue(getActivity());
        mRequestQueue.start();
        initPreferences();

    }

    private void initPreferences() {
        mEmail = findPreference("email");
        mChangePassword = findPreference("change_password");
        mUserName = findPreference("user_name");
        mBirthday = findPreference("birthday");
        mGender = findPreference("gender");
        mRegion = findPreference("region");

        mEmail.setSummary(mSessionManager.getEmail());
        mUserName.setSummary(mSessionManager.getUserName());
        mBirthday.setSummary(mSessionManager.getBirthday());

        mUserName.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.change_username)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .input(getString(R.string.username), mSessionManager.getUserName(), new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                        }
                    })
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            String newUserName = dialog.getInputEditText().getText().toString();
                            updateNewUserName(newUserName);
                        }
                    })
                    .show();
                return true;
            }
        });

        mBirthday.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .customView(R.layout.fragment_data_picker, false)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            DatePicker datePicker = (DatePicker)dialog.getCustomView().findViewById(R.id.dataPicker);
                            Logger.t(TAG).d("year: " + datePicker.getYear());
                            Date date = new Date(datePicker.getYear() - 1900, datePicker.getMonth(), datePicker.getDayOfMonth());
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                            String birthday = format.format(date);
                            updateBirthday(birthday);
                        }
                    })
                    .show();
                return true;
            }
        });
    }


    private void updateNewUserName(final String newUserName) {
        String url = Constants.API_USER_PROFILE;
        Map<String, String> params = new HashMap<>();
        params.put("userName", newUserName);
        String postBody = new JSONObject(params).toString();
        Logger.t(TAG).d("postBody: "  + postBody);
        AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST, url, postBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Logger.t(TAG).json(response.toString());
                mUserName.setSummary(newUserName);
                mSessionManager.saveUserName(newUserName);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        mRequestQueue.add(request);
    }

    private void updateBirthday(final String birthday) {
        String url = Constants.API_USER_PROFILE;
        Map<String, String> params = new HashMap<>();
        params.put("birthday", birthday);
        String postBody = new JSONObject(params).toString();
        Logger.t(TAG).d("postBody: "  + postBody);
        AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST, url, postBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Logger.t(TAG).json(response.toString());
                mBirthday.setSummary(birthday);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        mRequestQueue.add(request);
    }

}
