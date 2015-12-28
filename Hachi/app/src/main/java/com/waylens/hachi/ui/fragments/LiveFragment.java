package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.VdtCameraManager;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.ui.adapters.ClipFilmAdapter;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.RemoteClip;

import butterknife.Bind;

/**
 * Live Fragment
 * <p/>
 * Created by Xiaofei on 2015/8/4.
 */
public class LiveFragment extends BaseFragment implements FragmentNavigator,
        ClipFilmAdapter.OnEditClipListener, ClipEditFragment.OnActionListener {
    private static final String TAG = LiveFragment.class.getSimpleName();

    static final String TAG_CLIP_SET = "tag.clip_set";

    private VdbRequestQueue mVdbRequestQueue;
    private ClipFilmAdapter mClipSetAdapter;

    VdtCamera mVdtCamera;

    @Bind(R.id.video_type_tabs)
    TabLayout mTabLayout;

    @Bind(R.id.video_list_view)
    RecyclerView mRvCameraVideoList;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    LinearLayoutManager mLinearLayoutManager;

    ClipFilmAdapter.ClipEditViewHolder mHolder;

    VideoTab[] mVideoTabs = new VideoTab[]{
            new VideoTab(R.string.camera_video_bookmark, 0, RemoteClip.TYPE_MARKED),
            new VideoTab(R.string.camera_video_all, 0, RemoteClip.TYPE_BUFFERED),
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mVdtCamera != null) {
            retrieveVideoList(mVideoTabs[mTabLayout.getSelectedTabPosition()].tabTag);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_live, savedInstanceState);
        initViews();
        return view;
    }

    void initViews() {
        mVdtCamera = getCamera();
        if (mVdtCamera != null) {
            mVdbRequestQueue = Snipe.newRequestQueue(getActivity());
        } else {
            mViewAnimator.setDisplayedChild(2);
        }

        //mClipSetAdapter = new CameraClipSetAdapter(getActivity(), mVdbRequestQueue);
        //mClipSetAdapter.setClipActionListener(this);

        mClipSetAdapter = new ClipFilmAdapter();
        mClipSetAdapter.mOnEditClipListener = this;
        mRvCameraVideoList.setAdapter(mClipSetAdapter);
        mRvCameraVideoList.setLayoutManager(mLinearLayoutManager);

        for (VideoTab videoTab : mVideoTabs) {
            TabLayout.Tab tab = mTabLayout.newTab().setText(videoTab.textRes);

            mTabLayout.addTab(tab);
        }

        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (mVdtCamera != null) {
                    mClipSetAdapter.setClipSet(null);
                    retrieveVideoList(mVideoTabs[tab.getPosition()].tabTag);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mHolder != null && mHolder.clipEditFragment != null) {
            getActivity()
                    .getFragmentManager()
                    .beginTransaction().
                    remove(mHolder.clipEditFragment)
                    .commitAllowingStateLoss();
            mHolder.clipEditFragment = null;
            mHolder = null;
        }
    }

    @Override
    public void onDestroyView() {
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.cancelAll(TAG_CLIP_SET);
            mVdbRequestQueue = null;
        }
        mTabLayout.setOnTabSelectedListener(null);
        mRvCameraVideoList.setAdapter(null);
        mClipSetAdapter.mOnEditClipListener = null;
        mClipSetAdapter = null;
        mHolder = null;
        super.onDestroyView();
    }

    private void retrieveVideoList(int type) {
        Bundle parameter = new Bundle();
        parameter.putInt(ClipSetRequest.PARAMETER_TYPE, type);
        parameter.putInt(ClipSetRequest.PARAMETER_FLAG, ClipSetRequest.FLAG_CLIP_EXTRA);
        mVdbRequestQueue.add(new ClipSetRequest(ClipSetRequest.METHOD_GET, parameter,
                new VdbResponse.Listener<ClipSet>() {
                    @Override
                    public void onResponse(ClipSet clipSet) {
                        mClipSetAdapter.setClipSet(clipSet);
                        mViewAnimator.setDisplayedChild(1);
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "ClipSetRequest: " + error);
                    }
                }).setTag(TAG_CLIP_SET));
    }

    VdtCamera getCamera() {
        Bundle args = getArguments();
        VdtCamera camera = null;

        VdtCameraManager vdtCameraManager = VdtCameraManager.getManager();
        if (args != null) {
            String ssid = args.getString("ssid");
            String hostString = args.getString("hostString");
            if (ssid != null && hostString != null) {
                camera = vdtCameraManager.findConnectedCamera(ssid, hostString);
            }
        } else {
            if (vdtCameraManager.getConnectedCameras().size() > 0) {
                camera = vdtCameraManager.getConnectedCameras().get(0);
            }
        }
        return camera;
    }

    @Override
    public boolean onInterceptBackPressed() {
        if (VideoPlayFragment.fullScreenPlayer != null) {
            VideoPlayFragment.fullScreenPlayer.setFullScreen(false);
            return true;
        }
        return false;
    }

    @Override
    public void onStartDragging() {
        mRvCameraVideoList.setLayoutFrozen(true);
    }

    @Override
    public void onStopDragging() {
        mRvCameraVideoList.setLayoutFrozen(false);
    }

    @Override
    public void onEditClip(final Clip clip, final ClipFilmAdapter.ClipEditViewHolder holder, final int position) {
        if (mHolder != null && mHolder != holder) {
            if (mHolder.clipEditFragment != null) {
                getFragmentManager().beginTransaction().remove(mHolder.clipEditFragment).commit();
                mHolder.clipEditFragment = null;
            }
            mHolder.editorView.setVisibility(View.GONE);
            mHolder.clipFilm.setVisibility(View.VISIBLE);
        }

        mRvCameraVideoList.setLayoutFrozen(false);
        mHolder = holder;
        holder.editorView.setVisibility(View.VISIBLE);
        mHolder.clipFilm.setVisibility(View.GONE);
        ClipEditFragment fragment = ClipEditFragment.newInstance(clip, position, this);
        getFragmentManager().beginTransaction().replace(holder.editorView.getId(), fragment).commit();
        mHolder.clipEditFragment = fragment;
    }

    @Override
    public void onStopEditing(int position) {
        ClipFilmAdapter.ClipEditViewHolder holder = (ClipFilmAdapter.ClipEditViewHolder)
                mRvCameraVideoList.findViewHolderForAdapterPosition(position);
        if (holder != null) {
            if (holder.clipEditFragment != null) {
                getFragmentManager().beginTransaction().remove(holder.clipEditFragment).commit();
                holder.clipEditFragment = null;
            }
            holder.editorView.setVisibility(View.GONE);
            holder.clipFilm.setVisibility(View.VISIBLE);
        }
    }

    public void onFragmentStart(int position) {
        if (mLinearLayoutManager.findLastVisibleItemPosition() != mClipSetAdapter.getItemCount() - 1) {
            mLinearLayoutManager.scrollToPositionWithOffset(position, 0);
            mRvCameraVideoList.requestLayout();
        }
    }

    static class VideoTab {
        int textRes;
        int iconRes;
        int tabTag;

        public VideoTab(int textRes, int iconRes, int tabTag) {
            this.textRes = textRes;
            this.iconRes = iconRes;
            this.tabTag = tabTag;
        }
    }
}
