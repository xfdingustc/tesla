package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.upload.event.UploadAvatarEvent;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.avatar.AvatarActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;

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

    private SessionManager mSessionManager = SessionManager.getInstance();

    private HachiApi mHachi = HachiService.createHachiApiService();

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
                    refreshUserAvatar();
                    AccountSettingPreferenceFragment fragment = new AccountSettingPreferenceFragment();
                    if (!isDestroyed()) {
                        getFragmentManager().beginTransaction().replace(R.id.accountPref, fragment).commitAllowingStateLoss();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                }

                @Override
                public void onNext(Void integer) {

                }
            });

    }

    private void refreshUserAvatar() {
        Glide.with(this.getApplicationContext())
            .load(mSessionManager.getAvatarUrl())
            .dontAnimate()
            .placeholder(R.drawable.menu_profile_photo_default)
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
