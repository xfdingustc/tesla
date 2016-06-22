package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipExtentGetRequest;
import com.waylens.hachi.snipe.toolbox.ClipExtentUpdateRequest;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.ui.clips.player.ClipPlayFragment;
import com.waylens.hachi.ui.clips.player.ClipUrlProvider;
import com.waylens.hachi.ui.clips.player.UrlProvider;
import com.waylens.hachi.ui.clips.cliptrimmer.VideoTrimmer;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

import butterknife.BindView;


/**
 * Created by Xiaofei on 2016/3/3.
 */
public class ClipModifyActivity extends BaseActivity {
    private static final String TAG = ClipModifyActivity.class.getSimpleName();

    private static final String TAG_GET_EXTENT = "ClipModifyActivity.get.clip.extent";
    private static final String TAG_SET_EXTENT = "ClipModifyActivity.set.clip.extent";


    private ClipPlayFragment mClipPlayFragment;

    SharableClip mSharableClip;

    boolean hasUpdated;

    private int mOriginalTopMargin;
    private int mOriginalHeight;

    public static void launch(Activity activity, Clip clip) {
        Intent intent = new Intent(activity, ClipModifyActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("clip", clip);
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    @BindView(R.id.clipPlayFragment)
    FrameLayout mPlayerContainer;

    @BindView(R.id.clipTrimmer)
    VideoTrimmer mClipTrimmer;

    @BindView(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @BindView(R.id.root_view)
    View mRootView;


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mPlayerContainer.getLayoutParams();
        Logger.t(TAG).d("newConfig.orientation " + newConfig.orientation);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getToolbar().setVisibility(View.GONE);
            layoutParams.topMargin = 0;
            layoutParams.width = ViewGroup.MarginLayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.MarginLayoutParams.MATCH_PARENT;
            mPlayerContainer.setLayoutParams(layoutParams);
        } else {
            getToolbar().setVisibility(View.VISIBLE);
            layoutParams.width = ViewGroup.MarginLayoutParams.MATCH_PARENT;
            layoutParams.height = mOriginalHeight;

        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
//        mVdbRequestQueue = Snipe.newRequestQueue(this);
        initCamera();

        Bundle bundle = getIntent().getExtras();
        Clip clip = bundle.getParcelable("clip");
        if (clip != null) {
            mSharableClip = new SharableClip(clip);
        }
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_clip_modify);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mPlayerContainer.getLayoutParams();
        mOriginalTopMargin = layoutParams.topMargin;
        mOriginalHeight = layoutParams.height;
        setupToolbar();
        mViewAnimator.setDisplayedChild(0);
        embedVideoPlayFragment();
        getClipExtent();
    }


    @Override
    public void setupToolbar() {
        getToolbar().setTitle(R.string.modify);
        getToolbar().inflateMenu(R.menu.menu_clip_modify);


        getToolbar().setNavigationIcon(R.drawable.navbar_close);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasUpdated) {
                    LocalBroadcastManager.getInstance(ClipModifyActivity.this).sendBroadcast(new Intent(TagFragment.ACTION_RETRIEVE_CLIPS));
                }
                finish();
            }
        });
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.save:
                        doSaveClipTrimInfo();
                        break;
                }
                return false;
            }
        });

    }

    private void doSaveClipTrimInfo() {
        if (mSharableClip.selectedStartValue == mSharableClip.clip.getStartTimeMs() &&
                mSharableClip.getSelectedLength() == mSharableClip.clip.getDurationMs()) {
            return;
        }
        mVdbRequestQueue.add(new ClipExtentUpdateRequest(mSharableClip.clip.cid,
                mClipTrimmer.getLeftValue(),
                mClipTrimmer.getRightValue(),
                new VdbResponse.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
                        hasUpdated = true;
                        Snackbar.make(mRootView, R.string.bookmark_update_successful, Snackbar.LENGTH_SHORT).show();
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Snackbar.make(mRootView, R.string.bookmark_update_error, Snackbar.LENGTH_SHORT).show();
                    }
                }
        ).setTag(TAG_SET_EXTENT));
    }

    private void embedVideoPlayFragment() {

        UrlProvider vdtUriProvider = new ClipUrlProvider(mVdbRequestQueue,
                mSharableClip.bufferedCid, 0,
                mSharableClip.getSelectedLength());
        ClipSet clipSet = new ClipSet(Clip.TYPE_TEMP);
        clipSet.addClip(mSharableClip.clip);

        ClipSetManager.getManager().updateClipSet(ClipSetManager.CLIP_SET_TYPE_ENHANCE, clipSet);

        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, ClipSetManager.CLIP_SET_TYPE_ENHANCE, vdtUriProvider,
            ClipPlayFragment.ClipMode.SINGLE);
//        mClipPlayFragment.setShowsDialog(false);
        getFragmentManager().beginTransaction().replace(R.id.clipPlayFragment, mClipPlayFragment).commit();
    }

    void getClipExtent() {
        if (mSharableClip == null || mVdbRequestQueue == null) {
            return;
        }
        mVdbRequestQueue.add(new ClipExtentGetRequest(mSharableClip.clip, new VdbResponse.Listener<ClipExtent>() {
            @Override
            public void onResponse(ClipExtent clipExtent) {
                if (clipExtent != null) {
                    mSharableClip.calculateExtension(clipExtent);
                    initClipTrimmer();
                }
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e("test", "", error);
            }
        }).setTag(TAG_GET_EXTENT));
    }

    private void initClipTrimmer() {
        int defaultHeight = ViewUtils.dp2px(64);
        mClipTrimmer.setBackgroundClip(mVdbImageLoader, mSharableClip.clip, defaultHeight);
        mClipTrimmer.setEditing(true);
        mClipTrimmer.setInitRangeValues(mSharableClip.minExtensibleValue, mSharableClip.maxExtensibleValue);
        mClipTrimmer.setLeftValue(mSharableClip.selectedStartValue);
        mClipTrimmer.setRightValue(mSharableClip.selectedEndValue);
        mClipTrimmer.setOnChangeListener(new VideoTrimmer.OnTrimmerChangeListener() {
            @Override
            public void onStartTrackingTouch(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag) {
            }

            @Override
            public void onProgressChanged(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag, long start, long end, long progress) {
                long currentTimeMs = 0;
                if (flag == VideoTrimmer.DraggingFlag.LEFT) {
                    currentTimeMs = start;
                    mSharableClip.selectedStartValue = start;
                } else if (flag == VideoTrimmer.DraggingFlag.RIGHT) {
                    currentTimeMs = end;
                    mSharableClip.selectedEndValue = end;
                } else {
                    currentTimeMs = progress;
                }
                mClipPlayFragment.showClipPosThumbnail(mSharableClip.clip, currentTimeMs);
            }

            @Override
            public void onStopTrackingTouch(VideoTrimmer trimmer) {
                UrlProvider vdtUriProvider = new ClipUrlProvider(mVdbRequestQueue,
                        mSharableClip.bufferedCid, 0,
                        mSharableClip.getSelectedLength());
                mClipPlayFragment.setUrlProvider(vdtUriProvider);
            }
        });

        mViewAnimator.setDisplayedChild(1);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVdbRequestQueue.cancelAll(TAG_GET_EXTENT);
        mVdbRequestQueue.cancelAll(TAG_SET_EXTENT);
    }
}
