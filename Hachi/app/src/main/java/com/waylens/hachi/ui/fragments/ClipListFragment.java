package com.waylens.hachi.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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
import com.waylens.hachi.vdb.ClipSetManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/2/18.
 */
public class ClipListFragment extends BaseFragment implements FragmentNavigator {
    private static final String TAG = ClipListFragment.class.getSimpleName();
    private static final String ARG_CLIP_SET_TYPE = "clip.set.type";
    private static final String ARG_IS_MULTIPLE_MODE = "is.multiple.mode";
    private static final String ARG_IS_ADD_MORE = "is.add.more";

    private Map<String, ClipSet> mClipSetGroup = new HashMap<>();

    private ClipSetGroupAdapter mAdapter;


    @Bind(R.id.clipGroupList)
    RecyclerView mRvClipGroupList;


    @Bind(R.id.refreshLayout)
    SwipeRefreshLayout mRefreshLayout;


    private Handler mUiThreadHandler;

    private int mClipSetType;

    private boolean mIsMultipleMode;

    boolean mIsAddMore;

    ActionMode mActionMode;

    public static ClipListFragment newInstance(int clipSetType) {
        ClipListFragment fragment = new ClipListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLIP_SET_TYPE, clipSetType);
        args.putBoolean(ARG_IS_MULTIPLE_MODE, false);
        args.putBoolean(ARG_IS_ADD_MORE, false);
        fragment.setArguments(args);
        return fragment;
    }

    public static ClipListFragment newInstance(int clipSetType, boolean isMultipleSelectionMode, boolean isAddMore) {
        ClipListFragment fragment = new ClipListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLIP_SET_TYPE, clipSetType);
        args.putBoolean(ARG_IS_MULTIPLE_MODE, isMultipleSelectionMode);
        args.putBoolean(ARG_IS_ADD_MORE, isAddMore);
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
            mIsAddMore = args.getBoolean(ARG_IS_ADD_MORE, false);
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRefreshLayout.setRefreshing(true);

        if (getCamera() != null) {
            retrieveSharableClips();
        }

        if (mIsAddMore && mActionMode == null) {
            mActionMode = getActivity().startActionMode(mCABCallback);
        }
    }

    void toEnhance() {
        if (mIsAddMore) {
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra("clips.more", mAdapter.getSelectedClipList());
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        } else {
            ArrayList<Clip> selectedList = mAdapter.getSelectedClipList();
            ClipSet clipSet = new ClipSet(Clip.TYPE_TEMP);
            for (Clip clip : selectedList) {
                clipSet.addClip(clip);
            }
            ClipSetManager.getManager().updateClipSet(ClipSetManager.CLIP_SET_TYPE_ENHANCE, clipSet);
            EnhancementActivity.launch(getActivity(), ClipSetManager.CLIP_SET_TYPE_ENHANCE);
        }
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
                mIsMultipleMode = true;
                mAdapter.setMultiSelectedMode(true);
                if (mActionMode == null) {
                    mActionMode = getActivity().startActionMode(mCABCallback);
                }
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
        config.clipMode = ClipPlayFragment.Config.ClipMode.SINGLE;

        ClipSet clipSet = new ClipSet(Clip.TYPE_TEMP);
        clipSet.addClip(clip);

        ClipSetManager.getManager().updateClipSet(ClipSetManager.CLIP_SET_TYPE_ENHANCE, clipSet);

        UrlProvider vdtUriProvider = new ClipUrlProvider(mVdbRequestQueue, clip);
        ClipPlayFragment fragment = ClipPlayFragment.newInstance(getCamera(), ClipSetManager.CLIP_SET_TYPE_ENHANCE,
                vdtUriProvider, config);

        fragment.show(getFragmentManager(), "ClipPlayFragment");

    }


    @Override
    public boolean onInterceptBackPressed() {
        if (mIsMultipleMode) {
            mIsMultipleMode = false;
            mAdapter.setMultiSelectedMode(false);
            return true;
        }
        return false;
    }

    ActionMode.Callback mCABCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_clip_list, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (mIsAddMore) {
                MenuItem menuItem = menu.findItem(R.id.menu_to_upload);
                if (menuItem != null) {
                    menuItem.setVisible(false);
                }
                menuItem = menu.findItem(R.id.menu_to_delete);
                if (menuItem != null) {
                    menuItem.setVisible(false);
                }
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_to_enhance:
                    toEnhance();
                    return true;
                case R.id.menu_to_upload:
                    return false;
                case R.id.menu_to_delete:
                    return false;
                default:
                    return false;
            }

        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            if (mIsAddMore) {
                getActivity().finish();
            } else if (mIsMultipleMode) {
                mIsMultipleMode = false;
                mAdapter.setMultiSelectedMode(false);
            }
        }
    };
}
