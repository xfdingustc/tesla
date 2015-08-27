package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.adapters.ClipFragmentRvAdapter;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2015/8/14.
 */
public class ClipEditActivity extends BaseActivity {

    @Bind(R.id.ivPreviewPicture)
    ImageView mIvPreviewPicture;

    @Bind(R.id.rvClipFragments)
    RecyclerView mRvClipFragments;


    private static VdtCamera mSharedCamera;
    private static Clip mSharedClip;

    private VdtCamera mVdtCamera;
    private Clip mClip;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mVdbImageLoader;


    private ClipFragmentRvAdapter mClipFragmentAdapter;

    public static void launch(Context context, VdtCamera vdtCamera, Clip clip) {
        Intent intent = new Intent(context, ClipEditActivity.class);
        mSharedCamera = vdtCamera;
        mSharedClip = clip;
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mVdtCamera = mSharedCamera;
        mClip = mSharedClip;
        mVdbRequestQueue = Snipe.newRequestQueue(this,mVdtCamera.getVdbConnection());
        mVdbImageLoader = new VdbImageLoader(mVdbRequestQueue);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_clip_editor);
        ClipPos clipPos = new ClipPos(mClip, mClip.getStartTime(), ClipPos.TYPE_POSTER, false);
        mVdbImageLoader.displayVdbImage(clipPos, mIvPreviewPicture);


        initClipFragmentRecyclerView();
    }

    private void initClipFragmentRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRvClipFragments.setLayoutManager(linearLayoutManager);

        mClipFragmentAdapter = new ClipFragmentRvAdapter(this, mClip, mVdbRequestQueue);
        mRvClipFragments.setAdapter(mClipFragmentAdapter);


    }
}
