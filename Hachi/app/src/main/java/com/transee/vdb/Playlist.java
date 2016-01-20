package com.transee.vdb;

import com.transee.common.DateTime;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

public class Playlist {

	public int plistId; // TODO
	public int numClips; // equals clipSet.size()
	public ClipSet clipSet = new ClipSet(Clip.CAT_UNKNOWN, 0);

	public int mProperties;
	public int mTotalLengthMs; // TODO



	public boolean bDeleting;


	public final boolean isEmpty() {
		return numClips == 0;
	}

	public final Clip getClip(int index) {
		return clipSet.getClip(index);
	}



	public void clear() {
		bDeleting = false;
		numClips = 0;
		mTotalLengthMs = 0;
		clipSet.clear();
	}




}
