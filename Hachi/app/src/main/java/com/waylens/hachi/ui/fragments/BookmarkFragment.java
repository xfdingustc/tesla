package com.waylens.hachi.ui.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.ui.activities.EnhancementActivity;
import com.waylens.hachi.ui.activities.LoginActivity;
import com.waylens.hachi.ui.adapters.ClipSetGroupAdapter;
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

/**
 * Created by Xiaofei on 2016/2/18.
 */
public class BookmarkFragment extends BaseFragment implements FragmentNavigator {
    private static final String TAG = BookmarkFragment.class.getSimpleName();
    private static final String ARG_CLIP_SET_TYPE = "clip.set.type";
    private static final String ARG_IS_MULTIPLE_MODE = "is.multiple.mode";
    private static final String ARG_IS_ADD_MORE = "is.add.more";

    public static final String ACTION_RETRIEVE_CLIPS = "action.retrieve.clips";

    private Map<String, ClipSet> mClipSetGroup = new HashMap<>();

    private ClipSetGroupAdapter mAdapter;

    private Handler mUiThreadHandler;

    private int mClipSetType;

    private boolean mIsMultipleMode;

    boolean mIsAddMore;

    ActionMode mActionMode;

    private int mDeleteClipCount = 0;

    private LocalBroadcastManager mBroadcastManager;


    @Bind(R.id.clipGroupList)
    RecyclerView mRvClipGroupList;


    @Bind(R.id.refreshLayout)
    SwipeRefreshLayout mRefreshLayout;

    @Bind(R.id.rootViewSwitcher)
    ViewSwitcher mRootViewSwitcher;




    public static BookmarkFragment newInstance(int clipSetType) {
        return newInstance(clipSetType, false, false);
    }

    public static BookmarkFragment newInstance(int clipSetType, boolean isMultipleSelectionMode, boolean isAddMore) {
        BookmarkFragment fragment = new BookmarkFragment();
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

        if (mIsAddMore) {
            setHasOptionsMenu(true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_bookmark,
                savedInstanceState);
        mUiThreadHandler = new Handler();
        setupClipSetGroup();
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doGetBookmarkClips();
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRefreshLayout.setRefreshing(true);

        if (getCamera() != null) {
            doGetBookmarkClips();
        }

        mBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        if (mBroadcastManager != null) {
            mBroadcastManager.registerReceiver(localReceiver, new IntentFilter(ACTION_RETRIEVE_CLIPS));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_add_clip, menu);
    }

    /**
     * Don't remove
     * This callback is used by "Enhance -> Add more clips"
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_to_enhance:
                toEnhance();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void doDeleteSelectedClips() {
        ArrayList<Clip> selectedList = mAdapter.getSelectedClipList();
        final int toDeleteClipCount = selectedList.size();
        mDeleteClipCount = 0;

        for (Clip clip : selectedList) {
            ClipDeleteRequest request = new ClipDeleteRequest(clip.cid, new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    mDeleteClipCount++;
                    Logger.t(TAG).d("" + mDeleteClipCount + " clips deleted");
                    if (mDeleteClipCount == toDeleteClipCount) {
                        doGetBookmarkClips();
                    }
                }
            }, new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {

                }
            });

            mVdbRequestQueue.add(request);
        }
    }

    private void toEnhance() {
        if (mIsAddMore) {
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra(EnhancementActivity.EXTRA_CLIPS_TO_APPEND,
                    mAdapter.getSelectedClipList());
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        } else {
            ArrayList<Clip> selectedList = mAdapter.getSelectedClipList();
            EnhancementActivity.launch(getActivity(), selectedList, EnhancementActivity.LAUNCH_MODE_ENHANCE);
        }
    }

    @Override
    public void onCameraVdbConnected(VdtCamera camera) {
        super.onCameraVdbConnected(camera);
        mUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {

                mClipSetGroup.clear();
                doGetBookmarkClips();
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

    private void doGetBookmarkClips() {

        if (mVdbRequestQueue == null) {
            return;
        }
        mClipSetGroup.clear();

        mVdbRequestQueue.add(new ClipSetExRequest(mClipSetType, ClipSetExRequest.FLAG_CLIP_EXTRA,
                new VdbResponse.Listener<ClipSet>() {
                    @Override
                    public void onResponse(ClipSet clipSet) {
                        mRefreshLayout.setRefreshing(false);
                        if (clipSet.getCount() == 0) {
                            if (mRootViewSwitcher.getDisplayedChild() == 0) {
                                mRootViewSwitcher.showNext();
                            }
                        } else {
                            if (mRootViewSwitcher.getDisplayedChild() == 1) {
                                Logger.t(TAG).d("show previous");
                                mRootViewSwitcher.showPrevious();
                            }
                            calculateClipSetGroup(clipSet);
                            setupClipSetGroupView();
                        }

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
                return rhs.getClip(0).getDate() - lhs.getClip(0).getDate();
            }
        });

        mAdapter.setClipSetGroup(clipSetGroup);
    }

    private void popClipPreviewFragment(Clip clip) {
        ArrayList<Clip> clipList = new ArrayList<>();
        clipList.add(clip);
        EnhancementActivity.launch(getActivity(), clipList, EnhancementActivity.LAUNCH_MODE_QUICK_VIEW);
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
                    toShare();
                    return false;
                case R.id.menu_to_delete:
                    doDeleteSelectedClips();
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

    void toShare() {
        if (SessionManager.getInstance().isLoggedIn()) {
            ArrayList<Clip> selectedList = mAdapter.getSelectedClipList();
            EnhancementActivity.launch(getActivity(), selectedList, EnhancementActivity.LAUNCH_MODE_SHARE);
        } else {
            LoginActivity.launch(getActivity());
        }
    }

    BroadcastReceiver localReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            doGetBookmarkClips();
        }
    };
}
