package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.views.VideoPlayerProgressBar;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Richard on 12/18/15.
 */
public class EnhancementFragment extends Fragment implements FragmentNavigator, VideoPlayFragment.OnProgressListener {

    @Bind(R.id.clip_seek_bar)
    VideoPlayerProgressBar mSeekBar;

    @Bind(R.id.tv_clip_date)
    TextView mClipDateView;

    @Bind(R.id.video_cover)
    ImageView videoCover;

    @Bind(R.id.enhance_root_view)
    LinearLayout mEnhanceRootView;

    Clip mClip;
    //long mMinClipStartTimeMs;
    //long mMaxClipEndTimeMs;
    VdbImageLoader mImageLoader;

    CameraVideoPlayFragment mVideoPlayFragment;


    public static EnhancementFragment newInstance(Clip clip) {
        Bundle args = new Bundle();
        EnhancementFragment fragment = new EnhancementFragment();
        fragment.setArguments(args);
        fragment.mClip = clip;
        //fragment.mMinClipStartTimeMs = minClipStartTimeMs;
        //fragment.mMaxClipEndTimeMs = maxClipEndTimeMs;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageLoader = VdbImageLoader.getImageLoader(Snipe.newRequestQueue());
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

        ClipPos clipPos = new ClipPos(mClip, mClip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
        mImageLoader.displayVdbImage(clipPos, videoCover);

        initSeekBar();
        mClipDateView.setText(mClip.getDateTimeString());
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
        mVideoPlayFragment = CameraVideoPlayFragment.newInstance(Snipe.newRequestQueue(), mClip, null);
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

    private void initSeekBar() {
        final ClipPos clipPos = new ClipPos(mClip, mClip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);
        mSeekBar.setInitRangeValues(mClip.getStartTimeMs(), mClip.getStartTimeMs() + mClip.getStartTimeMs());
        mSeekBar.setClip(mClip, mImageLoader);
        mSeekBar.setOnSeekBarChangeListener(new VideoPlayerProgressBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(VideoPlayerProgressBar progressBar) {
                //Log.e("test", "Start dragging....");
            }

            @Override
            public void onProgressChanged(VideoPlayerProgressBar progressBar, long progress, boolean fromUser) {
                //Log.e("test", "Progress: " + progress);
                refreshThumbnail(mClip.getStartTimeMs() + progress, clipPos);
            }

            @Override
            public void onStopTrackingTouch(VideoPlayerProgressBar progressBar) {
                //Log.e("test", "Stop dragging....");
            }
        });
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
}
