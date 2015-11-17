package com.waylens.hachi.vdb;

import com.waylens.hachi.vdb.Clip;

public class RemoteClip extends Clip {

	public static final int TYPE_UNKNOWN = -1;
	public static final int TYPE_BUFFERED = 0;
	public static final int TYPE_MARKED = 1;

	public static final int GET_CLIP_EXTRA = 1 << 0;
	public static final int GET_CLIP_VDB_ID = 1 << 1;

	public static final int UUID_LEN = 36;

	// VDB version 1.0:
	// there are 3 playlists, types: 256, 257, 258

	// length(ms) removed at the head
	public long clipStartTime;

	public RemoteClip(int type, int subType, Object extra, int clipDate, int duration) {
		// a remote clip has 2 streams at most
		super(new Clip.ID(CAT_REMOTE, type, subType, extra), 2);
		this.clipDate = clipDate;
		this.mDurationMs = duration;
	}

	@Override
	public long getStartTimeMs() {
		return clipStartTime;
	}

	@Override
	public boolean contains(long timeMs) {
		return timeMs >= clipStartTime && timeMs < clipStartTime + mDurationMs;
	}

	@Override
	public String getVdbId() {
		return (String)cid.extra;
	}
}
