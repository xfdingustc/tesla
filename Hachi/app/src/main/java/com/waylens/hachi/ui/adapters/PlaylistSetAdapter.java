package com.waylens.hachi.ui.adapters;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;

import com.transee.common.HashCache;
import com.transee.common.Utils;
import com.transee.common.VideoListView;
import com.transee.common.VideoListView.Item;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.transee.vdb.ImageDecoder;
import com.transee.vdb.Playlist;
import com.transee.vdb.PlaylistSet;
import com.transee.vdb.Vdb;
import com.transee.viditcam.app.VdbFormatter;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

public abstract class PlaylistSetAdapter extends VideoListAdapter {

    abstract protected void onClipPlaylistItem(int index, Playlist playlist, Bitmap bitmap);

    private final BaseActivity mActivity;
    private final Vdb mVdb;
    private VideoListView mListView;
    private HashCache<Integer, Bitmap, Playlist> mBitmapCache;

    public PlaylistSetAdapter(BaseActivity activity, Vdb vdb) {
        mActivity = activity;
        mVdb = vdb;
        mBitmapCache = new HashCache<Integer, Bitmap, Playlist>() {

            @Override
            public int getStartIndex() {
                return mListView.getFirstVisiblePosition();
            }

            @Override
            public int getEndIndex() {
                return mListView.getLastVisiblePosition();
            }

            @Override
            public Integer getItemKey(int index) {
                Playlist playlist = PlaylistSetAdapter.this.getPlaylistItem(index);
                return playlist == null ? null : playlist.plistId;
            }

            @Override
            public Playlist requestValue(int index, Integer key) {
                Playlist playlist = PlaylistSetAdapter.this.getPlaylistItem(index);
                if (playlist != null) {
                    mVdb.getClient().requestPlaylistIndexImage(playlist.plistId, 0);
                }
                return playlist;
            }

            @Override
            public void itemReleased(HashCache.Item<Integer, Bitmap, Playlist> item) {
                Bitmap bitmap = item.value;
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }

        };
    }

    @Override
    public void setListView(VideoListView listView, boolean bEnableFastPreview) {
        mListView = listView;
        mListView.setCallback(new MyListViewCallback(), bEnableFastPreview);
        mListView.setSelColor(mActivity.getResources().getColor(R.color.selection));
        mListView.invalidate();
    }

    @Override
    public void onVdbUnmounted() {
    }

    @Override
    public void notifyDataSetChanged() {
        if (mListView != null) {
            mListView.update();
        }
    }

    @Override
    public void clear() {
        mBitmapCache.clear();
    }

    // API
    public void updatePlaylistItem(int index) {
        if (mListView != null) {
            mListView.update(index);
        }
    }

    public Playlist getPlaylistItem(int index) {
        return mVdb.getPlaylistSet().getPlaylist(index);
    }

    // API - mark the playlist as deleting
    public void requestClearPlaylist(int plistId) {
        PlaylistSet playlistSet = mVdb.getPlaylistSet();
        int index = playlistSet.getPlaylistIndex(plistId);
        if (index >= 0) {
            // only mark the playlist to be deleted
            Playlist playlist = playlistSet.getPlaylist(index);
            playlist.bDeleting = true;
            updatePlaylistItem(index);
        }
    }

    @Override
    public Rect getThumbnailRect(Clip.ID cid, int plistId, View other) {
        if (mListView == null)
            return null;

        int index = mVdb.getPlaylistSet().getPlaylistIndex(plistId);
        if (index < 0)
            return null;

        Rect rect = new Rect();
        Utils.getViewRectForView(mListView, other, rect);
        mListView.getPosterRect(index, rect);

        return rect;
    }

    // API - cleared or changed
    public void playlistChanged(int plistId) {
        PlaylistSet playlistSet = mVdb.getPlaylistSet();

        int index = playlistSet.getPlaylistIndex(plistId);
        if (index < 0)
            return;

        Playlist playlist = playlistSet.getPlaylist(index);
        if (playlist.isEmpty()) {
            mBitmapCache.setValue(plistId, null);
        } else {
            mBitmapCache.requestValue(index, plistId); //
        }

        updatePlaylistItem(index);
    }

    // API
    public void clipInserted(Clip clip) {
        PlaylistSet playlistSet = mVdb.getPlaylistSet();
        int index = playlistSet.getPlaylistIndex(clip.cid.type);
        if (index >= 0) {
            mBitmapCache.requestValue(index, clip.cid.type);
            updatePlaylistItem(index);
        }
    }

    @Override
    public boolean decodeImage(ImageDecoder decoder, ClipPos clipPos, byte[] data) {
        if (mListView != null && mBitmapCache.getItem(clipPos.cid.type) != null) {
            if (clipPos.getType() == ClipPos.TYPE_POSTER) {
                int width = mListView.getPosterWidth();
                int height = mListView.getPosterHeight();
                decoder.decode(data, width, height, 5, clipPos, mDecoderCallback);
                return true;
            }
        }
        return false;
    }

    private final ImageDecoder.Callback mDecoderCallback = new ImageDecoder.Callback() {
        @Override
        public void onDecodeDoneAsync(final Bitmap bitmap, final Object tag) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setBitmap((ClipPos) tag, bitmap);
                }
            });
        }
    };

    private void setBitmap(ClipPos clipPos, Bitmap bitmap) {
        PlaylistSet playlistSet = mVdb.getPlaylistSet();
        int index = playlistSet.getPlaylistIndex(clipPos.cid.type);
        if (index >= 0) {
            Playlist playlist = playlistSet.getPlaylist(index);
            if (mBitmapCache.setValue(playlist.plistId, bitmap)) {
                updatePlaylistItem(index);
            }
        }
    }

    private void getPlaylistItem(int index, Item item) {
        Playlist playlist = mVdb.getPlaylistSet().getPlaylist(index);
        if (playlist == null)
            return;

        // bitmap
        item.mPoster = mBitmapCache.getValue(playlist.plistId);
        if (playlist.isEmpty()) {
            item.mEmptyPoster = mActivity.getResources().getDrawable(R.drawable.empty_playlist);
        }

        // text
        item.setTextLeft(0, VdbFormatter.formatPlaylistName(mActivity, index));
        if (playlist.isEmpty()) {
            item.setTextLeft(1, VdbFormatter.getEmptyPlaylistString(mActivity));
        } else {
            item.setTextLeft(1, VdbFormatter.getPlaylistNumClipsString(mActivity, playlist));
            item.setTextLeft(2, playlist.getDurationString());
        }
    }

    private class MyListViewCallback implements VideoListView.Callback {

        @Override
        public void prepareItems() {
            mBitmapCache.update();
        }

        @Override
        public int getTotalItems() {
            return mVdb.getPlaylistSet().mNumPlaylists;
        }

        @Override
        public void getItem(int index, Item item) {
            getPlaylistItem(index, item);
        }

        @Override
        public void onClickItem(int index, int offset, int size) {
            Playlist playlist = mVdb.getPlaylistSet().getPlaylist(index);
            if (playlist != null && !playlist.isEmpty()) {
                Bitmap bitmap = mBitmapCache.getValue(playlist.plistId);
                onClipPlaylistItem(index, playlist, bitmap);
            }
        }

        @Override
        public void onRulerPosChanged(int index, int offset, int size) {
        }

    }

}
