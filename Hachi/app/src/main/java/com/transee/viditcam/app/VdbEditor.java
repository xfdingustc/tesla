package com.transee.viditcam.app;

import android.net.Uri;

import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.DownloadInfoEx;
import com.waylens.hachi.vdb.FileClip;
import com.transee.vdb.Playlist;
import com.transee.vdb.RemoteVdbClient;
import com.transee.vdb.Vdb;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.vdb.RawDataBlock;


public class VdbEditor {

	private static final int GPS_SEG_LEN = 3 * 60 * 1000; // 3 minutes

	public final Vdb mVdb;
	public final int mUrlType;
	public final boolean mbPlaySecondStream;

	// edit clip
	public Clip mClip;
	public boolean mbVideoOnly; // do not enable bitmap animation

	// edit playlist
	public Playlist mPlaylist;
	public int mPlaylistClipIndex; // current clip

	// for playlist
	public boolean bMuteAudio;
	public String audioFileName;

	// current animation position
	public ClipPos mCurrAnimPos;
	public ClipPos mRequestedAnimPos;

	public boolean mbDelete;

	public VdbEditor(Vdb vdb, boolean bPlaySecondStream) {
		mVdb = vdb;
		mUrlType = getUrlType();
		mbPlaySecondStream = bPlaySecondStream;
	}

	private int getUrlType() {
		// Meizu does not support .ts playback
		// if (Build.BRAND.equals("Meizu"))
		return VdbClient.URL_TYPE_HLS;
		// return VdbClient.URL_TYPE_TS;
	}

	private final void clearVariables() {
		mbVideoOnly = false;
		bMuteAudio = false;
		audioFileName = null;
		mCurrAnimPos = null;
		mRequestedAnimPos = null;
		mbDelete = false;
	}

	public final void editClip(Clip clip) {
		clearVariables();
		mClip = clip;
		mbVideoOnly = clip.isLocal();
		mPlaylist = null;
		mPlaylistClipIndex = -1;
	}

	public final void editPlaylist(Playlist playlist, int clipIndex) {
		clearVariables();
		mClip = null;
		mbVideoOnly = false;
		mPlaylist = playlist;
		mPlaylistClipIndex = clipIndex;
	}

	public final void endEdit() {
		mClip = null;
		mPlaylist = null;
	}

	public final boolean isEditing() {
		return mClip != null || mPlaylist != null;
	}

	public final boolean isEditingClip() {
		return mClip != null;
	}

	public final boolean isEditingClip(Clip.ID cid) {
		return isEditingClip() && mClip.cid.equals(cid);
	}

	public final boolean isEditingClip(Clip clip) {
		return isEditingClip() && mClip.cid.equals(clip.cid);
	}

	public final boolean isEditingPlaylist() {
		return mPlaylist != null;
	}

	public final boolean isEditingPlaylist(int plistId) {
		return isEditingPlaylist() && mPlaylist.plistId == plistId;
	}

	public final boolean isEditingPlaylist(Clip.ID cid) {
		return cid.cat == Clip.CAT_REMOTE && cid.subType == 0 && mPlaylist != null && mPlaylist.plistId == cid.type;
	}

	public final boolean isEditingLiveClip() {
		if (mClip != null) {
			ClipSet clipSet = mVdb.getClipSet(mClip.cid.type);
			if (clipSet != null && clipSet.isLiveClip(mClip))
				return true;
		}
		return false;
	}

	public final Clip.StreamInfo getClipStreamInfo(Clip.ID cid, int stream) {
		if (!isEditingClip(cid))
			return null;
		return mClip.getStream(stream);
	}

	public final Clip.StreamInfo getPlaylistStreamInfo(int listType, int stream) {
		if (!isEditingPlaylist(listType)) {
			return null;
		}
		Clip clip = mPlaylist.getClip(0);
		return clip == null ? null : clip.getStream(stream);
	}

