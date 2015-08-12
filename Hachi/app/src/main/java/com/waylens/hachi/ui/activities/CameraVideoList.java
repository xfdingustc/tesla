package com.waylens.hachi.ui.activities;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.transee.common.VideoListView;
import com.transee.common.android.ViewPager;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipPos;
import com.transee.vdb.ClipSet;
import com.transee.vdb.ImageDecoder;
import com.transee.vdb.Playlist;
import com.transee.vdb.PlaylistSet;
import com.transee.vdb.RemoteClip;
import com.transee.vdb.Vdb;
import com.transee.viditcam.app.ViditImageButton;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.adapters.ClipSetAdapter;
import com.waylens.hachi.ui.adapters.PlaylistSetAdapter;

import java.util.ArrayList;
import java.util.List;

abstract public class CameraVideoList {

    abstract protected void onClickClipItem(Bitmap bitmap, Clip clip, int offset, int size);

    abstract protected void onClickPlaylistItem(Bitmap bitmap, Playlist playlist);

    static final boolean DEBUG = false;
    static final String TAG = "CameraVideoList";

    static final int VIDEO_BUFFERED = 0;
    static final int VIDEO_MARKED = 1;
    static final int VIDEO_PLAYLIST = 2;

    private final BaseActivity mActivity;
    private final boolean mbPcServer;
    private final Vdb mVdb;
    private final ImageDecoder mDecoder;
    private View mLayout;

    private View mToolbar1;
    private ViditImageButton mBufferedButton;
    private ViditImageButton mMarkedButton;
    private ViditImageButton mPlaylistsButton;

    private View mToolbar2;
    private ViditImageButton mBufferedButton2;
    private ViditImageButton mMarkedButton2;
    private ViditImageButton mPlaylistsButton2;

    private int mVideoTypeIndex = VIDEO_BUFFERED;

    private View mVideoListControl;
    private com.transee.common.android.ViewPager mPager;

    private VideoListView mBufferedLV;
    private ClipSetAdapter mBufferedAdapter;

    private VideoListView mMarkedLV;
    private ClipSetAdapter mMarkedAdapter;

    private VideoListView mPlaylistLV;
    private PlaylistSetAdapter mPlaylistAdapter;

    private VideoListView mLocalLV;
    private ClipSetAdapter mLocalAdapter;

    private int mNormalTextColor;
    private int mHighlightTextColor;

    public CameraVideoList(BaseActivity activity, boolean bPcServer, Vdb vdb, ImageDecoder decoder) {
        mActivity = activity;
        mbPcServer = bPcServer;
        mVdb = vdb;
        mDecoder = decoder;

        if (mVdb.isLocal()) {
            mLocalAdapter = new MyNewClipSetAdapter(mActivity, vdb, Clip.CAT_LOCAL);
            initLocalListAndAdapter();
        } else {
            mBufferedAdapter = new MyNewClipSetAdapter(mActivity, vdb, RemoteClip.TYPE_BUFFERED);
            mMarkedAdapter = new MyNewClipSetAdapter(mActivity, vdb, RemoteClip.TYPE_MARKED);
            mPlaylistAdapter = new MyNewPlaylistSetAdapter(mActivity, vdb);
            initVdbListsAndAdapters();
        }
    }


    public void show() {
        mLayout.setVisibility(View.VISIBLE);
    }

    public void hide() {
        mLayout.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("InflateParams")
    private void initVdbListsAndAdapters() {
        LayoutInflater lf = mActivity.getLayoutInflater();

        mVideoListControl = lf.inflate(R.layout.group_video_list, null);
        mVideoListControl.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));
        mPager = (ViewPager) mVideoListControl.findViewById(R.id.viewPager1);
        mPager.setOffscreenPageLimit(2);

        View view;
        List<View> videoViews = new ArrayList<View>();
        String loadingStr = mActivity.getResources().getString(R.string.info_loading_clips);

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

