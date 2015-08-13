package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.orhanobut.logger.Logger;
import com.transee.ccam.Camera;
import com.transee.ccam.CameraManager;
import com.transee.common.GPSPath;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipPos;
import com.transee.vdb.ClipSet;
import com.transee.vdb.ImageDecoder;
import com.transee.vdb.Playlist;
import com.transee.vdb.PlaylistSet;
import com.transee.vdb.RemoteClip;
import com.transee.vdb.RemoteVdb;
import com.transee.vdb.Vdb;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.ui.adapters.ClipSetRecyclerAdapter;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2015/8/13.
 */
public class BrowseCameraActivity extends BaseActivity {
    private static final String TAG = BrowseCameraActivity.class.getSimpleName();

    private static final String IS_PC_SERVER = "isPcServer";
    private static final String SSID = "ssid";
    private static final String HOST_STRING = "hostString";

    private Vdb mVdb;
    private Camera mCamera;
    private ClipSetRecyclerAdapter mClipSetAdapter;

    private ClipSet mClipSet;
    private ImageDecoder mImageDecoder;

    ImageDecoder.Callback mDecoderCallback;

    public static void launch(Activity activity, boolean isPcServer, String ssid, String
        hostString) {
        Intent intent = new Intent(activity, BrowseCameraActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_PC_SERVER, isPcServer);
        bundle.putString(SSID, ssid);
        bundle.putString(HOST_STRING, hostString);
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    @Bind(R.id.rvCameraVideoList)
    RecyclerView mRvCameraVideoList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initCamera();
        Bundle bundle = getIntent().getExtras();
        String hostString = null;


        if (isServerActivity(bundle)) {
            hostString = getServerAddress(bundle);
        } else {
            mCamera = getCameraFromIntent(null);
            if (mCamera == null) {
                finish();
                return;
            }
            hostString = mCamera.getHostString();
        }


        mVdb.start(hostString);
    }

    @Override
    protected void init() {
        super.init();

        Bundle bundle = getIntent().getExtras();

        mVdb = new RemoteVdb(new BrowseCameraVdbCallback(), Hachi.getVideoDownloadPath(), isServerActivity(bundle));
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_browse_camera);

