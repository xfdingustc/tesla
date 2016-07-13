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
        mViewPager.setCurrentItem(mCount - 1, false);
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



}
