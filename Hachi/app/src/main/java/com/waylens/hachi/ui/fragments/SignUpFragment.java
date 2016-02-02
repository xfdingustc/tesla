package com.waylens.hachi.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ViewAnimator;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.orhanobut.logger.Logger;
import com.soundcloud.android.crop.Crop;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.app.JsonKey;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.utils.ContentUploader;
import com.waylens.hachi.utils.ImageUtils;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Richard on 8/20/15.
 */
public class SignUpFragment extends BaseFragment {

    private static final String TAG = "SignUpFragment";

    public static final String ARG_KEY_EMAIL = "arg.sign.up.email";

    private static final int TAKE_PHOTO = 0;
    private static final int CHOOSE_FROM_GALLERY = 1;

    private static final int REQUEST_IMAGE_CAPTURE = 100;

    private static final int REQUEST_IMAGE_PICK = 101;

    private static final int REQUEST_IMAGE_CROP = 102;

    private static final int PASSWORD_MIN_LENGTH = 6;

    private static final String PATTERN_USER_NAME = "[A-Za-z0-9_\\.]{1,32}";

    String mPreSetEmail;

    @Bind(R.id.sign_up_email)
    EditText mEtEmail;

    @Bind(R.id.sign_up_animator)
    ViewAnimator mSignUpAnimator;

    @Bind(R.id.sign_up_user_name)
    EditText mEtUserName;

    @Bind(R.id.sign_up_password)
    EditText mEtPassword;

    @Bind(R.id.sign_up_avatar)
    CircleImageView mAvatarView;

    @Bind(R.id.sign_up_avatar_plus)
    View mViewSignUpPlus;

    @Bind(R.id.sign_up_avatar_message)
    View mViewSignUpMessage;

    @Bind(R.id.email_progress)
    ProgressBar mEmailProgressBar;

    @Bind(R.id.user_name_progress)
    ProgressBar mUserNameProgressBar;

    @Bind(R.id.text_input_email)
    TextInputLayout mTextInputEmail;

    @Bind(R.id.text_input_user_name)
    TextInputLayout mTextInputUserName;

    @Bind(R.id.text_input_password)
    TextInputLayout mTextInputPassword;

    private RequestQueue mRequestQueue;

    private File mImageFile;

