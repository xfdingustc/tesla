package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.hardware.VdtCameraManager;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.ui.adapters.CameraClipSetAdapter;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RemoteClip;

import butterknife.Bind;

/**
 * Live Fragment
 * <p/>
 * Created by Xiaofei on 2015/8/4.
 */
public class LiveFragment extends BaseFragment implements CameraClipSetAdapter.ClipActionListener, FragmentNavigator {
    private static final String TAG = "LiveFragment";

    static final String TAG_CLIP_SET = "tag.clip_set";

    private VdbRequestQueue mVdbRequestQueue;
    private CameraClipSetAdapter mClipSetAdapter;

    VdtCamera mVdtCamera;

    @Bind(R.id.video_type_tabs)
    TabLayout mTabLayout;

    @Bind(R.id.video_list_view)
    RecyclerView mRvCameraVideoList;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    LinearLayoutManager mLinearLayoutManager;

    VideoTab[] mVideoTabs = new VideoTab[]{
            new VideoTab(R.string.camera_video_all, 0, RemoteClip.TYPE_BUFFERED),
            new VideoTab(R.string.camera_video_bookmark, 0, RemoteClip.TYPE_MARKED),
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
            mVdbRequestQueue = Snipe.newRequestQueue(getActivity(), mVdtCamera.getVdbConnection());
        } else {
            mViewAnimator.setDisplayedChild(2);
        }

        mClipSetAdapter = new CameraClipSetAdapter(getActivity(), mVdtCamera,
                mVdbRequestQueue);
        mClipSetAdapter.setClipActionListener(this);
        mRvCameraVideoList.setAdapter(mClipSetAdapter);
        mRvCameraVideoList.setLayoutManager(mLinearLayoutManager);

        for (VideoTab videoTab : mVideoTabs) {
            mTabLayout.addTab(mTabLayout.newTab().setText(videoTab.textRes));
        }

        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (mVdtCamera != null) {
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
    public void onDestroyView() {
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.cancelAll(TAG_CLIP_SET);
        }
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
    public void onRequestVideoPlay(final CameraClipSetAdapter.CameraClipViewHolder holder, Clip clip) {
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, clip.getStartTime());

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(clip, parameters, new VdbResponse.Listener<PlaybackUrl>() {
            @Override
            public void onResponse(PlaybackUrl playbackUrl) {
                Log.e("test", "URL: " + playbackUrl.url);
                //playVideo(holder, playbackUrl);
                VideoPlayFragment videoPlayFragment = new VideoPlayFragment();
                videoPlayFragment.setSource(playbackUrl.url);
                getFragmentManager().beginTransaction().replace(holder.videoContainer.getId(), videoPlayFragment).commit();

            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                //
            }
        });

        mVdbRequestQueue.add(request);
    }

    @Override
    public void onRemoveVideoPlayFragment(final CameraClipSetAdapter.CameraClipViewHolder holder) {
        Fragment fragment = getFragmentManager().findFragmentById(holder.videoContainer.getId());
        if (fragment != null) {
            getFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }

    @Override
    public boolean onInterceptBackPressed() {
        if (VideoPlayFragment.fullScreenPlayer != null) {
            VideoPlayFragment.fullScreenPlayer.setFullScreen(false);
            return true;
        }
        return false;
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
