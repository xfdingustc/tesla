package com.waylens.hachi.vdb;


public class Playlist {
    private int mPlayListId;

    private ClipSet mClipSet = new ClipSet(Clip.TYPE_BUFFERED);

    private int mProperties;
    private int mTotalLengthMs; // TODO

    public boolean bMute;
    public String audioUrl;

    public boolean bDeleting;

    public final boolean contains(int index) {
        return index >= 0 && index < mClipSet.getCount();
    }

    public final boolean isEmpty() {
        return mClipSet.getCount() == 0;
    }

    public final Clip getClip(int index) {
        return mClipSet.getClip(index);
    }

    public final int getClipIndex(Clip.ID cid) {
        return mClipSet.findClipIndex(cid);
    }

    public final long getOffsetMs() {
        return mClipSet.getCount() == 0 ? 0 : mClipSet.getClip(0).getStartTimeMs();
    }

    public void clear() {
        bDeleting = false;
        mTotalLengthMs = 0;
        mClipSet.clear();
    }

    public void setClipSet(ClipSet clipSet) {
        this.mClipSet = clipSet;
    }

    public ClipSet getClipSet() {
        return mClipSet;
    }

    public void insertClip(Clip clip) {
        mClipSet.insertClipByIndex(clip);
        mTotalLengthMs += clip.getDurationMs();
    }

    public boolean moveClip(Clip clip) {
        return mClipSet.moveClip(clip.cid, clip.index);
    }

    public boolean removeClip(Clip.ID cid) {
        if (mClipSet.removeClip(cid)) {
            mTotalLengthMs = mClipSet.getTotalLengthMs();
            return true;
        }
        return false;
    }


}
