package com.transee.vdb;

public class RemoteVdb extends Vdb {

	private final SimpleClipSet mBufferedCS;
	private final SimpleClipSet mMarkedCS;
	private final PlaylistSet mPlaylistSet;
	private final RemoteVdbClient mClient;
	private final boolean mbIsServer;

	public RemoteVdb(Callback callback, String tempFileDir, boolean bIsServer) {
		super(callback);
		mBufferedCS = new SimpleClipSet(Clip.CAT_REMOTE, RemoteClip.TYPE_BUFFERED);
		mMarkedCS = new SimpleClipSet(Clip.CAT_REMOTE, RemoteClip.TYPE_MARKED);
		mPlaylistSet = new PlaylistSet();
		mClient = new RemoteVdbClient(new VdbClientCallback(), tempFileDir);
		mbIsServer = bIsServer;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	private void requestVdbClips() {
		if (mbIsServer) {
			mClient.requestAllClipSetInfo();
		} else {
			mClient.requestClipSetInfo(RemoteClip.TYPE_BUFFERED);
			mClient.requestClipSetInfo(RemoteClip.TYPE_MARKED);
			mClient.requestPlaylistSetInfo(0);
		}
	}

	@Override
	public void start(String hostString) {
		requestVdbClips();
		mClient.start(hostString);
	}

	@Override
	public void stop() {
		mClient.stop();
	}

	@Override
	public SimpleClipSet getClipSet(int clipType) {
		if (clipType == RemoteClip.TYPE_BUFFERED)
			return mBufferedCS;
		if (clipType == RemoteClip.TYPE_MARKED)
			return mMarkedCS;
		return null;
	}

	@Override
	public PlaylistSet getPlaylistSet() {
		return mPlaylistSet;
	}

	@Override
	public VdbClient getClient() {
		return mClient;
	}

	@Override
	protected void onVdbMounted() {
		requestVdbClips();
	}

	@Override
	protected void onVdbUnmounted() {
		mBufferedCS.clear();
		mMarkedCS.clear();
		mPlaylistSet.clear();
	}
}
