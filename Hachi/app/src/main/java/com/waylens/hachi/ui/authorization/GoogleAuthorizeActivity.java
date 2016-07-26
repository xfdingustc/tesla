package com.waylens.hachi.ui.authorization;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.SocialProvider;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;

import java.io.IOException;

import retrofit2.Call;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/7/26.
 */
public class GoogleAuthorizeActivity extends BaseActivity {
    private static final String TAG = GoogleAuthorizeActivity.class.getSimpleName();

    private static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    private static final int AUTH_CODE_REQUEST_CODE = 1001;
    private String mEmail;
    private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/youtube.upload";

    private HachiApi mHachi = HachiService.createHachiApiService();

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, GoogleAuthorizeActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == RESULT_OK) {
                mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                // With the account name acquired, go get the auth token
                fetchYoutubeToken();
            } else if (resultCode == RESULT_CANCELED) {
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Toast.makeText(this, R.string.pick_account, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == AUTH_CODE_REQUEST_CODE) {
            Logger.t(TAG).d("requestcode");
            if (resultCode == RESULT_OK) {
                fetchYoutubeToken();
            } else {
                Toast.makeText(this, R.string.auth_code, Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void fetchYoutubeToken() {
        Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    String token = GoogleAuthUtil.getToken(GoogleAuthorizeActivity.this, mEmail, SCOPE);
                    Logger.t(TAG).d("token: " + token);

                    Call<SimpleBoolResponse> bindSocialMediaCall = mHachi.bindSocialProvider(SocialProvider.newYoutubeProvider(token));
                    Call<LinkedAccounts> linkedAccountCall = mHachi.getLinkedAccounts();

                    bindSocialMediaCall.execute();
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
                public void onCompleted() {
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    if (e instanceof UserRecoverableAuthException) {
                        startActivityForResult(((UserRecoverableAuthException) e).getIntent(), AUTH_CODE_REQUEST_CODE);
                    } else {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }

                @Override
                public void onNext(Void aVoid) {

                }
            });

    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }


    private void initViews() {
        setContentView(R.layout.activity_google_authorize);
        pickUserAccount();
    }

    private void pickUserAccount() {
        String[] accountTypes = new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
            accountTypes, true, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    
}
