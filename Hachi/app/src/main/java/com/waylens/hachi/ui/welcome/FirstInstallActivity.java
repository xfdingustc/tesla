package com.waylens.hachi.ui.welcome;

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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.orhanobut.logger.Logger;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.activities.WaylensAgreementActivity;
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

    private int mCount;
    private ImageView[] mImages;
    private int mCurrentItem;

    @BindView(R.id.viewPager)
    ViewPager mViewPager;



    @BindView(R.id.ll_point_indicator)
    LinearLayout mllPointIndicator;

    @BindView(R.id.btn_next)
    ImageView mBtnNext;

    @BindView(R.id.skip)
    View mSkip;

    @OnClick(R.id.btn_next)
    public void onBtnNextClicked() {
        mViewPager.setCurrentItem(mCurrentItem + 1);
    }

    @OnClick(R.id.skip)
    public void onBtnSkipClicked() {
        mViewPager.setCurrentItem(mCount - 1);
    }

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
        mAdapter.addFragment(new Welcome1Fragment());
        mAdapter.addFragment(new Welcome2Fragment());
        mAdapter.addFragment(new Welcome3Fragment());
        mAdapter.addFragment(new Welcome4Fragment());


        mViewPager.setAdapter(mAdapter);

        mCount = mAdapter.getCount();
        mImages = new ImageView[mCount];
        for (int i = 0; i < mCount; i++) {
            mImages[i] = (ImageView) mllPointIndicator.getChildAt(i);
            mImages[i].setEnabled(true);
            mImages[i].setTag(i);
        }
        mCurrentItem = 0;
        mImages[mCurrentItem].setEnabled(false);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position < 0 || position > mCount - 1 || mCurrentItem == position) {
                    return;
                }
                mImages[mCurrentItem].setEnabled(true);
                mImages[position].setEnabled(false);
                mCurrentItem = position;
                if (mCurrentItem == mCount - 1) {
                    mBtnNext.setVisibility(View.GONE);
                    mSkip.setVisibility(View.GONE);
                } else {
                    mBtnNext.setVisibility(View.VISIBLE);
                    mSkip.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private SimpleImageFragment.OnSkipClickListener mOnSkipClickListener = new SimpleImageFragment.OnSkipClickListener() {
        @Override
        public void onSkipClick() {
            mViewPager.setCurrentItem(mCount - 1);
        }
    };


    private class SimpleImageViewPagerAdapter extends FragmentPagerAdapter {

        private List<Fragment> mFragmentList = new ArrayList<>();

        public void addFragment(BaseFragment fragment) {
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



        public static SimpleImageFragment newInstance(@DrawableRes int imageRes, @StringRes int description, boolean hasEnther, OnSkipClickListener listener) {
            SimpleImageFragment fragment = new SimpleImageFragment();
            fragment.imageRes = imageRes;
            fragment.hasEnter = hasEnther;
            fragment.description = description;
            fragment.mOnSkipClickListener = listener;
            return fragment;
        }

        @DrawableRes
        int imageRes;

        @StringRes
        int description;


        boolean hasEnter;

        private OnSkipClickListener mOnSkipClickListener;


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

        @OnClick(R.id.skip)
        public void onBtnSkipClicked() {
            if (mOnSkipClickListener != null) {
                mOnSkipClickListener.onSkipClick();
            }
        }

        @OnClick(R.id.btnEnter)
        public void onBtnEnterClicked() {
            enter();
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

        private void enter() {
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

        private void writeVersionName() {
            PackageInfo pi = null;
            try {
                pi = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                PreferenceUtils.putInt(PreferenceUtils.VERSION_CODE, pi.versionCode);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }


        }

        public interface OnSkipClickListener {
            void onSkipClick();
        }
    }
}