	public final boolean hasMap() {
		if (mVdb.isLocal()) {
			if (isEditingClip()) {
				if (mClip instanceof FileClip) {
					FileClip fclip = (FileClip)mClip;
					return fclip.has_gps;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	public final int getLengthMs() {
		if (isEditingClip()) {
			return mClip.clipLengthMs;
		}
		if (isEditingPlaylist()) {
			return mPlaylist.mTotalLengthMs;
		}
		return 0;
	}

	public final long getOffsetMs() {
		if (isEditingClip()) {
			return mClip.getStartTime();
		}
		if (isEditingPlaylist()) {
			return mPlaylist.getOffsetMs();
		}
		return 0;
	}

	public final boolean canMoveClipLeft() {
		if (isEditingPlaylist()) {
			return mPlaylistClipIndex > 0;
		}
		return false;
	}

	public final boolean canMoveClipRight() {
		if (isEditingPlaylist()) {
			return mPlaylistClipIndex + 1 < mPlaylist.numClips;
		}
		return false;
	}

	private final void requestMoveClip(int newPos) {
		if (isEditingPlaylist() && newPos >= 0 && newPos < mPlaylist.numClips) {
			Clip clip = mPlaylist.getClip(mPlaylistClipIndex);
			if (clip != null) {
				mVdb.getClient().requestMoveClip(clip.cid, newPos);
			}
		}
	}

	public final void requestMoveClipLeft() {
		requestMoveClip(mPlaylistClipIndex - 1);
	}

	public final void requestMoveClipRight() {
		requestMoveClip(mPlaylistClipIndex + 1);
	}

	public final void requestRemoveClip() {
		if (isEditingPlaylist()) {
			Clip clip = mPlaylist.getClip(mPlaylistClipIndex);
			if (clip != null) {
				mVdb.getClient().requestDeleteClip(clip);
			}
		}
	}

	public final void requestToggleMute() {
		bMuteAudio = !bMuteAudio;
		audioFileName = null;
	}

	public final void setAudio(String url) {
		bMuteAudio = true;
		audioFileName = url;
	}

	public final Clip.StreamInfo[] getStreamInfo(DownloadInfoEx downloadInfo) {
		if (isEditingClip(downloadInfo.cid)) {
			return mClip.streams;
		}
		if (isEditingPlaylist(downloadInfo.cid.type)) {
			Clip clip = mPlaylist.getClip(0);
			if (clip != null)
				return clip.streams;
		}
		return null;
	}

	public final void requestClipPlaybackUrl(long clipTimeMs) {
		int stream = VdbClient.STREAM_MAIN;
		if (mbPlaySecondStream) {
			if (mClip.streams.length > 1 && mClip.streams[1].valid()) {
				stream = VdbClient.STREAM_SUB_1;
			}
		}
		mVdb.getClient().requestClipPlaybackUrl(mUrlType, mClip, stream, false, clipTimeMs, mClip.clipLengthMs);
	}

	public final void requestPlaylistPlaybackUrl(int playlistTimeMs) {
		int stream = VdbClient.STREAM_MAIN;
		if (mbPlaySecondStream) {
			Clip clip = mPlaylist.getClip(0);
			if (clip.streams.length > 1 && clip.streams[1].valid()) {
				stream = VdbClient.STREAM_SUB_1;
			}
		}
		mVdb.getClient().requestPlaylistPlaybackUrl(mUrlType, mPlaylist.plistId, playlistTimeMs, stream, bMuteAudio);
	}

	private final boolean isAnimationShown(Clip clip, long timeMs) {
		if (mCurrAnimPos == null)
			return false;
		if (!mCurrAnimPos.cid.equals(clip.cid))
			return false;
		if (timeMs >= mCurrAnimPos.getRealTimeMs() + mCurrAnimPos.getDuration())
			return false;
		if (timeMs < mCurrAnimPos.getRealTimeMs())
			return false;
		return true;
	}

	private final void requestClipAnimationImage(Clip clip, ClipPos clipPos) {
		mRequestedAnimPos = clipPos;
		mVdb.getClient().requestClipImage(clip, clipPos, 0, 0);
	}

	public final void requestClipAnimationImage(long clipTimeMs) {
		long endMs = mClip.getStartTime() + mClip.clipLengthMs - 1;
		boolean bIsLast = false;
		if (clipTimeMs >= endMs) {
			clipTimeMs = endMs;
			bIsLast = true;
		}
		if (!isAnimationShown(mClip, clipTimeMs)) {
			ClipPos clipPos = new ClipPos(mClip, clipTimeMs, ClipPos.TYPE_ANIMATION, bIsLast);
			requestClipAnimationImage(mClip, clipPos);
		}
	}

	public final void requestPlaylistAnimationImage(Clip clip, long clipTimeMs) {
		boolean bIsLast = false;
		if ((int)clipTimeMs > clip.clipLengthMs - 1) {
			clipTimeMs = clip.clipLengthMs - 1;
			bIsLast = true;
		}
		clipTimeMs += clip.getStartTime();
		if (!isAnimationShown(clip, clipTimeMs)) {
			ClipPos clipPos = new ClipPos(clip, clipTimeMs, ClipPos.TYPE_ANIMATION, bIsLast);
			requestClipAnimationImage(clip, clipPos);
		}
	}

	public final boolean checkAnimationPos(ClipPos animPos) {
		if (animPos == null || mRequestedAnimPos == null) {
			return false;
		}
		if (!animPos.cid.equals(mRequestedAnimPos.cid)) {
			return false;
		}
		return true;
	}

	public final void updateAnimationPos(ClipPos animPos) {
		mCurrAnimPos = animPos;
	}

	public final void requestClipGPSData(long clipTimeMs) {
		mVdb.getClient().requestRawData(mClip, clipTimeMs, RawDataBlock.F_RAW_DATA_GPS);
	}

	public final void requestPlaylistGPSData(Clip clip, long clipTimeMs) {
		mVdb.getClient().requestRawData(clip, clip.getStartTime() + clipTimeMs, RawDataBlock.F_RAW_DATA_GPS);
	}

	public final void requestMarkClip(long startTimeMs, long endTimeMs) {
		mVdb.getClient().requestMarkClip(mClip, startTimeMs, endTimeMs);
	}

	public final void requestInsertClip(long startTimeMs, long endTimeMs, int plistId) {
		mVdb.getClient().requestInsertClip(mClip.cid, startTimeMs, endTimeMs, plistId, -1);
	}

	public final void requestDeleteClip() {
		mVdb.getClient().requestDeleteClip(mClip);
	}

	public final void requestClearPlaylist() {
		mVdb.getClient().requestClearPlaylist(mPlaylist.plistId);
	}

	public final boolean deleteCurrent() {
		if (isEditingClip()) {
			requestDeleteClip();
			return true;
		}
		if (isEditingPlaylist()) {
			requestClearPlaylist();
			return true;
		}
		return false;
	}

	public final void requestClipSlideImage(int posMs, int rawDataBlocks, int lengthMs, int width, int height) {
		// slide image
		boolean bIsLast = false;
		if (posMs > mClip.clipLengthMs - 1) {
			posMs = mClip.clipLengthMs - 1;
		}
		long clipTimeMs = mClip.getStartTime() + posMs;
		ClipPos clipPoint = new ClipPos(mClip, clipTimeMs, ClipPos.TYPE_SLIDE, bIsLast);
		mVdb.getClient().requestClipImage(mClip, clipPoint, width, height);
		// acc raw data
		if ((rawDataBlocks & RawDataBlock.F_RAW_DATA_ACC) != 0) {
			mVdb.getClient().requestRawDataBlock(mClip.getVdbId(), mClip.cid, clipTimeMs, lengthMs,
				RawDataBlock.RAW_DATA_ACC, false);
		}
	}

	public final void requestPlaylistSlideImage(int index, int width, int height) {
		Clip clip = mPlaylist.getClip(index);
		if (clip != null) {
			ClipPos clipPoint = new ClipPos(clip, clip.getStartTime(), ClipPos.TYPE_SLIDE, false);
			mVdb.getClient().requestClipImage(clip, clipPoint, width, height);
		}
	}

	// first loop: get download url and prompt for confirm
	// second loop: get raw data and download url, and start download
	public final void requestClipDownloadUrl(long startMs, long endMs, boolean bFirstLoop) {
		if (isEditingClip()) {
			int lengthMs = (int)(endMs - startMs);
			VdbClient client = mVdb.getClient();
			int length = (int)(endMs - startMs);
			Clip clip = mClip;
			if (!bFirstLoop) {
				String vdbId = clip.getVdbId();
				client.requestRawDataBlock(vdbId, clip.cid, startMs, length, RawDataBlock.RAW_DATA_GPS, true);
				client.requestRawDataBlock(vdbId, clip.cid, startMs, length, RawDataBlock.RAW_DATA_ACC, true);
				client.requestRawDataBlock(vdbId, clip.cid, startMs, length, RawDataBlock.RAW_DATA_ODB, true);
			}
			client.requestClipDownloadUrl(clip, startMs, lengthMs, bFirstLoop);
		}
	}

	public final void requestPlaylistDownloadUrl(boolean bFirstLoop) {
		if (isEditingPlaylist()) {
			int length_ms = mPlaylist.mTotalLengthMs;
			if (!bFirstLoop) {
				Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, mPlaylist.plistId, 0, null);
				VdbClient client = mVdb.getClient();
				client.requestRawDataBlock(null, cid, 0, length_ms, RawDataBlock.RAW_DATA_GPS, true);
				client.requestRawDataBlock(null, cid, 0, length_ms, RawDataBlock.RAW_DATA_ACC, true);
				client.requestRawDataBlock(null, cid, 0, length_ms, RawDataBlock.RAW_DATA_ODB, true);
			}
			mVdb.getClient().requestPlaylistDownloadUrl(null, mPlaylist.plistId, 0, length_ms, bMuteAudio, bFirstLoop);
		}
	}

	private void requestGPS(Clip clip) {
		long clipTime = clip.getStartTime();
		int remain = clip.clipLengthMs;
		while (remain > 0) {
			int len = remain < GPS_SEG_LEN ? remain : GPS_SEG_LEN;
			mVdb.getClient().requestRawDataBlock(clip.getVdbId(), clip.cid, clipTime, len, RawDataBlock.RAW_DATA_GPS,
					false);
			clipTime += len;
			remain -= len;
		}
	}

	public final void requestMapTrack() {
		if (isEditingClip()) {
			requestGPS(mClip);
			return;
		}
		if (isEditingPlaylist()) {
			ClipSet clipSet = mPlaylist.clipSet;
			for (int i = 0; i < clipSet.getCount(); i++) {
				Clip clip = clipSet.getClip(i);
				requestGPS(clip);
			}
			return;
		}
	}

	public final Uri getClipUri() {
		return isEditingClip() ? mClip.getUri() : null;
	}
}
