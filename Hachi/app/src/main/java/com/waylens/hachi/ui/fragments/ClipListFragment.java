package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.ui.activities.EnhancementActivity;
import com.waylens.hachi.ui.adapters.ClipSetGroupAdapter;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.ClipUrlProvider;
import com.waylens.hachi.ui.fragments.clipplay2.UrlProvider;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/2/18.
 */
public class ClipListFragment extends BaseFragment {
    private static final String TAG = ClipListFragment.class.getSimpleName();
    private static final String ARG_CLIP_SET_TYPE = "clip.set.type";
    private static final String ARG_IS_MULTIPLE_MODE = "is.multiple.mode";

    private Map<String, ClipSet> mClipSetGroup = new HashMap<>();

    private ClipSetGroupAdapter mAdapter;


    @Bind(R.id.clipGroupList)
    RecyclerView mRvClipGroupList;


    @Bind(R.id.refreshLayout)
    SwipeRefreshLayout mRefreshLayout;

    @Bind(R.id.bottomSheet)
    LinearLayout mBottomSheet;

    @OnClick(R.id.btnEnhance)
    public void onBtnEnhanceClicked() {
        ArrayList selectedList = mAdapter.getSelectedClipList();
        EnhancementActivity.launch(getActivity(), selectedList);

    }

    private Handler mUiThreadHandler;

    private int mClipSetType;

    private boolean mIsMultipleMode;

    public static ClipListFragment newInstance(int clipSetType, boolean isMultipleSelectionMode) {
        ClipListFragment fragment = new ClipListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLIP_SET_TYPE, clipSetType);
        args.putBoolean(ARG_IS_MULTIPLE_MODE, isMultipleSelectionMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mClipSetType = args.getInt(ARG_CLIP_SET_TYPE, Clip.TYPE_MARKED);
            mIsMultipleMode = args.getBoolean(ARG_IS_MULTIPLE_MODE, false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mRefreshLayout.setRefreshing(true);

        if (getCamera() != null) {
            retrieveSharableClips();
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_clip_list,
                savedInstanceState);
        mUiThreadHandler = new Handler();
        setupClipSetGroup();
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                retrieveSharableClips();
            }
        });
        return view;
    }

    @Override
    public void onCameraVdbConnected(VdtCamera camera) {
        super.onCameraVdbConnected(camera);
        mUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {

                mClipSetGroup.clear();
                retrieveSharableClips();
            }
        });

    }

    private void setupClipSetGroup() {
        mRvClipGroupList.setLayoutManager(new StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL));

        mAdapter = new ClipSetGroupAdapter(getActivity(), null, new ClipSetGroupAdapter.OnClipClickListener() {
            @Override
            public void onClipClicked(Clip clip) {
                popClipPreviewFragment(clip);
            }

            @Override
            public void onClipLongClicked(Clip clip) {
                mAdapter.setMultiSelectedMode(true);
                popupBottomSheet();
            }
        });

        mAdapter.setMultiSelectedMode(mIsMultipleMode);

        mRvClipGroupList.setAdapter(mAdapter);

    }

    private void retrieveSharableClips() {
        mClipSetGroup.clear();
        mVdbRequestQueue.add(new ClipSetRequest(mClipSetType, ClipSetRequest.FLAG_CLIP_EXTRA,
                new VdbResponse.Listener<ClipSet>() {
                    @Override
                    public void onResponse(ClipSet clipSet) {
                        mRefreshLayout.setRefreshing(false);
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

            String clipDataString = clip.getDateString();
            ClipSet oneClipSet = mClipSetGroup.get(clipDataString);
            if (oneClipSet == null) {
                oneClipSet = new ClipSet(clipSet.getType());
                mClipSetGroup.put(clipDataString, oneClipSet);
            }

            oneClipSet.addClip(clip);

        }
    }

    private void setupClipSetGroupView() {

        List<ClipSet> clipSetGroup = new ArrayList<>();
        Iterator iter = mClipSetGroup.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            clipSetGroup.add((ClipSet) entry.getValue());
        }

        Collections.sort(clipSetGroup, new Comparator<ClipSet>() {
            @Override
            public int compare(ClipSet lhs, ClipSet rhs) {
                return rhs.getClip(0).clipDate - lhs.getClip(0).clipDate;
            }
        });

        mAdapter.setClipSetGroup(clipSetGroup);
    }

    private void popClipPreviewFragment(Clip clip) {

        ClipPlayFragment.Config config = new ClipPlayFragment.Config();
        config.progressBarStyle = ClipPlayFragment.Config.PROGRESS_BAR_STYLE_SINGLE;


        ClipSet clipSet = new ClipSet(0x107);
        clipSet.addClip(clip);


        UrlProvider vdtUriProvider = new ClipUrlProvider(mVdbRequestQueue, clip);
        ClipPlayFragment fragment = ClipPlayFragment.newInstance(getCamera(), clipSet,
                vdtUriProvider, config);

        fragment.show(getFragmentManager(), "ClipPlayFragment");

    }

    private void popupBottomSheet() {
//        BottomSheet.Builder builder = new BottomSheet.Builder(getActivity());
//        BottomSheet bottomSheet = builder
//            .grid()
//            .darkTheme()
//            .sheet(R.menu.menu_video_fragment_bottom_sheet)
//            .listener(new MenuItem.OnMenuItemClickListener() {
//                @Override
//                public boolean onMenuItemClick(MenuItem item) {
//                    //return onBottomSheetMenuItemClicked(item);
//                    return true;
//                }
//            }).build();
//
//        bottomSheet.setCanceledOnTouchOutside(false);
//
//        bottomSheet.show();
        mBottomSheet.setVisibility(View.VISIBLE);

    }
}