        initCameraVideoListView();
    }

    private void initCamera() {
        CameraManager cameraManager = ((Hachi) getApplication()).getCameraManager();
        if (cameraManager.getConnectedCameras().size() > 0) {
            mCamera = cameraManager.getConnectedCameras().get(0);

            mImageDecoder = new ImageDecoder();
            mImageDecoder.start();

            mDecoderCallback = new MyDecodeCallback();
        }

    }

    private void initCameraVideoListView() {
        mRvCameraVideoList.setLayoutManager(new LinearLayoutManager(this));
        mClipSet = mVdb.getClipSet(RemoteClip.TYPE_BUFFERED);

        mClipSetAdapter = new ClipSetRecyclerAdapter(mClipSet);
        mRvCameraVideoList.setAdapter(mClipSetAdapter);

    }


    public class BrowseCameraVdbCallback implements Vdb.Callback {

        @Override
        public void onConnectError(Vdb vdb) {

        }

        @Override
        public void onVdbMounted(Vdb vdb) {

        }

        @Override
        public void onVdbUnmounted(Vdb vdb) {

        }

        @Override
        public void onClipSetInfo(Vdb vdb, ClipSet clipSet) {
            if (clipSet.clipCat == Clip.CAT_REMOTE) {
                if (clipSet.clipType == RemoteClip.TYPE_BUFFERED) {
                    mClipSet = clipSet;
                    for (int i = 0; i < mClipSet.getCount(); i++) {
                        Clip clip = clipSet.getClip(i);
                        long clipTimeMs = clip.getStartTime();
                        ClipPos clipPos = new ClipPos(clip, clipTimeMs, ClipPos.TYPE_POSTER, false);
                        mVdb.getClient().requestClipImage(clip, clipPos, 0, 0);
                    }
                    mClipSetAdapter.setClipSet(clipSet);
                    return;
                }

            }

        }

        @Override
        public void onPlaylistSetInfo(Vdb vdb, PlaylistSet playlistSet) {

        }

        @Override
        public void onPlaylistChanged(Vdb vdb, Playlist playlist) {

        }

        @Override
        public void onImageData(Vdb vdb, ClipPos clipPos, byte[] data) {
            mImageDecoder.decode(data, 1032,  600, 5, clipPos, mDecoderCallback);
        }

        @Override
        public void onBitmapData(Vdb vdb, ClipPos clipPos, Bitmap bitmap) {

        }

        @Override
        public void onPlaylistIndexPicData(Vdb vdb, ClipPos clipPos, byte[] data) {

        }

        @Override
        public void onPlaybackUrlReady(Vdb vdb, VdbClient.PlaybackUrl playbackUrl) {

        }

        @Override
        public void onGetPlaybackUrlError(Vdb vdb) {

        }

        @Override
        public void onPlaylistPlaybackUrlReady(Vdb vdb, VdbClient.PlaylistPlaybackUrl playlistPlaybackUrl) {

        }

        @Override
        public void onGetPlaylistPlaybackUrlError(Vdb vdb) {

        }

        @Override
        public void onMarkClipResult(Vdb vdb, int error) {

        }

        @Override
        public void onDeleteClipResult(Vdb vdb, int error) {

        }

        @Override
        public void onInsertClipResult(Vdb vdb, int error) {

        }

        @Override
        public void onClipCreated(Vdb vdb, boolean isLive, Clip clip) {

        }

        @Override
        public void onClipChanged(Vdb vdb, boolean isLive, Clip clip) {

        }

        @Override
        public void onClipFinished(Vdb vdb, boolean isLive, Clip clip) {

        }

        @Override
        public void onClipInserted(Vdb vdb, boolean isLive, Clip clip) {

        }

        @Override
        public void onClipMoved(Vdb vdb, boolean isLive, Clip clip) {

        }

        @Override
        public void onClipRemoved(Vdb vdb, Clip.ID cid) {

        }

        @Override
        public void onPlaylistCleared(Vdb vdb, int plistId) {

        }

        @Override
        public void onRawDataResult(Vdb vdb, VdbClient.RawDataResult rawDataResult) {

        }

        @Override
        public void onRawDataBlock(Vdb vdb, VdbClient.RawDataBlock block) {

        }

        @Override
        public void onDownloadRawDataBlock(Vdb vdb, VdbClient.DownloadRawDataBlock block) {

        }

        @Override
        public void onGPSSegment(Vdb vdb, GPSPath.Segment segment) {

        }

        @Override
        public void onDownloadUrlFailed(Vdb vdb) {

        }

        @Override
        public void onDownloadUrlReady(Vdb vdb, VdbClient.DownloadInfoEx downloadInfo, boolean bFirstLoop) {

        }

        @Override
        public void onDownloadStarted(Vdb vdb, int id) {

        }

        @Override
        public void onDownloadFinished(Vdb vdb, Clip oldClip, Clip newClip) {

        }

        @Override
        public void onDownloadError(Vdb vdb, int id) {

        }

        @Override
        public void onDownloadProgress(Vdb vdb, int id, int progress) {

        }
    }

    class MyDecodeCallback implements ImageDecoder.Callback {

        @Override
        public void onDecodeDoneAsync(final Bitmap bitmap, Object tag) {
            final ClipPos clipPos = (ClipPos) tag;
            final int pos = mClipSet.findClipIndex(clipPos.cid);
            Logger.t(TAG).d("count: " + mClipSet.getCount() + "; pos: " + pos);
            if (pos == -1) {
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onImageDecode(bitmap, pos);
                }
            });
        }
    }

    void onImageDecode(Bitmap bitmap, int pos) {
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        mClipSetAdapter.setClipCover(bitmapDrawable, pos);
    }
}
