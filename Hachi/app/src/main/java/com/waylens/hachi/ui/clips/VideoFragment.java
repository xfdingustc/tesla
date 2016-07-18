package com.waylens.hachi.ui.clips;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.vdb.Clip;

import butterknife.BindView;


public class VideoFragment extends BaseFragment implements FragmentNavigator {
    private static final String TAG = VideoFragment.class.getSimpleName();

    private SimpleFragmentPagerAdapter mVideoAdapter;

    @BindView(R.id.viewpager)
    ViewPager viewPager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_video, savedInstanceState);
        setupVideoPager();
        return view;
    }

    private void setupVideoPager() {
        mVideoAdapter = new SimpleFragmentPagerAdapter(getChildFragmentManager());
        mVideoAdapter.addFragment(TagFragment.newInstance(Clip.TYPE_MARKED), getString(R.string.highlights));
        mVideoAdapter.addFragment(TagFragment.newInstance(Clip.TYPE_BUFFERED), getString(R.string.lable_buffered_video));

        viewPager.setAdapter(mVideoAdapter);

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {

                int oldPosition;
                if (position == 0) {
                    oldPosition = 1;
                } else {
                    oldPosition = 0;
                }

                TagFragment fragment = (TagFragment) mVideoAdapter.getItem(oldPosition);
                fragment.onDeselected();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        getTablayout().setupWithViewPager(viewPager);
    }


    @Override
    protected String getRequestTag() {
        return TAG;
    }

    @Override
    public void setupToolbar() {
        getToolbar().setTitle(R.string.video);
        super.setupToolbar();
    }


    @Override
    public boolean onInterceptBackPressed() {
        //BookmarkFragment fragment = (BookmarkFragment) mAdapter.getItem(mViewPager.getCurrentItem());
        return false;
    }
}
