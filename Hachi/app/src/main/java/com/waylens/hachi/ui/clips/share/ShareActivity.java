package com.waylens.hachi.ui.clips.share;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.orhanobut.logger.Logger;
import com.rest.HachiApi;
import com.rest.HachiService;
import com.rest.response.LinkedAccounts;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;
import com.waylens.hachi.bgjob.upload.event.UploadEvent;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.IconSpinnerAdapter;
import com.waylens.hachi.ui.clips.ClipPlayActivity;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor2;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.utils.ViewUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class ShareActivity extends ClipPlayActivity {
    private static final String TAG = ShareActivity.class.getSimpleName();
    private static final String EXTRA_PLAYLIST_ID = "playlist_id";
    private static final String EXTRA_AUDIO_ID = "audio_id";

    private int mPlayListId;
    private int mAudioId;

    private MaterialDialog mUploadDialog;

    private String mSocialPrivacy;

    private String[] mSupportedPrivacy;

    private boolean mIsFacebookShareChecked = false;

    private LinkedAccounts mLinkedAccounts;

    private HachiApi mHachi = HachiService.createHachiApiService();

    private CallbackManager callbackManager = CallbackManager.Factory.create();

    @BindView(R.id.user_avatar)
    CircleImageView mUserAvatar;

    @BindView(R.id.user_name)
    TextView mUserName;

    @BindView(R.id.user_email)
    TextView mUserEmail;

    @BindView(R.id.root_scroll_view)
    ScrollView mRootScrollView;

    @BindView(R.id.moment_title)
    TextInputEditText mEtMomentTitle;

    @BindView(R.id.moment_description)
    TextInputEditText mEtMomentDescription;

    @BindArray(R.array.social_privacy_text)
    CharSequence[] mPrivacyText;

    @BindView(R.id.spinner_social_privacy)
    Spinner mPrivacySpinner;

    @BindView(R.id.btn_facebook)
    ImageView mBtnFaceBook;

    @OnClick(R.id.btn_facebook)
    public void onBtnFackBookChecked() {
        mIsFacebookShareChecked = !mIsFacebookShareChecked;
        if (mIsFacebookShareChecked) {
            mBtnFaceBook.setBackgroundResource(R.drawable.btn_platform_facebook_s);
        } else {
            mBtnFaceBook.setBackgroundResource(R.drawable.btn_platform_facebook_n);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventUpload(UploadEvent event) {
        switch (event.getWhat()) {
            case UploadEvent.UPLOAD_WHAT_START:

                break;
            case UploadEvent.UPLOAD_WHAT_PROGRESS:
                if (mUploadDialog != null) {
                    int progress = event.getExtra();
                    mUploadDialog.getProgressBar().setProgress(progress);
                }
                break;
            case UploadEvent.UPLOAD_WHAT_FINISHED:
                if (mUploadDialog != null) {
                    mUploadDialog.dismiss();
                }
                MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .content("Uploading finished")
                    .show();
                mBtnFaceBook.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 2000);
                break;
        }
    }

    public static void launch(Activity activity, int playListId, int audioId) {
        Intent intent = new Intent(activity, ShareActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playListId);
        intent.putExtra(EXTRA_AUDIO_ID, audioId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void init() {
        super.init();
        mPlayListId = getIntent().getIntExtra(EXTRA_PLAYLIST_ID, -1);
        mAudioId = getIntent().getIntExtra(EXTRA_AUDIO_ID, -1);
        initViews();
    }





    private void initViews() {
        setContentView(R.layout.activity_share);
        setupToolbar();
        mPlaylistEditor = new PlayListEditor2(mVdbRequestQueue, mPlayListId);
        mPlaylistEditor.reconstruct();
        embedVideoPlayFragment();

        setupSocialPolicy();
        mPlayerContainer.post(new Runnable() {
            @Override
            public void run() {
                setupParallex();
            }
        });

        SessionManager sessionManager = SessionManager.getInstance();
        Glide.with(this)
            .load(sessionManager.getAvatarUrl())
            .placeholder(R.drawable.default_avatar)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(mUserAvatar);
        mUserName.setText(sessionManager.getUserName());
        mUserEmail.setText(sessionManager.getEmail());


        Logger.t(TAG).d("is linked with facebook: " + sessionManager.isLinked());
        if (sessionManager.isLinked()) {
            mBtnFaceBook.setVisibility(View.VISIBLE);
        } else {
            mBtnFaceBook.setVisibility(View.GONE);
        }

    }

    private void setupSocialPolicy() {
        TypedArray typedArray = getResources().obtainTypedArray(R.array.social_privacy_icon);
        Drawable[] drawables = new Drawable[typedArray.length()];
        for (int i = 0; i < drawables.length; i++) {
            drawables[i] = typedArray.getDrawable(i);
        }
        IconSpinnerAdapter mAdapter = new IconSpinnerAdapter(this, android.R.layout.simple_spinner_item, mPrivacyText, drawables,
            ViewUtils.dp2px(16));
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPrivacySpinner.setAdapter(mAdapter);

        mSupportedPrivacy = getResources().getStringArray(R.array.social_privacy_value);
        mSocialPrivacy = mSupportedPrivacy[0];
        mPrivacySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSocialPrivacy = mSupportedPrivacy[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSocialPrivacy = mSupportedPrivacy[0];
            }
        });
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.share);

        getToolbar().setTitleTextColor(getResources().getColor(R.color.app_text_color_primary));
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getToolbar().inflateMenu(R.menu.menu_share2);
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.share:
                        shareMoment();

                        break;
                }
                return true;
            }
        });


    }

    private void shareMoment() {
        if (mIsFacebookShareChecked) {
            checkFackbookPermission();
        } else {
            doShareMoment();
        }
    }

    private void checkFackbookPermission() {
        Logger.t(TAG).d("send check permission");


        Call<LinkedAccounts> callLinkedAccount = mHachi.getLinkedAccounts();
        callLinkedAccount.enqueue(new Callback<LinkedAccounts>() {
            @Override
            public void onResponse(Call<LinkedAccounts> call, retrofit2.Response<LinkedAccounts> response) {
                Logger.t(TAG).d("Get response");
                mLinkedAccounts = response.body();
                if (checkIfNeedGetFacebookPermission()) {
                    requestPublishPermission();
                } else {
                    doShareMoment();
                }

            }

            @Override
            public void onFailure(Call<LinkedAccounts> call, Throwable t) {
                Logger.t(TAG).d(t.toString());
            }
        });

    }

    private boolean checkIfNeedGetFacebookPermission() {

        for (LinkedAccounts.LinkedAccount account : mLinkedAccounts.linkedAccounts) {
            if (account.provider.equals("facebook") && TextUtils.isEmpty(account.accountName)) {
                return true;
            }
        }

        return false;
    }

    private void requestPublishPermission() {
        Logger.t(TAG).d("request publish permission");
        LoginManager loginManager = LoginManager.getInstance();


        loginManager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {
                Logger.t(TAG).d("on sucess!!!");
                sendShareTokenToServer(loginResult.getAccessToken().getToken());

            }

            @Override
            public void onCancel() {
                Logger.t(TAG).d("on cancel");
            }

            @Override
            public void onError(FacebookException error) {
                Logger.t(TAG).d("on error");
            }
        });
        loginManager.logInWithPublishPermissions(this, Arrays.asList("publish_actions"));



    }

    private void sendShareTokenToServer(String token) {
        String url = Constants.API_SHARE_ACCOUNTS;
        Map<String, String> param = new HashMap<>();
        param.put("provider", "facebook");
        param.put("accessToken", token);

        final JSONObject requestBody = new JSONObject(param);

        AuthorizedJsonRequest request = new AuthorizedJsonRequest(Request.Method.POST, url, requestBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Logger.t(TAG).json(response.toString());
                doShareMoment();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        mRequestQueue.add(request);
    }


    private void setupParallex() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            final int width = mPlayerContainer.getMeasuredWidth();
            final int height = mPlayerContainer.getMeasuredHeight();

            final float originY = mPlayerContainer.getY();


            mRootScrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, final int scrollY, int oldScrollX, int oldScrollY) {
//                Log.i("test", "scroolY = " + scrollY);

                    mPlayerContainer.setY(originY + scrollY / 2);

//                ViewGroup.LayoutParams newParam = new ViewGroup.LayoutParams(width, height - scrollY);
//                frame.setLayoutParams(newParam);

                    mPlayerContainer.setClipToOutline(true);
                    mPlayerContainer.setOutlineProvider(new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, Outline outline) {
                            Rect rect = new Rect(0, scrollY / 2, width, height - scrollY / 2);
                            outline.setRect(rect);
                        }
                    });

                }
            });
        }

    }

    private void doShareMoment() {
        String title = mEtMomentTitle.getEditableText().toString();
        if (TextUtils.isEmpty(title)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            title = "Created " + format.format(System.currentTimeMillis());
        }

        String descrption = mEtMomentDescription.getEditableText().toString();
        String[] tags = new String[]{"Shanghai", "car"};

        JSONObject gaugeSettings = null;


        Logger.t(TAG).d("share title: " + title);

        LocalMoment localMoment = new LocalMoment(mPlaylistEditor.getPlaylistId(), title, descrption, tags, mSocialPrivacy, mAudioId, gaugeSettings, mIsFacebookShareChecked);
        JobManager jobManager = BgJobManager.getManager();
        UploadMomentJob job = new UploadMomentJob(localMoment);
        jobManager.addJobInBackground(job);

        mUploadDialog = new MaterialDialog.Builder(this)
            .title(R.string.upload)
            .contentGravity(GravityEnum.CENTER)
            .progress(false, 100, true)
            .show();
        mUploadDialog.setCanceledOnTouchOutside(false);
//
    }
}
