package com.transee.common;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

public class BeepManager {
	private static final String TAG = "BeepManager";
	private static MediaPlayer mPlayer;

	// private static final float BEEP_VOLUME = 0.10f;
	// private static final long VIBRATE_DURATION = 200L;

	public static void play(Context context, int resource, boolean bLoop) {
		if (mPlayer != null) {
			mPlayer.release();
		}
		mPlayer = new MediaPlayer();
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		try {
			AssetFileDescriptor file = context.getResources().openRawResourceFd(resource);
			mPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
			// player.setVolume(BEEP_VOLUME, BEEP_VOLUME);
			mPlayer.prepare();
			mPlayer.start();
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
		}
	}
}
