package com.transee.viditcam.actions;

import android.app.Activity;
import android.content.res.Resources;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.transee.common.DateTime;
import com.transee.common.Utils;
import com.transee.vdb.Clip;
import com.transee.vdb.RemoteVdbClient;
import com.transee.vdb.VdbClient.DownloadInfoEx;
import com.transee.vdb.VdbClient.DownloadStreamInfo;
import com.transee.viditcam.app.VdbEditor;
import com.waylens.hachi.R;

import java.util.Locale;

abstract public class ConfirmDownload extends DialogBuilder {

	abstract public void onConfirmDownload(DownloadInfoEx downloadInfo, int stream, Clip.StreamInfo streamInfo);

	private final VdbEditor mEditor;
	private final DownloadInfoEx mDownloadInfo;
	private final Clip.StreamInfo[] mStreamInfo;
	private int mSelStream;
	private TextView mDurationText;

	public ConfirmDownload(Activity activity, VdbEditor editor, DownloadInfoEx downloadInfo,
			Clip.StreamInfo[] streamInfo) {
		super(activity);

		mEditor = editor;
		mDownloadInfo = downloadInfo;
		mStreamInfo = streamInfo;
		mSelStream = 0;

		if ((mDownloadInfo.opt & RemoteVdbClient.DOWNLOAD_OPT_PLAYLIST) != 0) {
			setTitle(R.string.title_download_playlist);
		} else {
			setTitle(R.string.title_download_sel_video);
		}

		setContent(R.layout.dialog_confirm_download);
		setButtons(DialogBuilder.DLG_OK_CANCEL);
	}

	private Clip.StreamInfo getStreamInfo(int index) {
		Clip clip = null;
		if (mEditor.isEditingClip()) {
			clip = mEditor.mClip;
		} else if (mEditor.isEditingPlaylist()) {
			clip = mEditor.mPlaylist.getClip(0);
		}
		return clip == null ? null : clip.getStream(index);
	}

	private String getStreamInfoText(int index) {
		Clip.StreamInfo si = getStreamInfo(index);
		if (si != null && si.video_width > 0) {
			return String.format(Locale.US, "%dx%d", si.video_width, si.video_height);
		}
		return null;
	}

	private void setItemInfo(String fmt, DownloadStreamInfo stream, int index, RadioButton radio) {
		if (stream.lengthMs > 0 && stream.url != null) {
			String videoInfo = getStreamInfoText(index);
			String spaceInfo = Utils.formatSpace((int) (stream.size / 1000));
			String text = videoInfo == null ? fmt + " " + spaceInfo : videoInfo + ", " + fmt + " " + spaceInfo;
			radio.setText(text);
			radio.setOnClickListener(mOnSelect);
			if (index == mSelStream) {
				radio.setChecked(true);
				setDurationInfo();
			}
		} else {
			radio.setVisibility(View.GONE);
		}
	}

	private View.OnClickListener mOnSelect = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.radioButton1) {
				mSelStream = 0;
				setDurationInfo();
			} else if (v.getId() == R.id.radioButton2) {
				mSelStream = 1;
				setDurationInfo();
			}
		}
	};

	private void setDurationInfo() {
		DownloadStreamInfo stream = mSelStream == 0 ? mDownloadInfo.main : mDownloadInfo.sub;
		String text = mContext.getResources().getString(R.string.lable_video_duration);
		String length = DateTime.secondsToString(stream.lengthMs / 1000);
		mDurationText.setText(text + length);
	}

	@Override
	protected void onDialogCreated(BaseDialog dialog, View layout) {
		Resources res = mContext.getResources();
		String fmt = res.getString(R.string.lable_video_size_about);

		mDurationText = (TextView)layout.findViewById(R.id.textView1);

		RadioButton radio = (RadioButton)layout.findViewById(R.id.radioButton1);
		setItemInfo(fmt, mDownloadInfo.main, 0, radio);

		radio = (RadioButton)layout.findViewById(R.id.radioButton2);
		setItemInfo(fmt, mDownloadInfo.sub, 1, radio);
	}

	@Override
	protected void onClickPositiveButton() {
		onConfirmDownload(mDownloadInfo, mSelStream, mStreamInfo[mSelStream]);
	}

}
