package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
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
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.SocialProvider;
import com.waylens.hachi.rest.body.UserProfileBody;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.authorization.FacebookAuthorizeActivity;
import com.waylens.hachi.ui.authorization.GoogleAuthorizeActivity;
import com.xfdingustc.rxutils.library.SimpleSubscribe;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/5/3.
 */
public class ProfileSettingPreferenceFragment extends PreferenceFragment {
    private static final String TAG = ProfileSettingPreferenceFragment.class.getSimpleName();

    private static final int REQUEST_FACEBOOK = 0x100;
    private static final int REQUEST_YOUTUBE = 0x101;
    private static final int REQUEST_PICKCAR = 0x102;

    private Preference mEmail;
    private Preference mChangePassword;
    private Preference mUserName;
    private Preference mBirthday;
    private Preference mGender;
    private Preference mRegion;
    private Preference mFacebook;
    private Preference mYoutube;


    private PreferenceCategory mVehicle;
    private Preference mAddCar;
    private List<Preference> mVehicleList = new ArrayList<>();

    private View positiveAction;
    private EditText oldPasswordInput;
    private EditText newPasswordInput;

    private SessionManager mSessionManager = SessionManager.getInstance();
    private RequestQueue mRequestQueue;

    private HachiApi mHachi = HachiService.createHachiApiService();

