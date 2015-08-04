package com.transee.viditcam.actions;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.transee.vdb.Playlist;
import com.transee.viditcam.app.VdbEditor;
import com.transee.viditcam.app.VdbFormatter;
import com.waylens.hachi.R;

abstract public class ConfirmDeleteClip extends DialogBuilder {

	abstract public void onConfirmed();

	protected final VdbEditor mEditor;

	public ConfirmDeleteClip(Activity activity, VdbEditor editor) {
		super(activity);
		mEditor = editor;
		if (editor.isEditingClip()) {
			setTitle(R.string.title_delete_clip);
		} else {
			setTitle(R.string.title_clear_playlist);
		}
		setContent(R.layout.dialog_confirm_delete_clip);
		setButtons(DialogBuilder.DLG_OK_CANCEL);
	}

	@Override
	protected void onDialogCreated(BaseDialog dialog, View layout) {
		TextView textView1 = (TextView)layout.findViewById(R.id.textView1);
		TextView textView2 = (TextView)layout.findViewById(R.id.textView2);
		TextView textView3 = (TextView)layout.findViewById(R.id.textView3);
		if (mEditor.isEditingClip()) {
			textView1.setText(mEditor.mClip.getDateTimeString());
			textView2.setText(mEditor.mClip.getWeekDayString());
			textView3.setText(mEditor.mClip.getDurationString());
		} else if (mEditor.isEditingPlaylist()) {
			Playlist playlist = mEditor.mPlaylist;
			int index = mEditor.mVdb.getPlaylistSet().getPlaylistIndex(playlist.plistId);
			textView1.setText(VdbFormatter.formatPlaylistName(mContext, index));
			textView2.setText(VdbFormatter.getPlaylistNumClipsString(mContext, playlist));
			textView3.setText(playlist.getDurationString());
		}
	}

	@Override
	protected void onClickPositiveButton() {
		onConfirmed();
	}

}
