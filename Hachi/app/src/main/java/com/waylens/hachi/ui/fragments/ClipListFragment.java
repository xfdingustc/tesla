package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.ui.adapters.ClipSetGroupAdapter;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/2/18.
 */
public class ClipListFragment extends BaseFragment {
    private static final String TAG = ClipListFragment.class.getSimpleName();

    private Map<String, ClipSet> mClipSetGroup = new HashMap<>();

    private ClipSetGroupAdapter mAdapter;


    @Bind(R.id.clipGroupList)
    RecyclerView mRvClipGroupList;

    @Bind(R.id.progressLoading)
    ProgressBar mLoadingProgressBar;

    private Handler mUiThreadHandler;

    private int mClipSetType;

    public static ClipListFragment newInstance(int clipSetType) {
        ClipListFragment fragment = new ClipListFragment();
        fragment.mClipSetType = clipSetType;
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getCamera() != null) {
            mClipSetGroup.clear();
            retrieveSharableClips();
        } else {
            mLoadingProgressBar.setVisibility(View.VISIBLE);
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_clip_list,
            savedInstanceState);
        mUiThreadHandler = new Handler();
        setupClipSetGroup();
        return view;
    }

    @Override
    public void onCameraVdbConnected(VdtCamera camera) {
        super.onCameraVdbConnected(camera);
        mUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mLoadingProgressBar.setVisibility(View.GONE);
                mClipSetGroup.clear();
                retrieveSharableClips();
            }
        });

    }

    private void setupClipSetGroup() {
        mRvClipGroupList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new ClipSetGroupAdapter(getActivity(), null, new ClipSetGroupAdapter
            .OnClipClickListener() {
            @Override
            public void onClipClicked(Clip clip) {
                popClipPreviewFragment(clip);

            }


        });
        mRvClipGroupList.setAdapter(mAdapter);

    }

    private void retrieveSharableClips() {
        mVdbRequestQueue.add(new ClipSetRequest(mClipSetType, ClipSetRequest.FLAG_CLIP_EXTRA,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet clipSet) {
                    calculateClipSetGroup(clipSet);
                    setupClipSetGroupView();
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("", error);

                }
            }));

    }


    private void calculateClipSetGroup(ClipSet clipSet) {
        for (Clip clip : clipSet.getClipList()) {
            int date = clip.clipDate;

            String clipDataString = clip.getDateString();
            ClipSet oneClipSet = mClipSetGroup.get(clipDataString);
            if (oneClipSet == null) {
                oneClipSet = new ClipSet(clipSet.getType());
                mClipSetGroup.put(clipDataString, oneClipSet);
            }

            oneClipSet.addClip(clip);

            //Logger.t(TAG).d("clipData: " + clip.getDateString() + " clipTime: " + clip
            //    .getDateTimeString());
        }
    }

    private void setupClipSetGroupView() {

        List<ClipSet> clipSetGroup = new ArrayList<>();
        Iterator iter = mClipSetGroup.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            clipSetGroup.add((ClipSet) entry.getValue());
        }

        mAdapter.setClipSetGroup(clipSetGroup);
    }

    private void popClipPreviewFragment(Clip clip) {

        ClipPlayFragment.Config config = new ClipPlayFragment.Config();
        config.progressBarStyle = ClipPlayFragment.Config.PROGRESS_BAR_STYLE_SINGLE;

        ClipPlayFragment fragment = ClipPlayFragment.newInstance(getCamera(), clip, config);

        fragment.show(getFragmentManager(), "ClipPlayFragment");

//        getFragmentManager().beginTransaction().replace(R.id.videoPlayFragmentContainer,
//            fragment).commit();
    }
}
