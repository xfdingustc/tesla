package com.transee.vdb;

import java.util.ArrayList;

public class SimpleClipSet extends ClipSet {

	public static final long U32_MASK = 0x0FFFFFFFFL;

	private Clip.ID liveClipId;
	private int totalClips; // equals clips.size()
	private int totalLengthMs;
	private ArrayList<Clip> clips; // all clips; not null

	public SimpleClipSet(int cat, int type) {
		super(cat, type);
		totalClips = 0;
		totalLengthMs = 0;
		liveClipId = null;
		clips = new ArrayList<Clip>();
	}

	@Override
	public int getCount() {
		return totalClips;
	}

	@Override
	public void set(ClipSet _other) {
		SimpleClipSet other = (SimpleClipSet)_other;
		this.liveClipId = other.liveClipId;
		this.totalClips = other.totalClips;
		this.totalLengthMs = other.totalLengthMs;
		this.clips = other.clips;
	}

	@Override
	public void clear() {
		totalClips = 0;
		totalLengthMs = 0;
		clips.clear();
	}

	@Override
	public Clip getClip(int index) {
		return index < 0 || index >= totalClips ? null : clips.get(index);
	}

	public void addClip(Clip clip) {
		clip.index = clips.size();
		clips.add(clip);
		totalClips++;
		totalLengthMs += clip.clipLengthMs;
	}

	public void setLiveClipId(Clip.ID liveClipId) {
		this.liveClipId = liveClipId;
	}

	// sorted by unsigned clip id
	@Override
	public void insertClipById(Clip clip) {
		long clipId = (long)clip.cid.subType & U32_MASK;
		int i = 0;
		for (Clip tmp : clips) {
			if (clipId < ((long)tmp.cid.subType & U32_MASK))
				break;
			i++;
		}
		clips.add(i, clip);
		totalClips++;
		totalLengthMs += clip.clipLengthMs;
	}

	// by index
	@Override
	public void insertClipByIndex(Clip clip) {
		int i = 0;
		for (i = 0; i < totalClips; i++) {
			if (clip.index <= i)
				break;
		}
		clips.add(i, clip);
		totalClips++;
		totalLengthMs += clip.clipLengthMs;
		for (; i < totalClips; i++) {
			clip = clips.get(i);
			clip.index = i;
		}
	}

	@Override
	public boolean removeClip(Clip.ID cid) {
		for (int i = 0; i < totalClips; i++) {
			Clip clip = clips.get(i);
			if (clip.cid.equals(cid)) {
				clips.remove(i);
				totalClips--;
				totalLengthMs -= clip.clipLengthMs;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean clipChanged(Clip clip, boolean isLive, boolean bFinished) {
		int i = 0;
		for (i = 0; i < clips.size(); i++) {
			Clip tmp = clips.get(i);
			if (tmp.cid.equals(clip.cid)) {
				totalLengthMs -= tmp.clipLengthMs;
				totalLengthMs += clip.clipLengthMs;
				clips.set(i, clip);
				if (isLive) {
					liveClipId = bFinished ? null : clip.cid;
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public int findClipIndex(Clip.ID cid) {
		for (int i = 0; i < totalClips; i++) {
			Clip clip = clips.get(i);
			if (clip.cid.equals(cid))
				return i;
		}
		return -1;
	}

	@Override
	public Clip findClip(Clip.ID cid) {
		for (int i = 0; i < totalClips; i++) {
			Clip clip = clips.get(i);
			if (clip.cid.equals(cid))
				return clip;
		}
		return null;
	}

	@Override
	public boolean isLiveClip(Clip clip) {
		return liveClipId != null && liveClipId.equals(clip.cid);
	}

	@Override
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

	@Override
	public int getTotalLengthMs() {
		return totalLengthMs;
	}
}
