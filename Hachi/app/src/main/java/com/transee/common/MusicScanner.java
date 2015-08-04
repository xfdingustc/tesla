package com.transee.common;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Handler;
import android.provider.MediaStore.Audio.Media;

import java.util.ArrayList;

abstract public class MusicScanner {

	abstract public void onScanResult(ArrayList<Music> list);

	public static class Music {
		public long id;
		public String displayName;
		public String album;
		public String artist;
		public int duration;
		public long size;
		public String url;
		public boolean bSelected;
	}

	private final ContentResolver mContentResolver;
	private final Handler mHandler;
	private Thread mThread;

	public MusicScanner(ContentResolver contentResolver) {
		mContentResolver = contentResolver;
		mHandler = new Handler();
	}

	public void startWork() {
		if (mThread == null) {
			mThread = new Thread() {
				@Override
				public void run() {
					scan();
				}
			};
			mThread.start();
		}
	}

	public void stopWork() {
		if (mThread != null) {
			mThread.interrupt();
			mThread = null;
			mHandler.removeCallbacks(null);
		}
	}

	private static String[] projection = { Media._ID, Media.DISPLAY_NAME, Media.DATA, Media.ALBUM, Media.ARTIST,
			Media.DURATION, Media.SIZE, };
	private static String where = "mime_type in ('audio/mpeg') and is_music > 0";

	private void scan() {
		ArrayList<Music> list = new ArrayList<Music>();
		Cursor cursor = mContentResolver.query(Media.EXTERNAL_CONTENT_URI, projection, where, null, Media.DATA);
		if (cursor != null && cursor.moveToFirst()) {
			int idCol = cursor.getColumnIndex(Media._ID);
			int displayNameCol = cursor.getColumnIndex(Media.DISPLAY_NAME);
			int albumCol = cursor.getColumnIndex(Media.ALBUM);
			int artistCol = cursor.getColumnIndex(Media.ARTIST);
			int durationCol = cursor.getColumnIndex(Media.DURATION);
			int sizeCol = cursor.getColumnIndex(Media.SIZE);
			int urlCol = cursor.getColumnIndex(Media.DATA);
			do {
				Music music = new Music();
				music.id = cursor.getLong(idCol);
				music.displayName = cursor.getString(displayNameCol);
				music.album = cursor.getString(albumCol);
				music.artist = cursor.getString(artistCol);
				music.duration = cursor.getInt(durationCol);
				music.size = cursor.getLong(sizeCol);
				music.url = cursor.getString(urlCol);
				list.add(music);
			} while (cursor.moveToNext());
		}
		postScanResult(list);
	}

	private void postScanResult(final ArrayList<Music> list) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				onScanResult(list);
			}
		});
	}

}
