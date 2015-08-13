package com.waylens.hachi.ui.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.transee.ccam.Camera;
import com.transee.common.GPSPath;
import com.transee.common.VideoListView;
import com.transee.common.android.ViewPager;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipPos;
import com.transee.vdb.ClipSet;
import com.transee.vdb.ImageDecoder;
import com.transee.vdb.Playlist;
import com.transee.vdb.PlaylistSet;
import com.transee.vdb.RemoteClip;
import com.transee.vdb.RemoteVdb;
import com.transee.vdb.Vdb;
import com.transee.vdb.VdbClient.DownloadInfoEx;
import com.transee.vdb.VdbClient.DownloadRawDataBlock;
import com.transee.vdb.VdbClient.PlaybackUrl;
import com.transee.vdb.VdbClient.PlaylistPlaybackUrl;
import com.transee.vdb.VdbClient.RawDataBlock;
import com.transee.vdb.VdbClient.RawDataResult;
import com.transee.viditcam.app.ViditImageButton;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.ui.adapters.ClipSetAdapter;
import com.waylens.hachi.ui.adapters.PlaylistSetAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

public class CameraVideoActivity extends BaseActivity {
    private static final String TAG = CameraVideoActivity.class.getSimpleName();

    private Camera mCamera;
    private Vdb mVdb;
    private ImageDecoder mImageDecoder;

    private CameraVideoEdit mCameraVideoEdit;

    private static final String IS_PC_SERVER = "isPcServer";
    private static final String SSID = "ssid";
    private static final String HOST_STRING = "hostString";



    private ClipSetAdapter mBufferedAdapter;
    private ClipSetAdapter mMarkedAdapter;
    private PlaylistSetAdapter mPlaylistAdapter;
    private ClipSetAdapter mLocalAdapter;

    private static final int VIDEO_BUFFERED = 0;
    private static final int VIDEO_MARKED = 1;
    private static final int VIDEO_PLAYLIST = 2;

    private View mVideoListControl;
    private ViewPager mPager;
    private VideoListView mBufferedLV;
    private VideoListView mMarkedLV;
    private VideoListView mPlaylistLV;

    private int mVideoTypeIndex = VIDEO_BUFFERED;


    private ViditImageButton mBufferedButton;
    private ViditImageButton mMarkedButton;
    private ViditImageButton mPlaylistsButton;

    private ViditImageButton mBufferedButton2;
    private ViditImageButton mMarkedButton2;
    private ViditImageButton mPlaylistsButton2;


    @Bind(R.id.titleBar)
    TextView mTitleText;


    public static void launch(Activity activity, boolean isPcServer, String ssid, String
            hostString) {
        Intent intent = new Intent(activity, CameraVideoActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_PC_SERVER, isPcServer);
        bundle.putString(SSID, ssid);
        bundle.putString(HOST_STRING, hostString);
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(savedInstanceState);
    }



    private boolean mbPcServer;

    protected void init(Bundle savedInstanceState) {

        Bundle bundle = getIntent().getExtras();

        mVdb = new RemoteVdb(new MyVdbCallback(), Hachi.getVideoDownloadPath(), isServerActivity(bundle));

        mImageDecoder = new ImageDecoder();
        mImageDecoder.start();

        mbPcServer = bundle.getBoolean(IS_PC_SERVER, false);

        mBufferedAdapter = new MyNewClipSetAdapter(this, mVdb, RemoteClip.TYPE_BUFFERED);
        mMarkedAdapter = new MyNewClipSetAdapter(this, mVdb, RemoteClip.TYPE_MARKED);
        mPlaylistAdapter = new MyNewPlaylistSetAdapter(this, mVdb);
        initVdbListsAndAdapters();

        mCameraVideoEdit = new MyCameraVideoEdit(this, mVdb, mImageDecoder);

        mCameraVideoEdit.onCreateActivity(savedInstanceState);
        initViews();
    }



