package com.waylens.hachi.vdb;

import java.util.ArrayList;
import java.util.List;

public class ClipSet {
    private final int mClipType;
    public static final long U32_MASK = 0x0FFFFFFFFL;

    private Clip.ID liveClipId;
    private int totalClips;
    private int totalLengthMs;
    private ArrayList<Clip> mClipList = new ArrayList<>();

    public ClipSet(int type) {
        this.mClipType = type;
        totalClips = 0;
        totalLengthMs = 0;
        liveClipId = null;

    }

    public int getType() {
        return mClipType;
    }

    public int getCount() {
        return totalClips;
    }


    public void set(ClipSet other) {
        this.liveClipId = other.liveClipId;
        this.totalClips = other.totalClips;
        this.totalLengthMs = other.totalLengthMs;
        this.mClipList = other.mClipList;
    }


    public void clear() {
        totalClips = 0;
        totalLengthMs = 0;
        mClipList.clear();
    }


    public Clip getClip(int index) {
        return index < 0 || index >= totalClips ? null : mClipList.get(index);
    }

    public ArrayList<Clip> getClipList() {
        return mClipList;
    }

    public void addClip(Clip clip) {
        clip.index = mClipList.size();
        mClipList.add(clip);
        totalClips++;
        totalLengthMs += clip.getDurationMs();
    }

    public void setLiveClipId(Clip.ID liveClipId) {
        this.liveClipId = liveClipId;
    }


    public void insertClipById(Clip clip) {
        long clipId = (long) clip.cid.subType & U32_MASK;
        int i = 0;
        for (Clip tmp : mClipList) {
            if (clipId < ((long) tmp.cid.subType & U32_MASK))
                break;
            i++;
        }
        mClipList.add(i, clip);
        totalClips++;
        totalLengthMs += clip.getDurationMs();
    }


    public void insertClipByIndex(Clip clip) {
        int i;
        for (i = 0; i < totalClips; i++) {
            if (clip.index <= i)
                break;
        }
        mClipList.add(i, clip);
        totalClips++;
        totalLengthMs += clip.getDurationMs();
        for (; i < totalClips; i++) {
            clip = mClipList.get(i);
            clip.index = i;
        }
    }


    public boolean removeClip(Clip.ID cid) {
        for (int i = 0; i < totalClips; i++) {
            Clip clip = mClipList.get(i);
            if (clip.cid.equals(cid)) {
                mClipList.remove(i);
                totalClips--;
                totalLengthMs -= clip.getDurationMs();
                return true;
            }
        }
        return false;
    }


    public boolean clipChanged(Clip clip, boolean isLive, boolean bFinished) {
        int i = 0;
        for (i = 0; i < mClipList.size(); i++) {
            Clip tmp = mClipList.get(i);
            if (tmp.cid.equals(clip.cid)) {
                totalLengthMs -= tmp.getDurationMs();
                totalLengthMs += clip.getDurationMs();
                mClipList.set(i, clip);
                if (isLive) {
                    liveClipId = bFinished ? null : clip.cid;
                }
                return true;
            }
        }
        return false;
    }


    public int findClipIndex(Clip.ID cid) {
        for (int i = 0; i < totalClips; i++) {
            Clip clip = mClipList.get(i);
            if (clip.cid.equals(cid))
                return i;
        }
        return -1;
    }


    public Clip findClip(Clip.ID cid) {
        for (int i = 0; i < totalClips; i++) {
            Clip clip = mClipList.get(i);
            if (clip.cid.equals(cid))
                return clip;
        }
        return null;
    }


    public boolean isLiveClip(Clip clip) {
        return liveClipId != null && liveClipId.equals(clip.cid);
    }


    public boolean moveClip(Clip.ID cid, int clipIndex) {
        if (clipIndex < 0 || clipIndex >= totalClips)
            return false;

        int index = findClipIndex(cid);
        if (index < clipIndex) {
            Clip clip = mClipList.get(index);
            for (int i = index; i < clipIndex; i++) {
                Clip tmp = mClipList.get(i + 1);
                tmp.index = i;
                mClipList.set(i, tmp);
            }
            clip.index = clipIndex;
            mClipList.set(clipIndex, clip);
            return true;
        }

        if (index > clipIndex) {
            Clip clip = mClipList.get(index);
            for (int i = index; i > clipIndex; i--) {
                Clip tmp = mClipList.get(i - 1);
                tmp.index = i;
                mClipList.set(i, tmp);
            }
            clip.index = clipIndex;
            mClipList.set(clipIndex, clip);
            return true;
        }

        return false;
    }


    public int getTotalLengthMs() {
        return totalLengthMs;
    }

    public int getTotalSelectedLengthMs() {
        int total = 0;
        for (Clip clip : mClipList) {
            total += clip.editInfo.getSelectedLength();
        }

        return total;
    }


}
