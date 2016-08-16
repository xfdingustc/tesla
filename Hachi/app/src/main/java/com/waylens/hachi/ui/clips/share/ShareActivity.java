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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.GaugeSettingManager;
import com.waylens.hachi.bgjob.BgJobManager;
import com.waylens.hachi.bgjob.upload.UploadMomentJob;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.response.CloudStorageInfo;
import com.waylens.hachi.rest.response.LinkedAccounts;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.IconSpinnerAdapter;
import com.waylens.hachi.ui.authorization.FacebookAuthorizeActivity;
import com.waylens.hachi.ui.authorization.GoogleAuthorizeActivity;
import com.waylens.hachi.ui.clips.ClipPlayActivity;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.settings.myvideo.MyMomentActivity;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.utils.ViewUtils;
import com.xfdingustc.snipe.vdb.ClipSetManager;

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

    private static final int REQUEST_CODE_FACEBOOK = 0x100;
    private static final int REQUEST_CODE_YOUTUBE = 0x101;

    private int mPlayListId;
    private int mAudioId;

    private MaterialDialog mUploadDialog;


    private String[] mSupportedPrivacy;

    private boolean mIsFacebookShareChecked = false;
    private boolean mIsYoutubeShareChecked = false;

    private LinkedAccounts mLinkedAccounts;

    private HachiApi mHachi = HachiService.createHachiApiService();

    private SessionManager mSessionManager = SessionManager.getInstance();


    private String mSocialPrivacy;
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

    @BindView(R.id.other_social)
    TextView mOtherSocial;

    @BindView(R.id.btn_facebook)
    ImageView mBtnFaceBook;

    @BindView(R.id.btn_youtube)
    ImageView mBtnYoutube;

    @OnClick(R.id.btn_facebook)
    public void onBtnFackBookChecked() {
        if (mSessionManager.getFacebookName() == null) {
            FacebookAuthorizeActivity.launch(this, REQUEST_CODE_FACEBOOK);
        } else {
            mIsFacebookShareChecked = !mIsFacebookShareChecked;
            if (mIsFacebookShareChecked) {
                mBtnFaceBook.setImageResource(R.drawable.btn_platform_facebook_s);
            } else {
                mBtnFaceBook.setImageResource(R.drawable.btn_platform_facebook_n);
            }
        }
    }

    @OnClick(R.id.btn_youtube)
    public void onBtnYoutubeChecked() {
        if (!mSessionManager.isYoutubeLinked()) {
            GoogleAuthorizeActivity.launch(this, REQUEST_CODE_YOUTUBE);
        } else {
            mIsYoutubeShareChecked = !mIsYoutubeShareChecked;
            if (mIsYoutubeShareChecked) {
                mBtnYoutube.setImageResource(R.drawable.btn_platform_youtube_s);
            } else {
                mBtnYoutube.setImageResource(R.drawable.btn_platform_youtube_n);
            }
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
        mPlaylistEditor = new PlayListEditor(mVdbRequestQueue, mPlayListId);
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
            .placeholder(R.drawable.menu_profile_photo_default)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(mUserAvatar);
        mUserName.setText(sessionManager.getUserName());
        mUserEmail.setText(sessionManager.getEmail());


        Logger.t(TAG).d("is linked with facebook: " + sessionManager.getIsLinked());
//        if (sessionManager.isLinked()) {
//            mBtnFaceBook.setVisibility(View.VISIBLE);
//        } else {
//            mBtnFaceBook.setVisibility(View.GONE);
//        }

//        checkLinkedAccount();

    }

//    private void checkLinkedAccount() {
//        Call<LinkedAccounts> callLinkedAccount = mHachi.getLinkedAccounts();
//        callLinkedAccount.enqueue(new Callback<LinkedAccounts>() {
//            @Override
//            public void onResponse(Call<LinkedAccounts> call, retrofit2.Response<LinkedAccounts> response) {
//
//                mLinkedAccounts = response.body();
//                Logger.t(TAG).d("Get response: " + mLinkedAccounts.linkedAccounts.size());
//                for (LinkedAccounts.LinkedAccount account : mLinkedAccounts.linkedAccounts) {
//                    Logger.t(TAG).d("account: " + account.toString());
//                }
//                updateSocailButtons();
//
//            }
//
//            @Override
//            public void onFailure(Call<LinkedAccounts> call, Throwable t) {
//                Logger.t(TAG).d(t.toString());
//            }
//        });
//    }


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

        getToolbar().inflateMenu(R.menu.menu_share);
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
        Call<CloudStorageInfo> createMomentResponseCall = mHachi.getCloudStorageInfo();
        createMomentResponseCall.enqueue(new Callback<CloudStorageInfo>() {
            @Override
            public void onResponse(Call<CloudStorageInfo> call, retrofit2.Response<CloudStorageInfo> response) {
                if (response.body() != null) {
                    CloudStorageInfo cloudStorageInfo = response.body();
                    int currentClipLength = ClipSetManager.getManager().getClipSet(mPlayListId).getTotalLengthMs();
                    Logger.t(TAG).d("used: " + cloudStorageInfo.current.durationUsed + "total: " + cloudStorageInfo.current.plan.durationQuota);
                    if (cloudStorageInfo.current.durationUsed + currentClipLength > cloudStorageInfo.current.plan.durationQuota) {
                        MaterialDialog dialog = new MaterialDialog.Builder(getParent())
                            .content(R.string.no_clould_space)
                            .positiveText(R.string.ok)
                            .negativeText(R.string.cancel)
                            .show();
                    } else {
                        doShareMoment();
                    }

                }
                Logger.t(TAG).d("error code: " + response.code() + response.body().current.durationUsed);
            }

            @Override
            public void onFailure(Call<CloudStorageInfo> call, Throwable t) {

            }
        });


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
//        if (TextUtils.isEmpty(title)) {
//            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
//            title = "Created " + format.format(System.currentTimeMillis());
//        }

        String descrption = mEtMomentDescription.getEditableText().toString();
        String[] tags = new String[]{};

        Map<String, String> gaugeSettings = GaugeSettingManager.getManager().getGaugeSettingMap();


        Logger.t(TAG).d("share title: " + title);

        LocalMoment localMoment = new LocalMoment(mPlaylistEditor.getPlaylistId(), title, descrption,
            tags, mSocialPrivacy, mAudioId, gaugeSettings,
            mIsFacebookShareChecked, mIsYoutubeShareChecked);
        JobManager jobManager = BgJobManager.getManager();
        UploadMomentJob job = new UploadMomentJob(localMoment);
        jobManager.addJobInBackground(job);

        MyMomentActivity.launch(this);
        finish();
//
    }


}
