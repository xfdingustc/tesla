package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.camera.events.CameraConnectionEvent;
import com.waylens.hachi.camera.events.MarkLiveMsgEvent;
import com.waylens.hachi.eventbus.events.MenuItemSelectEvent;
import com.waylens.hachi.presenter.ClipGridListPresenter;
import com.waylens.hachi.presenter.impl.ClipGridListPresenterImpl;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.remix.AvrproFilter;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipActionInfo;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.clips.enhance.EnhanceActivity;
import com.waylens.hachi.ui.clips.event.ActionButtonEvent;
import com.waylens.hachi.ui.clips.event.ToggleFabEvent;
import com.waylens.hachi.ui.clips.playlist.PlayListEditor;
import com.waylens.hachi.ui.clips.preview.PreviewActivity;
import com.waylens.hachi.ui.clips.share.ShareActivity;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.ui.fragments.BaseLazyFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.utils.ClipSetGroupHelper;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.rxjava.RxBus;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;
import com.waylens.hachi.view.ClipGridListView;

import net.steamcrafted.loadtoast.LoadToast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by Xiaofei on 2016/2/18.
 */
public class ClipGridListFragment extends BaseLazyFragment implements FragmentNavigator, ClipGridListView {
    private static final String ARG_CLIP_SET_TYPE = "clip.set.type";
    private static final String ARG_IS_MULTIPLE_MODE = "is.multiple.mode";
    private static final String ARG_IS_ADD_MORE = "is.add.more";
    private static final String ARG_IS_TO_SHARE = "is.to.share";

    public static final String ACTION_RETRIEVE_CLIPS = "action.retrieve.clips";

    private ClipSetGroupAdapter mAdapter;

    private int mClipSetType;

    private boolean mIsMultipleMode;

    private boolean mIsRemixMode;

    private boolean mIsAddMore;

    private boolean mIsToShare;

    private int mRemixLength = 20;

    private EventBus mEventBus = EventBus.getDefault();

    private ActionMode mActionMode;

    private ClipGridListPresenter mPresenter = null;

    private SeekBar mLengthSeekbar;

    private TextView mTvSmartRemix;

    private TextView mTvTextEnd;

    private Subscription mFabSubscription;

    private ClipSet mClipSet;

    private boolean mIsExposed = false;

    private LoadToast mLoadToast;

    @BindView(R.id.clipGroupList)
    RecyclerView mRvClipGroupList;

    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout mRefreshLayout;

    @BindView(R.id.layout_smart_remix)
    RelativeLayout mSmartRemixLayout;


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCameraConnection(CameraConnectionEvent event) {
        Logger.t(TAG).d("camera connection event: " + event.getWhat());
        switch (event.getWhat()) {
            case CameraConnectionEvent.VDT_CAMERA_SELECTED_CHANGED:
                hideCameraDisconnect();
                if (mActionMode != null) {
                    mActionMode.finish();
                }
                mPresenter.loadClipSet(false);
                break;
            case CameraConnectionEvent.VDT_CAMERA_DISCONNECTED:
                if (VdtCameraManager.getManager().getConnectedCameras().size() == 0) {
                    showCameraDisconnect();
                    if (mActionMode != null) {
                        mActionMode.finish();
                    }
                }
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
                Logger.t(TAG).d("menu item seleted");
                if (mIsAddMore) {
                    toEnhance();
                }
                if (mIsToShare) {
                    toShare();
                }
                break;

            default:
                break;
        }
    }

    public static ClipGridListFragment newInstance(int clipSetType) {
        return newInstance(clipSetType, false, false, false);
    }

    public static ClipGridListFragment newInstance(int clipSetType, boolean isMultipleSelectionMode, boolean isAddMore) {
        return newInstance(clipSetType, isMultipleSelectionMode, isAddMore, false);
    }

    public static ClipGridListFragment newInstance(int clipSetType, boolean isMultipleSelectionMode, boolean isAddMore, boolean isToShare) {
        ClipGridListFragment fragment = new ClipGridListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLIP_SET_TYPE, clipSetType);
        args.putBoolean(ARG_IS_MULTIPLE_MODE, isMultipleSelectionMode);
        args.putBoolean(ARG_IS_ADD_MORE, isAddMore);
        args.putBoolean(ARG_IS_TO_SHARE, isToShare);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected String getRequestTag() {
        return ClipGridListFragment.class.getSimpleName();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mClipSetType = args.getInt(ARG_CLIP_SET_TYPE, Clip.TYPE_MARKED);
            mIsMultipleMode = args.getBoolean(ARG_IS_MULTIPLE_MODE, false);
            mIsAddMore = args.getBoolean(ARG_IS_ADD_MORE, false);
            mIsToShare = args.getBoolean(ARG_IS_TO_SHARE, false);
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
            flag = ClipSetExRequest.FLAG_CLIP_EXTRA | ClipSetExRequest.FLAG_CLIP_DESC | ClipSetExRequest.FLAG_CLIP_SCENE_DATA;
            attr = 0;
        } else {
            flag = ClipSetExRequest.FLAG_CLIP_EXTRA | ClipSetExRequest.FLAG_CLIP_ATTR;
            attr = Clip.CLIP_ATTR_MANUALLY;
        }
        mPresenter = new ClipGridListPresenterImpl(getActivity(), mClipSetType, flag, attr, this);
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
        mIsExposed = false;
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public void showLoading(String msg) {
    }


