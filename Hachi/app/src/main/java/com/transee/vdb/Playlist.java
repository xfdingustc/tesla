package com.transee.vdb;

import com.transee.common.DateTime;

public class Playlist {

	public int plistId; // TODO
	public int numClips; // equals clipSet.size()
	public ClipSet clipSet = new SimpleClipSet(Clip.CAT_UNKNOWN, 0);

	public int mProperties;
	public int mTotalLengthMs; // TODO

	public boolean bMute;
	public String audioUrl;

	public boolean bDeleting;

	public final boolean contains(int index) {
		return index >= 0 && index < numClips;
	}

	public final boolean isEmpty() {
		return numClips == 0;
	}

	public final Clip getClip(int index) {
		return clipSet.getClip(index);
	}

	public final int getClipIndex(Clip.ID cid) {
		return clipSet.findClipIndex(cid);
	}

	public final long getOffsetMs() {
		return numClips == 0 ? 0 : clipSet.getClip(0).getStartTime();
	}

	public void clear() {
		bDeleting = false;
		numClips = 0;
		mTotalLengthMs = 0;
		clipSet.clear();
	}

	public void setClipSet(ClipSet clipSet) {
		this.clipSet = clipSet;
		this.numClips = clipSet.getCount();
	}

	public void insertClip(Clip clip) {
		clipSet.insertClipByIndex(clip);
		numClips++;
		mTotalLengthMs += clip.clipLengthMs;
	}

	public boolean moveClip(Clip clip) {
		return clipSet.moveClip(clip.cid, clip.index);
	}

	public boolean removeClip(Clip.ID cid) {
		if (clipSet.removeClip(cid)) {
			numClips--;
			mTotalLengthMs = clipSet.getTotalLengthMs();
			return true;
		}
		return false;
	}

	public final String getDurationString() {
		return DateTime.secondsToString(mTotalLengthMs / 1000);
	}

}