    private MaterialDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_account_setting);
        mRequestQueue = Volley.newRequestQueue(getActivity());
        mRequestQueue.start();
        initPreferences();

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.t(TAG).d("requestCode: " + requestCode + " resultCode: " + resultCode);
        switch (requestCode) {
            case REQUEST_FACEBOOK:
            case REQUEST_YOUTUBE:
                if (resultCode == Activity.RESULT_OK) {
                    updateSocialMedia();
                }
                break;
            case REQUEST_PICKCAR:
                if (resultCode == Activity.RESULT_OK) {
                    updateVehicle();
                }
                break;
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mRequestQueue.cancelAll(TAG);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateVehicle();

    }

    private void initPreferences() {
        mEmail = findPreference("email");
        mChangePassword = findPreference("change_password");
        mUserName = findPreference("user_name");
        mBirthday = findPreference("birthday");
        mGender = findPreference("gender");
        mRegion = findPreference("region");
        mVehicle = (PreferenceCategory) findPreference("vehicle");
        mAddCar = findPreference("add_car");


        setupSocialMedia();

        mEmail.setSummary(mSessionManager.getEmail());
        mUserName.setSummary(mSessionManager.getUserName());
        mBirthday.setSummary(mSessionManager.getBirthday());
        mRegion.setSummary(mSessionManager.getRegion());


        mGender.setSummary(mSessionManager.getGender());


        mChangePassword.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.change_password)
                    .customView(R.layout.dialog_change_password, true)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
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
                    .itemsCallbackSingleChoice(mSessionManager.getGenderInt(), new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                            return true;
                        }
                    })
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
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
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
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
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            DatePicker datePicker = (DatePicker) dialog.getCustomView().findViewById(R.id.dataPicker);
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

        mAddCar.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                VehiclePickActivity.launch(getActivity(), REQUEST_PICKCAR);
                return true;
            }
        });


    }

    private void setupSocialMedia() {
        mFacebook = findPreference("facebook");
        mYoutube = findPreference("youtube");
        setupFacebook();
        setupYoutube();
    }

    private void setupFacebook() {


        updateSocialMedia();

        mFacebook.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mSessionManager.getFacebookName() == null) {
                    FacebookAuthorizeActivity.launch(ProfileSettingPreferenceFragment.this, REQUEST_FACEBOOK);
                } else {
                    unbindSocialMedia(SocialProvider.FACEBOOK);
                }
                return true;
            }
        });

    }

    private void setupYoutube() {
        updateSocialMedia();
        mYoutube.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mSessionManager.getYoutubeName() == null) {
                    GoogleAuthorizeActivity.launch(ProfileSettingPreferenceFragment.this, REQUEST_YOUTUBE);
                } else {
                    unbindSocialMedia(SocialProvider.YOUTUBE);
                }

                return true;
            }

        });
    }


    private void unbindSocialMedia(final String facebook) {
        Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                subscriber.onStart();
                Call<SimpleBoolResponse> unbindSocialMediaCall = mHachi.unbindSocialProvider(facebook);
                Call<LinkedAccounts> callLinkedAccount = mHachi.getLinkedAccounts();
                try {
                    SimpleBoolResponse response = unbindSocialMediaCall.execute().body();
                    mSessionManager.saveLinkedAccounts(callLinkedAccount.execute().body());
                    subscriber.onCompleted();
                } catch (IOException e) {
                    subscriber.onError(e);
                }
            }
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Void>() {
                @Override
                public void onStart() {
                    super.onStart();
                    showDialog();
                }

                @Override
                public void onCompleted() {
                    hideDialog();
                    updateSocialMedia();
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    hideDialog();
                }

                @Override
                public void onNext(Void aVoid) {

                }
            });


    }

    private void updateSocialMedia() {
        String facebookName = mSessionManager.getFacebookName();
        if (facebookName != null) {
            mFacebook.setSummary(facebookName);
        } else {
            mFacebook.setSummary(getResources().getString(R.string.click_2_bind_facebook));
        }

        String youtubeName = mSessionManager.getYoutubeName();
        if (youtubeName != null) {
            mYoutube.setSummary(youtubeName);
        } else {
            mYoutube.setSummary(getResources().getString(R.string.click_2_bind_youtube));
        }
    }

    private void updateVehicle() {
        Logger.t(TAG).d("update Vehicle!");
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_USER_VEHICLE)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    renderVehicle(response);
                }
            })
            .build();
        mRequestQueue.add(request);
    }

    public void renderVehicle(JSONObject response) {
        Logger.t(TAG).d("render vehicle");
        try {
            JSONArray country = response.getJSONArray("vehicles");
            mVehicleList.clear();
            for (int i = 0; i < country.length(); i++) {
                JSONObject object = country.getJSONObject(i);
                Vehicle oneVehicle = new Vehicle();
                oneVehicle.modelYearID = object.getLong("modelYearID");
                oneVehicle.maker = object.getString("maker");
                oneVehicle.year = object.getInt("year");
                oneVehicle.model = object.getString("model");
                Preference oneCar = new Preference(this.getActivity());
                oneCar.setKey(String.valueOf(oneVehicle.modelYearID));
                oneCar.setSummary(oneVehicle.maker + "  " + oneVehicle.model + "  " + oneVehicle.year);
                oneCar.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(final Preference preference) {
                        final long modelYearID = Long.valueOf(preference.getKey());
                        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                            .content(R.string.delete_car_confirm)
                            .positiveText(R.string.ok)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
                                        .delete()
                                        .url(Constants.API_USER_VEHICLE + "/" + modelYearID)
                                        .listner(new Response.Listener<JSONObject>() {
                                            @Override
                                            public void onResponse(JSONObject response) {
                                                mVehicle.removePreference(preference);
                                            }
                                        })
                                        .build();

                                    mRequestQueue.add(request.setTag(TAG));
                                }
                            })
                            .build();
                        dialog.show();
                        return false;
                    }
                });
                mVehicleList.add(oneCar);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mVehicle.removeAll();
        mVehicle.addPreference(mAddCar);
        for (Preference item : mVehicleList) {
            mVehicle.addPreference(item);
        }

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
        HachiApi hachiApi = HachiService.createHachiApiService();
        UserProfileBody userProfileBody = new UserProfileBody();
        userProfileBody.gender = gender;
        hachiApi.changeProfileRx(userProfileBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<SimpleBoolResponse>() {
                @Override
                public void onNext(SimpleBoolResponse simpleBoolResponse) {
                    mSessionManager.setGender(gender);
                    mGender.setSummary(mSessionManager.getGender());
                    Snackbar.make(getView(), R.string.gender_update, Snackbar.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Throwable e) {
                    Snackbar.make(getView(), new String(e.getMessage()), Snackbar.LENGTH_LONG).show();
                }
            });
    }


    private void updateNewUserName(final String newUserName) {
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_USER_PROFILE)
            .postBody("userName", newUserName)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mUserName.setSummary(newUserName);
                    mSessionManager.setUserName(newUserName);
                    Snackbar.make(getView(), R.string.username_update, Snackbar.LENGTH_SHORT).show();
                }
            })
            .errorListener(new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Logger.t(TAG).d(new String(error.networkResponse.data));
                    if (error.networkResponse.statusCode == 400) {
                        try {
                            JSONObject jsonObject = new JSONObject(new String(error.networkResponse.data));
                            String msg = jsonObject.optString("msg", "Failed to update username.");
                            Snackbar.make(getView(), msg, Snackbar.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
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
                    mSessionManager.setBirthday(birthday);
                    Snackbar.make(getView(), R.string.birthday_update, Snackbar.LENGTH_SHORT).show();
                }
            })
            .errorListener(new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Snackbar.make(getView(), new String(error.networkResponse.data), Snackbar.LENGTH_SHORT).show();
                }
            })
            .build();


        mRequestQueue.add(request.setTag(TAG));
    }

    public void showDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            return;
        }
        mProgressDialog = new MaterialDialog.Builder(getActivity())
            .title(R.string.unbinding)
            .progress(true, 0)
            .progressIndeterminateStyle(true)
            .build();

        mProgressDialog.show();
    }

    public void hideDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    public class Vehicle {
        public long modelYearID;
        public String maker;
        public String model;
        public int year;
    }

}
