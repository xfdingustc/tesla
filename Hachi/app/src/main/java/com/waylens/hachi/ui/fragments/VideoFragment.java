package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.vdb.Clip;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/2/17.
 */
public class VideoFragment extends BaseFragment implements FragmentNavigator {
    private static final String TAG = VideoFragment.class.getSimpleName();


    @Bind(R.id.videoSpinner)
    Spinner mVideoSpinner;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_video, savedInstanceState);

        setupVideoSpinner();
        return view;
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
