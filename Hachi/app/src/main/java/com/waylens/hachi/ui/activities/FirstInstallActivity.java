package com.waylens.hachi.ui.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.manualsetup.StartupActivity;
import com.waylens.hachi.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;


import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/4/1.
 */
public class FirstInstallActivity extends BaseActivity {
    private static final String TAG = FirstInstallActivity.class.getSimpleName();
    @BindView(R.id.viewPager)
    ViewPager mViewPager;

    private SimpleImageViewPagerAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_first_install);
        hideStatusBar();
        setupViewPager();
    }

    private void setupViewPager() {
        mAdapter = new SimpleImageViewPagerAdapter(getFragmentManager());
        mAdapter.addFragment(SimpleImageFragment.newInstance(R.drawable.content0, R.string.description1, false, false));
        mAdapter.addFragment(SimpleImageFragment.newInstance(R.drawable.content1, R.string.description4, false, true));
        mAdapter.addFragment(SimpleImageFragment.newInstance(R.drawable.content2, R.string.description3, false, true));
        mAdapter.addFragment(SimpleImageFragment.newInstance(R.drawable.content3, R.string.description2, false, true));
        mAdapter.addFragment(SimpleImageFragment.newInstance(R.drawable.content4, -1, true, false));

        mViewPager.setAdapter(mAdapter);
    }


    private class SimpleImageViewPagerAdapter extends FragmentPagerAdapter {

        private List<Fragment> mFragmentList = new ArrayList<>();

        public void addFragment(SimpleImageFragment fragment) {
            mFragmentList.add(fragment);
        }

        public SimpleImageViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }
    }


    public static class SimpleImageFragment extends BaseFragment {

        public static SimpleImageFragment newInstance(@DrawableRes int imageRes, @StringRes int description, boolean hasEnther, boolean hasShadow) {
            SimpleImageFragment fragment = new SimpleImageFragment();
            fragment.imageRes = imageRes;
            fragment.hasEnter = hasEnther;
            fragment.hasShadow = hasShadow;
            fragment.description = description;
            return fragment;
        }

        @DrawableRes
        int imageRes;

        @StringRes
        int description;


        boolean hasEnter;

        boolean hasShadow;

        @BindView(R.id.policy_layout)
        View mPolicyLayout;

        @BindView(R.id.description)
        TextView mDescripion;

        @BindView(R.id.agree_check_box)
        CheckBox mAgreeCheckBox;

        @BindView(R.id.waylens_agreement)
        TextView mWaylensAgreement;

        @BindView(R.id.imageCover)
        ImageView imageCover;

        @BindView(R.id.btnEnter)
        Button mBtnEnter;

        @BindView(R.id.withoutCamera)
        TextView mWithoutCamera;

        @BindView(R.id.top_shade)
        ImageView mTopShadow;

        @BindView(R.id.bottom_shade)
        ImageView mBottomShadow;

        @OnClick(R.id.btnEnter)
        public void onBtnEnterClicked() {
            Intent intent = new Intent();
            boolean enterSetup = VdtCameraManager.getManager().isConnected();
            if (!enterSetup) {
                intent.setClass(getActivity(), StartupActivity.class);
            } else {
                intent.setClass(getActivity(), MainActivity.class);
            }
            startActivity(intent);
            getActivity().finish();
            writeVersionName();
        }

        @OnClick(R.id.withoutCamera)
        public void OnWithoutCameraClicked() {
            MainActivity.launch(getActivity());
            getActivity().finish();
            writeVersionName();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = createFragmentView(inflater, container, R.layout.fragment_simple_image, savedInstanceState);
            imageCover.setImageResource(imageRes);
            if (hasEnter == true) {
                mPolicyLayout.setVisibility(View.VISIBLE);
            } else {
                mPolicyLayout.setVisibility(View.GONE);
            }

            int shadowVisibility = View.GONE;
            if (hasShadow) {
                shadowVisibility = View.VISIBLE;
            }

            mTopShadow.setVisibility(shadowVisibility);
            mBottomShadow.setVisibility(shadowVisibility);

            if (description != -1) {
                mDescripion.setText(getString(description));
            }

            mAgreeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    int visibility = b ? View.VISIBLE : View.INVISIBLE;
                    mBtnEnter.setVisibility(visibility);
                    mWithoutCamera.setVisibility(visibility);
                }
            });

            SpannableStringBuilder ssb = new SpannableStringBuilder(getString(R.string.agree_line1));
            int start = ssb.length();
            ssb.append(getString(R.string.agree_line2))
                .setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Logger.t(TAG).d("on clicked");
                        WaylensAgreementActivity.launch(getActivity());
                    }
                }, start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append(getString(R.string.agree_line3));
            mWaylensAgreement.setText(ssb);
            mWaylensAgreement.setMovementMethod(LinkMovementMethod.getInstance());
            return view;
        }


        private void writeVersionName() {
            PackageInfo pi = null;
            try {
                pi = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                PreferenceUtils.putInt(PreferenceUtils.VERSION_CODE, pi.versionCode);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }


        }
    }
}
