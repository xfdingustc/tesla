package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;

import com.android.volley.Response;
import com.bumptech.glide.Glide;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.avatar.AvatarActivity;
import com.waylens.hachi.utils.ImageUtils;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/4/26.
 */
public class AccountActivity extends BaseActivity {
    private static final String TAG = AccountActivity.class.getSimpleName();

    private SessionManager mSessionManager = SessionManager.getInstance();

    @BindView(R.id.avatar)
    CircleImageView mAvatar;

    @BindView(R.id.btnAddPhoto)
    ImageButton mBtnAddPhoto;


    @OnClick(R.id.avatar)
    public void onBtnAvatarClicked() {
        AvatarActivity.launch(this, false);
    }

    @OnClick(R.id.btnAddPhoto)
    public void onBtnAddPhotoClick() {
        AvatarActivity.launch(this, true);
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
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_USER_ME)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).json(response.toString());
                    mSessionManager.saveUserProfile(response);
                    showUserProfile(response);
                    AccountSettingPreferenceFragment fragment = new AccountSettingPreferenceFragment();
                    getFragmentManager().beginTransaction().replace(R.id.accountPref, fragment).commit();
                }
            })
            .build();

        mRequestQueue.add(request);
    }

    private void showUserProfile(JSONObject response) {

        Logger.t(TAG).d("avatart: " + mSessionManager.getAvatarUrl());


        Glide.with(this)
            .load(mSessionManager.getAvatarUrl())
            .placeholder(R.drawable.default_avatar)
            .crossFade()
            .into(mAvatar);
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_account);
        setupToolbar();

        Glide.with(this).load(mSessionManager.getAvatarUrl()).crossFade().into(mAvatar);
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
}
