package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.ui.adapters.CameraClipSetAdapter;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2015/8/13.
 */
public class BrowseCameraActivity extends BaseActivity {
    private static final String TAG = BrowseCameraActivity.class.getSimpleName();

    private static final String IS_PC_SERVER = "isPcServer";
    private static final String SSID = "ssid";
    private static final String HOST_STRING = "hostString";


    private VdtCamera mVdtCamera;
    private CameraClipSetAdapter mClipSetAdapter = null;

    private VdbRequestQueue mVdbRequestQueue;


    public static void launch(Context context, VdtCamera camera) {
        Intent intent = new Intent(context, BrowseCameraActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_PC_SERVER, camera.isPcServer());
        bundle.putString(SSID, camera.getSSID());
        bundle.putString(HOST_STRING, camera.getHostString());
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    @Bind(R.id.rvCameraVideoList)
    RecyclerView mRvCameraVideoList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        initViews();
        initCamera();
        Bundle bundle = getIntent().getExtras();
        String hostString = null;


        if (isServerActivity(bundle)) {
            hostString = getServerAddress(bundle);
        } else {
            mVdtCamera = getCameraFromIntent(null);
            if (mVdtCamera == null) {
                finish();
                return;
            }
            hostString = mVdtCamera.getHostString();
        }

        mVdbRequestQueue = Snipe.newRequestQueue(this);

        initCameraVideoListView();
    }

    private void initViews() {
        setContentView(R.layout.activity_browse_camera);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void initCamera() {
        VdtCameraManager vdtCameraManager = VdtCameraManager.getManager();
        if (vdtCameraManager.getConnectedCameras().size() > 0) {
            mVdtCamera = vdtCameraManager.getConnectedCameras().get(0);

        }

    }

    private void initCameraVideoListView() {
        mRvCameraVideoList.setLayoutManager(new LinearLayoutManager(this));
        mClipSetAdapter = new CameraClipSetAdapter(BrowseCameraActivity.this, mVdbRequestQueue);
        mRvCameraVideoList.setAdapter(mClipSetAdapter);


        ClipSetRequest request = new ClipSetRequest(Clip.TYPE_BUFFERED, ClipSetRequest
            .FLAG_CLIP_EXTRA,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet clipSet) {
                    mClipSetAdapter.setClipSet(clipSet);
                }

            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);


    }


}
