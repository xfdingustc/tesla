package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.adapters.IconSpinnerAdapter;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.PlaylistEditor;
import com.waylens.hachi.ui.fragments.clipplay2.PlaylistUrlProvider;
import com.waylens.hachi.ui.fragments.clipplay2.UrlProvider;
import com.waylens.hachi.ui.helpers.MomentShareHelper;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Richard on 3/9/16.
 */

public class ShareActivity extends BaseActivity implements MomentShareHelper.OnShareMomentListener {
    private static final String TAG = "ShareActivity";

    private static final String EXTRA_CLIP_SET_INDEX = "extra.clip.set.index";
    private static final String EXTRA_GAUGE_SETTINGS = "extra.gauge.settings";
    private static final String EXTRA_AUDIO_ID = "extra.audio.id";

    private static final String EXTRA_IS_FROM_ENHANCE = "extra.is.from.enhance";

    int mClipSetIndex;

    PlaylistEditor mPlaylistEditor;
    ClipPlayFragment mClipPlayFragment;

    @Bind(R.id.spinner_social_privacy)
    Spinner mPrivacySpinner;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.moment_title)
    TextView mTitleView;

    private MomentShareHelper mShareHelper;

    String[] mSupportedPrivacy;

    String mSocialPrivacy;

    String mGaugeSettings;

    int mAudioID;

    boolean mIsFromEnhance;

    VdbRequestQueue mVdbRequestQueue;
    VdtCamera mVdtCamera;

    public static void launch(Context context, int clipSetIndex) {
        Intent intent = new Intent(context, ShareActivity.class);
        intent.putExtra(EXTRA_CLIP_SET_INDEX, clipSetIndex);
        intent.putExtra(EXTRA_GAUGE_SETTINGS, "");
        intent.putExtra(EXTRA_AUDIO_ID, -1);
        intent.putExtra(EXTRA_IS_FROM_ENHANCE, false);
        context.startActivity(intent);
    }

    public static void launch(Context context, int clipSetIndex, String gaugeSettings, int audioID, boolean isFromEnhance) {
        Intent intent = new Intent(context, ShareActivity.class);
        intent.putExtra(EXTRA_CLIP_SET_INDEX, clipSetIndex);
        intent.putExtra(EXTRA_GAUGE_SETTINGS, gaugeSettings);
        intent.putExtra(EXTRA_AUDIO_ID, audioID);
        intent.putExtra(EXTRA_IS_FROM_ENHANCE, isFromEnhance);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mClipSetIndex = ClipSetManager.CLIP_SET_TYPE_ENHANCE;
        mGaugeSettings = "";
        mAudioID = -1;
        mIsFromEnhance = false;
        if (intent != null) {
            mClipSetIndex = intent.getIntExtra(EXTRA_CLIP_SET_INDEX, ClipSetManager.CLIP_SET_TYPE_ENHANCE);
            mGaugeSettings = intent.getStringExtra(EXTRA_GAUGE_SETTINGS);
            if (mGaugeSettings == null) {
                mGaugeSettings = "";
            }
            mAudioID = intent.getIntExtra(EXTRA_AUDIO_ID, -1);
            mIsFromEnhance = intent.getBooleanExtra(EXTRA_IS_FROM_ENHANCE, false);
        }
        mVdbRequestQueue = Snipe.newRequestQueue();

        initViews();
    }

    private void initViews() {
        setContentView(R.layout.fragment_share);

        if (mIsFromEnhance) {
            embedVideoPlayFragment();
        } else {
            mPlaylistEditor = new PlaylistEditor(this, mVdtCamera, mClipSetIndex, 0x100);
            mPlaylistEditor.build(new PlaylistEditor.OnBuildCompleteListener() {
                @Override
                public void onBuildComplete(ClipSet clipSet) {
                    embedVideoPlayFragment();
                }
            });
        }
        CharSequence[] strings = getResources().getTextArray(R.array.social_privacy_text);
        TypedArray typedArray = getResources().obtainTypedArray(R.array.social_privacy_icon);
        Drawable[] drawables = new Drawable[typedArray.length()];
        for (int i = 0; i < drawables.length; i++) {
            drawables[i] = typedArray.getDrawable(i);
        }
        typedArray.recycle();
        IconSpinnerAdapter mAdapter = new IconSpinnerAdapter(this,
                android.R.layout.simple_spinner_item,
                strings,
                drawables,
                ViewUtils.dp2px(16, getResources()));
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

    void embedVideoPlayFragment() {
        ClipPlayFragment.Config config = new ClipPlayFragment.Config();
        config.clipMode = ClipPlayFragment.Config.ClipMode.MULTI;

        UrlProvider vdtUriProvider = new PlaylistUrlProvider(mVdbRequestQueue, 0x100);
        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, mClipSetIndex,
                vdtUriProvider,
                config);
        mClipPlayFragment.setShowsDialog(false);
        getFragmentManager().beginTransaction().replace(R.id.share_fragment_content, mClipPlayFragment).commit();
    }

    @Override
    public void setupToolbar() {
        if (mToolbar == null) {
            return;
        }
        mToolbar.setTitle(R.string.share);
        mToolbar.setNavigationIcon(R.drawable.navbar_close);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @OnClick(R.id.btn_share)
    void shareVideo() {
        mViewAnimator.setDisplayedChild(1);
        mShareHelper = new MomentShareHelper(this, this);
        String title = mTitleView.getText().toString();
        String[] tags = new String[]{"Shanghai", "car"};
        mShareHelper.shareMoment(0x100, title, tags, mSocialPrivacy, mAudioID, mGaugeSettings);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mShareHelper != null) {
            mShareHelper.cancel(true);
        }
    }

    @Override
    public void onShareSuccessful(LocalMoment localMoment) {
        Logger.t(TAG).e("onShareSuccessful");
        mViewAnimator.setDisplayedChild(2);
    }

    @Override
    public void onCancelShare() {
        Logger.t(TAG).e("onCancelShare");
        mViewAnimator.setDisplayedChild(0);
    }

    @Override
    public void onShareError(int errorCode, int errorResId) {
        Logger.t(TAG).e("onShareError:" + errorCode);
        mViewAnimator.setDisplayedChild(3);
    }

    @Override
    public void onUploadProgress(int uploadPercentage) {
        //Logger.t(TAG).e("onUploadProgress: "+ uploadPercentage);
    }

    static class IconAdapter extends RecyclerView.Adapter<IconVH> {

        int[] drawables = new int[]{
                R.drawable.toggle_waylens,
                R.drawable.toggle_facebook,
                R.drawable.toggle_twitter,
                R.drawable.toggle_youtube,
                R.drawable.toggle_vimeo,
                R.drawable.toggle_pinterest,
                R.drawable.toggle_instagram,
        };

        @Override
        public IconVH onCreateViewHolder(ViewGroup parent, int viewType) {
            CheckBox checkBox = new CheckBox(parent.getContext());
            return new IconVH(checkBox);
        }

        @Override
        public void onBindViewHolder(IconVH holder, int position) {
            holder.mCheckBox.setButtonDrawable(drawables[position]);
            if (position == 0) {
                holder.mCheckBox.setChecked(true);
            } else {
                holder.mCheckBox.setEnabled(false);
            }
        }

        @Override
        public int getItemCount() {
            return drawables.length;
        }
    }

    static class IconVH extends RecyclerView.ViewHolder {
        public CheckBox mCheckBox;

        public IconVH(View itemView) {
            super(itemView);
            mCheckBox = (CheckBox) itemView;
        }
    }
}