    private Uri mAvatarFileUri;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mPreSetEmail = args.getString(ARG_KEY_EMAIL, "");
        }
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_signup, savedInstanceState);
        initViews();
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == Activity.RESULT_OK) {
                    beginCrop(data.getData());
                }
                break;
            case REQUEST_IMAGE_PICK:

                break;
            case Crop.REQUEST_CROP:
                handleCrop(data);
                break;
        }
    }

    private void initViews() {
        if (!TextUtils.isEmpty(mPreSetEmail)) {
            mEtEmail.setText(mPreSetEmail);
            mEtEmail.setTextColor(getResources().getColor(R.color.material_green_600));
        }
        mTextInputEmail.setErrorEnabled(true);
        mTextInputUserName.setErrorEnabled(true);
    }

    @OnFocusChange(R.id.sign_up_email)
    public void validateSignUpEmail(boolean hasFocus) {
        if (mEtEmail == null || hasFocus) {
            return;
        }


        String email = mEtEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mTextInputEmail.setError(getString(R.string.the_field_is_required));
            return;
        }

        boolean isValid = Patterns.EMAIL_ADDRESS.matcher(email).matches();
        if (!isValid) {
            mTextInputEmail.setError(getString(R.string.email_invalid));
            return;
        }
        mTextInputEmail.setError(null);
        if (!email.equals(mPreSetEmail)) {
            validateEmailOnCloud(email);
        }
    }

    private void validateEmailOnCloud(String email) {
        mEmailProgressBar.setVisibility(View.VISIBLE);
        String url = Constants.API_CHECK_EMAIL + email;
        mRequestQueue.add(new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response.optBoolean("result")) {
                            mTextInputEmail.setError(null);
                            mEtEmail.setTextColor(getResources().getColor(R.color.material_green_600));
                        } else {
                            mTextInputEmail.setError(getString(R.string.server_msg_email_has_been_used));
                            mEtEmail.setTextColor(Color.BLACK);
                        }
                        mEmailProgressBar.setVisibility(View.GONE);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                        showMessage(errorInfo.msgResID);
                        mEmailProgressBar.setVisibility(View.GONE);
                    }
                }));
    }

    @OnFocusChange(R.id.sign_up_user_name)
    public void validateSignUpUserName(boolean hasFocus) {
        if (mEtUserName == null || hasFocus) {
            return;
        }

        if (TextUtils.isEmpty(mEtUserName.getText())) {
            mTextInputUserName.setError(getString(R.string.the_field_is_required));
            return;
        }

        String userName = mEtUserName.getText().toString();
        Pattern pattern = Pattern.compile(PATTERN_USER_NAME);
        boolean isValid = pattern.matcher(userName).matches();
        if (!isValid) {
            mTextInputUserName.setError(getString(R.string.user_name_invalid));
        }

        mTextInputUserName.setError(null);
        validateUserNameOnCloud(userName);
    }

    private void validateUserNameOnCloud(String userName) {
        mUserNameProgressBar.setVisibility(View.VISIBLE);

        String url = Constants.API_CHECK_USER_NAME + userName;
        mRequestQueue.add(new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response.optBoolean("result")) {
                            mTextInputUserName.setError(null);
                            mEtUserName.setTextColor(getResources().getColor(R.color.material_green_600));
                        } else {
                            mTextInputUserName.setError(getString(R.string.server_msg_user_name_has_been_used));
                            mEtUserName.setTextColor(Color.BLACK);
                        }
                        mUserNameProgressBar.setVisibility(View.GONE);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                        showMessage(errorInfo.msgResID);
                        mUserNameProgressBar.setVisibility(View.GONE);
                    }
                }));
    }

    @OnFocusChange(R.id.sign_up_password)
    public void validatePassword(boolean hasFocus) {
        if (mEtPassword == null || hasFocus) {
            return;
        }

        if (TextUtils.isEmpty(mEtPassword.getText()) || mEtPassword.getText().length() < PASSWORD_MIN_LENGTH) {
            mTextInputPassword.setError(getString(R.string.password_min_error, PASSWORD_MIN_LENGTH));
            return;
        }
        mTextInputPassword.setError(null);
    }

    @OnClick(R.id.sign_up_next)
    public void signUp() {
        mEtEmail.clearFocus();
        mEtUserName.clearFocus();
        mEtPassword.clearFocus();

        if (mTextInputEmail.getError() != null
                || mTextInputUserName.getError() != null
                || mTextInputPassword.getError() != null) {
            showMessage(R.string.please_correct_errors);
            return;
        }

        mSignUpAnimator.setDisplayedChild(1);

        JSONObject params = new JSONObject();
        try {
            params.put(JsonKey.USERNAME, mEtUserName.getText().toString());
            params.put(JsonKey.EMAIL, mEtEmail.getText().toString());
            params.put(JsonKey.PASSWORD, mEtPassword.getText().toString());
        } catch (JSONException e) {
            Logger.t(TAG).e(e, "");
        }
        mRequestQueue.start();
        mRequestQueue.add(new JsonObjectRequest(Request.Method.POST, Constants.API_SIGN_UP, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onSignUpSuccessful(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onSignUpFailed(error);
                    }
                }));
    }

    @OnClick(R.id.sign_up_avatar)
    void chooseAvatar() {
        new MaterialDialog.Builder(getActivity())
                .title("SET AVATAR")
                .items(R.array.avatarSource)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int which, CharSequence charSequence) {
                        if (which == TAKE_PHOTO) {
                            takePhoto();
                        } else {
                            chooseFromGallery();
                        }
                    }
                }).show();
    }

    @OnTextChanged(R.id.sign_up_email)
    public void inputEmail(CharSequence text) {
        mTextInputEmail.setError(null);
        if (text.toString().equals(mPreSetEmail)) {
            mEtEmail.setTextColor(getResources().getColor(R.color.material_green_600));
        } else {
            mEtEmail.setTextColor(Color.BLACK);
        }
    }

    @OnTextChanged(R.id.sign_up_user_name)
    public void inputUserName(CharSequence text) {
        mTextInputUserName.setError(null);
    }

    @OnTextChanged(R.id.sign_up_password)
    public void inputPassword(CharSequence text) {
        mTextInputPassword.setError(null);
    }

    void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getActivity().getPackageManager()) == null) {
            return;
        }
        if (!ImageUtils.isExternalStorageReady()) {
            showMessage(R.string.no_sdcard);
            return;
        }
        mImageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), System.currentTimeMillis() + ".png");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mImageFile));
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    private void beginCrop(Uri source) {
        File file = new File(ImageUtils.getStorageDir(getActivity(), "cropped"), "avatar.png");
        Uri destination = Uri.fromFile(file);
        Crop.of(Uri.fromFile(mImageFile), destination).asSquare().start(getActivity(), this);
    }

    private void handleCrop(Intent result) {
        mAvatarFileUri = Crop.getOutput(result);
        mAvatarView.setImageURI(mAvatarFileUri);
        mViewSignUpPlus.setVisibility(View.GONE);
        mViewSignUpMessage.setVisibility(View.GONE);
    }

    void chooseFromGallery() {
        showMessage("Choose from Gallery");
    }

    void uploadAvatar() {
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, Constants.API_START_UPLOAD_AVATAR,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        uploadAvatarImage(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }));
    }

    void uploadAvatarImage(final JSONObject token) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentUploader uploader = new ContentUploader(token, new File(mAvatarFileUri.getPath()));
                uploader.setUploaderListener(new AvatarUploadListener());
                uploader.upload();
            }
        }).start();
    }

    void onSignUpSuccessful(JSONObject response) {
        SessionManager.getInstance().saveLoginInfo(response);
        if (mAvatarFileUri == null) {
            onFinishSignUp();
        } else {
            uploadAvatar();
        }
    }

    void onSignUpFailed(VolleyError error) {
        ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
        showMessage(errorInfo.msgResID);
        mSignUpAnimator.setDisplayedChild(0);
    }

    void onFinishSignUp() {
        getActivity().finish();
    }

    class AvatarUploadListener implements ContentUploader.UploadListener {

        @Override
        public void onUploadStarted() {
            Log.e("test", "onUploadStarted");
        }

        @Override
        public void onUploadProgress(float progress) {
            Log.e("test", "onUploadProgress: " + progress);
        }

        @Override
        public void onUploadFinished() {
            getActivity().finish();
        }

        @Override
        public void onUploadError(String error) {
            Log.e("test", "onUploadError: " + error);
        }
    }
}
