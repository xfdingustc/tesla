package com.waylens.hachi.vdb;

import java.util.ArrayList;
import java.util.List;

public class ClipSet {
    private final int mClipType; // clip type
    public static final long U32_MASK = 0x0FFFFFFFFL;

    private Clip.ID liveClipId;
    private int totalClips; // equals clips.size()
    private int totalLengthMs;
    private ArrayList<Clip> clips; // all clips; not null

    public ClipSet(int type) {
        this.mClipType = type;
        totalClips = 0;
        totalLengthMs = 0;
        liveClipId = null;
        clips = new ArrayList<>();
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
        this.clips = other.clips;
    }


    public void clear() {
        totalClips = 0;
        totalLengthMs = 0;
        clips.clear();
    }


    public Clip getClip(int index) {
        return index < 0 || index >= totalClips ? null : clips.get(index);
    }

    public void addClip(Clip clip) {
        clip.index = clips.size();
        clips.add(clip);
        totalClips++;
        totalLengthMs += clip.getDurationMs();
    }

    public void setLiveClipId(Clip.ID liveClipId) {
        this.liveClipId = liveClipId;
    }

    // sorted by unsigned clip id

    public void insertClipById(Clip clip) {
        long clipId = (long) clip.cid.subType & U32_MASK;
        int i = 0;
        for (Clip tmp : clips) {
            if (clipId < ((long) tmp.cid.subType & U32_MASK))
                break;
            i++;
        }
        clips.add(i, clip);
        totalClips++;
        totalLengthMs += clip.getDurationMs();
    }

    // by index

    public void insertClipByIndex(Clip clip) {
        int i = 0;
        for (i = 0; i < totalClips; i++) {
            if (clip.index <= i)
                break;
        }
        clips.add(i, clip);
        totalClips++;
        totalLengthMs += clip.getDurationMs();
        for (; i < totalClips; i++) {
            clip = clips.get(i);
            clip.index = i;
        }
    }


    public boolean removeClip(Clip.ID cid) {
        for (int i = 0; i < totalClips; i++) {
            Clip clip = clips.get(i);
            if (clip.cid.equals(cid)) {
                clips.remove(i);
                totalClips--;
                totalLengthMs -= clip.getDurationMs();
                return true;
            }
        }
        return false;
    }


    public boolean clipChanged(Clip clip, boolean isLive, boolean bFinished) {
        int i = 0;
        for (i = 0; i < clips.size(); i++) {
            Clip tmp = clips.get(i);
            if (tmp.cid.equals(clip.cid)) {
                totalLengthMs -= tmp.getDurationMs();
                totalLengthMs += clip.getDurationMs();
                clips.set(i, clip);
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
            Clip clip = clips.get(i);
            if (clip.cid.equals(cid))
                return i;
        }
        return -1;
    }


    public Clip findClip(Clip.ID cid) {
        for (int i = 0; i < totalClips; i++) {
            Clip clip = clips.get(i);
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
            Clip clip = clips.get(index);
            for (int i = index; i < clipIndex; i++) {
                Clip tmp = clips.get(i + 1);
                tmp.index = i;
                clips.set(i, tmp);
            }
            clip.index = clipIndex;
            clips.set(clipIndex, clip);
            return true;
        }

        if (index > clipIndex) {
            Clip clip = clips.get(index);
            for (int i = index; i > clipIndex; i--) {
                Clip tmp = clips.get(i - 1);
                tmp.index = i;
                clips.set(i, tmp);
            }
            clip.index = clipIndex;
            clips.set(clipIndex, clip);
            return true;
        }

        return false;
    }


    public int getTotalLengthMs() {
        return totalLengthMs;
    }

    public List<Clip> getInternalList() {
        return clips;
    }
}
