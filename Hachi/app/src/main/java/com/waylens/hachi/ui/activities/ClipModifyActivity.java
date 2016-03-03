package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.ClipUrlProvider;
import com.waylens.hachi.ui.fragments.clipplay2.PlaylistUrlProvider;
import com.waylens.hachi.ui.fragments.clipplay2.UrlProvider;
import com.waylens.hachi.ui.views.cliptrimmer.VideoTrimmer;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/3/3.
 */
public class ClipModifyActivity extends BaseActivity {

    private VdtCamera mVdtCamera;
    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;
    private Clip mClip;

    private ClipPlayFragment mClipPlayFragment;

    public static void launch(Activity activity, Clip clip) {
        Intent intent = new Intent(activity, ClipModifyActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("clip", clip);
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }


    @Bind(R.id.clipTrimmer)
    VideoTrimmer mClipTrimmer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mVdbRequestQueue = Snipe.newRequestQueue(this);
        mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
        Bundle bundle = getIntent().getExtras();
        mClip = bundle.getParcelable("clip");
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_clip_modify);
        embedVideoPlayFragment();
        initClipTrimmer();
    }


    private void embedVideoPlayFragment() {
        ClipPlayFragment.Config config = new ClipPlayFragment.Config();
        config.progressBarStyle = ClipPlayFragment.Config.PROGRESS_BAR_STYLE_SINGLE;
        config.showControlPanel = false;

        UrlProvider vdtUriProvider = new ClipUrlProvider(mVdbRequestQueue, mClip);
        ClipSet clipSet = new ClipSet(0x107);
        clipSet.addClip(mClip);

        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, clipSet, vdtUriProvider,
                config);
        mClipPlayFragment.setShowsDialog(false);
        getFragmentManager().beginTransaction().replace(R.id.clipPlayFragment, mClipPlayFragment).commit();
    }

    private void initClipTrimmer() {
        int defaultHeight = ViewUtils.dp2px(64, getResources());

        mClipTrimmer.setBackgroundClip(mVdbImageLoader, mClip, defaultHeight);

        mClipTrimmer.setEditing(true);
        mClipTrimmer.setInitRangeValues(mClip.getStartTimeMs(), mClip.getEndTimeMs());
        //mSelectedClipStartTimeMs = mClipExtent.clipStartTimeMs;
        //mSelectedClipEndTimeMs = mClipExtent.clipEndTimeMs;

        mClipTrimmer.setLeftValue(mClip.getStartTimeMs());
        mClipTrimmer.setRightValue(mClip.getEndTimeMs());

        mClipTrimmer.setOnChangeListener(new VideoTrimmer.OnTrimmerChangeListener() {
            @Override
            public void onStartTrackingTouch(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag) {
//                mTrimmerFlag = flag;
//                if (mOnActionListener != null) {
//                    mOnActionListener.onStartDragging();
//                }
//                if (isInPlaybackState() && mMediaPlayer.isPlaying()) {
//                    pauseVideo();
//                }
            }

            @Override
            public void onProgressChanged(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag, long start, long end, long progress) {
//                if (videoCover != null) {
//                    videoCover.setVisibility(View.VISIBLE);
//                    Clip.ID cid = getWorkableCid();
//                    ClipPos clipPos = new ClipPos(mClip.getVdbId(), cid, mClip.clipDate, progress, ClipPos.TYPE_POSTER, false);
//                    mImageLoader.displayVdbImage(clipPos, videoCover, true, false);
//                }
//                mSeekToPosition = progress;
//                mTypedPosition.clear();
            }

            @Override
            public void onStopTrackingTouch(VideoTrimmer trimmer) {
//                if (mOnActionListener != null) {
//                    mOnActionListener.onStopDragging();
//                }
//                if (mTrimmerFlag == VideoTrimmer.DraggingFlag.LEFT || mTrimmerFlag == VideoTrimmer.DraggingFlag.RIGHT) {
//                    mSelectedClipStartTimeMs = trimmer.getLeftValue();
//                    mSelectedClipEndTimeMs = trimmer.getRightValue();
//                    loadClipInfo();
//                    return;
//                }
//                if (isInPlaybackState()) {
//                    seekTo((int) mSeekToPosition);
//                } else {
//                    mBtnPlay.setImageResource(R.drawable.ic_play_circle_outline_white_48dp);
//                    mBtnPlay.setVisibility(View.VISIBLE);
//                }
            }
        });
    }
}
