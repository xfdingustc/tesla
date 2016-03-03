package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipExtentUpdateRequest;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.ClipUrlProvider;
import com.waylens.hachi.ui.fragments.clipplay2.UrlProvider;
import com.waylens.hachi.ui.views.cliptrimmer.VideoTrimmer;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/3/3.
 */
public class ClipModifyActivity extends BaseActivity {
    private static final String TAG = ClipModifyActivity.class.getSimpleName();

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


    @Override
    public void setupToolbar() {
        mToolbar.setTitle(R.string.modify);
        mToolbar.inflateMenu(R.menu.menu_clip_modify);


        mToolbar.setNavigationIcon(R.drawable.navbar_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.save:
                        doSaveClipTrimInfo();
                        finish();
                        break;
                }
                return false;
            }
        });

    }

    private void doSaveClipTrimInfo() {
        mVdbRequestQueue.add(new ClipExtentUpdateRequest(mClip,
                mClipTrimmer.getLeftValue(),
                mClipTrimmer.getRightValue(),
                new VdbResponse.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
                        //doGetPlaylistInfo(ACTION_TRIM);
                        Logger.t(TAG).d("Trim successfully");
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

    private void embedVideoPlayFragment() {
        ClipPlayFragment.Config config = new ClipPlayFragment.Config();
        config.clipMode = ClipPlayFragment.Config.ClipMode.SINGLE;

        UrlProvider vdtUriProvider = new ClipUrlProvider(mVdbRequestQueue, mClip);
        ClipSet clipSet = new ClipSet(Clip.TYPE_TEMP);
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
                long currentTimeMs = 0;
                if (flag == VideoTrimmer.DraggingFlag.LEFT) {
                    currentTimeMs = start;
                } else if (flag == VideoTrimmer.DraggingFlag.RIGHT) {
                    currentTimeMs = end;
                } else {
                    currentTimeMs = progress;
                }
                mClipPlayFragment.showClipPosThumbnail(mClip, currentTimeMs);
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
