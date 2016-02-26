package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.cocosw.bottomsheet.BottomSheet;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.vdb.Clip;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/2/17.
 */
public class VideoFragment extends BaseFragment {
    private static final String TAG = VideoFragment.class.getSimpleName();
    private TabLayout mTabLayout;

    @Bind(R.id.clipListViewPager)
    ViewPager mViewPager;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_bookmark2,
            savedInstanceState);
        mTabLayout = (TabLayout) getActivity().findViewById(R.id.tabs);
        setupViewPager();
        return view;
    }


    private void setupViewPager() {
        SimpleFragmentPagerAdapter adapter = new SimpleFragmentPagerAdapter(getActivity()
            .getFragmentManager());
        adapter.addFragment(ClipListFragment.newInstance(Clip.TYPE_MARKED), getString(R.string
            .bookmark));
        adapter.addFragment(ClipListFragment.newInstance(Clip.TYPE_BUFFERED), getString(R.string.all));

        mViewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(mViewPager);


    }


    @Override
    public ViewPager getViewPager() {
        return mViewPager;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.smartRemix:
                popupBottomSheet();
                break;
        }
        return true;
    }


    private boolean onBottomSheetMenuItemClicked(MenuItem item) {
        switch (item.getItemId()) {
//            break;
        }
        return true;
    }

    private void popupBottomSheet() {
        BottomSheet.Builder builder = new BottomSheet.Builder(getActivity());
        BottomSheet bottomSheet = builder
            .grid()
            .darkTheme()
            .sheet(R.menu.menu_video_fragment_bottom_sheet)
            .listener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    //return onBottomSheetMenuItemClicked(item);
                    return true;
                }
            }).build();

        bottomSheet.setCanceledOnTouchOutside(false);

        bottomSheet.show();

    }
}
