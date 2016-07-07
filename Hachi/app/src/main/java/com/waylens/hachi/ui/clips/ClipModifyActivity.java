package com.waylens.hachi.ui.clips;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.ClipEditEvent;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipExtentGetRequest;
import com.waylens.hachi.snipe.toolbox.ClipExtentUpdateRequest;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.clips.cliptrimmer.VideoTrimmer;
import com.waylens.hachi.ui.clips.player.ClipPlayFragment;
import com.waylens.hachi.ui.clips.player.ClipUrlProvider;
import com.waylens.hachi.ui.clips.player.UrlProvider;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;


/**
 * Created by Xiaofei on 2016/3/3.
 */
public class ClipModifyActivity extends BaseActivity {
    private static final String TAG = ClipModifyActivity.class.getSimpleName();

    private static final int MAX_EXTENSION = 1000 * 30;

    private static final String TAG_GET_EXTENT = "ClipModifyActivity.get.clip.extent";
    private static final String TAG_SET_EXTENT = "ClipModifyActivity.set.clip.extent";


    private ClipPlayFragment mClipPlayFragment;


    boolean hasUpdated;

    private int mOriginalTopMargin;
    private int mOriginalHeight;

    private EventBus mEventBus = EventBus.getDefault();

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
        ClipSet clipSet = new ClipSet(Clip.TYPE_TEMP);
        clipSet.addClip(clip);

        ClipSetManager.getManager().updateClipSet(ClipSetManager.CLIP_SET_TYPE_ENHANCE, clipSet);
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
        if (getClip().editInfo.selectedStartValue == getClip().getStartTimeMs() &&
            getClip().editInfo.getSelectedLength() == getClip().getDurationMs()) {
            return;
        }
        mVdbRequestQueue.add(new ClipExtentUpdateRequest(getClip().cid,
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

        UrlProvider vdtUriProvider = new ClipUrlProvider(mVdbRequestQueue, getClip().cid,
            getClip().editInfo.selectedStartValue,
            getClip().editInfo.getSelectedLength());


        mClipPlayFragment = ClipPlayFragment.newInstance(mVdtCamera, ClipSetManager.CLIP_SET_TYPE_ENHANCE, vdtUriProvider,
            ClipPlayFragment.ClipMode.SINGLE);
//        mClipPlayFragment.setShowsDialog(false);
        getFragmentManager().beginTransaction().replace(R.id.clipPlayFragment, mClipPlayFragment).commit();
    }

    private void getClipExtent() {
        if (getClip() == null || mVdbRequestQueue == null) {
            return;
        }
        mVdbRequestQueue.add(new ClipExtentGetRequest(getClip(), new VdbResponse.Listener<ClipExtent>() {
            @Override
            public void onResponse(ClipExtent clipExtent) {
                if (clipExtent != null) {
                    calculateExtension(clipExtent);
                    initClipTrimmer();
                }
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Logger.e("test", "", error);
            }
        }).setTag(TAG_GET_EXTENT));
    }


    private void calculateExtension(ClipExtent clipExtent) {
        Clip clip = getClip();
        clip.editInfo.minExtensibleValue = clipExtent.clipStartTimeMs - MAX_EXTENSION;
        if (clip.editInfo.minExtensibleValue < clipExtent.minClipStartTimeMs) {
            clip.editInfo.minExtensibleValue = clipExtent.minClipStartTimeMs;
        }
        clip.editInfo.maxExtensibleValue = clipExtent.clipEndTimeMs + MAX_EXTENSION;
        if (clip.editInfo.maxExtensibleValue > clipExtent.maxClipEndTimeMs) {
            clip.editInfo.maxExtensibleValue = clipExtent.maxClipEndTimeMs;
        }
        clip.editInfo.selectedStartValue = clipExtent.clipStartTimeMs;
        clip.editInfo.selectedEndValue = clipExtent.clipEndTimeMs;

//        bufferedCid = clipExtent.bufferedCid;
//        realCid = clipExtent.realCid;

        if (clipExtent.bufferedCid != null) {
            clip.editInfo.bufferedCid = clipExtent.bufferedCid;
        }

        if (clipExtent.realCid != null) {
            clip.editInfo.realCid = clipExtent.realCid;
        }
    }

    private void initClipTrimmer() {
        int defaultHeight = ViewUtils.dp2px(64);
        final Clip clip = getClip();
        mClipTrimmer.setBackgroundClip(clip, defaultHeight);
        mClipTrimmer.setEditing(true);
        mClipTrimmer.setInitRangeValues(clip.editInfo.minExtensibleValue, clip.editInfo.maxExtensibleValue);
        mClipTrimmer.setLeftValue(clip.editInfo.selectedStartValue);
        mClipTrimmer.setRightValue(clip.editInfo.selectedEndValue);
        mClipTrimmer.setOnChangeListener(new VideoTrimmer.OnTrimmerChangeListener() {
            @Override
            public void onStartTrackingTouch(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag) {
            }

            @Override
            public void onProgressChanged(VideoTrimmer trimmer, VideoTrimmer.DraggingFlag flag, long start, long end, long progress) {
                long currentTimeMs = 0;
                if (flag == VideoTrimmer.DraggingFlag.LEFT) {
                    currentTimeMs = start;
                    clip.editInfo.selectedStartValue = start;
                } else if (flag == VideoTrimmer.DraggingFlag.RIGHT) {
                    currentTimeMs = end;
                    clip.editInfo.selectedEndValue = end;
                } else {
                    currentTimeMs = progress;
                }
                mClipPlayFragment.showClipPosThumbnail(clip, currentTimeMs);

                mEventBus.post(new ClipEditEvent());
            }

            @Override
            public void onStopTrackingTouch(VideoTrimmer trimmer) {
                UrlProvider vdtUriProvider = new ClipUrlProvider(mVdbRequestQueue,
                    clip.editInfo.bufferedCid, clip.editInfo.selectedStartValue,
                    clip.editInfo.getSelectedLength());
                mClipPlayFragment.setUrlProvider(vdtUriProvider, clip.editInfo.selectedStartValue);
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


    private Clip getClip() {
        return ClipSetManager.getManager().getClipSet(ClipSetManager.CLIP_SET_TYPE_ENHANCE).getClip(0);
    }
}
