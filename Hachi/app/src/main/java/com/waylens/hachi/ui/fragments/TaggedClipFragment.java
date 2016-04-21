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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.StackView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.eventbus.events.MenuItemSelectEvent;
import com.waylens.hachi.eventbus.events.MultiSelectEvent;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.EnhancementActivity;
import com.waylens.hachi.ui.activities.LoginActivity;
import com.waylens.hachi.ui.adapters.ClipSetGroupAdapter;
import com.waylens.hachi.utils.ClipSetGroupHelper;
import com.waylens.hachi.utils.Utils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/2/18.
 */
public class TaggedClipFragment extends BaseFragment implements FragmentNavigator {
    private static final String TAG = TaggedClipFragment.class.getSimpleName();
    private static final String ARG_CLIP_SET_TYPE = "clip.set.type";
    private static final String ARG_IS_MULTIPLE_MODE = "is.multiple.mode";
    private static final String ARG_IS_ADD_MORE = "is.add.more";

    public static final String ACTION_RETRIEVE_CLIPS = "action.retrieve.clips";



    private ClipSetGroupAdapter mAdapter;

    private Handler mUiThreadHandler;

    private int mClipSetType;

    private boolean mIsMultipleMode;

    boolean mIsAddMore;

    ActionMode mActionMode;

    private int mDeleteClipCount = 0;

    private LocalBroadcastManager mBroadcastManager;

    private EventBus mEventBus = EventBus.getDefault();


    @Bind(R.id.clipGroupList)
    RecyclerView mRvClipGroupList;


    @Bind(R.id.refreshLayout)
    SwipeRefreshLayout mRefreshLayout;

    @Bind(R.id.llNoBookmark)
    LinearLayout mLlNoBookmark;

    @Bind(R.id.bottomLayout)
    LinearLayout mBottomLayout;

    @Bind(R.id.svBuffer)
    StackView mSvBufferClips;

    @Bind(R.id.tvBufferTime)
    TextView mTvBufferTime;

    @OnClick(R.id.bottomLayout)
    public void onBottomLayoutClicked() {
        ClipSetExRequest request = new ClipSetExRequest(Clip.TYPE_BUFFERED, ClipSetExRequest.FLAG_CLIP_EXTRA,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet response) {
                    launchFootageActivity(response);
                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
    }

    @Subscribe
    public void onEventMenuItemSelected(MenuItemSelectEvent event) {
        switch (event.getMenuItemId()) {
            case R.id.menu_to_enhance:
                toEnhance();
                break;
            case R.id.menu_to_upload:
                toShare();
                break;
            case R.id.menu_to_delete:
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .content(R.string.delete_bookmark_confirm)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            doDeleteSelectedClips();
                        }
                    })
                    .build();
                dialog.show();

