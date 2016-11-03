package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.upload.event.UploadAvatarEvent;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.avatar.AvatarActivity;
import com.waylens.hachi.utils.CircleTransform;
import com.waylens.hachi.utils.ServerErrorHelper;
import com.waylens.hachi.utils.rxjava.RxBus;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/8/15.
 */
public class ProfileSettingActivity extends BaseActivity {
    private Subscription mAvatarUploadSubscription;
    private SessionManager mSessionManager = SessionManager.getInstance();
    private HachiApi mHachi = HachiService.createHachiApiService();

    @BindView(R.id.avatar)
    ImageView mAvatar;

    @BindView(R.id.btnAddPhoto)
    ImageButton mBtnAddPhoto;

    @BindView(R.id.avatar_upload_progress)
    ProgressBar mAvatarUploadProgress;

    @BindView(R.id.blur_avatar)
    ImageView mBlurAvatar;

    @OnClick(R.id.avatar)
    public void onBtnAvatarClicked() {
        AvatarActivity.launch(this, false);
    }

    @OnClick(R.id.btnAddPhoto)
    public void onBtnAddPhotoClick() {
        AvatarActivity.launch(this, true);
    }

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, ProfileSettingActivity.class);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setting);
        setupToolbar();
        refreshUserAvatar();

        Fragment fragment = new ProfileSettingPreferenceFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAvatarUploadSubscription = RxBus.getDefault().toObserverable(UploadAvatarEvent.class)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<UploadAvatarEvent>() {
                @Override
                public void onNext(UploadAvatarEvent uploadAvatarEvent) {
                    switch (uploadAvatarEvent.getWhat()) {
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
            });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!mAvatarUploadSubscription.isUnsubscribed()) {
            mAvatarUploadSubscription.unsubscribe();
        }

    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.profile_setting);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void fetchUserProfile() {
        HachiService.createHachiApiService().getMyUserInfoRx()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<UserInfo>() {
                @Override
                public void onNext(UserInfo userInfo) {
                    mSessionManager.saveUserProfile(userInfo);
                    refreshUserAvatar();
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    ServerErrorHelper.showErrorMessage(mAvatar, e);
                }
            });
    }

    private void refreshUserAvatar() {

        Glide.with(this)
            .load(mSessionManager.getAvatarUrl())
            .transform(new CircleTransform(this))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_account_circle_placeholder)
            .crossFade()
            .into(mAvatar);


    }


}
