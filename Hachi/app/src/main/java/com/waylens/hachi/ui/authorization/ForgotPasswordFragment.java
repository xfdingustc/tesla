package com.waylens.hachi.ui.authorization;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiService;
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
public class ForgotPasswordFragment extends BaseFragment {
    private static final String TAG = ForgotPasswordFragment.class.getSimpleName();


    @BindView(R.id.sign_up_email)
    CompoundEditView mTvSignUpEmail;

    @BindView(R.id.button_animator)
    ViewAnimator mButtonAnimator;

    private String mEmail;

    @Override
    protected String getRequestTag() {
        return TAG;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_forgot_password, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTvSignUpEmail.setText(PreferenceUtils.getString(PreferenceUtils.KEY_SIGN_UP_EMAIL, ""));
    }


    @OnClick(R.id.btn_send)
    public void onClickSend() {
        if (!mTvSignUpEmail.isValid()) {
            return;
        }
        mButtonAnimator.setDisplayedChild(1);
        sendEmail();
    }

    private void sendEmail() {
        mEmail = mTvSignUpEmail.getText().toString();
        HachiService.createHachiApiService().sendPwdResetEmailRx(mEmail)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<SimpleBoolResponse>() {
                @Override
                public void onNext(SimpleBoolResponse simpleBoolResponse) {
                    onSendSuccessful(simpleBoolResponse);
                }

                @Override
                public void onError(Throwable e) {
                    ServerErrorHelper.showErrorMessage(mRootView, e);
                    PreferenceUtils.remove(PreferenceUtils.KEY_RESET_EMAIL_SENT);
                    mButtonAnimator.setDisplayedChild(0);
                }
            });
    }

    private void onSendSuccessful(SimpleBoolResponse response) {
        if (response.result) {
            PreferenceUtils.putBoolean(PreferenceUtils.KEY_RESET_EMAIL_SENT, true);
            PreferenceUtils.putString(PreferenceUtils.KEY_SIGN_UP_EMAIL, mEmail);
            getFragmentManager().beginTransaction().replace(R.id.fragment_content, new ChangePasswordFragment()).commit();
        } else {
            PreferenceUtils.remove(PreferenceUtils.KEY_RESET_EMAIL_SENT);
            showMessage(R.string.server_msg_reset_email_not_sent);
            mButtonAnimator.setDisplayedChild(0);
        }
    }
}
