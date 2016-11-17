package com.waylens.hachi.ui.welcome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.view.InkPageIndicator;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/4/1.
 */
public class FirstInstallActivity extends BaseActivity {
    private static final String TAG = FirstInstallActivity.class.getSimpleName();

    private int mCount;
    private int mCurrentItem;

    @BindView(R.id.viewPager)
    ViewPager mViewPager;

    @BindView(R.id.indicator)
    InkPageIndicator inkPageIndicator;


    @BindView(R.id.skip)
    View mSkip;

    @OnClick(R.id.skip)
    public void onBtnSkipClicked() {
        mViewPager.setCurrentItem(mCount - 1, false);
    }

    private SimpleFragmentPagerAdapter mAdapter;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, FirstInstallActivity.class);
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
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_first_install);
        hideStatusBar();
        setupViewPager();
    }

    private void setupViewPager() {

        mAdapter = new SimpleFragmentPagerAdapter(getFragmentManager());
        mAdapter.addFragment(new Welcome1Fragment());
        mAdapter.addFragment(new Welcome2Fragment());
//        mAdapter.addFragment(new VideoWelcomeFragment());
        mAdapter.addFragment(new Welcome3Fragment());
        mAdapter.addFragment(new Welcome4Fragment());


        mViewPager.setAdapter(mAdapter);

        mCount = mAdapter.getCount();

        mCurrentItem = 0;

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position < 0 || position > mCount - 1 || mCurrentItem == position) {
                    return;
                }
                mCurrentItem = position;
                if (mCurrentItem == mCount - 1) {
                    mSkip.setVisibility(View.GONE);
                } else {
                    mSkip.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        inkPageIndicator.setViewPager(mViewPager);
    }


}
