package com.waylens.hachi.ui.clips;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.presenter.Presenter;
import com.waylens.hachi.presenter.impl.ClipVideoPresenterImpl;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.ui.clips.event.ActionButtonEvent;
import com.waylens.hachi.ui.clips.event.ToggleFabEvent;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.BaseMVPFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.utils.rxjava.RxBus;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;
import com.waylens.hachi.view.ClipVideoView;

import java.util.List;

import butterknife.BindView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;


public class ClipVideoFragment extends BaseMVPFragment implements FragmentNavigator, ClipVideoView {
    private static final String TAG = ClipVideoFragment.class.getSimpleName();

    private SimpleFragmentPagerAdapter mVideoAdapter;

    private Presenter mVideoPresenter = null;


    @BindView(R.id.viewpager)
    ViewPager viewPager;

    @BindView(R.id.fab_smart_remix)
    FloatingActionButton mFabSmartRemix;

    private Subscription mInnerOperationSubscription;

    @Override
    public void onStart() {
        super.onStart();
        mInnerOperationSubscription = RxBus.getDefault().toObserverable(ToggleFabEvent.class)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleSubscribe<ToggleFabEvent>() {
                    @Override
                    public void onNext(ToggleFabEvent toggleFabEvent) {
                        switch (toggleFabEvent.mWhat) {
                            case ToggleFabEvent.FAB_VISIBLE:
                                mFabSmartRemix.setVisibility(View.VISIBLE);
                                break;
                            case ToggleFabEvent.FAB_INVISIBLE:
                                mFabSmartRemix.setVisibility(View.INVISIBLE);
                                break;
                        }
                    }
                });

    }


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
        setupToolbar();
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
        mFabSmartRemix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFabSmartRemix.setVisibility(View.INVISIBLE);
                int currentPage = viewPager.getCurrentItem();
                int clipType = -1;
                switch (currentPage) {
                    case 0:
                        clipType = Clip.TYPE_MARKED;
                        break;
                    case 1:
                        clipType = Clip.TYPE_BUFFERED;
                        break;
                    default:
                        clipType = Clip.TYPE_MARKED;
                        break;
                }
                RxBus.getDefault().post(new ActionButtonEvent(ActionButtonEvent.FAB_SMART_REMIX, clipType));
            }
        });

        getTablayout().setupWithViewPager(viewPager);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!mInnerOperationSubscription.isUnsubscribed()) {
            mInnerOperationSubscription.unsubscribe();
        }
    }


    @Override
    protected String getRequestTag() {
        return TAG;
    }



    @Override
    public boolean onInterceptBackPressed() {
        return false;
    }

    @Override
    public void onSelected() {

    }

    @Override
    public void onDeselected() {
        int current = viewPager.getCurrentItem();
        ClipGridListFragment fragment = (ClipGridListFragment) mVideoAdapter.getItem(current);
        fragment.onDeselected();
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.video);
    }
}
