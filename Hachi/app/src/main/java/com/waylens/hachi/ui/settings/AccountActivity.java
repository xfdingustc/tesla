package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Response;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.bgjob.upload.event.UploadAvatarEvent;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.avatar.AvatarActivity;
import com.waylens.hachi.utils.ImageUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
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

    private MaterialDialog mUploadDialog;

    @BindView(R.id.avatar)
    CircleImageView mAvatar;

    @BindView(R.id.btnAddPhoto)
    ImageButton mBtnAddPhoto;

    @BindView(R.id.avatar_upload_progress)
    ProgressBar mAvatarUploadProgress;


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
                    if (!isDestroyed()) {
                        getFragmentManager().beginTransaction().replace(R.id.accountPref, fragment).commitAllowingStateLoss();
                    }
                }
            })
            .build();

        mRequestQueue.add(request);
    }

    private void showUserProfile(JSONObject response) {

        Logger.t(TAG).d("avatart: " + mSessionManager.getAvatarUrl());


        Glide.with(this.getApplicationContext())
            .load(mSessionManager.getAvatarUrl())
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
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

        Glide.with(this)
            .load(mSessionManager.getAvatarUrl())
            .asBitmap()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    mAvatar.setImageBitmap(resource);
                }
            });
        fetchUserProfile();
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
