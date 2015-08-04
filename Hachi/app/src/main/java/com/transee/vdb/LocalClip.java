package com.transee.vdb;

public class LocalClip extends Clip {

	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_FILE = 1; // downloaded
	public static final int TYPE_DOWNLOADING = 2; // downloading

	protected LocalClip(int type, int subType, Object extra, int numStreams) {
		super(new Clip.ID(CAT_LOCAL, type, subType, extra), numStreams);
	}

	@Override
	public boolean isLocal() {
		return true;
	}

}
