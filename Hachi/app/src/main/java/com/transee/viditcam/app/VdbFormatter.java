package com.transee.viditcam.app;

import android.content.Context;

import com.transee.vdb.Playlist;
import com.waylens.hachi.R;

public class VdbFormatter {

	static private String mPlaylistName;
	static private String mPlaylistEmpty;
	static private String mNumClipsString;

	static public String formatPlaylistName(Context context, int index) {
		if (mPlaylistName == null) {
			mPlaylistName = context.getResources().getString(R.string.sel_playlist);
		}
		return mPlaylistName + " " + Integer.toString(index + 1);
	}

	static public String getEmptyPlaylistString(Context context) {
		if (mPlaylistEmpty == null) {
			mPlaylistEmpty = context.getResources().getString(R.string.info_empty_list);
		}
		return mPlaylistEmpty;
	}

	static public String getPlaylistNumClipsString(Context context, Playlist playlist) {
		if (mNumClipsString == null) {
			mNumClipsString = context.getResources().getString(R.string.info_num_clips);
		}
		return mNumClipsString + playlist.numClips;
	}

}
