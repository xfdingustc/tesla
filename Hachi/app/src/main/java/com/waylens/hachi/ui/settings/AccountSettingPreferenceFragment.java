package com.waylens.hachi.ui.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;

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
    private Preference mLogout;

    private View positiveAction;
    private EditText oldPasswordInput;
    private EditText newPasswordInput;

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

    @Override
    public void onDetach() {
        super.onDetach();
        mRequestQueue.cancelAll(TAG);
    }

    private void initPreferences() {
        mEmail = findPreference("email");
        mChangePassword = findPreference("change_password");
        mUserName = findPreference("user_name");
        mBirthday = findPreference("birthday");
        mGender = findPreference("gender");
        mRegion = findPreference("region");
        mLogout = findPreference("logout");

        mEmail.setSummary(mSessionManager.getEmail());
        mUserName.setSummary(mSessionManager.getUserName());
        mBirthday.setSummary(mSessionManager.getBirthday());
        mRegion.setSummary(mSessionManager.getRegion());


        final int gender = mSessionManager.getGender();
        switch (gender) {
            case 0:
                mGender.setSummary(R.string.male);
                break;
            case 1:
                mGender.setSummary(R.string.female);
                break;
            default:
                mGender.setSummary(R.string.rather_not_to_say);
                break;
        }

        mChangePassword.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.change_password)
                    .customView(R.layout.dialog_change_password, true)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            uploadPassword(oldPasswordInput.getText().toString(), newPasswordInput.getText().toString());
                        }
                    })
                    .show();

                positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
                //noinspection ConstantConditions
                newPasswordInput = (EditText) dialog.getCustomView().findViewById(R.id.newPassword);
                oldPasswordInput = (EditText) dialog.getCustomView().findViewById(R.id.oldPassword);
                oldPasswordInput.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        positiveAction.setEnabled(s.toString().trim().length() > 0);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });

                CheckBox checkbox = (CheckBox) dialog.getCustomView().findViewById(R.id.showPassword);
                checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        oldPasswordInput.setInputType(!isChecked ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT);
                        oldPasswordInput.setTransformationMethod(!isChecked ? PasswordTransformationMethod.getInstance() : null);
                        newPasswordInput.setInputType(!isChecked ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT);
                        newPasswordInput.setTransformationMethod(!isChecked ? PasswordTransformationMethod.getInstance() : null);
                    }
                });
                return true;
            }
        });

        mGender.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title("Please choose your gender")
                    .items(R.array.gender_list)
                    .itemsCallbackSingleChoice(gender, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                            return true;
                        }
                    })
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            int gender = dialog.getSelectedIndex();
                            String genderStr = "";
                            switch (gender) {
                                case 0:
                                    genderStr = "MALE";
                                    break;
                                case 1:
                                    genderStr = "FEMALE";
                                    break;

                            }
                            updateGender(genderStr);
                        }
                    })
                    .show();
                return true;
            }
        });

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

        mRegion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                CountryActivity.launch(getActivity());
                return true;
            }
        });

        mLogout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.logout)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            mSessionManager.logout();
                            getActivity().finish();
                        }
                    })
                    .show();
                return true;
            }
        });
    }

    private void uploadPassword(String oldPwd, String newPwd) {
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_USER_CHANGE_PASSWORD)
            .postBody("curPassword", oldPwd)
            .postBody("newPassword", newPwd)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).json(response.toString());
                    mSessionManager.saveLoginInfo(response);
                    Snackbar.make(getView(), R.string.change_password_successfully, Snackbar.LENGTH_LONG).show();
                }
            })
            .errorListener(new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Snackbar.make(getView(), R.string.change_password_failed, Snackbar.LENGTH_LONG).show();
                }
            })
            .build();
        mRequestQueue.add(request.setTag(TAG));
    }

    private void updateGender(final String gender) {
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_USER_PROFILE)
            .postBody("gender", gender)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    String i18nGender;
                    if (gender.equals("MALE")) {
                        i18nGender = getString(R.string.male);
                    } else if (gender.equals("FEMALE")) {
                        i18nGender = getString(R.string.female);
                    } else {
                        i18nGender = getString(R.string.rather_not_to_say);
                    }
                    mGender.setSummary(i18nGender);
                }
            })
            .build();
        mRequestQueue.add(request.setTag(TAG));
    }


    private void updateNewUserName(final String newUserName) {
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_USER_PROFILE)
            .postBody("userName", newUserName)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mUserName.setSummary(newUserName);
                    mSessionManager.saveUserName(newUserName);
                }
            })
            .build();
        mRequestQueue.add(request);
    }

    private void updateBirthday(final String birthday) {
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_USER_PROFILE)
            .postBody("birthday", birthday)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mBirthday.setSummary(birthday);
                }
            })
            .build();


        mRequestQueue.add(request.setTag(TAG));
    }

}
