package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.bgjob.upload.event.UploadAvatarEvent;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.SocialProvider;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.authorization.FacebookAuthorizeActivity;
import com.waylens.hachi.ui.authorization.GoogleAuthorizeActivity;
import com.waylens.hachi.ui.avatar.AvatarActivity;
import com.waylens.hachi.utils.FastBlurUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/4/26.
 */
public class AccountActivity extends BaseActivity {
    private static final String TAG = AccountActivity.class.getSimpleName();
    private static final int REQUEST_FACEBOOK = 0x100;
    private static final int REQUEST_YOUTUBE = 0x101;
    private static final int REQUEST_PICKCAR = 0x102;

    private SessionManager mSessionManager = SessionManager.getInstance();

    private VehicleAdapter mVehicleAdapter;

    private HachiApi mHachi = HachiService.createHachiApiService();

    private EditText oldPasswordInput;
    private EditText newPasswordInput;

    private View positiveAction;

    @BindView(R.id.avatar)
    CircleImageView mAvatar;

    @BindView(R.id.btnAddPhoto)
    ImageButton mBtnAddPhoto;

    @BindView(R.id.avatar_upload_progress)
    ProgressBar mAvatarUploadProgress;

    @BindView(R.id.blur_avatar)
    ImageView mBlurAvatar;

    @BindView(R.id.user_name)
    TextView mUsername;

    @BindView(R.id.email)
    TextView mEmail;

    @BindView(R.id.birthday)
    TextView mBirthday;

    @BindView(R.id.gender)
    TextView mGender;

    @BindView(R.id.region)
    TextView mRegion;

    @BindView(R.id.facebook)
    TextView mFacebook;

    @BindView(R.id.youtube)
    TextView mYoutube;

    @BindView(R.id.vehicle_list)
    RecyclerView mVehicleList;


