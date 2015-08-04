package com.transee.viditcam.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.transee.viditcam.app.comp.MapProvider;

public class CameraVideoEditPref {

	public int mapProvider;
	public boolean showAcc;
	public boolean autoFastBrowse;
	public boolean playLowBitrateStream;
	public boolean showButtonHint;

	static public final String PREF_VIDEO_EDIT = "videoEdit";
	static public final String PREF_MAP_PROVIDER = "mapProvider";
	static public final String PREF_SHOW_ACC = "showAcc";
	static public final String PREF_AUTO_FAST_BROWSE = "autoFastBrowse";
	static public final String PREF_PLAY_LOWBITRATE = "playLowBitrateStream";
	static public final String PREF_SHOW_BUTTON_HINT = "showButtonHint";

	public void load(Context context) {
		SharedPreferences pref = context.getSharedPreferences(PREF_VIDEO_EDIT, Context.MODE_PRIVATE);
		mapProvider = pref.getInt(PREF_MAP_PROVIDER, MapProvider.MAP_UNKNOWN);
		showAcc = pref.getBoolean(PREF_SHOW_ACC, true);
		autoFastBrowse = pref.getBoolean(PREF_AUTO_FAST_BROWSE, false);
		playLowBitrateStream = pref.getBoolean(PREF_PLAY_LOWBITRATE, true);
		showButtonHint = pref.getBoolean(PREF_SHOW_BUTTON_HINT, true);
	}

	public void save(Context context) {
		SharedPreferences pref = context.getSharedPreferences(PREF_VIDEO_EDIT, Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putInt(PREF_MAP_PROVIDER, mapProvider);
		editor.putBoolean(PREF_SHOW_ACC, showAcc);
		editor.putBoolean(PREF_AUTO_FAST_BROWSE, autoFastBrowse);
		editor.putBoolean(PREF_PLAY_LOWBITRATE, playLowBitrateStream);
		editor.putBoolean(PREF_SHOW_BUTTON_HINT, showButtonHint);
		editor.commit();
	}
}
