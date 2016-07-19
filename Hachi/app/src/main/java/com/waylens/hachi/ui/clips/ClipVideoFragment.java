package com.waylens.hachi.ui.clips;

import android.support.v4.view.ViewPager;

import com.waylens.hachi.R;
import com.waylens.hachi.presenter.Presenter;
import com.waylens.hachi.presenter.impl.ClipVideoPresenterImpl;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.BaseMVPFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.view.ClipVideoView;

import java.util.List;

import butterknife.BindView;


public class ClipVideoFragment extends BaseMVPFragment implements FragmentNavigator, ClipVideoView {
    private static final String TAG = ClipVideoFragment.class.getSimpleName();

    private SimpleFragmentPagerAdapter mVideoAdapter;

    private Presenter mVideoPresenter = null;


    @BindView(R.id.viewpager)
    ViewPager viewPager;


    @Override
    protected void init() {
        mVideoPresenter = new ClipVideoPresenterImpl(getActivity(), this);
        mVideoPresenter.initialized();
    }

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.fragment_video;
    }


    @Override
    public void initViews(List<BaseFragment> fragments, List<Integer> pageTitleList) {
        mVideoAdapter = new SimpleFragmentPagerAdapter(getChildFragmentManager());
        for (int i = 0; i < fragments.size(); i++) {
            mVideoAdapter.addFragment(fragments.get(i), getString(pageTitleList.get(i)));
        }


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

                ClipGridListFragment fragment = (ClipGridListFragment) mVideoAdapter.getItem(oldPosition);
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
        return false;
    }
}
