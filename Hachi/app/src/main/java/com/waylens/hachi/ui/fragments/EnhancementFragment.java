package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.ui.views.VideoPlayerProgressBar;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.Playlist;

import org.jcodec.common.ArrayUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Richard on 12/18/15.
 */
public class EnhancementFragment extends Fragment implements FragmentNavigator, VideoPlayFragment.OnProgressListener {
    private static final int MODE_SINGLE_CLIP = 0;
    private static final int MODE_PLAYLIST = 1;

    @Bind(R.id.clip_seek_bar)
    VideoPlayerProgressBar mSeekBar;

    @Bind(R.id.tv_clip_date)
    TextView mClipDateView;

    @Bind(R.id.video_cover)
    ImageView videoCover;

    @Bind(R.id.enhance_root_view)
    LinearLayout mEnhanceRootView;

    @Bind(R.id.view_pager)
    ViewPager mViewPager;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    private int mEditMode;
    private SharableClip mSharableClip;
    private Playlist mPlaylist;

    private VdbImageLoader mImageLoader;


    CameraVideoPlayFragment mVideoPlayFragment;
    SimplePagerAdapter mPagerAdapter;

    public static EnhancementFragment newInstance(SharableClip sharableClip) {
        Bundle args = new Bundle();
        EnhancementFragment fragment = new EnhancementFragment();
        fragment.setArguments(args);
        fragment.mSharableClip = sharableClip;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageLoader = VdbImageLoader.getImageLoader(Snipe.newRequestQueue());
        mPagerAdapter = new SimplePagerAdapter();
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
        ClipPos clipPos;
        if (mEditMode == MODE_SINGLE_CLIP) {
            clipPos = mSharableClip.getThumbnailClipPos(mSharableClip.currentPosition);
            mClipDateView.setText(mSharableClip.clip.getDateTimeString());
            initSeekBarSingleClip();
        } else {
            Clip firstClip = mPlaylist.getClip(0);
            clipPos = new ClipPos(firstClip, firstClip.getStartTimeMs(), ClipPos.TYPE_POSTER,
                false);
            mClipDateView.setText(firstClip.getDateTimeString());
            initSeekBarPlayList();
        }
        mImageLoader.displayVdbImage(clipPos, videoCover);


        mViewPager.setAdapter(mPagerAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mVideoPlayFragment != null) {
            getFragmentManager().beginTransaction().remove(mVideoPlayFragment).commitAllowingStateLoss();
            mVideoPlayFragment = null;
        }
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    @OnClick(R.id.btn_play)
    void playVideo() {
        mVideoPlayFragment = CameraVideoPlayFragment.newInstance(Snipe.newRequestQueue(), mSharableClip.clip, null);
        mVideoPlayFragment.setOnProgressListener(this);
        getFragmentManager().beginTransaction().replace(R.id.enhance_fragment_content, mVideoPlayFragment).commit();
        videoCover.setVisibility(View.INVISIBLE);
    }

    @OnClick(R.id.btn_cancel)
    void onCancel() {
        close();
    }

    @OnClick(R.id.btn_ok)
    void onClickDone() {
        close();
    }

    @OnClick(R.id.btn_share)
    void onClickShare() {

    }

    @OnClick(R.id.btn_gauge)
    void showGauge() {
        mViewAnimator.setDisplayedChild(1);
    }

    private void initSeekBarSingleClip() {
        final ClipPos clipPos = new ClipPos(mSharableClip.clip, mSharableClip.clip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
        mSeekBar.setInitRangeValues(mSharableClip.clip.getStartTimeMs(), mSharableClip.clip
            .getStartTimeMs() + mSharableClip.clip.getDurationMs());

        ClipSet clipSet = new ClipSet(0);
        clipSet.addClip(mSharableClip.clip);
        mSeekBar.setClipSet(clipSet, mImageLoader);
        mSeekBar.setOnSeekBarChangeListener(new VideoPlayerProgressBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(VideoPlayerProgressBar progressBar) {
                //Log.e("test", "Start dragging....");
            }

            @Override
            public void onProgressChanged(VideoPlayerProgressBar progressBar, long progress, boolean fromUser) {
                //Log.e("test", "Progress: " + progress);
                refreshThumbnail(mSharableClip.clip.getStartTimeMs() + progress, clipPos);
            }

            @Override
            public void onStopTrackingTouch(VideoPlayerProgressBar progressBar) {
                //Log.e("test", "Stop dragging....");
            }
        });
    }

    private void initSeekBarPlayList() {
        ClipSet clipSet = mPlaylist.getClipSet();
        mSeekBar.setClipSet(clipSet, mImageLoader);
    }

    void refreshThumbnail(long clipTimeMs, ClipPos clipPos) {
        clipPos.setClipTimeMs(clipTimeMs);
        mImageLoader.displayVdbImage(clipPos, videoCover, true, false);
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
    public void onProgress(int position, int duration) {
        mSeekBar.setProgress(position, duration);

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
}
