package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
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
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.ui.adapters.ClipFilmAdapter;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import butterknife.Bind;

/**
 * Live Fragment
 * <p/>
 * Created by Xiaofei on 2015/8/4.
 */
public class BookmarkFragment extends BaseFragment implements FragmentNavigator,
        ClipFilmAdapter.OnEditClipListener {
    private static final String TAG = BookmarkFragment.class.getSimpleName();

    static final String TAG_CLIP_SET = "tag.clip_set";


    private ClipFilmAdapter mClipSetAdapter;



    @Bind(R.id.video_type_tabs)
    TabLayout mTabLayout;

    @Bind(R.id.video_list_view)
    RecyclerView mRvCameraVideoList;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    LinearLayoutManager mLinearLayoutManager;

    ClipFilmAdapter.ClipEditViewHolder mHolder;

    VideoTab[] mVideoTabs = new VideoTab[]{
            new VideoTab(R.string.camera_video_bookmark, 0, Clip.TYPE_MARKED),
            new VideoTab(R.string.camera_video_all, 0, Clip.TYPE_BUFFERED),
    };

    HandlerThread mBgThread;
    Handler mBgHandler;
    Handler mUIHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mBgThread = new HandlerThread("LiveFragment-bg-thread");
        mBgThread.start();
        mBgHandler = new Handler(mBgThread.getLooper());
        mUIHandler = new Handler();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mVdtCamera != null) {
            retrieveSharableClips(mVideoTabs[mTabLayout.getSelectedTabPosition()].tabTag);
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
                    retrieveSharableClips(mVideoTabs[tab.getPosition()].tabTag);
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
        super.onDestroyView();
    }

    void retrieveSharableClips(final int clipType) {
        mViewAnimator.setDisplayedChild(0);
        mBgHandler.post(new Runnable() {
            @Override
            public void run() {
                ClipSet clipSet = retrieveVideoList(clipType);
                updateAdapter(SharableClip.processClipSet(clipSet, mVdbRequestQueue));
            }
        });
    }

    void updateAdapter(final List<SharableClip> sharableClips) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                mClipSetAdapter.setClipSet(sharableClips);
                mViewAnimator.setDisplayedChild(1);
            }
        });
    }

    ClipSet retrieveVideoList(int type) {
        final CountDownLatch latch = new CountDownLatch(1);
        final ClipSet[] clipSets = new ClipSet[]{null};

        mVdbRequestQueue.add(new ClipSetRequest(type, ClipSetRequest.FLAG_CLIP_EXTRA,
                new VdbResponse.Listener<ClipSet>() {
                    @Override
                    public void onResponse(ClipSet clipSet) {
                        clipSets[0] = clipSet;
                        latch.countDown();
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "", error);
                        latch.countDown();
                    }
                }).setTag(TAG_CLIP_SET));
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e("test", "", e);
        }
        return clipSets[0];
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
    public void onEnhanceClip(SharableClip sharableClip) {
        getFragmentManager().beginTransaction().add(R.id.root_container, EnhancementFragment.newInstance(sharableClip)).commit();
    }

    @Override
    public void onShareClip(SharableClip sharableClip) {
        getFragmentManager().beginTransaction().replace(R.id.root_container, ShareFragment.newInstance(sharableClip, 0)).commit();
    }

    @Override
    public void onEditClip(final SharableClip sharableClip, final ClipFilmAdapter.ClipEditViewHolder holder, final int position) {
        if (mHolder == holder && holder.videoTrimmer.isInEditMode()) {
            return;
        }

        if (mHolder != null) {
            mHolder.stopEditing();
        }

        mRvCameraVideoList.setLayoutFrozen(false);
        holder.startEditing(sharableClip);
        mLinearLayoutManager.scrollToPositionWithOffset(position, 0);
        mRvCameraVideoList.requestLayout();
        mHolder = holder;
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
