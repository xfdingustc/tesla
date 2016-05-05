package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.adapters.IconSpinnerAdapter;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.helpers.MomentShareHelper;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.OnClick;


public class ShareFragment extends BaseFragment implements MomentShareHelper.OnShareMomentListener {
    private static final String TAG = "ShareFragment";
    private static final int PLAYLIST_SHARE = 0x100;

    private final int mClipSetIndex = ClipSetManager.CLIP_SET_TYPE_SHARE;


    @BindView(R.id.spinner_social_privacy)
    Spinner mPrivacySpinner;

    @BindView(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @BindView(R.id.moment_title)
    TextView mTitleView;

    @BindArray(R.array.social_privacy_text)
    CharSequence[] mPrivacyText;

    private ClipSet mClipSet = new ClipSet(Clip.TYPE_TEMP);

    String[] mSupportedPrivacy;

    String mSocialPrivacy;

    private MomentShareHelper mShareHelper;

    public static ShareFragment newInstance(ArrayList<Clip> clipList) {
        ShareFragment fragment = new ShareFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("cliplist", clipList);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        ArrayList<Clip> clipList = bundle.getParcelableArrayList("cliplist");
        for (Clip clip : clipList) {
            mClipSet.addClip(clip);
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
        initViews();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mShareHelper != null) {
            mShareHelper.cancel(true);
        }
    }

    private void initViews() {
        TypedArray typedArray = getResources().obtainTypedArray(R.array.social_privacy_icon);
        Drawable[] drawables = new Drawable[typedArray.length()];
        for (int i = 0; i < drawables.length; i++) {
            drawables[i] = typedArray.getDrawable(i);
        }
        typedArray.recycle();
        IconSpinnerAdapter mAdapter = new IconSpinnerAdapter(getActivity(), android.R.layout.simple_spinner_item, mPrivacyText, drawables,
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

    @OnClick(R.id.btn_share)
    void shareVideo() {
        ClipSetManager manager = ClipSetManager.getManager();
        manager.updateClipSet(ClipSetManager.CLIP_SET_TYPE_SHARE, mClipSet);
//        PlaylistEditor playlistEditor = new PlaylistEditor(mVdbRequestQueue, PLAYLIST_SHARE);
//        playlistEditor.build(mClipSetIndex, new PlaylistEditor.OnBuildCompleteListener() {
//            @Override
//            public void onBuildComplete(ClipSet clipSet) {
//                mClipSet = clipSet;
//                mViewAnimator.setDisplayedChild(1);
//                mShareHelper = new MomentShareHelper(getActivity(), mVdbRequestQueue, ShareFragment.this);
//                String title = mTitleView.getText().toString();
//                String[] tags = new String[]{"Shanghai", "car"};
//                Activity activity = getActivity();
//                int audioID = EnhanceFragment.DEFAULT_AUDIO_ID;
//                JSONObject gaugeSettings = null;
//                if (activity instanceof EnhancementActivity) {
//                    audioID = ((EnhancementActivity) activity).getAudioID();
//                    gaugeSettings = ((EnhancementActivity) activity).getGaugeSettings();
//                }
//                mShareHelper.shareMoment(PLAYLIST_SHARE, title, tags, mSocialPrivacy, audioID, gaugeSettings);
//            }
//        });


        mViewAnimator.setDisplayedChild(1);
        mShareHelper = new MomentShareHelper(getActivity(), mVdbRequestQueue, ShareFragment.this);
        String title = mTitleView.getText().toString();
        String[] tags = new String[]{"Shanghai", "car"};
        Activity activity = getActivity();
        int audioID = EnhanceFragment.DEFAULT_AUDIO_ID;
        JSONObject gaugeSettings = null;
        if (activity instanceof EnhancementActivity) {
            audioID = ((EnhancementActivity) activity).getAudioID();
            gaugeSettings = ((EnhancementActivity) activity).getGaugeSettings();
        }
        mShareHelper.shareMoment(PLAYLIST_SHARE, title, tags, mSocialPrivacy, audioID, gaugeSettings);

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

}
