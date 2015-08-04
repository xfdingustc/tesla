package com.transee.viditcam.actions;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;

import com.transee.vdb.ClipSet;
import com.transee.vdb.Playlist;
import com.transee.vdb.PlaylistSet;
import com.transee.vdb.RemoteClip;
import com.waylens.hachi.R;
import com.transee.viditcam.app.VdbEditor;
import com.transee.viditcam.app.VdbFormatter;

abstract public class SelectMarkType extends SingleSelect {

	abstract protected void onSelectMarkType(int markType);

	private Context mContext;
	private final VdbEditor mEditor;
	private int mMarkType; // -1: marked cs; >=0: index of playlist

	public SelectMarkType(Activity activity, VdbEditor editor) {
		super(activity);
		mContext = activity;
		mEditor = editor;
		mMarkType = -1;
	}

	@Override
	protected void onSelectItem(int id) {
		mMarkType = id;
	}

	@Override
	protected void onClickPositiveButton() {
		onSelectMarkType(mMarkType);
	}

	private void addClipSet(Resources res, int resId, int index, ClipSet clipSet) {
		int totalClips = clipSet.getCount();
		String numClips = clipSet == null || totalClips == 0 ? VdbFormatter.getEmptyPlaylistString(mContext) : Integer
				.toString(totalClips);
		String listName = res.getString(resId);
		if (index >= 0) {
			listName += Integer.toString(index + 1);
		}
		String title = listName + " (" + numClips + ")";
		addItem(title, index);
	}

	@Override
	public void show() {
		Resources res = mContext.getResources();

		addClipSet(res, R.string.sel_marked_list, -1, mEditor.mVdb.getClipSet(RemoteClip.TYPE_MARKED));
		setSelId(-1); // TODO

		PlaylistSet playlistSet = mEditor.mVdb.getPlaylistSet();
		for (int i = 0; i < playlistSet.mPlaylists.size(); i++) {
			Playlist playlist = playlistSet.mPlaylists.get(i);
			if (playlist != null) {
				addClipSet(res, R.string.sel_playlist, i, playlist.clipSet);
			}
		}

		setTitle(R.string.title_mark);
		setButtons(DialogBuilder.DLG_OK_CANCEL);
		mbNoAutoDismiss = true;

		super.show();
	}

}
