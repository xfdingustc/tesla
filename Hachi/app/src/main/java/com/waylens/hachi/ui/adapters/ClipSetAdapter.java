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
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.DownloadingClip;
import com.transee.vdb.ImageDecoder;
import com.transee.vdb.Vdb;
import com.transee.viditcam.app.MediaHelper;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

import java.util.Locale;

public abstract class ClipSetAdapter extends VideoListAdapter {

    abstract protected void onClickClipItem(int index, int offset, int size, Clip clip, Bitmap bitmap);

    static final String TAG = "NewClipSetAdapter";

    private final BaseActivity mActivity;
    private final Vdb mVdb;
    private final ClipSet mClipSet;
    private final HashCache<Clip.ID, Bitmap, Clip> mBitmapCache;

    private VideoListView mListView;

    private String mStrToBeDownloaded;
    private String mStrDownloading;

    public ClipSetAdapter(BaseActivity activity, Vdb vdb, int clipType) {

        mActivity = activity;
        mVdb = vdb;
        mClipSet = vdb.getClipSet(clipType);

        mBitmapCache = new HashCache<Clip.ID, Bitmap, Clip>() {

            @Override
            public int getStartIndex() {
                return mListView.getFirstVisiblePosition() - 1;
            }

            @Override
            public int getEndIndex() {
                return mListView.getLastVisiblePosition() + 1;
            }

            @Override
            public Clip.ID getItemKey(int index) {
                Clip clip = ClipSetAdapter.this.getClipItem(index);
                return clip == null ? null : clip.cid;
            }

            @Override
            public Clip requestValue(int index, Clip.ID key) {
                return requestBitmap(index, key);
            }

            @Override
            public void itemReleased(HashCache.Item<Clip.ID, Bitmap, Clip> item) {
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
    public void updateClipItem(int index) {
        if (mListView != null) {
            mListView.update(index);
        }
    }

    @Override
    public boolean decodeImage(ImageDecoder decoder, ClipPos clipPos, byte[] data) {
        if (mBitmapCache.getItem(clipPos.cid) != null && mListView != null) {
            switch (clipPos.getType()) {
                case ClipPos.TYPE_POSTER:
                    int width = mListView.getPosterWidth();
                    int height = mListView.getPosterHeight();
                    decoder.decode(data, width, height, 5, clipPos, mDecoderCallback);
                    return true;
                case ClipPos.TYPE_PREVIEW:
                    decoder.decode(data, 0, 0, 0, clipPos, mDecoderCallback);
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

    @Override
    public Rect getThumbnailRect(Clip.ID cid, int plistId, View other) {
        if (mListView == null)
            return null;

        int index = mClipSet.findClipIndex(cid);
        if (index < 0)
            return null;

        Rect rect = new Rect();
        Utils.getViewRectForView(mListView, other, rect);
        mListView.getPosterRect(index, rect);

        return rect;
    }

    // API
    public void requestDeleteClip(Clip.ID cid) {
        int index = mClipSet.findClipIndex(cid);
        if (index >= 0) {
            // only mark the clip as to be deleted
            Clip clip = mClipSet.getClip(index);
            clip.bDeleting = true;
            updateClipItem(index);
        }
    }

    // API
    public void clipChanged(Clip clip, boolean bFinished) {
        int index = mClipSet.findClipIndex(clip.cid);
        if (index >= 0) {
            updateClipItem(index);
        }
    }

    // API
    public void onDownloadStarted(int id) {
        // TODO
    }

    // API
    public void onDownloadFinished(Clip oldClip, Clip newClip) {
        // TODO
        if (mBitmapCache.changeKey(oldClip.cid, newClip.cid)) {
            notifyDataSetChanged();
        }
    }

    public void onDownloadProgress(int id, int progress) {
        Clip.ID cid = DownloadingClip.createClipId(id);
        int index = mClipSet.findClipIndex(cid);
        if (index >= 0) {
            updateClipItem(index);
        }
    }

    // API
    public void onDownloadError(int id) {
        // TODO
    }

    private final ImageDecoder.Callback mDecoderCallback = new ImageDecoder.Callback() {
        @Override
        public void onDecodeDoneAsync(final Bitmap bitmap, final Object tag) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBitmapDecoded((ClipPos) tag, bitmap);
                }
            });
        }
    };

    private void onBitmapDecoded(ClipPos clipPos, Bitmap bitmap) {
        int index = mClipSet.findClipIndex(clipPos.cid);
        if (index >= 0) {
            Clip clip = mClipSet.getClip(index);
            switch (clipPos.getType()) {
                case ClipPos.TYPE_POSTER:
                    if (mBitmapCache.setValue(clip.cid, bitmap)) {
                        if (mListView != null) {
                            mListView.update(index);
                        }
                    }
                    break;
                case ClipPos.TYPE_PREVIEW:
                    if (mListView != null) {
                        mListView.setPreviewBitmap(bitmap);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private final Clip getClipItem(int position) {
        return mClipSet.getClip(position);
    }

    private final Clip requestBitmap(int index, Clip.ID cid) {
        Clip clip = getClipItem(index);
        if (clip != null) {
            ClipPos clipPos = new ClipPos(clip, 0, ClipPos.TYPE_POSTER, false);
            mVdb.getClient().requestClipImage(clip, clipPos, 0, 0);
        }
        return clip;
    }

    private final void getClipPreviewImage(int index, int offset, int size) {
        Clip clip = getClipItem(index);
        if (clip != null) {
            long clipTimeMs = (long) clip.clipLengthMs * offset / size + clip.getStartTime();
            ClipPos clipPos = new ClipPos(clip, clipTimeMs, ClipPos.TYPE_PREVIEW, false);
            mVdb.getClient().requestClipImage(clip, clipPos, 0, 0);
        }
    }

    private void getClipItem(int index, VideoListView.Item item) {
        Clip clip = mClipSet.getClip(index);
        if (clip != null) {
            item.mPoster = mBitmapCache.getValue(clip.cid);

            if (clip.bDeleting) {
                item.mbSetBackground = true;
                item.mBackgroundColor = mActivity.getResources().getColor(R.color.selection);
            }

            // line 0
            item.setTextLeft(0, clip.getDateString());
            item.setTextRight(0, clip.getWeekDayString());
            // line 1
            item.setTextLeft(1, clip.getTimeString());
            // line 2, left
            String length_info = clip.getDurationString();
            if (clip.streams.length > 0) {
                length_info += MediaHelper.formatVideoInfo(mActivity, clip.streams[0]);
            }

            if (clip.clipSize >= 0) {
                length_info += " (" + Utils.formatSpace((int) (clip.clipSize / 1000)) + ")";
            }
            item.setTextLeft(2, length_info);
            // line 2, right
            if (clip.isDownloading()) {
                int progress = clip.getDownloadProgress();
                if (progress < 0) {
                    if (mStrToBeDownloaded == null) {
                        mStrToBeDownloaded = mActivity.getResources().getString(R.string.lable_to_be_downloaded);
                    }
                    item.setTextRight(2, mStrToBeDownloaded);
                } else {
                    if (mStrDownloading == null) {
                        mStrDownloading = mActivity.getResources().getString(R.string.lable_dowloading);
                    }
                    String text = String.format(Locale.US, mStrDownloading, progress);
                    item.setTextRight(2, text);
                }
            }
        }
    }

    private class MyListViewCallback implements VideoListView.Callback {

        @Override
        public void prepareItems() {
            mBitmapCache.update();
        }

        @Override
        public int getTotalItems() {
            return mClipSet.getCount();
        }

        @Override
        public void getItem(int index, Item item) {
            getClipItem(index, item);
        }

        @Override
        public void onClickItem(int index, int offset, int size) {
            Clip clip = mClipSet.getClip(index);
            if (clip != null) {
                Bitmap bitmap = mBitmapCache.getValue(clip.cid);
                onClickClipItem(index, offset, size, clip, bitmap);
            }
        }

        @Override
        public void onRulerPosChanged(int index, int offset, int size) {
            // Log.d(TAG, "ruler: " + index + ", " + offset);
            getClipPreviewImage(index, offset, size);
        }

    }
}
