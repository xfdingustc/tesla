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

	public Playlist getPlaylist(int index) {
		if (index < 0 || index >= mPlaylists.size())
			return null;
		return mPlaylists.get(index);
	}

	public Playlist findPlaylist(int plistId) {
		for (Playlist tmp : mPlaylists) {
			if (plistId == tmp.plistId)
				return tmp;
		}
		return null;
	}

	public int getPlaylistIndex(int plistId) {
		for (int i = 0; i < mPlaylists.size(); i++) {
			Playlist tmp = mPlaylists.get(i);
			if (plistId == tmp.plistId)
				return i;
		}
		return -1;
	}

	public boolean clearPlaylist(int plistId) {
		for (int i = 0; i < mPlaylists.size(); i++) {
			Playlist tmp = mPlaylists.get(i);
			if (plistId == tmp.plistId) {
				tmp.clear();
				return true;
			}
		}
		return false;
	}

	public void clear() {
		mNumPlaylists = 0;
		mPlaylists.clear();
	}

}
