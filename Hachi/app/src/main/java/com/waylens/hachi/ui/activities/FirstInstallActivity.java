package com.waylens.hachi.ui.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.TextViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/4/1.
 */
public class FirstInstallActivity extends BaseActivity {

    @Bind(R.id.viewPager)
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
        setContentView(R.layout.activitiy_first_install);
        setupViewPager();
    }

    private void setupViewPager() {
        mAdapter = new SimpleImageViewPagerAdapter(getFragmentManager());
        mAdapter.addFragment(SimpleImageFragment.newInstance(R.drawable.tutorial1, false));
        mAdapter.addFragment(SimpleImageFragment.newInstance(R.drawable.tutorial2, true));
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

        public static SimpleImageFragment newInstance(@DrawableRes int imageRes, boolean hasEnther) {
            SimpleImageFragment fragment = new SimpleImageFragment();
            fragment.imageRes = imageRes;
            fragment.hasEnter = hasEnther;
            return fragment;
        }

        @DrawableRes
        int imageRes;


        boolean hasEnter;

        @Bind(R.id.imageCover)
        ImageView imageCover;

        @Bind(R.id.btnEnter)
        Button mBtnEnter;

        @Bind(R.id.withoutCamera)
        TextView mWithoutCamera;

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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = createFragmentView(inflater, container, R.layout.fragment_simple_image, savedInstanceState);
            imageCover.setImageResource(imageRes);
            if (hasEnter == true) {
                mBtnEnter.setVisibility(View.VISIBLE);
                mWithoutCamera.setVisibility(View.VISIBLE);
            } else {
                mBtnEnter.setVisibility(View.GONE);
                mWithoutCamera.setVisibility(View.GONE);
            }
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
