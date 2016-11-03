package com.waylens.hachi.ui.authorization;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.body.ResetPwdBody;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.views.CompoundEditView;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.ServerErrorHelper;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import butterknife.BindView;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Richard on 3/23/16.
 */
public class ChangePasswordFragment extends BaseFragment {
    private static final String TAG = ChangePasswordFragment.class.getSimpleName();

    private static final String TAG_REQUEST_RESET_PASSWORD = "request.reset.password";

    @BindView(R.id.forgot_password_code)
    CompoundEditView mEvCode;

    @BindView(R.id.new_password)
    CompoundEditView mEvPassword;

    @BindView(R.id.button_animator)
    ViewAnimator mButtonAnimator;

    @BindView(R.id.tv_resend)
    TextView mTvResend;

    @BindView(R.id.tv_change_password_hint)
    TextView mTvChangePasswordHint;

    private String mPassword;
    private String mCode;
    private String mEmail;


    @Override
    protected String getRequestTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEmail = PreferenceUtils.getString(PreferenceUtils.KEY_SIGN_UP_EMAIL, "");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_change_password, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTvChangePasswordHint.setText(getString(R.string.forgot_password_hint4) + " " + mEmail);
        SpannableStringBuilder ssb = new SpannableStringBuilder(getString(R.string.forgot_password_hint5));
        int start = ssb.length();
        ssb.append(getString(R.string.resend))
            .setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    onResendEmail();
                }
            }, start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mTvResend.setText(ssb);
        mTvResend.setMovementMethod(LinkMovementMethod.getInstance());

    }

    private void onResendEmail() {
        getFragmentManager().beginTransaction()
            .replace(R.id.fragment_content, new ForgotPasswordFragment())
            .commit();
    }


    @OnClick(R.id.btn_change_password)
    public void onClickChangePassword() {
        mCode = mEvCode.getText().toString();
        mPassword = mEvPassword.getText().toString();

        if (!mEvCode.isValid() || !mEvPassword.isValid()) {
            return;
        }
        mButtonAnimator.setDisplayedChild(1);
        changePassword();
    }

    private void changePassword() {
        ResetPwdBody resetPwdBody = new ResetPwdBody();
        resetPwdBody.email = mEmail;
        resetPwdBody.token = mCode;
        resetPwdBody.newPassword = mPassword;
        HachiService.createHachiApiService().resetPasswordRx(resetPwdBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<SimpleBoolResponse>() {
                @Override
                public void onNext(SimpleBoolResponse simpleBoolResponse) {
                    onResetPasswordSuccessful(simpleBoolResponse);
                }

                @Override
                public void onError(Throwable e) {
                    mButtonAnimator.setDisplayedChild(0);
                    ServerErrorHelper.showErrorMessage(mRootView, e);
                }
            });
    }


    private void onResetPasswordSuccessful(SimpleBoolResponse response) {
        PreferenceUtils.remove(PreferenceUtils.KEY_RESET_EMAIL_SENT);
        if (response.result) {
            getFragmentManager().beginTransaction()
                .replace(R.id.fragment_content, new SignInFragment())
                .commit();
        } else {
            mButtonAnimator.setDisplayedChild(0);
        }
    }

}