                break;
            case -1:
                toggleMultiMode(false);
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void onEventCameraConnection(CameraConnectionEvent event) {
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_SELECTED_CHANGED:
                initCamera();
                initViews();
                break;
        }
    }


    public static TaggedClipFragment newInstance(int clipSetType) {
        return newInstance(clipSetType, false, false);
    }

    public static TaggedClipFragment newInstance(int clipSetType, boolean isMultipleSelectionMode, boolean isAddMore) {
        TaggedClipFragment fragment = new TaggedClipFragment();
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
        View view = createFragmentView(inflater, container, R.layout.fragment_tagged_clip,
            savedInstanceState);
        mUiThreadHandler = new Handler();
        //setupClipSetGroup();
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initViews();
            }
        });
        initViews();
        return view;
    }



    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRefreshLayout.setRefreshing(true);

        mBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        if (mBroadcastManager != null) {
            mBroadcastManager.registerReceiver(localReceiver, new IntentFilter(ACTION_RETRIEVE_CLIPS));
        }
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_add_clip, menu);
    }


    private void initViews() {
        if (mVdtCamera != null) {
            doGetBookmarkClips();
            if (mClipSetType != Clip.TYPE_MARKED) {
                mBottomLayout.setVisibility(View.VISIBLE);
                doGetBufferedClips();
            }
        }
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
            intent.putParcelableArrayListExtra(EnhancementActivity.EXTRA_CLIPS_TO_APPEND, mAdapter.getSelectedClipList());
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        } else {
            ArrayList<Clip> selectedList = mAdapter.getSelectedClipList();
            EnhancementActivity.launch(getActivity(), selectedList, EnhancementActivity.LAUNCH_MODE_ENHANCE);
        }
    }





    private void setupClipSetGroup() {
        int spanCount = mClipSetType == Clip.TYPE_MARKED ? 4 : 2;
        int layoutRes = mClipSetType == Clip.TYPE_MARKED ? R.layout.item_clip_set_grid : R.layout.item_clip_set_card;
        mRvClipGroupList.setLayoutManager(new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL));

        mAdapter = new ClipSetGroupAdapter(getActivity(), layoutRes, mVdbRequestQueue, null, new ClipSetGroupAdapter.OnClipClickListener() {
            @Override
            public void onClipClicked(Clip clip) {
                if (mClipSetType == Clip.TYPE_MARKED) {
                    popClipPreviewFragment(clip);
                } else {
                    ClipSet clipSet = new ClipSet(Clip.TYPE_BUFFERED);
                    clipSet.addClip(clip);
                    launchFootageActivity(clipSet);
                }
            }

            @Override
            public void onClipLongClicked(Clip clip) {
                mIsMultipleMode = true;
                mAdapter.setMultiSelectedMode(true);
//                if (mActionMode == null) {
//                    mActionMode = getActivity().startActionMode(mCABCallback);
//                }

                mEventBus.post(new MultiSelectEvent(true));
            }
        });

        mAdapter.setMultiSelectedMode(mIsMultipleMode);

        mRvClipGroupList.setAdapter(mAdapter);

    }



    private void doGetBookmarkClips() {
        if (mVdbRequestQueue == null) {
            return;
        }

        int flag;
        int attr;

        if (mClipSetType == Clip.TYPE_MARKED) {
            flag = ClipSetExRequest.FLAG_CLIP_EXTRA;
            attr = 0;
        } else {
            flag = ClipSetExRequest.FLAG_CLIP_EXTRA | ClipSetExRequest.FLAG_CLIP_ATTR;
            attr = Clip.CLIP_ATTR_MANUALLY;
        }

        mVdbRequestQueue.add(new ClipSetExRequest(mClipSetType, flag, attr,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet clipSet) {
                    mRefreshLayout.setRefreshing(false);
                    if (clipSet.getCount() == 0) {
                        mLlNoBookmark.setVisibility(View.VISIBLE);
                        mRvClipGroupList.setVisibility(View.GONE);
                    } else {
                        mLlNoBookmark.setVisibility(View.GONE);
                        mRvClipGroupList.setVisibility(View.VISIBLE);
                        setupClipSetGroup();
                        ClipSetGroupHelper helper = new ClipSetGroupHelper(clipSet);
                        mAdapter.setClipSetGroup(helper.getClipSetGroup());
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

    private void doGetBufferedClips() {
        if (mVdbRequestQueue == null) {
            return;
        }

        ClipSetExRequest request = new ClipSetExRequest(Clip.TYPE_BUFFERED, ClipSetExRequest.FLAG_CLIP_EXTRA, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet response) {
                BufferClipAdapter adapter = new BufferClipAdapter(response);
                mSvBufferClips.setAdapter(adapter);

                if (response.getCount() > 0) {
                    Clip clip = response.getClip(0);
                    Date date = new Date((long)clip.getDate() * 1000);
                    PrettyTime t = new PrettyTime(new Date());

                    mTvBufferTime.setText(t.format(date));
                }

            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });

        mVdbRequestQueue.add(request);
    }




    private void popClipPreviewFragment(Clip clip) {
        ArrayList<Clip> clipList = new ArrayList<>();
        clipList.add(clip);
        Logger.t(TAG).d("clip id: " + clip.cid.toString());
        EnhancementActivity.launch(getActivity(), clipList, EnhancementActivity.LAUNCH_MODE_QUICK_VIEW);
    }

    private void launchFootageActivity(ClipSet clipSet) {

        ClipSetManager.getManager().updateClipSet(ClipSetManager.CLIP_SET_TYPE_MENUAL, clipSet);
        FootageActivity.launch(getActivity(), ClipSetManager.CLIP_SET_TYPE_MENUAL);
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

    private void toggleMultiMode(boolean isMultiMode) {
        mIsMultipleMode = isMultiMode;
        mAdapter.setMultiSelectedMode(mIsMultipleMode);

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


    private class BufferClipAdapter extends BaseAdapter {

        private final ClipSet mClipSet;

        public BufferClipAdapter(ClipSet clipSet) {
            this.mClipSet = clipSet;
        }

        @Override
        public int getCount() {
            return mClipSet == null ? 0 : mClipSet.getCount();
        }

        @Override
        public Object getItem(int position) {
            return mClipSet.getClip(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Clip clip = mClipSet.getClip(position);
            ImageView iv = new ImageView(getContext());
            int imageSize = (int)Utils.dp2px(getContext(), 64);
            iv.setLayoutParams(new LinearLayout.LayoutParams(imageSize, imageSize));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

            ClipPos clipPos = new ClipPos(clip);

            mVdbImageLoader.displayVdbImage(clipPos, iv);

            return iv;
        }
    }
}
