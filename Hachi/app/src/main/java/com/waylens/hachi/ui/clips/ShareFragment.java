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
import com.waylens.hachi.ui.clips.EnhancementActivity;
import com.waylens.hachi.ui.adapters.IconSpinnerAdapter;
import com.waylens.hachi.ui.clips.EnhanceFragment;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.helpers.MomentShareHelper;
import com.waylens.hachi.utils.ViewUtils;

import org.json.JSONObject;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Richard on 3/22/16.
 */
public class ShareFragment extends BaseFragment implements MomentShareHelper.OnShareMomentListener {
    private static final String TAG = "ShareFragment";

    @Bind(R.id.spinner_social_privacy)
    Spinner mPrivacySpinner;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.moment_title)
    TextView mTitleView;


    String[] mSupportedPrivacy;

    String mSocialPrivacy;

    private MomentShareHelper mShareHelper;

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

    @OnClick(R.id.btn_share)
    void shareVideo() {
        mViewAnimator.setDisplayedChild(1);
        mShareHelper = new MomentShareHelper(getActivity(), mVdbRequestQueue, this);
        String title = mTitleView.getText().toString();
        String[] tags = new String[]{"Shanghai", "car"};
        Activity activity = getActivity();
        int audioID = EnhanceFragment.DEFAULT_AUDIO_ID;
        JSONObject gaugeSettings = null;
        if (activity instanceof EnhancementActivity) {
            audioID = ((EnhancementActivity)activity).getAudioID();
            gaugeSettings = ((EnhancementActivity)activity).getGaugeSettings();
        }
        mShareHelper.shareMoment(0x100, title, tags, mSocialPrivacy, audioID, gaugeSettings);
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
