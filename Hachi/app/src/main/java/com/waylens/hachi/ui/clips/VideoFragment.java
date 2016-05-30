package com.waylens.hachi.ui.clips;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.MenuItemSelectEvent;
import com.waylens.hachi.eventbus.events.MultiSelectEvent;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.vdb.Clip;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;



public class VideoFragment extends BaseFragment implements FragmentNavigator {
    private static final String TAG = VideoFragment.class.getSimpleName();

    private EventBus mEventBus = EventBus.getDefault();

    @BindView(R.id.spinner)
    Spinner mVideoSpinner;

    @Subscribe
    public void onEventMultiSelect(MultiSelectEvent event) {
        if (event.getIsMultiSeleted()) {
            getToolbar().getMenu().clear();
            getToolbar().inflateMenu(R.menu.menu_clip_list);
            if (event.getSelectClipCount() > 1) {
                getToolbar().getMenu().removeItem(R.id.menu_to_upload);
            }
            getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    mEventBus.post(new MenuItemSelectEvent(item.getItemId()));
                    return true;
                }
            });
            getToolbar().setNavigationIcon(R.drawable.navbar_close);
            getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setupToolbar();
                    mEventBus.post(new MenuItemSelectEvent(-1));
                }
            });

            mVideoSpinner.setVisibility(View.GONE);
        } else {
            getToolbar().getMenu().clear();
            setupToolbar();

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
        getToolbar().getMenu().clear();
//        getToolbar().inflateMenu(R.menu.menu_smart_remix);
//        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                switch (item.getItemId()) {
//                    case R.id.smart_remix:
//                        SmartRemixActivity.launch(getActivity(), mVdtCamera);
//                        break;
//                }
//                return true;
//            }
//        });
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
                    fragment = TagFragment.newInstance(Clip.TYPE_MARKED);
                } else {
                    fragment = TagFragment.newInstance(Clip.TYPE_BUFFERED);
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
