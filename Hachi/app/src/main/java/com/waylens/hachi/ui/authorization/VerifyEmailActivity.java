package com.waylens.hachi.ui.authorization;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Created by lshw on 16/7/22.
 */
public class VerifyEmailActivity extends BaseActivity{
    private static final String TAG = VerifyEmailActivity.class.getSimpleName();

    @BindView(R.id.btn_verify_email_address)
    Button mBtnVerifyEmailAddress;

    @BindView(R.id.resend_email)
    TextView mResendEmail;

    @BindView(R.id.tvEmailAddress)
    TextView mEmailAddress;

    private String mEmail;

    @OnClick(R.id.btn_verify_email_address)
    public void onBtnVerifyEmailAddressClicked(){
        StringBuffer urlText = new StringBuffer("http://mail.");
        urlText.append(mEmail.substring(mEmail.indexOf('@') + 1));
        Uri uri = Uri.parse(urlText.toString());
        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @OnClick(R.id.resend_email)
    public void onResendEmailClicked() {
        Logger.d("butterknife works here", this);
        HachiApi hachiApi = HachiService.createHachiApiService();
        Call<SimpleBoolResponse> resendVerifyEmailCall = hachiApi.resendVerifyEmail();
        resendVerifyEmailCall.enqueue(new Callback<SimpleBoolResponse>() {
            @Override
            public void onResponse(Call<SimpleBoolResponse> call, retrofit2.Response<SimpleBoolResponse> response) {
                if (response.code() == 200) {
                    Snackbar.make(mResendEmail, getText(R.string.resend_email_succeed), Snackbar.LENGTH_SHORT).show();
                } else if (response.code() == 400) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.errorBody().string());
                        int code = jsonObject.getInt("code");
                        String message = null;
                        switch (code) {
                            case 32:
                                message = getText(R.string.email_not_exist).toString();
                                break;
                            case 39:
                                message = getText(R.string.exceed_retry_count).toString();
                                break;
                            case 40:
                                message = getText(R.string.in_min_interval).toString();
                                break;
                            default:
                                return;
                        }
                        Snackbar.make(mResendEmail, message, Snackbar.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        Logger.t(TAG).d(e.getMessage());
                    } catch (IOException e) {
                        Logger.t(TAG).d(e.getMessage());
                    }
                }
            }
            @Override
            public void onFailure(Call<SimpleBoolResponse> call, Throwable t) {
            }
        });
    }

    public static void launch(Context activity) {
        Intent intent = new Intent(activity, VerifyEmailActivity.class);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);
        mEmail = SessionManager.getInstance().getEmail();
        mEmailAddress.setText(SessionManager.getInstance().getEmail());
    }
}
