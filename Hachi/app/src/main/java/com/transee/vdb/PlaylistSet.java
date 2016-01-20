package com.transee.vdb;

import java.util.ArrayList;

// TODO - should copy the playlistSet instead of replacing
public class PlaylistSet {

	public int mFlags;
	public int mNumPlaylists;
	public ArrayList<Playlist> mPlaylists = new ArrayList<Playlist>();

	public void set(PlaylistSet other) {
		mFlags = other.mFlags;
		mNumPlaylists = other.mNumPlaylists;
		mPlaylists = other.mPlaylists;
	}





	public void clear() {
		mNumPlaylists = 0;
		mPlaylists.clear();
	}

}
