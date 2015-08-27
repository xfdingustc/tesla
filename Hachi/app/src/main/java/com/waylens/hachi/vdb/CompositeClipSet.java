package com.waylens.hachi.vdb;

import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

// one category, two different types
public class CompositeClipSet extends ClipSet {

	private static final int NUM_CLIPSET = 2;
	private final ClipSet[] clipSet;

	public CompositeClipSet(int clipCat, ClipSet clipSet1, ClipSet clipSet2) {
		super(clipCat, 0);
		clipSet = new ClipSet[NUM_CLIPSET];
		this.clipSet[0] = clipSet1;
		this.clipSet[1] = clipSet2;
		if (clipSet1.clipCat != clipSet2.clipCat || clipSet1.clipType == clipSet2.clipType) {
			throw new IllegalArgumentException("2 clipset not same cat");
		}
	}

	@Override
	public int getCount() {
		int count = 0;
		for (int i = 0; i < NUM_CLIPSET; i++) {
			count += clipSet[i].getCount();
		}
		return count;
	}

	@Override
	public void clear() {
		for (int i = 0; i < NUM_CLIPSET; i++) {
			clipSet[i].clear();
		}
	}

	@Override
	public void set(ClipSet other) {
		for (int i = 0; i < NUM_CLIPSET; i++) {
			if (other.clipType == clipSet[i].clipType) {
				clipSet[i].set(other);
				return;
			}
		}
		throw new IllegalArgumentException("bad clipType " + other.clipType);
	}

	@Override
	public Clip getClip(int index) {
		for (int i = 0; i < NUM_CLIPSET; i++) {
			int total = clipSet[i].getCount();
			if (index < total)
				return clipSet[i].getClip(index);
			index -= total;
		}
		return null;
	}

	@Override
	public void insertClipById(Clip clip) {
		for (int i = 0; i < NUM_CLIPSET; i++) {
			if (clip.cid.type == clipSet[i].clipType) {
				clipSet[i].insertClipById(clip);
				return;
			}
		}
		throw new IllegalArgumentException("bad clip type " + clip.cid.type);
	}

	@Override
	public void insertClipByIndex(Clip clip) {
		for (int i = 0; i < NUM_CLIPSET; i++) {
			if (clip.cid.type == clipSet[i].clipType) {
				clipSet[i].insertClipByIndex(clip);
				return;
			}
		}
		throw new IllegalArgumentException("bad clip cat " + clip.cid.type);
	}

	@Override
	public boolean removeClip(Clip.ID cid) {
		for (int i = 0; i < NUM_CLIPSET; i++) {
			if (cid.type == clipSet[i].clipType) {
				return clipSet[i].removeClip(cid);
			}
		}
		return false;
	}

	@Override
	public int findClipIndex(Clip.ID cid) {
		int start = 0;
		for (int i = 0; i < NUM_CLIPSET; i++) {
			if (cid.type == clipSet[i].clipType) {
				int index = clipSet[i].findClipIndex(cid);
				return index >= 0 ? start + index : index;
			}
			start += clipSet[i].getCount();
		}
		return -1;
	}

	@Override
	public Clip findClip(Clip.ID cid) {
		for (int i = 0; i < NUM_CLIPSET; i++) {
			if (cid.type == clipSet[i].clipType) {
				return clipSet[i].findClip(cid);
			}
		}
		return null;
	}

	@Override
	public boolean isLiveClip(Clip clip) {
		for (int i = 0; i < NUM_CLIPSET; i++) {
			if (clip.cid.type == clipSet[i].clipType) {
				return clipSet[i].isLiveClip(clip);
			}
		}
		return false;
	}

	@Override
	public boolean clipChanged(Clip clip, boolean isLive, boolean bFinished) {
		for (int i = 0; i < NUM_CLIPSET; i++) {
			if (clip.cid.type == clipSet[i].clipType) {
				return clipSet[i].clipChanged(clip, isLive, bFinished);
			}
		}
		return false;
	}

	@Override
	public boolean moveClip(Clip.ID cid, int clipIndex) {
		for (int i = 0; i < NUM_CLIPSET; i++) {
			if (cid.type == clipSet[i].clipType) {
				return clipSet[i].moveClip(cid, clipIndex);
			}
		}
		return false;
	}

	@Override
	public int getTotalLengthMs() {
		int total = 0;
		for (int i = 0; i < NUM_CLIPSET; i++) {
			total += clipSet[i].getTotalLengthMs();
		}
		return total;
	}

}