    @OnClick(R.id.btn_logout)
    public void onLogoutClicked() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(R.string.logout)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    mSessionManager.logout();
                    finish();
                }
            })
            .show();
    }

    @OnClick(R.id.avatar)
    public void onBtnAvatarClicked() {
        AvatarActivity.launch(this, false);
    }

    @OnClick(R.id.btnAddPhoto)
    public void onBtnAddPhotoClick() {
        AvatarActivity.launch(this, true);
    }

    @OnClick(R.id.layout_user_name)
    public void onUserNameClicked() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
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
                    mUsername.setText(newUserName);
                    mSessionManager.setUserName(newUserName);
                    updateNewUserName(newUserName);
                }
            })
            .show();
    }

    @OnClick(R.id.change_password)
    public void onChangePasswordClicked() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
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
    }

    @OnClick(R.id.layout_birthday)
    public void onBirthdayClicked() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
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
                    mSessionManager.setBirthday(birthday);
                    mBirthday.setText(birthday);
                    updateBirthday(birthday);
                }
            })
            .show();
    }

    @OnClick(R.id.layout_gender)
    public void onGenderClicked() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
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
                    mSessionManager.setGender(gender);
                    mGender.setText(mSessionManager.getGender());
                }
            })
            .show();
    }

    @OnClick(R.id.layout_region)
    public void onRegionClicked() {
        CountryActivity.launch(this);
    }

    @OnClick(R.id.layout_add_vehicle)
    public void onAddVehicleClicked() {
        VehiclePickActivity.launch(this, REQUEST_PICKCAR);
    }

    @OnClick(R.id.layout_facebook)
    public void onLayoutFacebookClicked() {
        if (!mSessionManager.isFacebookLinked()) {
            FacebookAuthorizeActivity.launch(this, REQUEST_FACEBOOK);
        } else {
            new MaterialDialog.Builder(this)
                .content(R.string.unbind_confirm)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        unbindSocialMedia(SocialProvider.FACEBOOK);
                    }
                }).show();

        }
    }

    @OnClick(R.id.layout_youtube)
    public void onLayoutYoutubeClicked() {
        if (!mSessionManager.isYoutubeLinked()) {
            GoogleAuthorizeActivity.launch(this, REQUEST_YOUTUBE);
        } else {
            new MaterialDialog.Builder(this)
                .content(R.string.unbind_confirm)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        unbindSocialMedia(SocialProvider.YOUTUBE);
                    }
                }).show();

        }
    }


    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, AccountActivity.class);
        activity.startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventUpload(UploadAvatarEvent event) {
        switch (event.getWhat()) {
            case UploadAvatarEvent.UPLOAD_WHAT_START:
            case UploadAvatarEvent.UPLOAD_WHAT_PROGRESS:
                if (mAvatarUploadProgress.getVisibility() != View.VISIBLE) {
                    mAvatarUploadProgress.setVisibility(View.VISIBLE);
                }
                break;
            case UploadAvatarEvent.UPLOAD_WHAT_FINISHED:
                mAvatarUploadProgress.setVisibility(View.GONE);
                fetchUserProfile();
                break;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.t(TAG).d("requestCode: " + requestCode + " resultCode: " + resultCode);
        switch (requestCode) {
            case REQUEST_FACEBOOK:
            case REQUEST_YOUTUBE:
                if (resultCode == Activity.RESULT_OK) {
                    refreshSocialMedia();
                }
                break;
            case REQUEST_PICKCAR:
                if (resultCode == Activity.RESULT_OK) {
                    updateVehicle();
                }
                break;
        }

    }

    private void fetchUserProfile() {
        Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                Call<UserInfo> userInfoCall = mHachi.getMyUserInfo();
                Call<LinkedAccounts> callLinkedAccount = mHachi.getLinkedAccounts();
                try {
                    mSessionManager.saveUserProfile(userInfoCall.execute().body());
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
                public void onCompleted() {
                    if (isDestroyed()) {
                        return;
                    }
                    refreshUserAvatar();
//                    AccountSettingPreferenceFragment fragment = new AccountSettingPreferenceFragment();
//                    if (!isDestroyed()) {
//                        getFragmentManager().beginTransaction().replace(R.id.accountPref, fragment).commitAllowingStateLoss();
//                    }
                    refreshUserProfile();
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    showErrorSnakeBar();
                }

                @Override
                public void onNext(Void integer) {

                }
            });

    }

    private void showErrorSnakeBar() {
        Snackbar snackbar = Snackbar.make(mAvatar, R.string.fetch_account_profile_failed, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.retry, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchUserProfile();
            }
        });
        snackbar.show();

    }

    private void refreshUserAvatar() {
        Glide.with(this)
            .load(mSessionManager.getAvatarUrl())
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(mAvatar);

        Target blurTarget = new SimpleTarget() {
            @Override
            public void onResourceReady(Object resource, GlideAnimation glideAnimation) {
                if (!(resource instanceof Bitmap)) {
                    return;
                }

                Bitmap bitmap = (Bitmap) resource;
                Bitmap blurBitmap = FastBlurUtil.doBlur(bitmap, 8, true);
                mBlurAvatar.setImageBitmap(blurBitmap);
            }
        };

        Glide.with(this)
            .load(mSessionManager.getAvatarUrl())
            .asBitmap()
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .into(blurTarget);
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_account);
        setupToolbar();
        mVehicleList.setLayoutManager(new LinearLayoutManager(this));
        mVehicleAdapter = new VehicleAdapter(this, new VehicleAdapter.OnVehicleClickListener() {
            @Override
            public void onVehicleClicked(long modelId) {
                AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
                    .delete()
                    .url(Constants.API_USER_VEHICLE + "/" + modelId)
                    .listner(new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                        }
                    })
                    .build();

                mRequestQueue.add(request.setTag(TAG));
            }
        });
        mVehicleList.setAdapter(mVehicleAdapter);

        refreshUserProfile();
        fetchUserProfile();
        refreshUserAvatar();
        updateVehicle();
    }

    private void refreshUserProfile() {
        mEmail.setText(mSessionManager.getEmail());
        mUsername.setText(mSessionManager.getUserName());
        mBirthday.setText(mSessionManager.getBirthday());
        mGender.setText(mSessionManager.getGender());
        mRegion.setText(mSessionManager.getRegion());

        if (mSessionManager.isFacebookLinked()) {
            mFacebook.setText(mSessionManager.getFacebookName());
        } else {
            mFacebook.setText(R.string.click_2_bind_facebook);
        }

        if (mSessionManager.isYoutubeLinked()) {
            mYoutube.setText(mSessionManager.getYoutubeName());
        } else {
            mYoutube.setText(R.string.click_2_bind_youtube);
        }

        refreshSocialMedia();

        refreshVecihleList(mSessionManager.getVehicle());
    }

    private void refreshVecihleList(String vehicle) {
        mVehicleAdapter.setVehicleList(vehicle);
    }

    private void refreshSocialMedia() {
        String facebookName = mSessionManager.getFacebookName();
        if (facebookName != null) {
            mFacebook.setText(facebookName);
        } else {
            mFacebook.setText(getResources().getString(R.string.click_2_bind_facebook));
        }

        String youtubeName = mSessionManager.getYoutubeName();
        if (youtubeName != null) {
            mYoutube.setText(youtubeName);
        } else {
            mYoutube.setText(getResources().getString(R.string.click_2_bind_youtube));
        }
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();

        getToolbar().setNavigationIcon(R.drawable.navbar_back);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getToolbar().setTitle(R.string.account);
    }

    private void updateNewUserName(final String newUserName) {
        Logger.t(TAG).d("update User name: " + newUserName);
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_USER_PROFILE)
            .postBody("userName", newUserName)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mSessionManager.setUserName(newUserName);
                    Snackbar.make(mUsername, R.string.username_update, Snackbar.LENGTH_SHORT).show();
                }
            })
            .build();
        mRequestQueue.add(request);
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
                    Snackbar.make(mAvatar, R.string.change_password_successfully, Snackbar.LENGTH_LONG).show();
                }
            })
            .errorListener(new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Snackbar.make(mAvatar, R.string.change_password_failed, Snackbar.LENGTH_LONG).show();
                }
            })
            .build();
        mRequestQueue.add(request.setTag(TAG));
    }

    private void updateBirthday(final String birthday) {
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_USER_PROFILE)
            .postBody("birthday", birthday)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Snackbar.make(mGender, R.string.birthday_update, Snackbar.LENGTH_SHORT).show();
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
                   Snackbar.make(mGender, R.string.gender_update, Snackbar.LENGTH_SHORT).show();

                }
            })
            .build();
        mRequestQueue.add(request.setTag(TAG));
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
                    refreshSocialMedia();
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

    private void updateVehicle() {
        Logger.t(TAG).d("update Vehicle!");
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_USER_VEHICLE)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mSessionManager.setVehicle(response.toString());
                    refreshVecihleList(response.toString());
                }
            })
            .build();
        mRequestQueue.add(request);
    }

    public void showDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            return;
        }
        mProgressDialog = new MaterialDialog.Builder(this)
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
}
