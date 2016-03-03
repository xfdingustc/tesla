package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

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
    }
}
