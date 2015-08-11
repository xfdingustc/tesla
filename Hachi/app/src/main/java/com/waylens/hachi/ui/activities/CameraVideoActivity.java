package com.waylens.hachi.ui.activities;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.transee.ccam.Camera;
import com.transee.common.GPSPath;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipPos;
import com.transee.vdb.ClipSet;
import com.transee.vdb.ImageDecoder;
import com.transee.vdb.LocalVdb;
import com.transee.vdb.Playlist;
import com.transee.vdb.PlaylistSet;
import com.transee.vdb.RemoteVdb;
import com.transee.vdb.Vdb;
import com.transee.vdb.VdbClient.DownloadInfoEx;
import com.transee.vdb.VdbClient.DownloadRawDataBlock;
import com.transee.vdb.VdbClient.PlaybackUrl;
import com.transee.vdb.VdbClient.PlaylistPlaybackUrl;
import com.transee.vdb.VdbClient.RawDataBlock;
import com.transee.vdb.VdbClient.RawDataResult;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;

public class CameraVideoActivity extends BaseActivity {
    private static final String TAG = CameraVideoActivity.class.getSimpleName();

    private Camera mCamera;
    private Vdb mVdb;
    private ImageDecoder mImageDecoder;

    private CameraVideoList mCameraVideoList;
    private CameraVideoEdit mCameraVideoEdit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(savedInstanceState);
    }


    protected void init(Bundle savedInstanceState) {

        Bundle bundle = getIntent().getExtras();

        if (isLocalActivity(bundle)) {
            mVdb = new LocalVdb(this, new MyVdbCallback());
        } else {
            mVdb = new RemoteVdb(new MyVdbCallback(), Hachi.getVideoDownloadPath(), isServerActivity(bundle));
        }

        mImageDecoder = new ImageDecoder();
        mImageDecoder.start();

        mCameraVideoList = new MyCameraVideoList(this, isPcServer(bundle), mVdb, mImageDecoder);
        mCameraVideoEdit = new MyCameraVideoEdit(this, mVdb, mImageDecoder);

        mCameraVideoList.onCreateActivity(savedInstanceState);
        mCameraVideoEdit.onCreateActivity(savedInstanceState);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_camera_video);

        TextView titleText = (TextView) findViewById(R.id.titleBar);
        if (mVdb.isLocal()) {
            titleText.setText(R.string.title_activity_downloaded_video);
        } else {
            String text = getResources().getString(R.string.title_activity_camera_video);
            if (mCamera != null) {
                String name = Camera.getCameraStates(mCamera).mCameraName;
                if (name.length() == 0) {
                    name = getResources().getString(R.string.lable_camera_noname);
                }
                text += " - " + name;
            }
            titleText.setText(text);
        }


        mCameraVideoList.onInitUI();
        mCameraVideoEdit.onInitUI();

        mCameraVideoList.onSetupUI();
        mCameraVideoEdit.onSetupUI();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraVideoList.onDestroyActivity();
        mCameraVideoEdit.onDestroyActivity();

        mImageDecoder.interrupt();
        mImageDecoder = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Bundle bundle = getIntent().getExtras();
        String hostString = null;

        if (!isLocalActivity(bundle)) {
            if (isServerActivity(bundle)) {
                hostString = getServerAddress(bundle);
            } else {
                mCamera = getCameraFromIntent(null);
                if (mCamera == null) {
                    performFinish();
                    return;
                }
                hostString = mCamera.getHostString();
            }
        }

        mCameraVideoList.onStartActivity(hostString);
        mCameraVideoEdit.onStartActivity();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCameraVideoList.onStopActivity();
        mCameraVideoEdit.onStopActivity();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraVideoList.onPauseActivity();
        mCameraVideoEdit.onPauseActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraVideoList.onResumeActivity();
        mCameraVideoEdit.onResumeActivity();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mCameraVideoEdit.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        mCameraVideoEdit.onLowMemory();
        super.onLowMemory();
    }


    protected void onReleaseUI() {
        mCameraVideoList.onReleaseUI();
        mCameraVideoEdit.onReleaseUI();
    }


    @Override
    public void onBackPressed() {
        if (isEditing()) {
            // exit editing
            mCameraVideoEdit.endEdit(false);
        } else {
            // finish activity
            performFinish();
        }
    }

    private void performFinish() {
        if (!isFinishing()) {
            finish();
            Hachi.slideOutToRight(this, false);
        }
    }

    private final boolean isEditing() {
        return mCameraVideoEdit.isEditing();
    }

    private void onVdbImageData(ClipPos clipPos, byte[] data) {
        if (mImageDecoder != null) {
            switch (clipPos.getType()) {
                case ClipPos.TYPE_POSTER:
                    // video list accepts posters
                    mCameraVideoList.decodeImage(clipPos, data);
                    break;
                case ClipPos.TYPE_PREVIEW:
                    // clip preview
                    if (!isEditing()) {
                        mCameraVideoList.decodeImage(clipPos, data);
                    }
                    break;
                default:
                    // slide, animation, preview
                    if (isEditing()) {
                        mCameraVideoEdit.decodeImage(clipPos, data);
                    }
                    break;
            }
        }
    }

    private void onBitmapData(ClipPos clipPos, Bitmap bitmap) {
        if (mImageDecoder != null) {
            if (clipPos.getType() == ClipPos.TYPE_POSTER) {
                // TODO
            } else {
                // slide, animation
                if (isEditing()) {
                    mCameraVideoEdit.onBitmapDecoded(clipPos, bitmap);
                }
            }
        }
    }

    private void onPlaylistIndexPicData(ClipPos clipPos, byte[] data) {
        if (mImageDecoder != null) {
            if (clipPos.getType() == ClipPos.TYPE_POSTER) {
                mCameraVideoList.decodePlaylistIndexPic(clipPos, data);
            }
        }
    }

    // ===============================================================================
    // CameraVideoList
    // ===============================================================================

    class MyCameraVideoList extends CameraVideoList {

        MyCameraVideoList(BaseActivity activity, boolean bIsPcServer, Vdb vdb, ImageDecoder decoder) {
            super(activity, bIsPcServer, vdb, decoder);
        }

        @Override
        protected void onClickClipItem(Bitmap bitmap, Clip clip, int offset, int size) {
            if (clip.isDownloading()) {
                // TODO : display info & prompt for cancel
            } else {
                if (!isEditing()) {
                    mCameraVideoEdit.editClip(bitmap, clip, offset, size);
                }
            }
        }

        @Override
        protected void onClickPlaylistItem(Bitmap bitmap, Playlist playlist) {
            if (!isEditing()) {
                mCameraVideoEdit.editPlaylist(bitmap, playlist);
            }
        }

    }

    // ===============================================================================
    // CameraVideoEdit
    // ===============================================================================

    class MyCameraVideoEdit extends CameraVideoEdit {

        public MyCameraVideoEdit(BaseActivity activity, Vdb vdb, ImageDecoder decoder) {
            super(activity, vdb, decoder);
        }

        @Override
        public void requestDeleteClip(Clip.ID cid) {
            mCameraVideoList.requestDeleteClip(cid);
        }

        @Override
        public void requestClearPlaylist(int plistId) {
            mCameraVideoList.requestClearPlaylist(plistId);
        }

        @Override
        public Rect getClipThumbnailRect(Clip.ID cid, View other) {
            return mCameraVideoList.getClipThumbnailRect(cid, other);
        }

        @Override
        public Rect getPlaylistThumbnailRect(int plistId, View other) {
            return mCameraVideoList.getPlaylistThumbnailRect(plistId, other);
        }

        @Override
        public void onBeginEdit() {
            mCameraVideoList.hide();
        }

        @Override
        public void onEndEdit() {
            mCameraVideoList.show();
        }

    }

    private void updateSpaceInfo() {
        if (mCamera != null) {
            mCamera.getClient().cmd_Cam_get_getStorageInfor();
        }
    }

    // ===============================================================================
    // Vdb.Callback
    // ===============================================================================

    class MyVdbCallback implements Vdb.Callback {

        @Override
        public void onConnectError(Vdb vdb) {
            // TODO - if dialog/popupmenu exists
            performFinish();
        }

        @Override
        public void onVdbMounted(Vdb vdb) {
        }

        @Override
        public void onVdbUnmounted(Vdb vdb) {
            mCameraVideoEdit.endEdit(false);
            mCameraVideoList.onVdbUnmounted();
        }

        @Override
        public void onClipSetInfo(Vdb vdb, ClipSet clipSet) {
            mCameraVideoList.onClipSetInfo(clipSet);
        }

        @Override
        public void onPlaylistSetInfo(Vdb vdb, PlaylistSet playlistSet) {
            mCameraVideoList.onPlaylistSetInfo(playlistSet);
        }

        @Override
        public void onPlaylistChanged(Vdb vdb, Playlist playlist) {
            updateSpaceInfo();
        }

        @Override
        public void onImageData(Vdb vdb, ClipPos clipPos, byte[] data) {
            CameraVideoActivity.this.onVdbImageData(clipPos, data);
        }

        @Override
        public void onBitmapData(Vdb vdb, ClipPos clipPos, Bitmap bitmap) {
            CameraVideoActivity.this.onBitmapData(clipPos, bitmap);
        }

        @Override
        public void onPlaylistIndexPicData(Vdb vdb, ClipPos clipPos, byte[] data) {
            CameraVideoActivity.this.onPlaylistIndexPicData(clipPos, data);
        }

        @Override
        public void onPlaybackUrlReady(Vdb vdb, PlaybackUrl playbackUrl) {
            if (isEditing()) {
                mCameraVideoEdit.onPlaybackUrl(playbackUrl);
            }
        }

        @Override
        public void onGetPlaybackUrlError(Vdb vdb) {
            if (isEditing()) {
                mCameraVideoEdit.onPlaybackUrlError();
            }
        }

        @Override
        public void onPlaylistPlaybackUrlReady(Vdb vdb, PlaylistPlaybackUrl playlistPlaybackUrl) {
            if (isEditing()) {
                mCameraVideoEdit.onPlaylistPlaybackUrl(playlistPlaybackUrl);
            }
        }

        @Override
        public void onGetPlaylistPlaybackUrlError(Vdb vdb) {
            if (isEditing()) {
                mCameraVideoEdit.onPlaylistPlaybackUrlError();
            }
        }

        @Override
        public void onMarkClipResult(Vdb vdb, int error) {
            if (isEditing()) {
                mCameraVideoEdit.onMarkClipResult(error);
            }
        }

        @Override
        public void onDeleteClipResult(Vdb vdb, int error) {
            // TODO
        }

        @Override
        public void onInsertClipResult(Vdb vdb, int error) {
            if (isEditing()) {
                mCameraVideoEdit.onInsertClipResult(error);
            }
        }

        @Override
        public void onClipCreated(Vdb vdb, boolean isLive, Clip clip) {
            mCameraVideoList.onClipCreated(isLive, clip);
        }

        @Override
        public void onClipChanged(Vdb vdb, boolean isLive, Clip clip) {
            mCameraVideoList.onClipChanged(clip, false);
            if (isEditing()) {
                mCameraVideoEdit.onClipChanged(isLive, clip, false);
            }
            updateSpaceInfo();
        }

        @Override
        public void onClipFinished(Vdb vdb, boolean isLive, Clip clip) {
            mCameraVideoList.onClipChanged(clip, true);
            if (isEditing()) {
                mCameraVideoEdit.onClipChanged(isLive, clip, true);
            }
            updateSpaceInfo();
        }

        @Override
        public void onClipInserted(Vdb vdb, boolean isLive, Clip clip) {
            mCameraVideoList.onClipInserted(clip);
            if (isEditing()) {
                mCameraVideoEdit.onClipInserted(isLive, clip);
            }
        }

        @Override
        public void onClipMoved(Vdb vdb, boolean isLive, Clip clip) {
            mCameraVideoList.onClipMoved(isLive, clip);
            if (isEditing()) {
                mCameraVideoEdit.onClipMoved(isLive, clip);
            }
        }

        @Override
        public void onClipRemoved(Vdb vdb, Clip.ID cid) {
            mCameraVideoList.onClipRemoved(cid);
            if (isEditing()) {
                mCameraVideoEdit.onClipRemoved(cid);
            }
            updateSpaceInfo();
        }

        @Override
        public void onPlaylistCleared(Vdb vdb, int plistId) {
            mCameraVideoList.onPlaylistCleared(plistId);
            if (isEditing()) {
                mCameraVideoEdit.onPlaylistCleared(plistId);
            }
            updateSpaceInfo();
        }

        @Override
        public void onRawDataResult(Vdb vdb, RawDataResult rawDataResult) {
            if (isEditing()) {
                mCameraVideoEdit.onRawDataResult(rawDataResult);
            }
        }

        @Override
        public void onRawDataBlock(Vdb vdb, RawDataBlock block) {
            if (isEditing()) {
                mCameraVideoEdit.onRawDataBlock(block);
            }
        }

        @Override
        public void onDownloadRawDataBlock(Vdb vdb, DownloadRawDataBlock block) {
            if (isEditing()) {
                mCameraVideoEdit.onDownloadRawDataBlock(block);
            }
        }

        @Override
        public void onDownloadUrlFailed(Vdb vdb) {
            if (isEditing()) {
                mCameraVideoEdit.onDownloadUrlFailed();
            }
        }

        @Override
        public void onDownloadUrlReady(Vdb vdb, DownloadInfoEx downloadInfo, boolean bFirstLoop) {
            if (isEditing()) {
                mCameraVideoEdit.onDownloadUrlReady(downloadInfo, bFirstLoop);
            }
        }

        @Override
        public void onDownloadStarted(Vdb vdb, int id) {
            mCameraVideoList.onDownloadStarted(id);
        }

        @Override
        public void onDownloadFinished(Vdb vdb, Clip oldClip, Clip newClip) {
            mCameraVideoList.onDownloadFinished(oldClip, newClip);
        }

        @Override
        public void onDownloadError(Vdb vdb, int id) {
            mCameraVideoList.onDownloadError(id);
        }

        @Override
        public void onDownloadProgress(Vdb vdb, int id, int progress) {
            mCameraVideoList.onDownloadProgress(id, progress);
        }

        @Override
        public void onGPSSegment(Vdb vdb, GPSPath.Segment segment) {
            if (isEditing()) {
                mCameraVideoEdit.onGPSSegment(segment);
            }
        }

    }

}
