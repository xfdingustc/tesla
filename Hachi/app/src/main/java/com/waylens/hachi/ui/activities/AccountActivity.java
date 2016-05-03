package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.avatar.AvatarActivity;
import com.waylens.hachi.utils.ImageUtils;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/4/26.
 */
public class AccountActivity extends BaseActivity {
    private static final String TAG = AccountActivity.class.getSimpleName();
    private ImageLoader mImageLoader = ImageLoader.getInstance();
    private SessionManager mSessionManager = SessionManager.getInstance();

    @BindView(R.id.avatar)
    CircleImageView mAvatar;

    @BindView(R.id.btnAddPhoto)
    ImageButton mBtnAddPhoto;

    @BindView(R.id.email)
    TextView mTvEmail;

    @BindView(R.id.tvUserName)
    TextView mTvUserName;

    @BindView(R.id.tvBirthday)
    TextView mBirthday;

    @OnClick(R.id.avatar)
    public void onBtnAvatarClicked() {
        AvatarActivity.start(this, false);
    }

    @OnClick(R.id.btnAddPhoto)
    public void onBtnAddPhotoClick() {
        AvatarActivity.start(this, true);
    }

    @OnClick(R.id.tvUserName)
    public void onTvUserNameClick() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
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

    }

    @OnClick(R.id.tvBirthday)
    public void onTvBirthdayClicked() {
//        SublimePickerFragment pickerFrag = new SublimePickerFragment();
//        pickerFrag.setCallback(new SublimePickerFragment.Callback(){
//
//            @Override
//            public void onCancelled() {
//
//            }
//
//            @Override
//            public void onDateTimeRecurrenceSet(SelectedDate selectedDate, int hourOfDay, int minute, SublimeRecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule) {
//
//            }
//        });
        MaterialDialog dialog = new MaterialDialog.Builder(this)
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

    }



    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, AccountActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchUserProfile();
    }

    private void fetchUserProfile() {
        String url = Constants.API_USER_ME;
        AuthorizedJsonRequest request = new AuthorizedJsonRequest(url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Logger.t(TAG).json(response.toString());
                mSessionManager.saveUserProfile(response);
                showUserProfile(response);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        mRequestQueue.add(request);
    }

    private void showUserProfile(JSONObject response) {
        mTvUserName.setText(mSessionManager.getUserName());
        mTvEmail.setText(mSessionManager.getEmail());
        mImageLoader.displayImage(mSessionManager.getAvatarUrl(), mAvatar);
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
                mTvUserName.setText(newUserName);
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
//                mTvUserName.setText(newUserName);
//                mSessionManager.saveUserName(newUserName);
                mBirthday.setText(birthday);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        mRequestQueue.add(request);
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_account);
        setupToolbar();
        mImageLoader.displayImage(mSessionManager.getAvatarUrl(), mAvatar, ImageUtils.getAvatarOptions());
        mTvEmail.setText(mSessionManager.getEmail());
        mTvUserName.setText(mSessionManager.getUserName());
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();

        mToolbar.setNavigationIcon(R.drawable.navbar_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolbar.setTitle(R.string.account);
    }
}
