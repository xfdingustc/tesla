package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.bgjob.upload.event.UploadAvatarEvent;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.UserInfo;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.NotificationActivity;
import com.waylens.hachi.ui.avatar.AvatarActivity;
import com.waylens.hachi.ui.settings.myvideo.DownloadVideoActivity;
import com.waylens.hachi.ui.settings.myvideo.MyMomentActivity;
import com.waylens.hachi.ui.settings.myvideo.UploadingMomentActivity;
import com.waylens.hachi.utils.FastBlurUtil;
import com.xfdingustc.rxutils.library.RxBus;
import com.xfdingustc.rxutils.library.SimpleSubscribe;

import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/4/26.
 */
public class AccountActivity extends BaseActivity {
    private static final String TAG = AccountActivity.class.getSimpleName();

    private SessionManager mSessionManager = SessionManager.getInstance();


    private HachiApi mHachi = HachiService.createHachiApiService();

    private Subscription mAvatarUploadSubscription;


    @BindView(R.id.avatar)
    ImageView mAvatar;

    @BindView(R.id.btnAddPhoto)
    ImageButton mBtnAddPhoto;

    @BindView(R.id.avatar_upload_progress)
    ProgressBar mAvatarUploadProgress;

    @BindView(R.id.blur_avatar)
    ImageView mBlurAvatar;


    @OnClick(R.id.profile_setting)
    public void onProfileSettingClicked() {
        ProfileSettingActivity.launch(this);
    }


    @OnClick(R.id.uploading)
    public void onUploadingClicked() {
        UploadingMomentActivity.launch(this);
    }

    @OnClick(R.id.my_moments)
    public void onMyMomentsClicked() {
        MyMomentActivity.launch(this);
    }

    @OnClick(R.id.my_videos)
    public void onMyVideosClicked() {
        DownloadVideoActivity.launch(this);
    }

    @OnClick(R.id.notification)
    public void onNotificationClicked() {
        NotificationActivity.launch(this);
    }


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
//                    refreshUserProfile();
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
        mBlurAvatar.post(new Runnable() {
            @Override
            public void run() {
                Glide.with(AccountActivity.this)
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

                        float aspectRatio = (float) mBlurAvatar.getWidth() / mBlurAvatar.getHeight();
                        int cropHeight = (int) (bitmap.getWidth() / aspectRatio);
                        Logger.t(TAG).d("ratio: " + aspectRatio + " cropHeight: " + cropHeight);
                        Bitmap cropBitmap = Bitmap.createBitmap(bitmap, 0, (bitmap.getHeight() - cropHeight) / 2, bitmap.getWidth(), cropHeight);
                        Palette.generateAsync(cropBitmap, 24, new Palette.PaletteAsyncListener() {
                            @Override
                            public void onGenerated(Palette palette) {
                                Palette.Swatch vibrant = palette.getVibrantSwatch();
                                if (vibrant != null) {
                                    getToolbar().setBackgroundColor(vibrant.getRgb());
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                                    Palette.Swatch darkvibrant = palette.getDarkVibrantSwatch();
                                    if (darkvibrant != null) {
                                        Window window = getWindow();
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                                        window.setStatusBarColor(darkvibrant.getRgb());
                                    }
                                }


                            }
                        });
                        Bitmap blurBitmap = FastBlurUtil.doBlur(cropBitmap, 8, true);
                        mBlurAvatar.setImageBitmap(blurBitmap);

                    }
                };

                Glide.with(AccountActivity.this)
                    .load(mSessionManager.getAvatarUrl())
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(blurTarget);
            }
        });

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
        refreshUserAvatar();
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();

        getToolbar().setNavigationIcon(R.drawable.ic_arrow_back);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getToolbar().setTitle(R.string.account);
    }


}