    private void showLoadingProgress() {
        mRefreshLayout.setRefreshing(true);
    }

    @Override
    public void hideLoading() {
        mRefreshLayout.setRefreshing(false);
    }

    @Override
    public void refreshClipiSet(ClipSet clipSet) {
        if (mIsExposed) {
            if (clipSet == null || clipSet.getClipList().size() == 0) {
                RxBus.getDefault().post(new ToggleFabEvent(ToggleFabEvent.FAB_INVISIBLE, null));
            } else {
                RxBus.getDefault().post(new ToggleFabEvent(ToggleFabEvent.FAB_VISIBLE, null));
            }
        }
        Logger.t(TAG).d("Get clip set");
        if (mRefreshLayout != null) {
            mRefreshLayout.setRefreshing(false);
        }
        mClipSet = clipSet;
        ClipSetGroupHelper helper = new ClipSetGroupHelper(clipSet);
        mAdapter.setClipSetGroup(helper.getClipSetGroup());

        if (mClipSetType == Clip.TYPE_MARKED) {
            PreferenceUtils.putBoolean(PreferenceUtils.BOOKMARK_NEED_REFRESH, false);
        }
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

        mRefreshLayout.setColorSchemeResources(R.color.style_color_accent, android.R.color.holo_green_light,
            android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPresenter.loadClipSet(true);
            }
        });

        int spanCount = mClipSetType == Clip.TYPE_MARKED ? 3 : 2;
        int layoutRes = mClipSetType == Clip.TYPE_MARKED ? R.layout.item_clip_set_grid : R.layout.item_clip_set_card;
        mRvClipGroupList.setLayoutManager(new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL));
        mAdapter = new ClipSetGroupAdapter(getActivity(), layoutRes, mVdbRequestQueue, null, new ClipSetGroupAdapter.OnClipClickListener() {
            @Override
            public void onClipClicked(Clip clip, View transitionView) {

                if (mIsMultipleMode || clip == null) {
                    return;
                }
                if (mClipSetType == Clip.TYPE_MARKED) {
                    toPreview(clip, transitionView);
                } else {
                    toFootage(clip, transitionView);
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
    public void onStart() {
        super.onStart();
        mFabSubscription = RxBus.getDefault().toObserverable(ActionButtonEvent.class)
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<ActionButtonEvent>() {
                @Override
                public void onNext(ActionButtonEvent actionButtonEvent) {
                    switch (actionButtonEvent.mWhat) {
                        case ActionButtonEvent.FAB_SMART_REMIX:
                            if ((Integer) actionButtonEvent.mExtra == mClipSetType) {
                                mIsRemixMode = true;
                                //mSmartRemixLayout.setVisibility(View.VISIBLE);
                                if (mActionMode == null) {
                                    mRefreshLayout.setEnabled(false);
                                    mActionMode = getActivity().startActionMode(mRemixCallback);
                                    updateActionMode();
                                }
                                mAdapter.setMultiSelectedMode(true);
                                mAdapter.toggleSelectAll(false);
                            }
                            break;
                        default:
                            break;
                    }
                }
            });
        if (!mEventBus.isRegistered(this)) {
            mEventBus.register(this);
        }
        if (mClipSetType == Clip.TYPE_MARKED && PreferenceUtils.getBoolean(PreferenceUtils.BOOKMARK_NEED_REFRESH, false)) {
            if (mRefreshLayout != null) {
                mRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPresenter.loadClipSet(false);
                    }
                }, 200);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!mFabSubscription.isUnsubscribed()) {
            mFabSubscription.unsubscribe();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mEventBus.unregister(this);
    }


    private void doDeleteSelectedClips() {
        ArrayList<Clip> selectedList = new ArrayList<>();
        selectedList.addAll(mAdapter.getSelectedClipList());

        mPresenter.deleteClipList(selectedList);

    }


    private ActionMode.Callback mRemixCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_smart_remix, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_to_remix:
                    showRemixDialog();
                    //mode.finish();
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
            if (mIsRemixMode) {
                mAdapter.setMultiSelectedMode(false);
                RxBus.getDefault().post(new ToggleFabEvent(ToggleFabEvent.FAB_VISIBLE, null));
                mIsRemixMode = false;
                mAdapter.toggleSelectAll(false);
            }
            mRefreshLayout.setEnabled(true);
        }
    };


    private ActionMode.Callback mCABCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_clip_list, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            RxBus.getDefault().post(new ToggleFabEvent(ToggleFabEvent.FAB_INVISIBLE, null));
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
                    DialogHelper.showDeleteHighlightConfirmDialog(getActivity(), new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            doDeleteSelectedClips();
                            mode.finish();
                        }
                    });
                    break;
                case R.id.menu_to_remix:
                    toRemix();
                    mode.finish();
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
            RxBus.getDefault().post(new ToggleFabEvent(ToggleFabEvent.FAB_VISIBLE, null));
            mRefreshLayout.setEnabled(true);

        }
    };


    private void updateActionMode() {
        if (mActionMode != null) {
            mActionMode.setTitle("" + mAdapter.getSelectedClipList().size());
            if (mIsRemixMode) {
                MenuItem remixMenuItem = mActionMode.getMenu().findItem(R.id.menu_to_remix);
                remixMenuItem.setVisible(true);
            } else {
                MenuItem uploadMenuItem = mActionMode.getMenu().findItem(R.id.menu_to_upload);
                if (mClipSetType == Clip.TYPE_MARKED && mAdapter.getSelectedClipList().size() == 1) {
                    uploadMenuItem.setVisible(true);
                } else {
                    uploadMenuItem.setVisible(false);
                }
                MenuItem enhanceItem = mActionMode.getMenu().findItem(R.id.menu_to_enhance);
                if (mClipSetType == Clip.TYPE_MARKED) {
                    enhanceItem.setVisible(true);
                } else {
                    enhanceItem.setVisible(false);
                }
            }
        }
    }

    @Override
    public boolean onInterceptBackPressed() {
        if (mIsMultipleMode) {
            mIsMultipleMode = false;
            mAdapter.setMultiSelectedMode(false);
            return true;
        }
        if (mIsRemixMode) {
            mIsRemixMode = false;
            mAdapter.setMultiSelectedMode(false);
            mSmartRemixLayout.setVisibility(View.INVISIBLE);
            RxBus.getDefault().post(new ToggleFabEvent(ToggleFabEvent.FAB_VISIBLE, null));
            return true;
        }
        return false;
    }

    @Override
    public void onSelected() {
        mIsExposed = true;
        if (mClipSet != null && mClipSet.getClipList().size() > 0 && VdtCameraManager.getManager().getConnectedCameras().size() > 0) {
            RxBus.getDefault().post(new ToggleFabEvent(ToggleFabEvent.FAB_VISIBLE, null));
            Logger.t(TAG).d("visible");
        } else {
            RxBus.getDefault().post(new ToggleFabEvent(ToggleFabEvent.FAB_INVISIBLE, null));
            Logger.t(TAG).d("invisible");
        }
    }

    private void showRemixDialog() {
        int totalLength = 0;
        if (mAdapter.getSelectedClipList().size() <= 0) {
            return;
        } else {
            for (Clip clip : mAdapter.getSelectedClipList()) {
                totalLength += clip.getDurationMs() / 1000;
            }
            if (totalLength < 20) {
                Toast.makeText(getActivity(), "Please add more clips!", Snackbar.LENGTH_SHORT).show();
                return;
            }
        }
        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
            .customView(R.layout.dialog_smart_remix, true)
            .show();
        mRemixLength = 20;
        mLengthSeekbar = (SeekBar) dialog.getCustomView().findViewById(R.id.length_seekbar);
        mTvSmartRemix = (TextView) dialog.getCustomView().findViewById(R.id.tv_smart_remix);
        mTvTextEnd = (TextView) dialog.getCustomView().findViewById(R.id.text_end);
        mTvSmartRemix.setText(String.format(getString(R.string.smart_remix_length), mRemixLength));
        mLengthSeekbar.setMax(Math.min(60 - 15, totalLength - 15));
        mTvTextEnd.setText(Math.min(60, totalLength) + "s");
        mLengthSeekbar.setProgress(5);
        mLengthSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mRemixLength = 15 + seekBar.getProgress();
                mTvSmartRemix.setText(String.format(getString(R.string.smart_remix_length), mRemixLength));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mTvSmartRemix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toRemix();
                if (mActionMode != null) {
                    mActionMode.finish();
                }
                dialog.dismiss();
            }
        });
    }

    private void toRemix() {
        Logger.t(TAG).d("isAddMore: " + mIsAddMore + " isToShare: " + mIsToShare);
        ArrayList<Clip> selectedList = mAdapter.getSelectedClipList();
        int mode = 0;
        if (mClipSetType == Clip.TYPE_MARKED) {
            mode = AvrproFilter.SMART_RANDOMPICK;
        } else {
            mode = AvrproFilter.SMART_RANDOMCUTTING;
        }

        RemixActivity.launch(getActivity(), selectedList, mRemixLength, mode);
        Logger.t(TAG).d("selected list size: " + selectedList.size());
    }


    private void toPreview(final Clip clip, final View transitionView) {
        if (mLoadToast != null) {
            return;
        }
        mLoadToast = new LoadToast(getActivity());
        mLoadToast.setText(getString(R.string.loading));
        mLoadToast.show();

        final int playlistId = 0x100;
        PlayListEditor playListEditor = new PlayListEditor(mVdbRequestQueue, playlistId);
        playListEditor.buildRx(clip)
            .subscribe(new Subscriber<Void>() {
                @Override
                public void onCompleted() {
                    mLoadToast.success();
                    mLoadToast = null;
                    Logger.t(TAG).d("type race:" + clip.typeRace);
                    PreviewActivity.launch(getActivity(), playlistId, transitionView);
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    mLoadToast.error();
                    mLoadToast = null;
                    Snackbar snackbar = Snackbar.make(mRefreshLayout, R.string.camera_no_response, Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            toPreview(clip, transitionView);
                        }
                    });
                    snackbar.show();
                }

                @Override
                public void onNext(Void aVoid) {

                }
            });
    }


    private void toFootage(final Clip clip, final View transitionView) {
        if (mLoadToast != null) {
            return;
        }
        mLoadToast = new LoadToast(getActivity());
        mLoadToast.setText(getString(R.string.loading));
        mLoadToast.show();

        final int playlistId = 0x100;
        PlayListEditor playListEditor = new PlayListEditor(mVdbRequestQueue, playlistId);
        playListEditor.buildRx(clip)
            .subscribe(new Subscriber<Void>() {
                @Override
                public void onCompleted() {
                    mLoadToast.success();
                    mLoadToast = null;
                    FootageActivity.launch(getActivity(), playlistId, transitionView);
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    mLoadToast.error();
                    mLoadToast = null;
                    Snackbar snackbar = Snackbar.make(mRefreshLayout, R.string.camera_no_response, Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            toFootage(clip, transitionView);
                        }
                    });
                    snackbar.show();
                }

                @Override
                public void onNext(Void aVoid) {

                }
            });
    }


    private void toEnhance() {
        Logger.t(TAG).d("isAddMore: " + mIsAddMore + " isToShare: " + mIsToShare);
        if (!mIsAddMore) {
            ArrayList<Clip> selectedList = mAdapter.getSelectedClipList();
            if (mLoadToast != null) {
                return;
            }
            mLoadToast = new LoadToast(getActivity());
//            mLoadToast.setText(getString(R.string.loading));
            mLoadToast.show();
            Logger.t(TAG).d("selected list size: " + selectedList.size());

            final int playlistId = 0x100;
            PlayListEditor playListEditor = new PlayListEditor(mVdbRequestQueue, playlistId);

            playListEditor.buildRx(selectedList)
                .subscribe(new SimpleSubscribe<Void>() {
                    @Override
                    public void onCompleted() {
                        mLoadToast.success();
                        mLoadToast = null;
                        if (mIsToShare) {
                            ShareActivity.launch(getActivity(), playlistId, -1);
                            getActivity().finish();
                        } else {
                            EnhanceActivity.launch(getActivity(), playlistId);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mLoadToast.error();
                        mLoadToast = null;
                    }

                    @Override
                    public void onNext(Void aVoid) {

                    }


                });

        } else {
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra(EnhanceActivity.EXTRA_CLIPS_TO_APPEND, mAdapter.getSelectedClipList());
            Logger.t(TAG).d("add clip size: " + mAdapter.getSelectedClipList().size());
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }
    }

    private void toShare() {
        if (SessionManager.getInstance().isLoggedIn()) {

            if (mLoadToast != null) {
                return;
            }
            mLoadToast = new LoadToast(getActivity());
            mLoadToast.setText(getString(R.string.loading));
            mLoadToast.show();
            ArrayList<Clip> selectedList = mAdapter.getSelectedClipList();

            final int playlistId = 0x100;
            PlayListEditor playListEditor = new PlayListEditor(mVdbRequestQueue, playlistId);
            playListEditor.buildRx(selectedList)
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        mLoadToast.success();
                        mLoadToast = null;
                        ShareActivity.launch(getActivity(), playlistId, -1);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mLoadToast.error();
                        mLoadToast = null;
                    }

                    @Override
                    public void onNext(Void aVoid) {

                    }
                });
        } else {
            AuthorizeActivity.launch(getActivity());
        }
    }



}
