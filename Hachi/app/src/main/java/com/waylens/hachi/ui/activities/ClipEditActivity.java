package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

import com.orhanobut.logger.Logger;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.adapters.ClipFragmentRvAdapter;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/8/14.
 */
public class ClipEditActivity extends BaseActivity {
    private static final String TAG = ClipEditActivity.class.getSimpleName();

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


    @OnClick(R.id.btnPlay)
    public void onBtnPlayClicked() {
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTime());

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(mClip, parameters, new VdbResponse.Listener<VdbClient.PlaybackUrl>() {
            @Override
            public void onResponse(VdbClient.PlaybackUrl response) {
                Logger.t(TAG).d("On Response!!!!!!! " + response.url);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });

        mVdbRequestQueue.add(request);
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
