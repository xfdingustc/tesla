package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.eventbus.events.MarkLiveMsgEvent;
import com.waylens.hachi.eventbus.events.MenuItemSelectEvent;
import com.waylens.hachi.library.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.library.vdb.Clip;
import com.waylens.hachi.library.vdb.ClipActionInfo;
import com.waylens.hachi.library.vdb.ClipSet;
import com.waylens.hachi.library.vdb.ClipSetManager;
import com.waylens.hachi.presenter.ClipGridListPresenter;
import com.waylens.hachi.presenter.impl.ClipGridListPresenterImpl;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.clips.enhance.EnhanceActivity;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.clips.preview.PreviewActivity;
import com.waylens.hachi.ui.clips.share.ShareActivity;
import com.waylens.hachi.ui.fragments.BaseLazyFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.utils.ClipSetGroupHelper;
import com.waylens.hachi.view.ClipGridListView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/2/18.
 */
public class ClipGridListFragment extends BaseLazyFragment implements FragmentNavigator, ClipGridListView {

    private static final String ARG_CLIP_SET_TYPE = "clip.set.type";
    private static final String ARG_IS_MULTIPLE_MODE = "is.multiple.mode";
    private static final String ARG_IS_ADD_MORE = "is.add.more";

    public static final String ACTION_RETRIEVE_CLIPS = "action.retrieve.clips";

    private ClipSetGroupAdapter mAdapter;

    private int mClipSetType;

    private boolean mIsMultipleMode;

    private boolean mIsAddMore;


    private EventBus mEventBus = EventBus.getDefault();

    private ActionMode mActionMode;

    private ClipGridListPresenter mPresenter = null;


    @BindView(R.id.clipGroupList)
    RecyclerView mRvClipGroupList;

    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout mRefreshLayout;


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraConnection(CameraConnectionEvent event) {
        Logger.t(TAG).d("camera connection event: " + event.getWhat());
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_SELECTED_CHANGED:
                mPresenter.loadClipSet(false);
                break;
            case CameraConnectionEvent.VDT_CAMERA_DISCONNECTED:
                showCameraDisconnect();
                break;

            case CameraConnectionEvent.VDT_CAMERA_CONNECTED:
                hideCameraDisconnect();
                mPresenter.loadClipSet(false);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMarkLiveMsg(MarkLiveMsgEvent event) {
        if (event.getClipActionInfo().action == ClipActionInfo.CLIP_ACTION_CREATED) {
            mPresenter.loadClipSet(false);
        }
    }

    @Subscribe
    public void onEventMenuItemSelected(MenuItemSelectEvent event) {
        switch (event.getMenuItemId()) {
            case R.id.menu_to_enhance:
                toEnhance();
                break;

            default:
                break;
        }
    }


    public static ClipGridListFragment newInstance(int clipSetType) {
        return newInstance(clipSetType, false, false);
    }

    public static ClipGridListFragment newInstance(int clipSetType, boolean isMultipleSelectionMode, boolean isAddMore) {
        ClipGridListFragment fragment = new ClipGridListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLIP_SET_TYPE, clipSetType);
        args.putBoolean(ARG_IS_MULTIPLE_MODE, isMultipleSelectionMode);
        args.putBoolean(ARG_IS_ADD_MORE, isAddMore);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected String getRequestTag() {
        return TAG;
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


    @Override
    protected void init() {
        int flag;
        int attr;

        if (
            true || mClipSetType == Clip.TYPE_MARKED) {
            flag = ClipSetExRequest.FLAG_CLIP_EXTRA;
            attr = 0;
        } else {
            flag = ClipSetExRequest.FLAG_CLIP_EXTRA | ClipSetExRequest.FLAG_CLIP_ATTR;
            attr = Clip.CLIP_ATTR_MANUALLY;
        }
        mPresenter = new ClipGridListPresenterImpl(getActivity(), TAG, mClipSetType, flag, attr, this);
        initViews();
    }

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.fragment_tagged_clip;
    }

    @Override
    protected int getEmptyViewResId() {
        if (mClipSetType == Clip.TYPE_MARKED) {
            return R.layout.layout_no_bookmark;
        } else {
            return R.layout.layout_no_footage;
        }
    }

    public void onDeselected() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public void refreshClipiSet(ClipSet clipSet) {
        if (mRefreshLayout != null) {
            mRefreshLayout.setRefreshing(false);
        }

        ClipSetGroupHelper helper = new ClipSetGroupHelper(clipSet);
        mAdapter.setClipSetGroup(helper.getClipSetGroup());
    }


    @Override
    public void showError(String msg) {

    }


    @Override
    protected void onFirstUserVisible() {


    }

    @Override
    protected boolean isBindEventBusHere() {
        return false;
    }

    @Override
    protected View getLoadingTargetView() {
        return mRefreshLayout;
    }

    private void initViews() {

        mRefreshLayout.setColorSchemeResources(R.color.style_color_primary, android.R.color.holo_green_light,
            android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPresenter.loadClipSet(true);
            }
        });

