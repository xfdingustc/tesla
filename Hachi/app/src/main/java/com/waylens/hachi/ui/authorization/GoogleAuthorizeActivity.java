package com.waylens.hachi.ui.authorization;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.IHachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.SocialProvider;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;

import butterknife.BindString;
import butterknife.BindView;
import retrofit2.Call;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/7/26.
 */
public class GoogleAuthorizeActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = GoogleAuthorizeActivity.class.getSimpleName();


    private static final int RC_SIGN_IN = 1002;

    private static final String SCOPE = "https://www.googleapis.com/auth/youtube.upload";

    private IHachiApi mHachi = HachiService.createHachiApiService();

    private GoogleApiClient mGoogleApiClient;


    public static void launch(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, GoogleAuthorizeActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }


    public static void launch(Fragment fragment, int requestCode) {
        Intent intent = new Intent(fragment.getActivity(), GoogleAuthorizeActivity.class);
        fragment.startActivityForResult(intent, requestCode);
    }

    @BindString(R.string.google_client_id)
    String mGoogleClientId;

    @BindString(R.string.server_client_id)
    String mServerClientId;

    @BindView(R.id.connectIndicator)
    ImageView mIvConnectIdicator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.t(TAG).d("on activity result, requestcode: " + requestCode);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount acct = result.getSignInAccount();

                String code = acct.getServerAuthCode();
//                Logger.t(TAG).d("ID Token: " + code);
                sendAuthCode2WaylensServer(code);
            } else {
                Logger.t(TAG).d("not success " + result.getStatus());
            }
        }

    }

    private void sendAuthCode2WaylensServer(final String code) {
        Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    subscriber.onStart();
                    Call<SimpleBoolResponse> bindSocialMediaCall = mHachi.bindSocialProvider(SocialProvider.newYoutubeProvider(code));
                    Call<LinkedAccounts> linkedAccountCall = mHachi.getLinkedAccounts();

                    bindSocialMediaCall.execute().body();

                    SessionManager.getInstance().saveLinkedAccounts(linkedAccountCall.execute().body());

                    subscriber.onCompleted();
                } catch (Exception e) {
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
                    showDialog(R.string.send_google_code);
                }

                @Override
                public void onCompleted() {
                    setResult(RESULT_OK);
                    hideDialog();
                    finish();
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    hideDialog();
                    setResult(RESULT_CANCELED);
                    finish();

                }

                @Override
                public void onNext(Void aVoid) {

                }
            });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Logger.t(TAG).d("connect failed");
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }


    private void initViews() {
        setContentView(R.layout.activity_google_authorize);

        mIvConnectIdicator.setBackgroundResource(R.drawable.camera_connecting);
        AnimationDrawable animationDrawable = (AnimationDrawable) mIvConnectIdicator.getBackground();
        animationDrawable.start();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(new Scope(SCOPE))
            .requestServerAuthCode(mServerClientId, false)
            .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build();

        signIn();

    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


}
