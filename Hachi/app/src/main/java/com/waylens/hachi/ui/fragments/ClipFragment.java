package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.eventbus.events.MultiSelectEvent;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.ui.adapters.ClipSetGroupAdapter;
import com.waylens.hachi.utils.ClipSetGroupHelper;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/4/20.
 */
public class ClipFragment extends BaseFragment {
    private static final String TAG = ClipFragment.class.getSimpleName();

    private ClipSetGroupAdapter mAdapter;

    private EventBus mEventBus = EventBus.getDefault();

    public static ClipFragment newInstance() {
        ClipFragment fragment = new ClipFragment();
        return fragment;
    }

    @Bind(R.id.refreshLayout)
    SwipeRefreshLayout mRefreshLayout;


    @Bind(R.id.menualClipGroupList)
    RecyclerView mMenualClipGroupList;

    @Subscribe
    public void onEventCameraConnection(CameraConnectionEvent event) {
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_SELECTED_CHANGED:
                initCamera();
                doGetAllMenualClipSet();
                break;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_clip, savedInstanceState);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doGetAllMenualClipSet();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mVdtCamera != null) {
            doGetAllMenualClipSet();
        }
        mEventBus.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(this);
    }

    private void doGetAllMenualClipSet() {
        if (mVdbRequestQueue == null) {
            return;
        }
        mVdbRequestQueue.add(new ClipSetExRequest(Clip.TYPE_BUFFERED, ClipSetExRequest.FLAG_CLIP_EXTRA | ClipSetExRequest.FLAG_CLIP_ATTR,
            Clip.CLIP_ATTR_MANUALLY, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet clipSet) {
                if (clipSet.getCount() == 0) {
//                        onHandleEmptyCamera();
                    return;
                }
                Logger.t(TAG).d("clipSet number: " + clipSet.getCount());
//                    mAllFootageClipSet = clipSet;
//                    setupClipPlayFragment(clipSet);
//                    //setupClipProgressBar(clipSet);
//                    refreshBookmarkClipSet();
                mRefreshLayout.setRefreshing(false);
                ClipSetGroupHelper helper = new ClipSetGroupHelper(clipSet);
                setupClipSetGroup();
                mAdapter.setClipSetGroup(helper.getClipSetGroup());
            }
        },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("", error);

                }
            }));
    }

    private void setupClipSetGroup() {
        mMenualClipGroupList.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        mAdapter = new ClipSetGroupAdapter(getActivity(), R.layout.item_clip_set_card, mVdbRequestQueue, null, new ClipSetGroupAdapter.OnClipClickListener() {
            @Override
            public void onClipClicked(Clip clip) {
//                popClipPreviewFragment(clip);
            }

            @Override
            public void onClipLongClicked(Clip clip) {

            }
        });

        mAdapter.setMultiSelectedMode(false);

        mMenualClipGroupList.setAdapter(mAdapter);

    }
}