        int spanCount = mClipSetType == Clip.TYPE_MARKED ? 4 : 2;
        int layoutRes = mClipSetType == Clip.TYPE_MARKED ? R.layout.item_clip_set_grid : R.layout.item_clip_set_card;
        mRvClipGroupList.setLayoutManager(new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL));
        mAdapter = new ClipSetGroupAdapter(getActivity(), layoutRes, mVdbRequestQueue, null, new ClipSetGroupAdapter.OnClipClickListener() {
            @Override
            public void onClipClicked(Clip clip) {

                if (mIsMultipleMode || clip == null) {
                    return;
                }
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


                mRefreshLayout.setEnabled(false);
                if (mActionMode == null) {
                    mActionMode = getActivity().startActionMode(mCABCallback);
                    updateActionMode();
                }


            }

            @Override
            public void onSelectedClipListChanged(List<Clip> clipList) {
                if (mActionMode != null) {
                    if (mAdapter.getSelectedClipList().size() == 0) {
                        mActionMode.finish();
                        return;
                    } else {
                        updateActionMode();
                    }
                }
            }
        });

        mAdapter.setMultiSelectedMode(mIsMultipleMode);

        mRvClipGroupList.setAdapter(mAdapter);


    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRefreshLayout.setRefreshing(true);

    }


    @Override
    public void onStart() {
        super.onStart();
        mEventBus.register(this);
        if (mRefreshLayout != null) {
            mRefreshLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPresenter.loadClipSet(false);
                }
            }, 200);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(this);
    }





    private void doDeleteSelectedClips() {
        ArrayList<Clip> selectedList = new ArrayList<>();
        selectedList.addAll(mAdapter.getSelectedClipList());

        mPresenter.deleteClipList(selectedList);

    }


    private ActionMode.Callback mCABCallback = new ActionMode.Callback() {
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
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_to_enhance:
                    toEnhance();
                    mode.finish();
                    break;
                case R.id.menu_to_upload:
                    toShare();
                    mode.finish();
                    break;
                case R.id.menu_to_delete:
                    MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .content(R.string.delete_bookmark_confirm)
                        .positiveText(R.string.ok)
                        .negativeText(R.string.cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                doDeleteSelectedClips();
                                mode.finish();
                            }
                        })
                        .build();
                    dialog.show();
                    break;
                case R.id.menu_selete_all:
                    mAdapter.toggleSelectAll(true);
                    break;
                case R.id.menu_deselete_all:
                    mAdapter.toggleSelectAll(false);
                    mode.finish();
                    break;
                default:
                    break;
            }


            return true;

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
            mRefreshLayout.setEnabled(true);

        }
    };


    private void updateActionMode() {
        if (mActionMode != null) {
            mActionMode.setTitle("" + mAdapter.getSelectedClipList().size());
            MenuItem uploadMenuItem = mActionMode.getMenu().findItem(R.id.menu_to_upload);
            if (mAdapter.getSelectedClipList().size() == 1) {
                uploadMenuItem.setVisible(true);
            } else {
                uploadMenuItem.setVisible(false);
            }


        }
    }




    private void popClipPreviewFragment(Clip clip) {
        ArrayList<Clip> clipList = new ArrayList<>();
        clipList.add(clip);
        mRefreshLayout.setRefreshing(true);
        final int playlistId = 0x100;
        PlayListEditor playListEditor = new PlayListEditor(mVdbRequestQueue, playlistId);
        playListEditor.build(clipList, new PlayListEditor.OnBuildCompleteListener() {
            @Override
            public void onBuildComplete(ClipSet clipSet) {
                mRefreshLayout.setRefreshing(false);
                PreviewActivity.launch(getActivity(), playlistId);

            }
        });
    }

    private void launchFootageActivity(ClipSet clipSet) {
        ClipSetManager.getManager().updateClipSet(ClipSetManager.CLIP_SET_TYPE_MANUAL, clipSet);
        FootageActivity.launch(getActivity(), ClipSetManager.CLIP_SET_TYPE_MANUAL);
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


    private void toEnhance() {
        if (mIsAddMore) {
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra(EnhanceActivity.EXTRA_CLIPS_TO_APPEND, mAdapter.getSelectedClipList());
            Logger.t(TAG).d("add clip size: " + mAdapter.getSelectedClipList().size());
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        } else {
            ArrayList<Clip> selectedList = mAdapter.getSelectedClipList();

            mRefreshLayout.setRefreshing(true);
            Logger.t(TAG).d("selected list size: " + selectedList.size());

            final int playlistId = 0x100;
            PlayListEditor playListEditor = new PlayListEditor(mVdbRequestQueue, playlistId);
            playListEditor.build(selectedList, new PlayListEditor.OnBuildCompleteListener() {
                @Override
                public void onBuildComplete(ClipSet clipSet) {

                    mRefreshLayout.setRefreshing(false);
                    EnhanceActivity.launch(getActivity(), playlistId);
                }
            });
        }
    }

    private void toShare() {
        if (SessionManager.getInstance().isLoggedIn()) {

            mRefreshLayout.setRefreshing(true);
            ArrayList<Clip> selectedList = mAdapter.getSelectedClipList();

            final int playlistId = 0x100;
            PlayListEditor playListEditor = new PlayListEditor(mVdbRequestQueue, playlistId);
            playListEditor.build(selectedList, new PlayListEditor.OnBuildCompleteListener() {
                @Override
                public void onBuildComplete(ClipSet clipSet) {
                    mRefreshLayout.setRefreshing(false);
                    ShareActivity.launch(getActivity(), playlistId, -1);
                }
            });
        } else {
            AuthorizeActivity.launch(getActivity());
        }
    }


}