    @SuppressLint("InflateParams")
    private void initLocalListAndAdapter() {
        LayoutInflater lf = mActivity.getLayoutInflater();

        mVideoListControl = lf.inflate(R.layout.layout_video_downloaded, null);
        mVideoListControl.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));

        mLocalLV = (VideoListView) mVideoListControl.findViewById(R.id.downloadedListView);
        mLocalAdapter.setListView(mLocalLV, false);

        String loadingStr = mActivity.getResources().getString(R.string.info_loading_clips);
        mLocalLV.startLoading(loadingStr);
    }

    // API
    public void onStartActivity(String hostString) {
        mVdb.start(hostString);
    }

    // API
    public void onStopActivity() {
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


    private final View findViewById(int id) {
        return mLayout.findViewById(id);
    }

    private final ViewGroup findVideoListHolder() {
        return (ViewGroup) mLayout.findViewById(R.id.videoListHolder);
    }

    public void onReleaseUI() {
        findVideoListHolder().removeAllViews();
    }

    public void onInitUI() {
        mLayout = mActivity.findViewById(R.id.cameraVideoList);
        findVideoListHolder().addView(mVideoListControl);

        Resources res = mActivity.getResources();
        mNormalTextColor = res.getColor(R.color.textColor1);
        mHighlightTextColor = res.getColor(R.color.textColor2);

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
    public void onSetupUI() {
        if (mVdb.isLocal()) {
            mToolbar1.setVisibility(View.GONE);
            mToolbar2.setVisibility(View.GONE);
        } else {
            if (mActivity.isPortrait()) {
                mToolbar1.setVisibility(View.GONE);
                mToolbar2.setVisibility(View.VISIBLE);
            } else {
                mToolbar1.setVisibility(View.VISIBLE);
                mToolbar2.setVisibility(View.GONE);
            }
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

    public void requestDeleteClip(Clip.ID cid) {
        ClipSetAdapter adapter = getAdapter(cid);
        if (adapter != null) {
            adapter.requestDeleteClip(cid);
        }
    }

    public void requestClearPlaylist(int plistId) {
        mPlaylistAdapter.requestClearPlaylist(plistId);
    }

    public Rect getClipThumbnailRect(Clip.ID cid, View other) {
        ClipSetAdapter adapter = getAdapter(cid);
        if (adapter != null) {
            return adapter.getThumbnailRect(cid, 0, other);
        }
        return null;
    }

    public Rect getPlaylistThumbnailRect(int plistId, View other) {
        return mPlaylistAdapter.getThumbnailRect(null, plistId, other);
    }

    class MyNewClipSetAdapter extends ClipSetAdapter {

        public MyNewClipSetAdapter(BaseActivity activity, Vdb vdb, int subType) {
            super(activity, vdb, subType);
        }

        @Override
        protected void onClickClipItem(int index, int offset, int size, Clip clip, Bitmap bitmap) {
            CameraVideoList.this.onClickClipItem(bitmap, clip, offset, size);
        }

    }

    class MyNewPlaylistSetAdapter extends PlaylistSetAdapter {

        public MyNewPlaylistSetAdapter(BaseActivity activity, Vdb vdb) {
            super(activity, vdb);
        }

        @Override
        protected void onClipPlaylistItem(int index, Playlist playlist, Bitmap bitmap) {
            CameraVideoList.this.onClickPlaylistItem(bitmap, playlist);
        }

    }

    // API
    public void onVdbUnmounted() {
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
    public void onClipSetInfo(ClipSet clipSet) {
        String emptyStr = mActivity.getResources().getString(R.string.info_no_clips);
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
    public void onPlaylistSetInfo(PlaylistSet playlistSet) {
        String emptyStr = mActivity.getResources().getString(R.string.info_no_clips);
        mPlaylistLV.finishLoading(emptyStr);
        mPlaylistAdapter.notifyDataSetChanged();
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
    public void onClipCreated(boolean isLive, Clip clip) {
        clipSetChanged(clip.cid);
    }

    // API
    public void onClipRemoved(Clip.ID cid) {
        clipSetChanged(cid);
    }

    // API
    public void onClipChanged(Clip clip, boolean bFinished) {
        ClipSetAdapter adapter = getAdapter(clip.cid);
        if (adapter != null) {
            adapter.clipChanged(clip, bFinished);
        }
    }

    // API
    public void onPlaylistCleared(int plistId) {
        mPlaylistAdapter.playlistChanged(plistId);
    }

    // API
    public void onClipInserted(Clip clip) {
        mPlaylistAdapter.clipInserted(clip);
    }

    // API
    public void onClipMoved(boolean isLive, Clip clip) {
        // TODO
    }

    // API
    public void decodeImage(ClipPos clipPos, byte[] data) {
        ClipSetAdapter adapter = getAdapter(clipPos.cid);
        if (adapter != null) {
            adapter.decodeImage(mDecoder, clipPos, data);
        }
    }

    // API
    public void decodePlaylistIndexPic(ClipPos clipPos, byte[] data) {
        mPlaylistAdapter.decodeImage(mDecoder, clipPos, data);
    }

    // API
    public void onDownloadStarted(int id) {
        if (mLocalAdapter != null) {
            mLocalAdapter.onDownloadStarted(id);
        }
    }

    // API
    public void onDownloadFinished(Clip oldClip, Clip newClip) {
        if (mLocalAdapter != null) {
            mLocalAdapter.onDownloadFinished(oldClip, newClip);
        }
    }

    // API
    public void onDownloadError(int id) {
        if (mLocalAdapter != null) {
            mLocalAdapter.onDownloadError(id);
        }
    }

    // API
    public void onDownloadProgress(int id, int progress) {
        if (mLocalAdapter != null) {
            mLocalAdapter.onDownloadProgress(id, progress);
        }
    }

    private void checkButton(int index, boolean bChecked) {
        ViditImageButton button = null;
        ViditImageButton button2 = null;
        switch (index) {
            case VIDEO_BUFFERED:
                button = mBufferedButton;
                button2 = mBufferedButton2;
                break;
            case VIDEO_MARKED:
                button = mMarkedButton;
                button2 = mMarkedButton2;
                break;
            case VIDEO_PLAYLIST:
                button = mPlaylistsButton;
                button2 = mPlaylistsButton2;
                break;
        }
        if (button != null) {
            button.setChecked(bChecked);
            button.setTextColor(bChecked ? mHighlightTextColor : mNormalTextColor);
        }
        if (button2 != null) {
            button2.setChecked(bChecked);
            button2.setTextColor(bChecked ? mHighlightTextColor : mNormalTextColor);
        }
    }

    private void switchToVideoType(boolean bFromPager, int oldIndex) {
        checkButton(oldIndex, false);
        checkButton(mVideoTypeIndex, true);

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

}
