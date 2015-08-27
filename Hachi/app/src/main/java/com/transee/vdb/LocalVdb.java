package com.transee.vdb;

import android.content.Context;

import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.CompositeClipSet;
import com.waylens.hachi.vdb.DownloadingClip;
import com.waylens.hachi.vdb.FileClip;
import com.waylens.hachi.vdb.LocalClip;
import com.waylens.hachi.vdb.SimpleClipSet;

import java.io.File;

public class LocalVdb extends Vdb {

	private final SimpleClipSet mFileClipSet;
	private final SimpleClipSet mDownloadingClipSet;
	private final CompositeClipSet mClipSet;
	private final LocalVdbClient mClient;

	public LocalVdb(Context context, Callback callback) {
		super(callback);
		mFileClipSet = new SimpleClipSet(Clip.CAT_LOCAL, LocalClip.TYPE_FILE);
		mDownloadingClipSet = new SimpleClipSet(Clip.CAT_LOCAL, LocalClip.TYPE_DOWNLOADING);
		mClipSet = new CompositeClipSet(Clip.CAT_LOCAL, mFileClipSet, mDownloadingClipSet);
		mClient = new LocalVdbClient(context, new VdbClientCallback());
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public void start(String hostString) {
		mClient.requestClipSetInfo(LocalClip.TYPE_UNKNOWN); // not used by
															// LocalVdbClient
		mClient.start();
	}

	@Override
	public void stop() {
		mClient.stop();
	}

	@Override
	protected void onVdbMounted() {
	}

	@Override
	protected void onVdbUnmounted() {
	}

	@Override
	public ClipSet getClipSet(int clipType) {
		return mClipSet;
	}

	@Override
	public PlaylistSet getPlaylistSet() {
		return null;
	}

	@Override
	public VdbClient getClient() {
		return mClient;
	}

	@Override
	protected void handleClipSetInfo(ClipSet clipSet) {
		if (clipSet.clipType == LocalClip.TYPE_FILE) {
			mFileClipSet.set(clipSet);
			mCallback.onClipSetInfo(this, mClipSet);
			return;
		}
		if (clipSet.clipType == LocalClip.TYPE_DOWNLOADING) {
			mDownloadingClipSet.set(clipSet);
			mCallback.onClipSetInfo(this, mClipSet);
			return;
		}
	}

	@Override
	protected void handleDownloadStarted(int id) {
		// TODO
	}

	@Override
	protected void handleDownloadFinished(int id, String outputFile) {
		Clip.ID cid = DownloadingClip.createClipId(id);
		DownloadingClip clip = (DownloadingClip)mDownloadingClipSet.findClip(cid);
		if (clip != null) {
			if (mDownloadingClipSet.removeClip(cid)) {
				clip.outputFile = outputFile;
				FileClip fclip = new FileClip(new File(clip.outputFile));
				fclip.streams[0] = clip.streams[0];
				fclip.index = clip.index;
				fclip.clipDate = clip.clipDate;
				fclip.clipLengthMs = clip.clipLengthMs;
				mFileClipSet.addClip(fclip);
				mCallback.onDownloadFinished(this, clip, fclip);
			}
		}
	}

	@Override
	protected void handleDownloadError(int id) {
		// TODO
	}

	@Override
	protected void handleDownloadProgress(int id, int progress) {
		Clip.ID cid = DownloadingClip.createClipId(id);
		DownloadingClip clip = (DownloadingClip)mDownloadingClipSet.findClip(cid);
		if (clip != null) {
			clip.progress = progress;
			mCallback.onDownloadProgress(this, id, progress);
		}
	}

}
