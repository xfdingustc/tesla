package com.waylens.hachi.vdb;

abstract public class ClipSet {

	public final int clipCat; // clip category
	public final int clipType; // clip type

	abstract public int getCount();

	abstract public int getTotalLengthMs();

	abstract public void clear();

	abstract public void set(ClipSet other);

	abstract public Clip getClip(int index);

	abstract public void insertClipById(Clip clip);

	abstract public void insertClipByIndex(Clip clip);

	abstract public boolean removeClip(Clip.ID cid);

	abstract public int findClipIndex(Clip.ID cid);

	abstract public Clip findClip(Clip.ID cid);

	abstract public boolean isLiveClip(Clip clip);

	abstract public boolean clipChanged(Clip clip, boolean isLive, boolean bFinished);

	abstract public boolean moveClip(Clip.ID cid, int clipIndex);

	public ClipSet(int cat, int type) {
		this.clipCat = cat;
		this.clipType = type;
	}

}
