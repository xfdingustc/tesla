package com.waylens.hachi.ui.authorization;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.IHachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.SocialProvider;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;

import java.io.IOException;
import java.util.Arrays;

import butterknife.BindView;
import retrofit2.Call;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/7/25.
 */
public class FacebookAuthorizeActivity extends BaseActivity {
    private static final String TAG = FacebookAuthorizeActivity.class.getSimpleName();

    private CallbackManager mCallbackManager = CallbackManager.Factory.create();

    private IHachiApi mHachi = HachiService.createHachiApiService();



    public static void launch(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, FacebookAuthorizeActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void launch(Fragment fragment, int requestCode) {
        Intent intent = new Intent(fragment.getActivity(), FacebookAuthorizeActivity.class);
        fragment.startActivityForResult(intent, requestCode);
    }

    @BindView(R.id.connectIndicator)
    ImageView mIvConnectIdicator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_facebook_auth);
        mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
        AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
        animationDrawable.start();
        requestFacebookPublishPermission();
    }


    private void requestFacebookPublishPermission() {
        Logger.t(TAG).d("request publish permission");
        LoginManager loginManager = LoginManager.getInstance();


        loginManager.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {
                Logger.t(TAG).d("on sucess!!!");
                sendFbToken2Server(loginResult.getAccessToken().getToken());
            }

            @Override
            public void onCancel() {
                setResult(RESULT_CANCELED);
                finish();
                Logger.t(TAG).d("on cancel");
            }

            @Override
            public void onError(FacebookException error) {
                Logger.t(TAG).d("on error");
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        loginManager.logInWithPublishPermissions(this, Arrays.asList("publish_actions"));
    }

    private void sendFbToken2Server(final String facebookToken) {
        Logger.t(TAG).d("Send Facebook token: " + facebookToken);
        Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                subscriber.onStart();
                Call<SimpleBoolResponse> bindSocialMediaCall = mHachi.bindSocialProvider(SocialProvider.newFaceBookProvider(facebookToken));
                Call<LinkedAccounts> linkedAccountCall = mHachi.getLinkedAccounts();

                try {
                    bindSocialMediaCall.execute();
                    SessionManager.getInstance().saveLinkedAccounts(linkedAccountCall.execute().body());
                } catch (IOException e) {
                    subscriber.onError(e);
                }

                subscriber.onCompleted();
            }
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Void>() {
                @Override
                public void onStart() {
                    super.onStart();
                    showDialog(getString(R.string.send_facebook_token));
                }

                @Override
                public void onCompleted() {
                    hideDialog();
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    Logger.t(TAG).d("on Failure");
                    hideDialog();
                    setResult(RESULT_CANCELED);
                    finish();
                }

                @Override
                public void onNext(Void aVoid) {

                }
            });



    }
}
