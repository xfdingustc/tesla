package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.StackView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.eventbus.events.MenuItemSelectEvent;
import com.waylens.hachi.eventbus.events.MultiSelectEvent;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.ui.authorization.AuthorizeActivity;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
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

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/2/18.
 */
public class TagFragment extends BaseFragment implements FragmentNavigator {
    private static final String TAG = TagFragment.class.getSimpleName();
    private static final String ARG_CLIP_SET_TYPE = "clip.set.type";
    private static final String ARG_IS_MULTIPLE_MODE = "is.multiple.mode";
    private static final String ARG_IS_ADD_MORE = "is.add.more";

    public static final String ACTION_RETRIEVE_CLIPS = "action.retrieve.clips";

    private ClipSetGroupAdapter mAdapter;

    private int mClipSetType;

    private boolean mIsMultipleMode;

    private boolean mIsAddMore;

    private int mDeleteClipCount = 0;

    private LocalBroadcastManager mBroadcastManager;

    private EventBus mEventBus = EventBus.getDefault();


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


    public static TagFragment newInstance(int clipSetType) {
        return newInstance(clipSetType, false, false);
    }

    public static TagFragment newInstance(int clipSetType, boolean isMultipleSelectionMode, boolean isAddMore) {
        TagFragment fragment = new TagFragment();
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
        View view = createFragmentView(inflater, container, R.layout.fragment_tagged_clip, savedInstanceState);
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

//        mBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
//        if (mBroadcastManager != null) {
//            mBroadcastManager.registerReceiver(localReceiver, new IntentFilter(ACTION_RETRIEVE_CLIPS));
//        }
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


    private void initViews() {
        if (mClipSetType == Clip.TYPE_MARKED) {
            mVsNoBookmark.showNext();
        }
        if (mVdtCamera != null) {
            doGetClips();
            if (mClipSetType != Clip.TYPE_MARKED) {
                //mBottomLayout.setVisibility(View.VISIBLE);
                doGetBufferedClips();
            }
        }
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
                        doGetClips();
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
            Logger.t(TAG).d("add clip size: " + mAdapter.getSelectedClipList().size());
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
                if (mIsMultipleMode && clip == null) {
                    mEventBus.post(new MultiSelectEvent(true, mAdapter.getSelectedClipList()));
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
//                if (mActionMode == null) {
//                    mActionMode = getActivity().startActionMode(mCABCallback);
//                }

                mEventBus.post(new MultiSelectEvent(true, mAdapter.getSelectedClipList()));
            }
        });

        mAdapter.setMultiSelectedMode(mIsMultipleMode);

        mRvClipGroupList.setAdapter(mAdapter);

    }


    private void doGetClips() {
        if (mVdbRequestQueue == null) {
            return;
        }

        int flag;
        int attr;

        if (true || mClipSetType == Clip.TYPE_MARKED) {
            flag = ClipSetExRequest.FLAG_CLIP_EXTRA;
            attr = 0;
        } else {
//            flag = ClipSetExRequest.FLAG_CLIP_EXTRA | ClipSetExRequest.FLAG_CLIP_ATTR;
//            attr = Clip.CLIP_ATTR_MANUALLY;
        }

        mVdbRequestQueue.add(new ClipSetExRequest(mClipSetType, flag, attr,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet clipSet) {
                    mRefreshLayout.setRefreshing(false);
                    if (clipSet.getCount() == 0) {
                        mVsNoBookmark.setVisibility(View.VISIBLE);
                        mRvClipGroupList.setVisibility(View.GONE);
                        mEventBus.post(new MultiSelectEvent(false, null));
                    } else {
                        mVsNoBookmark.setVisibility(View.GONE);
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
                    Date date = new Date((long) clip.getDate() * 1000);
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

    private void toggleMultiMode(boolean isMultiMode) {
        mIsMultipleMode = isMultiMode;
        mAdapter.setMultiSelectedMode(mIsMultipleMode);

    }


    private void toShare() {
        if (SessionManager.getInstance().isLoggedIn()) {
            ArrayList<Clip> selectedList = mAdapter.getSelectedClipList();
            EnhancementActivity.launch(getActivity(), selectedList, EnhancementActivity.LAUNCH_MODE_SHARE);
        } else {
            AuthorizeActivity.launch(getActivity());
        }
    }

//    BroadcastReceiver localReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            doGetClips();
//        }
//    };


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
            ImageView iv = new ImageView(getActivity());
            int imageSize = (int) Utils.dp2Px(64);
            iv.setLayoutParams(new LinearLayout.LayoutParams(imageSize, imageSize));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

            ClipPos clipPos = new ClipPos(clip);

            mVdbImageLoader.displayVdbImage(clipPos, iv);

            return iv;
        }
    }
}
