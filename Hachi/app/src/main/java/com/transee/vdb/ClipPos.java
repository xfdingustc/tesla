package com.transee.vdb;

// position in a clip
public class ClipPos {

	public static final int TYPE_POSTER = 0; // for clip poster
	public static final int TYPE_SLIDE = 1; // for clip slides
	public static final int TYPE_ANIMATION = 2; // for clip animation
	public static final int TYPE_PREVIEW = 3; // for fast preview in video list view

	public static final int F_IS_LAST = 0x80; // last point in the clip

	public String vdbId;
	public final Clip.ID cid;

	private final boolean mbIsLast;
	private final int mDate;
	private final int mType; // TYPE_POSTER etc.
	private final long mClipTimeMs; // absolute time in the clip

	private long mRealTimeMs; // real time returned by server
	private int mDuration; // returned by server

	public ClipPos(String vdbId, Clip.ID cid, int date, long timeMs, int type, boolean bIsLast) {
		this.vdbId = vdbId;
		this.cid = cid;
		this.mbIsLast = bIsLast;
		this.mDate = date;
		this.mType = type;
		this.mClipTimeMs = timeMs;
		this.mRealTimeMs = timeMs; // fixed later by server
		this.mDuration = 0; // fixed later by server
	}

	public ClipPos(Clip clip, long clipTimeMs, int type, boolean bIsLast) {
		this(clip.getVdbId(), clip.cid, clip.clipDate, clipTimeMs, type, bIsLast);
	}

	public final void setVdbId(String vdbId) {
		this.vdbId = vdbId;
	}

	public final int getType() {
		return mType;
	}

	public final boolean isDiscardable() {
		return mType == TYPE_ANIMATION || mType == TYPE_PREVIEW;
	}

	public final boolean isLast() {
		return mbIsLast;
	}

	public final int getClipDate() {
		return mDate;
	}

	public final long getClipTimeMs() {
		return mClipTimeMs;
	}

	public final long getRealTimeMs() {
		return mRealTimeMs;
	}

	public final int getDuration() {
		return mDuration;
	}

	// set value returned by server
	public final void setRealTimeMs(long realTimeMs) {
		mRealTimeMs = realTimeMs;
	}

	// set value returned by server
	public final void setDuration(int duration) {
		mDuration = duration;
	}

}
