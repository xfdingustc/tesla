package com.waylens.hachi.ui.fragments;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.adapters.IconSpinnerAdapter;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.entities.SharableClip;
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
 * Created by Richard on 12/18/15.
 */
public class ShareFragment extends BaseFragment implements MomentShareHelper.OnShareMomentListener {
    private static final String TAG = "ShareFragment";

    private static final String ARG_CLIP_SET_INDEX = "arg.clip.set.index";
    private static final String ARG_GAUGE_SETTINGS = "arg.gauge.settings";
    private static final String ARG_AUDIO_ID = "arg.audio.id";
    private static final String ARG_IS_FROM_ENHANCE = "arg.is.from.enhance";

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

    public static ShareFragment newInstance(int clipSetIndex, String gaugeSettings, int audioID, boolean isFromEnhance) {
        Bundle args = new Bundle();
        args.putInt(ARG_CLIP_SET_INDEX, clipSetIndex);
        args.putString(ARG_GAUGE_SETTINGS, gaugeSettings);
        args.putInt(ARG_AUDIO_ID, audioID);
        args.putBoolean(ARG_IS_FROM_ENHANCE, isFromEnhance);
        ShareFragment fragment = new ShareFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mClipSetIndex = args.getInt(ARG_CLIP_SET_INDEX, ClipSetManager.CLIP_SET_TYPE_ENHANCE);
            mGaugeSettings = args.getString(ARG_GAUGE_SETTINGS, "");
            mAudioID = args.getInt(ARG_AUDIO_ID, -1);
            mIsFromEnhance = args.getBoolean(ARG_IS_FROM_ENHANCE, false);
        } else {
            mClipSetIndex = ClipSetManager.CLIP_SET_TYPE_ENHANCE;
            mGaugeSettings = "";
            mAudioID = -1;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_share, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mIsFromEnhance) {
            embedVideoPlayFragment();
        } else {
            mPlaylistEditor = new PlaylistEditor(getActivity(), mVdtCamera, mClipSetIndex, 0x100);
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
        IconSpinnerAdapter mAdapter = new IconSpinnerAdapter(getActivity(),
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
        mClipPlayFragment = ClipPlayFragment.newInstance(getCamera(), mClipSetIndex,
                vdtUriProvider,
                config);
        mClipPlayFragment.setShowsDialog(false);
        getChildFragmentManager().beginTransaction().replace(R.id.share_fragment_content, mClipPlayFragment).commit();
    }

    @OnClick(R.id.btn_share)
    void shareVideo() {
        mViewAnimator.setDisplayedChild(1);
        mShareHelper = new MomentShareHelper(getActivity(), this);
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
