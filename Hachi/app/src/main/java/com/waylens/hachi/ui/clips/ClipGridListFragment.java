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
import android.widget.LinearLayout;
import android.widget.StackView;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.eventbus.events.MarkLiveMsgEvent;
import com.waylens.hachi.eventbus.events.MenuItemSelectEvent;
import com.waylens.hachi.presenter.ClipGridListPresenter;
import com.waylens.hachi.presenter.impl.ClipGridListPresenterImpl;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.clips.enhance.EnhanceActivity;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor2;
import com.waylens.hachi.ui.clips.preview.PreviewActivity;
import com.waylens.hachi.ui.clips.share.ShareActivity;
import com.waylens.hachi.ui.fragments.BaseLazyFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.utils.ClipSetGroupHelper;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipActionInfo;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;
import com.waylens.hachi.view.ClipGridListView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/2/18.
 */
public class ClipGridListFragment extends BaseLazyFragment implements FragmentNavigator, ClipGridListView {

    private static final String ARG_CLIP_SET_TYPE = "clip.set.type";
    private static final String ARG_IS_MULTIPLE_MODE = "is.multiple.mode";
    private static final String ARG_IS_ADD_MORE = "is.add.more";

    public static final String ACTION_RETRIEVE_CLIPS = "action.retrieve.clips";

    private static final int ROOT_CHILD_CLIPSET = 0;
    private static final int ROOT_CHILD_CAMERA_DISCONNECT = 1;
    private static final int ROOT_CHILD_TIMEOUT = 2;

    private static final int DEFAULT_TIMEOUT_SECOND = 10;

    private ClipSetGroupAdapter mAdapter;

    private int mClipSetType;

    private boolean mIsMultipleMode;

    private boolean mIsAddMore;


    private EventBus mEventBus = EventBus.getDefault();

    private ActionMode mActionMode;

    private ClipGridListPresenter mPresenter = null;


    @BindView(R.id.rootViewAnimator)
    ViewAnimator mRootViewAnimator;

    @BindView(R.id.clipGroupList)
    RecyclerView mRvClipGroupList;


    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout mRefreshLayout;

    @BindView(R.id.llNoBookmark)
    ViewSwitcher mVsNoBookmark;

    @BindView(R.id.bottomLayout)
    LinearLayout mBottomLayout;

    @BindView(R.id.svBuffer)
    StackView mSvBufferClips;

    @BindView(R.id.tvBufferTime)
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

    @OnClick(R.id.btn_retry)
    public void onBtnRetryClicked() {
        doGetClipSet();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraConnection(CameraConnectionEvent event) {
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_SELECTED_CHANGED:
                doGetClipSet();
                break;
            case CameraConnectionEvent.VDT_CAMERA_DISCONNECTED:
                showRootViewChild(ROOT_CHILD_CAMERA_DISCONNECT);
                break;

            case CameraConnectionEvent.VDT_CAMERA_CONNECTED:
                doGetClipSet();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMarkLiveMsg(MarkLiveMsgEvent event) {
        if (event.getClipActionInfo().action == ClipActionInfo.CLIP_ACTION_CREATED) {
            doGetClipSet();
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

        if (true || mClipSetType == Clip.TYPE_MARKED) {
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

    public void onDeselected() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public void refreshClipiSet(ClipSet clipSet) {
        Logger.t(TAG).d("get clip set: " + clipSet.getCount());
        showRootViewChild(ROOT_CHILD_CLIPSET);
        mRefreshLayout.setRefreshing(false);
        if (clipSet.getCount() == 0) {
            mVsNoBookmark.setVisibility(View.VISIBLE);
            mRvClipGroupList.setVisibility(View.GONE);

        } else {
            mVsNoBookmark.setVisibility(View.GONE);
            mRvClipGroupList.setVisibility(View.VISIBLE);
            ClipSetGroupHelper helper = new ClipSetGroupHelper(clipSet);
            showRootViewChild(ROOT_CHILD_CLIPSET);
            int spanCount = mClipSetType == Clip.TYPE_MARKED ? 4 : 2;
            int layoutRes = mClipSetType == Clip.TYPE_MARKED ? R.layout.item_clip_set_grid : R.layout.item_clip_set_card;
            mRvClipGroupList.setLayoutManager(new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL));
            mAdapter = new ClipSetGroupAdapter(getActivity(), layoutRes, mVdbRequestQueue, helper.getClipSetGroup(), new ClipSetGroupAdapter.OnClipClickListener() {
                @Override
                public void onClipClicked(Clip clip) {
                    if (mActionMode != null) {
                        if (mAdapter.getSelectedClipList().size() == 0) {
                            mActionMode.finish();
                            return;
                        } else {
                            updateActionMode();
                        }
                    }


                    if (mIsMultipleMode && clip == null) {

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
            });

            mAdapter.setMultiSelectedMode(mIsMultipleMode);

            mRvClipGroupList.setAdapter(mAdapter);
        }
    }

    @Override
    public void showLoading(String msg) {

    }

    @Override
    public void hideLoading() {

    }

    @Override
    public void showError(String msg) {

    }

    @Override
    protected void onFirstUserVisible() {

        doGetClipSet();
    }

    @Override
    protected boolean isBindEventBusHere() {
        return false;
    }

    private void initViews() {
        if (mClipSetType == Clip.TYPE_MARKED) {
            mVsNoBookmark.showNext();
        }
        mRefreshLayout.setColorSchemeResources(R.color.style_color_primary, android.R.color.holo_green_light,
            android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doGetClipSet();
            }
        });


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
    }

    @Override
    public void onStop() {
        super.onStop();
        mEventBus.unregister(this);
    }


    private void doGetClipSet() {
        mPresenter.loadClipSet(false);
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

    private void showRootViewChild(int child) {
        mRootViewAnimator.setDisplayedChild(child);
    }


    private void popClipPreviewFragment(Clip clip) {
        ArrayList<Clip> clipList = new ArrayList<>();
        clipList.add(clip);
        Logger.t(TAG).d("clip id: " + clip.cid.toString());
//        EnhancementActivity.launch(getActivity(), clipList, EnhancementActivity.LAUNCH_MODE_QUICK_VIEW);
        PreviewActivity.launch(getActivity(), clipList);
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
            PlayListEditor2 playListEditor2 = new PlayListEditor2(mVdbRequestQueue, playlistId);
            playListEditor2.build(selectedList, new PlayListEditor2.OnBuildCompleteListener() {
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
//            EnhancementActivity.launch(getActivity(), selectedList, EnhancementActivity.LAUNCH_MODE_SHARE);
//            ShareActivity.launch(getActivity());
            final int playlistId = 0x100;
            PlayListEditor2 playListEditor2 = new PlayListEditor2(mVdbRequestQueue, playlistId);
            playListEditor2.build(selectedList, new PlayListEditor2.OnBuildCompleteListener() {
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
