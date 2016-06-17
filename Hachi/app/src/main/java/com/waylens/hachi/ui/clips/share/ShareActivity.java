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
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.IconSpinnerAdapter;
import com.waylens.hachi.ui.clips.ClipPlayActivity;
import com.waylens.hachi.ui.clips.EnhanceFragment;
import com.waylens.hachi.ui.clips.EnhancementActivity;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor2;
import com.waylens.hachi.ui.clips.upload.UploadActivity;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.helpers.MomentShareHelper;
import com.waylens.hachi.utils.ViewUtils;

import org.json.JSONObject;

import java.text.SimpleDateFormat;

import butterknife.BindArray;
import butterknife.BindView;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class ShareActivity extends ClipPlayActivity implements MomentShareHelper.OnShareMomentListener {
    private static final String TAG = ShareActivity.class.getSimpleName();
    private static final String EXTRA_PLAYLIST_ID = "playlist_id";

    private int mPlayListId;


    private MomentShareHelper mShareHelper;

    private String mSocialPrivacy;

    private String[] mSupportedPrivacy;

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

    public static void launch(Activity activity, int playListId) {
        Intent intent = new Intent(activity, ShareActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playListId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void init() {
        super.init();
        mPlayListId = getIntent().getIntExtra(EXTRA_PLAYLIST_ID, -1);
        initViews();
    }



    @Override
    public void onCancelShare() {

    }

    @Override
    public void onShareError(int errorCode, int errorResId) {

    }

    @Override
    public void onUploadStarted() {
        UploadActivity.launch(this);
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
        Glide.with(this).load(sessionManager.getAvatarUrl()).crossFade().into(mUserAvatar);
        mUserName.setText(sessionManager.getUserName());
        mUserEmail.setText(sessionManager.getEmail());

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
                        doShareMoment();
                        break;
                }
                return true;
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
        mShareHelper = new MomentShareHelper(this, mVdbRequestQueue, this);
        String title = mEtMomentTitle.getEditableText().toString();
        if (TextUtils.isEmpty(title)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            title = "Created " + format.format(System.currentTimeMillis());
        }

        String descrption = mEtMomentDescription.getEditableText().toString();
        String[] tags = new String[]{"Shanghai", "car"};
        Activity activity = this;
        int audioID = EnhanceFragment.DEFAULT_AUDIO_ID;
        JSONObject gaugeSettings = null;
        if (activity instanceof EnhancementActivity) {
            audioID = ((EnhancementActivity) activity).getAudioID();
            gaugeSettings = ((EnhancementActivity) activity).getGaugeSettings();
        }

        Logger.t(TAG).d("share title: " + title);
        mShareHelper.shareMoment(mPlaylistEditor.getPlaylistId(), title, descrption, tags,
            mSocialPrivacy, audioID, gaugeSettings, false);

//
    }
}
