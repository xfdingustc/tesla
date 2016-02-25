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
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.ui.fragments.clipplay.CameraVideoPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay.VideoPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.views.clipseditview.ClipsEditView;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.Playlist;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.util.ArrayList;

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


    void onCancel() {
        close();
    }

    void onClickDone() {
        close();
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
        ClipPlayFragment fragment = ClipPlayFragment.newInstance(getCamera(), mSharableClips.get(0).clip, config);
        fragment.setShowsDialog(false);
        getChildFragmentManager().beginTransaction().replace(R.id.enhance_fragment_content, fragment).commit();
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
        //TODO
    }

    @Override
    public void onClipMoved(int fromPosition, int toPosition) {
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