    @SuppressLint("InflateParams")
    private void initVdbListsAndAdapters() {
        LayoutInflater lf = getLayoutInflater();

        mVideoListControl = lf.inflate(R.layout.group_video_list, null);
        mVideoListControl.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
        mPager = (ViewPager) mVideoListControl.findViewById(R.id.viewPager1);
        mPager.setOffscreenPageLimit(2);

        View view;
        List<View> videoViews = new ArrayList<View>();
        String loadingStr = getResources().getString(R.string.info_loading_clips);

        // buffered clip set
        view = lf.inflate(R.layout.layout_video_buffered, null);
        videoViews.add(view);
        mBufferedLV = (VideoListView) view.findViewById(R.id.bufferedListView);
        mBufferedAdapter.setListView(mBufferedLV, true);
        mBufferedLV.startLoading(loadingStr);

        // marked clip set
        view = lf.inflate(R.layout.layout_video_marked, null);
        videoViews.add(view);
        mMarkedLV = (VideoListView) view.findViewById(R.id.markedListView);
        mMarkedAdapter.setListView(mMarkedLV, true);
        mMarkedLV.startLoading(loadingStr);

        // playlist set
        if (!mbPcServer) {
            view = lf.inflate(R.layout.layout_video_playlists, null);
            videoViews.add(view);
            mPlaylistLV = (VideoListView) view.findViewById(R.id.playlistsListView);
            mPlaylistAdapter.setListView(mPlaylistLV, false);
            mPlaylistLV.startLoading(loadingStr);
        }

        mPager.setAdapter(new MyPagerAdapter(videoViews));
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int arg0) {
                int oldIndex = mVideoTypeIndex;
                mVideoTypeIndex = arg0;
                switchToVideoType(true, oldIndex);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });

        mVideoTypeIndex = VIDEO_BUFFERED;
    }

    private void switchToVideoType(boolean bFromPager, int oldIndex) {
        if (!bFromPager) {
            mPager.setCurrentItem(mVideoTypeIndex);
        }

        switch (oldIndex) {
            case 0:
                mBufferedLV.clearPreview();
                break;
            case 1:
                mMarkedLV.clearPreview();
                break;
            case 2:
                if (mPlaylistLV != null) {
                    mPlaylistLV.clearPreview();
                }
                break;
        }
    }


    private void initViews() {
        setContentView(R.layout.activity_camera_video);

        if (mVdb.isLocal()) {
            mTitleText.setText(R.string.title_activity_downloaded_video);
        } else {
            String text = getResources().getString(R.string.title_activity_camera_video);
            if (mCamera != null) {
                String name = Camera.getCameraStates(mCamera).mCameraName;
                if (name.length() == 0) {
                    name = getResources().getString(R.string.lable_camera_noname);
                }
                text += " - " + name;
            }
            mTitleText.setText(text);
        }


        onCameraVideoListInitUI();
        mCameraVideoEdit.onInitUI();

        onCameraVideoListSetupUI();
        mCameraVideoEdit.onSetupUI();
    }

    private View mLayout;

    private final ViewGroup findVideoListHolder() {
        return (ViewGroup) mLayout.findViewById(R.id.videoListHolder);
    }


    private View mToolbar1;
    private View mToolbar2;

    public void onCameraVideoListInitUI() {
        mLayout = findViewById(R.id.cameraVideoList);
        findVideoListHolder().addView(mVideoListControl);

        Resources res = getResources();

        mToolbar1 = findViewById(R.id.linearLayout1);
        mToolbar2 = findViewById(R.id.linearLayout2);

        if (!mVdb.isLocal()) {
            View.OnClickListener onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int oldIndex = mVideoTypeIndex;
                    int id = v.getId();
                    if (id == R.id.btnMarked) {
                        mVideoTypeIndex = VIDEO_MARKED;
                    } else if (id == R.id.btnPlaylists) {
                        mVideoTypeIndex = VIDEO_PLAYLIST;
                    } else {
                        mVideoTypeIndex = VIDEO_BUFFERED;
                    }
                    switchToVideoType(false, oldIndex);
                }
            };

            // toolbar on title bar
            mBufferedButton = (ViditImageButton) mToolbar1.findViewById(R.id.btnBuffered);
            mBufferedButton.setOnClickListener(onClick);

            mMarkedButton = (ViditImageButton) mToolbar1.findViewById(R.id.btnMarked);
            mMarkedButton.setOnClickListener(onClick);

            mPlaylistsButton = (ViditImageButton) mToolbar1.findViewById(R.id.btnPlaylists);
            if (mbPcServer) {
                mPlaylistsButton.setVisibility(View.GONE);
            } else {
                mPlaylistsButton.setOnClickListener(onClick);
            }

            // toolbar at bottom
            mBufferedButton2 = (ViditImageButton) mToolbar2.findViewById(R.id.btnBuffered);
            mBufferedButton2.setOnClickListener(onClick);

            mMarkedButton2 = (ViditImageButton) mToolbar2.findViewById(R.id.btnMarked);
            mMarkedButton2.setOnClickListener(onClick);

            mPlaylistsButton2 = (ViditImageButton) mToolbar2.findViewById(R.id.btnPlaylists);
            if (mbPcServer) {
                mPlaylistsButton2.setVisibility(View.GONE);
            } else {
                mPlaylistsButton2.setOnClickListener(onClick);
            }

            switchToVideoType(false, -1);
        }
    }


    // API
    public void onCameraVideoListSetupUI() {
        if (mVdb.isLocal()) {
            mToolbar1.setVisibility(View.GONE);
            mToolbar2.setVisibility(View.GONE);
        } else {
            if (isPortrait()) {
                mToolbar1.setVisibility(View.GONE);
                mToolbar2.setVisibility(View.VISIBLE);
            } else {
                mToolbar1.setVisibility(View.VISIBLE);
                mToolbar2.setVisibility(View.GONE);
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraVideoEdit.onDestroyActivity();

        mImageDecoder.interrupt();
        mImageDecoder = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Bundle bundle = getIntent().getExtras();
        String hostString = null;


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


        mVdb.start(hostString);
        mCameraVideoEdit.onStartActivity();
    }


    @Override
    protected void onStop() {
        super.onStop();
        onCameraVideoListStopActivity();
        mCameraVideoEdit.onStopActivity();
    }


    // API
    public void onCameraVideoListStopActivity() {
        mVdb.stop();
        if (mLocalAdapter != null) {
            mLocalAdapter.clear();
        }
        if (mBufferedAdapter != null) {
            mBufferedAdapter.clear();
        }
        if (mMarkedAdapter != null) {
            mMarkedAdapter.clear();
        }
        if (mPlaylistAdapter != null) {
            mPlaylistAdapter.clear();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraVideoEdit.onPauseActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                    cameraVideoListDecodeImage(clipPos, data);
                    break;
                case ClipPos.TYPE_PREVIEW:
                    // clip preview
                    if (!isEditing()) {
                        cameraVideoListDecodeImage(clipPos, data);
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


    public void cameraVideoListDecodeImage(ClipPos clipPos, byte[] data) {
        ClipSetAdapter adapter = getAdapter(clipPos.cid);
        if (adapter != null) {
            adapter.decodeImage(mImageDecoder, clipPos, data);
        }
    }


    private ClipSetAdapter getAdapter(Clip.ID cid) {
        if (cid.cat == Clip.CAT_LOCAL) {
            return mLocalAdapter;
        }
        if (cid.cat == Clip.CAT_REMOTE) {
            if (cid.type == RemoteClip.TYPE_BUFFERED)
                return mBufferedAdapter;
            if (cid.type == RemoteClip.TYPE_MARKED)
                return mMarkedAdapter;
        }
        return null;
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
                cameraVideoListDecodePlaylistIndexPic(clipPos, data);
            }
        }
    }


    public void cameraVideoListDecodePlaylistIndexPic(ClipPos clipPos, byte[] data) {
        mPlaylistAdapter.decodeImage(mImageDecoder, clipPos, data);
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
            cameraVideoListRequestDeleteClip(cid);
        }

        @Override
        public void requestClearPlaylist(int plistId) {
            cameraVideoListRequestClearPlaylist(plistId);
        }

        @Override
        public Rect getClipThumbnailRect(Clip.ID cid, View other) {
            return cameraVideoListGetClipThumbnailRect(cid, other);
        }

        @Override
        public Rect getPlaylistThumbnailRect(int plistId, View other) {
            return cameraVideoListGetPlaylistThumbnailRect(plistId, other);
        }

        @Override
        public void onBeginEdit() {
            cameraVideoListHide();
        }

        @Override
        public void onEndEdit() {
            cameraVideoListShow();
        }

    }


    public void cameraVideoListRequestDeleteClip(Clip.ID cid) {
        ClipSetAdapter adapter = getAdapter(cid);
        if (adapter != null) {
            adapter.requestDeleteClip(cid);
        }
    }


    public void cameraVideoListRequestClearPlaylist(int plistId) {
        mPlaylistAdapter.requestClearPlaylist(plistId);
    }


    private void updateSpaceInfo() {
        if (mCamera != null) {
            mCamera.getClient().cmd_Cam_get_getStorageInfor();
        }
    }


    public Rect cameraVideoListGetClipThumbnailRect(Clip.ID cid, View other) {
        ClipSetAdapter adapter = getAdapter(cid);
        if (adapter != null) {
            return adapter.getThumbnailRect(cid, 0, other);
        }
        return null;
    }


    public Rect cameraVideoListGetPlaylistThumbnailRect(int plistId, View other) {
        return mPlaylistAdapter.getThumbnailRect(null, plistId, other);
    }


    public void cameraVideoListShow() {
        mLayout.setVisibility(View.VISIBLE);
    }

    public void cameraVideoListHide() {
        mLayout.setVisibility(View.INVISIBLE);
    }


    private void clipSetChanged(Clip.ID cid) {
        ClipSetAdapter adapter = getAdapter(cid);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        } else {
            if (mPlaylistAdapter != null) {
                mPlaylistAdapter.playlistChanged(cid.type);
            }
        }
    }

    // API
    public void cvlonClipCreated(boolean isLive, Clip clip) {
        clipSetChanged(clip.cid);
    }

    // API
    public void cvlonClipRemoved(Clip.ID cid) {
        clipSetChanged(cid);
    }

    // API
    public void cvlonClipChanged(Clip clip, boolean bFinished) {
        ClipSetAdapter adapter = getAdapter(clip.cid);
        if (adapter != null) {
            adapter.clipChanged(clip, bFinished);
        }
    }


    public void cvlOnPlaylistSetInfo(PlaylistSet playlistSet) {
        String emptyStr = getResources().getString(R.string.info_no_clips);
        mPlaylistLV.finishLoading(emptyStr);
        mPlaylistAdapter.notifyDataSetChanged();
    }

    private VideoListView mLocalLV;

    public void cvlOnClipSetInfo(ClipSet clipSet) {
        String emptyStr = getResources().getString(R.string.info_no_clips);
        if (clipSet.clipCat == Clip.CAT_LOCAL) {
            mLocalLV.finishLoading(emptyStr);
            mLocalAdapter.notifyDataSetChanged();
            return;
        }
        if (clipSet.clipCat == Clip.CAT_REMOTE) {
            if (clipSet.clipType == RemoteClip.TYPE_BUFFERED) {
                mBufferedLV.finishLoading(emptyStr);
                mBufferedAdapter.notifyDataSetChanged();
                return;
            }
            if (clipSet.clipType == RemoteClip.TYPE_MARKED) {
                mMarkedLV.finishLoading(emptyStr);
                mMarkedAdapter.notifyDataSetChanged();
                return;
            }
        }
    }

    // API
    public void cvlOnVdbUnmounted() {
        if (mLocalAdapter != null) {
            mLocalAdapter.onVdbUnmounted();
        }
        if (mBufferedAdapter != null) {
            mBufferedAdapter.onVdbUnmounted();
        }
        if (mMarkedAdapter != null) {
            mMarkedAdapter.onVdbUnmounted();
        }
        if (mPlaylistAdapter != null) {
            mPlaylistAdapter.onVdbUnmounted();
        }
    }

    // API
    public void cvlonPlaylistCleared(int plistId) {
        mPlaylistAdapter.playlistChanged(plistId);
    }

    // API
    public void cvlonClipInserted(Clip clip) {
        mPlaylistAdapter.clipInserted(clip);
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
            cvlOnVdbUnmounted();
        }

        @Override
        public void onClipSetInfo(Vdb vdb, ClipSet clipSet) {
            cvlOnClipSetInfo(clipSet);
        }

        @Override
        public void onPlaylistSetInfo(Vdb vdb, PlaylistSet playlistSet) {
            cvlOnPlaylistSetInfo(playlistSet);
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
            cvlonClipCreated(isLive, clip);
        }

        @Override
        public void onClipChanged(Vdb vdb, boolean isLive, Clip clip) {
            cvlonClipChanged(clip, false);
            if (isEditing()) {
                mCameraVideoEdit.onClipChanged(isLive, clip, false);
            }
            updateSpaceInfo();
        }

        @Override
        public void onClipFinished(Vdb vdb, boolean isLive, Clip clip) {
            cvlonClipChanged(clip, true);
            if (isEditing()) {
                mCameraVideoEdit.onClipChanged(isLive, clip, true);
            }
            updateSpaceInfo();
        }

        @Override
        public void onClipInserted(Vdb vdb, boolean isLive, Clip clip) {
            cvlonClipInserted(clip);
            if (isEditing()) {
                mCameraVideoEdit.onClipInserted(isLive, clip);
            }
        }

        @Override
        public void onClipMoved(Vdb vdb, boolean isLive, Clip clip) {
            if (isEditing()) {
                mCameraVideoEdit.onClipMoved(isLive, clip);
            }
        }

        @Override
        public void onClipRemoved(Vdb vdb, Clip.ID cid) {
            cvlonClipRemoved(cid);
            if (isEditing()) {
                mCameraVideoEdit.onClipRemoved(cid);
            }
            updateSpaceInfo();
        }

        @Override
        public void onPlaylistCleared(Vdb vdb, int plistId) {
            cvlonPlaylistCleared(plistId);
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
            cvlonDownloadStarted(id);
        }

        @Override
        public void onDownloadFinished(Vdb vdb, Clip oldClip, Clip newClip) {
            cvlonDownloadFinished(oldClip, newClip);
        }

        @Override
        public void onDownloadError(Vdb vdb, int id) {
            cvlonDownloadError(id);
        }

        @Override
        public void onDownloadProgress(Vdb vdb, int id, int progress) {
            cvlonDownloadProgress(id, progress);
        }

        @Override
        public void onGPSSegment(Vdb vdb, GPSPath.Segment segment) {
            if (isEditing()) {
                mCameraVideoEdit.onGPSSegment(segment);
            }
        }

    }


    // API
    public void cvlonDownloadStarted(int id) {
        if (mLocalAdapter != null) {
            mLocalAdapter.onDownloadStarted(id);
        }
    }

    // API
    public void cvlonDownloadFinished(Clip oldClip, Clip newClip) {
        if (mLocalAdapter != null) {
            mLocalAdapter.onDownloadFinished(oldClip, newClip);
        }
    }

    // API
    public void cvlonDownloadError(int id) {
        if (mLocalAdapter != null) {
            mLocalAdapter.onDownloadError(id);
        }
    }

    // API
    public void cvlonDownloadProgress(int id, int progress) {
        if (mLocalAdapter != null) {
            mLocalAdapter.onDownloadProgress(id, progress);
        }
    }


    private static class MyPagerAdapter extends PagerAdapter {
        List<View> mListViews;

        public MyPagerAdapter(List<View> listViews) {
            mListViews = listViews;
        }

        @Override
        public void startUpdate(View arg0) {

        }

        @Override
        public void finishUpdate(View view) {

        }

        @Override
        public int getCount() {
            return mListViews.size();
        }

        @Override
        public Object instantiateItem(View view, int index) {
            ((ViewPager) view).addView(mListViews.get(index), 0);
            return mListViews.get(index);
        }

        @Override
        public void destroyItem(View view, int index, Object object) {
            ((ViewPager) view).removeView(mListViews.get(index));
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {

        }

        @Override
        public Parcelable saveState() {
            return null;
        }
    }


    class MyNewClipSetAdapter extends ClipSetAdapter {

        public MyNewClipSetAdapter(BaseActivity activity, Vdb vdb, int subType) {
            super(activity, vdb, subType);
        }

        @Override
        protected void onClickClipItem(int index, int offset, int size, Clip clip, Bitmap bitmap) {
            if (clip.isDownloading()) {
                // TODO : display info & prompt for cancel
            } else {
                if (!isEditing()) {
                    mCameraVideoEdit.editClip(bitmap, clip, offset, size);
                }
            }
        }

    }


    class MyNewPlaylistSetAdapter extends PlaylistSetAdapter {

        public MyNewPlaylistSetAdapter(BaseActivity activity, Vdb vdb) {
            super(activity, vdb);
        }

        @Override
        protected void onClipPlaylistItem(int index, Playlist playlist, Bitmap bitmap) {
            if (!isEditing()) {
                mCameraVideoEdit.editPlaylist(bitmap, playlist);
            }
        }

    }

}
