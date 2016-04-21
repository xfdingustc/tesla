package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ViewAnimator;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipExtentGetRequest;
import com.waylens.hachi.snipe.toolbox.ClipExtentUpdateRequest;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.ui.fragments.TaggedClipFragment;
import com.waylens.hachi.ui.fragments.clipplay2.ClipPlayFragment;
import com.waylens.hachi.ui.fragments.clipplay2.ClipUrlProvider;
import com.waylens.hachi.ui.fragments.clipplay2.UrlProvider;
import com.waylens.hachi.ui.views.cliptrimmer.VideoTrimmer;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/3/3.
 */
public class ClipModifyActivity extends BaseActivity {
    private static final String TAG = ClipModifyActivity.class.getSimpleName();

    private static final String TAG_GET_EXTENT = "ClipModifyActivity.get.clip.extent";
    private static final String TAG_SET_EXTENT = "ClipModifyActivity.set.clip.extent";

    private VdtCamera mVdtCamera;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;
    private ClipPlayFragment mClipPlayFragment;

    SharableClip mSharableClip;

    boolean hasUpdated;

    public static void launch(Activity activity, Clip clip) {
        Intent intent = new Intent(activity, ClipModifyActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("clip", clip);
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    @Bind(R.id.clipTrimmer)
    VideoTrimmer mClipTrimmer;

    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;

    @Bind(R.id.root_view)
    View mRootView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
//        mVdbRequestQueue = Snipe.newRequestQueue(this);
        mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
        Bundle bundle = getIntent().getExtras();
        Clip clip = bundle.getParcelable("clip");
        if (clip != null) {
            mSharableClip = new SharableClip(clip);
        }
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_clip_modify);
        mViewAnimator.setDisplayedChild(0);
        embedVideoPlayFragment();
        getClipExtent();
    }


    @Override
    public void setupToolbar() {
        mToolbar.setTitle(R.string.modify);
        mToolbar.inflateMenu(R.menu.menu_clip_modify);


        mToolbar.setNavigationIcon(R.drawable.navbar_close);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasUpdated) {
                    LocalBroadcastManager.getInstance(ClipModifyActivity.this).sendBroadcast(new Intent(TaggedClipFragment.ACTION_RETRIEVE_CLIPS));
                }
                finish();
            }
        });
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
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
        if (mSharableClip == null) {
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
        int defaultHeight = ViewUtils.dp2px(64, getResources());
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
