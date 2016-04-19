package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.ClipSelectEvent;
import com.waylens.hachi.eventbus.events.MenuItemSelectEvent;
import com.waylens.hachi.eventbus.events.MultiSelectEvent;
import com.waylens.hachi.ui.activities.SmartRemixActivity;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.vdb.Clip;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/2/17.
 */
public class VideoFragment extends BaseFragment implements FragmentNavigator {
    private static final String TAG = VideoFragment.class.getSimpleName();

    private EventBus mEventBus = EventBus.getDefault();

    @Bind(R.id.spinner)
    Spinner mVideoSpinner;

    @Subscribe
    public void onEventMultiSelect(MultiSelectEvent event) {
        if (event.getIsMultiSeleted()) {
            mToolbar.getMenu().clear();
            mToolbar.inflateMenu(R.menu.menu_clip_list);
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    mEventBus.post(new MenuItemSelectEvent(item.getItemId()));
                    return true;
                }
            });
            mToolbar.setNavigationIcon(R.drawable.navbar_close);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setupToolbar();
                    mEventBus.post(new MenuItemSelectEvent(-1));
                }
            });

            mVideoSpinner.setVisibility(View.GONE);
        } else {

        }
    }

    @Subscribe
    public void onEventClipSelectEvent(ClipSelectEvent event) {
        mToolbar.getMenu().clear();
        if (event.getClip() != null) {
            mToolbar.inflateMenu(R.menu.menu_clip_list);
        }

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_video, savedInstanceState);
        setupVideoSpinner();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mEventBus.register(this);
    }


    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(this);
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        mToolbar.getMenu().clear();
        mToolbar.inflateMenu(R.menu.menu_smart_remix);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.smart_remix:
                        SmartRemixActivity.launch(getActivity(), mVdtCamera);
                        break;
                }
                return true;
            }
        });
        mVideoSpinner.setVisibility(View.VISIBLE);
    }

    private void setupVideoSpinner() {
        String[] items = getResources().getStringArray(R.array.videoOptions);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.item_spinner, items);
        mVideoSpinner.setAdapter(adapter);

        mVideoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                        Logger.t(TAG).d("Item Position clicked: " + position);
                //changeCurrentCamera(position);
                BaseFragment fragment;
                if (position == 0) {
                    fragment = BookmarkFragment.newInstance(Clip.TYPE_MARKED);
                } else {
                    fragment = AllFootageFragment.newInstance();
                }

                getChildFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }




    @Override
    public boolean onInterceptBackPressed() {
        //BookmarkFragment fragment = (BookmarkFragment) mAdapter.getItem(mViewPager.getCurrentItem());
        return false;
    }
}
