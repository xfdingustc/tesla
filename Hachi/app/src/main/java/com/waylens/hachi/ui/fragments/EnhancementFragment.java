package com.waylens.hachi.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.snipe.toolbox.ClipExtentUpdateRequest;
import com.waylens.hachi.snipe.toolbox.ClipMoveRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.snipe.toolbox.PlaylistEditRequest;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.ui.fragments.clipplay.CameraVideoPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay.VideoPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.views.clipseditview.ClipsEditView;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.Playlist;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Richard on 12/18/15.
 */
public class EnhancementFragment extends BaseFragment implements FragmentNavigator, ClipsEditView.OnClipEditListener {
    private static final int MODE_SINGLE_CLIP = 0;
    private static final int MODE_PLAYLIST = 1;


    private int mEditMode;
    private ArrayList<SharableClip> mSharableClips;
    private Playlist mPlaylist;

    private int mAudioID;
    private String mAudioPath;

    CameraVideoPlayFragment mVideoPlayFragment;
    SimplePagerAdapter mPagerAdapter;

    private ClipPlayFragment mClipPlayFragment;

    @Bind(R.id.enhance_root_view)
    LinearLayout mEnhanceRootView;

    @Bind(R.id.view_pager)
    ViewPager mViewPager;

    @Bind(R.id.clips_edit_view)
    ClipsEditView mClipsEditView;

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, new IntentFilter("choose-bg-music"));
        doGetPlaylistInfo();
    }

    private void doGetPlaylistInfo() {
        mVdbRequestQueue = Snipe.newRequestQueue();
        /*
        PlaylistSetRequest request = new PlaylistSetRequest(0, new VdbResponse.Listener<PlaylistSet>() {
            @Override
            public void onResponse(PlaylistSet response) {
                Log.e("test", "PlaylistSet: " + response);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
        */
        /*
        new Thread(new Runnable() {
            @Override
            public void run() {
                buildPlayList();
            }
        }).start();
        */
        fetchPlayList();
    }

    void buildPlayList() {
        final CountDownLatch latch = new CountDownLatch(mSharableClips.size());

        for (SharableClip sharableClip : mSharableClips) {
            PlaylistEditRequest request = new PlaylistEditRequest(
                    PlaylistEditRequest.METHOD_INSERT_CLIP,
                    sharableClip.clip,
                    sharableClip.selectedStartValue,
                    sharableClip.selectedEndValue,
                    0x100,
                    new VdbResponse.Listener<Integer>() {
                        @Override
                        public void onResponse(Integer response) {
                            latch.countDown();
                            Log.e("test", "Response: " + response);
                        }
                    },
                    new VdbResponse.ErrorListener() {
                        @Override
                        public void onErrorResponse(SnipeError error) {
                            latch.countDown();
                            Log.e("test", "", error);
                        }
                    });
            mVdbRequestQueue.add(request);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e("test", "", e);
        }
    }

    void fetchPlayList() {
        mVdbRequestQueue.add(new ClipSetRequest(0x100, ClipSetRequest.FLAG_CLIP_EXTRA,
                new VdbResponse.Listener<ClipSet>() {
                    @Override
                    public void onResponse(ClipSet clipSet) {
                        Log.e("test", "PlayList clips: " + clipSet);
                        //updateClip(clipSet.getClip(1));
                        //moveClip(clipSet.getClip(2), 0);
                        //deleteClip(clipSet.getClip(0));
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "", error);

                    }
                }));
    }

    void updateClip(Clip clip) {
        mVdbRequestQueue.add(new ClipExtentUpdateRequest(clip,
                clip.getStartTimeMs() + 2000,
                clip.getStartTimeMs() + clip.getDurationMs() - 5000,
                new VdbResponse.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
                        Log.e("test", "ClipExtentUpdateRequest response: " + response);
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "ClipExtentUpdateRequest", error);
                    }
                }
        ));
    }

    void moveClip(Clip clip, int newPosition) {
        mVdbRequestQueue.add(new ClipMoveRequest(clip.cid, newPosition,
                new VdbResponse.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
                        Log.e("test", "ClipMoveRequest response: " + response);
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "ClipMoveRequest", error);
                    }
                }));
    }

    void deleteClip(Clip clip) {
        mVdbRequestQueue.add(new ClipDeleteRequest(clip.cid,
                new VdbResponse.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
                        Log.e("test", "ClipDeleteRequest response: " + response);
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "ClipDeleteRequest", error);
                    }
                }));
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mVideoPlayFragment != null) {
            getFragmentManager().beginTransaction().remove(mVideoPlayFragment).commitAllowingStateLoss();
            mVideoPlayFragment = null;
        }
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    void playVideo() {
        if (mEditMode == MODE_SINGLE_CLIP) {
            mVideoPlayFragment = CameraVideoPlayFragment.newInstance(Snipe.newRequestQueue(), mSharableClips.get(0).clip, null);
        } else {
            mVideoPlayFragment = CameraVideoPlayFragment.newInstance(Snipe.newRequestQueue(),
                    mPlaylist, null);
        }
        mVideoPlayFragment.setBackgroundMusic(mAudioPath);
        getFragmentManager().beginTransaction().replace(R.id.enhance_fragment_content, mVideoPlayFragment).commit();
    }

    void onClickShare() {
        //getFragmentManager().beginTransaction().replace(R.id.root_container, ShareFragment.newInstance(mSharableClip, mAudioID)).commit();
    }

    @OnClick(R.id.btn_music)
    void onClickMusic() {
        getFragmentManager().beginTransaction()
                .add(R.id.root_container, new MusicFragment())
                .addToBackStack(null)
                .commit();
    }

    @OnClick(R.id.btn_gauge)
    void showGauge() {
        //TODO
    }


    public static EnhancementFragment newInstance(SharableClip sharableClip) {
        Bundle args = new Bundle();
        EnhancementFragment fragment = new EnhancementFragment();
        fragment.setArguments(args);
        //fragment.mSharableClip = sharableClip;
        fragment.mEditMode = MODE_SINGLE_CLIP;
        return fragment;
    }

    public static EnhancementFragment newInstance(Playlist playlist) {
        Bundle args = new Bundle();
        EnhancementFragment fragment = new EnhancementFragment();
        fragment.setArguments(args);
        fragment.mPlaylist = playlist;
        fragment.mEditMode = MODE_PLAYLIST;
        return fragment;
    }

    public static EnhancementFragment newInstance(ArrayList<Clip> clips) {
        Bundle args = new Bundle();
        EnhancementFragment fragment = new EnhancementFragment();
        fragment.setArguments(args);
        fragment.mSharableClips = new ArrayList<>();
        for (Clip clip : clips) {
            fragment.mSharableClips.add(new SharableClip(clip));
        }
        fragment.mEditMode = MODE_SINGLE_CLIP;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPagerAdapter = new SimplePagerAdapter();
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enhance, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEnhanceRootView.requestDisallowInterceptTouchEvent(true);
        mViewPager.setAdapter(mPagerAdapter);
        embedVideoPlayFragment();
        mClipsEditView.setSharableClips(mSharableClips);
        mClipsEditView.setOnClipEditListener(this);
    }

    void embedVideoPlayFragment() {
        ClipPlayFragment.Config config = new ClipPlayFragment.Config();
        config.progressBarStyle = ClipPlayFragment.Config.PROGRESS_BAR_STYLE_SINGLE;
        config.showControlPanel = false;
        mClipPlayFragment = ClipPlayFragment.newInstance(getCamera(), mSharableClips.get(0).clip, config);
        mClipPlayFragment.setShowsDialog(false);
        getChildFragmentManager().beginTransaction().replace(R.id.enhance_fragment_content, mClipPlayFragment).commit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_enhance, menu);
    }

    void close() {
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public boolean onInterceptBackPressed() {
        if (VideoPlayFragment.fullScreenPlayer != null) {
            VideoPlayFragment.fullScreenPlayer.setFullScreen(false);
            return true;
        }
        close();
        return true;
    }

    @Override
    public void onClipSelected(int position, SharableClip sharableClip) {
        mClipPlayFragment.setClip(sharableClip);
        //TODO
    }

    @Override
    public void onClipMoved(int fromPosition, int toPosition) {
        //TODO
    }

    @Override
    public void onClipsAppended(List<SharableClip> sharableClips) {
        //TODO
    }

    @Override
    public void onClipRemoved(int position) {
        //TODO
    }

    @Override
    public void onExitEditing() {
        //TODO
    }

    @Override
    public void onStartTrimming() {
        //TODO
    }

    @Override
    public void onTrimming(int flag, long value) {
        //TODO
    }

    @Override
    public void onStopTrimming() {
        //TODO
    }

    static class SimplePagerAdapter extends PagerAdapter {

        static int[] view_layouts = new int[]{
                R.layout.layout_gauge_one,
                R.layout.layout_gauge_two,
        };

        @Override
        public int getCount() {
            return view_layouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = LayoutInflater.from(container.getContext()).inflate(view_layouts[position], container, false);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
            Log.e("test", "destroyItem");
        }
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mAudioID = intent.getIntExtra("music-id", 0);
            String name = intent.getStringExtra("name");
            mAudioPath = intent.getStringExtra("path");
            if (mVideoPlayFragment != null) {
                getFragmentManager().beginTransaction().remove(mVideoPlayFragment).commit();
                mVideoPlayFragment = null;
            }
        }
    };
}
