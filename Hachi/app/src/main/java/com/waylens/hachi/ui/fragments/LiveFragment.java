package com.waylens.hachi.ui.fragments;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

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
 * Live Fragment
 * <p/>
 * Created by Xiaofei on 2015/8/4.
 */
public class LiveFragment extends BaseFragment {

    private static final String TAG = "LiveFragment";
    @Bind(R.id.view_animator)
    ViewAnimator mViewAnimator;
    @Bind(R.id.video_list_view)
    RecyclerView mVideoListView;

    Camera mCamera;
    Vdb mVdb;
    ImageDecoder mImageDecoder;
    ClipSet mClipSet;
    ImageDecoder.Callback mDecoderCallback;
    LinearLayoutManager mLinearLayoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        initCamera();
        initViews();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createFragmentView(inflater, container, R.layout.fragment_live, savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mVdb != null) {
            mVdb.stop();
        }
    }

    private void initViews() {
        mVideoListView.setHasFixedSize(true);
    }

    private void initCamera() {
        CameraManager cameraManager = CameraManager.getManager();
        if (cameraManager.getConnectedCameras().size() > 0) {
            mCamera = cameraManager.getConnectedCameras().get(0);
            mVdb = new RemoteVdb(new MyVdbCallback(), Hachi.getVideoDownloadPath(), false);
            mImageDecoder = new ImageDecoder();
            mImageDecoder.start();
            mVdb.start(mCamera.getHostString());
            mDecoderCallback = new MyDecodeCallback();
        }

    }

    class MyVdbCallback implements Vdb.Callback {

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
            mViewAnimator.setDisplayedChild(1);
            switch (clipSet.clipType) {
                case RemoteClip.TYPE_BUFFERED:
                    ClipSetRecyclerAdapter adapter = new ClipSetRecyclerAdapter(clipSet);
                    mVideoListView.setAdapter(adapter);
                    mVideoListView.setLayoutManager(mLinearLayoutManager);
                    mClipSet = clipSet;
                    for (int i = 0; i < mClipSet.getCount(); i++) {
                        Clip clip = clipSet.getClip(i);
                        long clipTimeMs = clip.getStartTime();
                        ClipPos clipPos = new ClipPos(clip, clipTimeMs, ClipPos.TYPE_POSTER, false);
                        mVdb.getClient().requestClipImage(clip, clipPos, 0, 0);
                        mVdb.getClient().requestClipPlaybackUrl(VdbClient.URL_TYPE_HLS, clip, VdbClient.STREAM_SUB_1, false, clipTimeMs, clip.clipLengthMs);
                    }
                    break;
            }

            //Logger.t(TAG).e("Count: " + clipSet.getCount());
        }

        @Override
        public void onPlaylistSetInfo(Vdb vdb, PlaylistSet playlistSet) {

        }

        @Override
        public void onPlaylistChanged(Vdb vdb, Playlist playlist) {

        }

        @Override
        public void onImageData(Vdb vdb, ClipPos clipPos, byte[] data) {
            mImageDecoder.decode(data, 1032,
                    600, 5, clipPos, mDecoderCallback);
        }

        @Override
        public void onBitmapData(Vdb vdb, ClipPos clipPos, Bitmap bitmap) {

        }

        @Override
        public void onPlaylistIndexPicData(Vdb vdb, ClipPos clipPos, byte[] data) {

        }

        @Override
        public void onPlaybackUrlReady(Vdb vdb, final VdbClient.PlaybackUrl playbackUrl) {
            if (mVideoListView == null) {
                return;
            }
            final int pos = mClipSet.findClipIndex(playbackUrl.cid);
            if (pos == -1) {
                return;
            }
            LiveFragment.this.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mVideoListView == null) {
                        return;
                    }
                    ((ClipSetRecyclerAdapter) mVideoListView.getAdapter()).setPlaybackURL(playbackUrl.url, pos);
                }
            });

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
            if (mVideoListView == null) {
                return;
            }

            final ClipPos clipPos = (ClipPos) tag;
            final int pos = mClipSet.findClipIndex(clipPos.cid);
            //Logger.t(TAG).e("count: " + mClipSet.getCount() + "; pos: " + pos);
            if (pos == -1) {
                return;
            }
            LiveFragment.this.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mVideoListView == null) {
                        return;
                    }
                    BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
                    ((ClipSetRecyclerAdapter) mVideoListView.getAdapter()).setClipCover(bitmapDrawable, pos);
                }
            });
        }
    }

}
