package com.transee.viditcam.app.comp;

import android.view.View;

import com.transee.vdb.Clip;
import com.transee.vdb.RemoteVdbClient;
import com.transee.vdb.RemuxHelper;
import com.transee.vdb.RemuxerParams;
import com.transee.vdb.SlideView;
import com.transee.vdb.VdbClient;
import com.transee.vdb.VdbClient.DownloadInfoEx;
import com.transee.vdb.VdbClient.DownloadRawDataBlock;
import com.transee.vdb.VdbClient.DownloadStreamInfo;
import com.waylens.hachi.R;
import com.transee.viditcam.actions.ConfirmDeleteClip;
import com.transee.viditcam.actions.ConfirmDownload;
import com.transee.viditcam.actions.DialogBuilder;
import com.transee.viditcam.actions.PlaylistOperations;
import com.transee.viditcam.actions.SelectAudio;
import com.transee.viditcam.actions.SelectMarkType;
import com.transee.viditcam.app.BaseActivity;
import com.transee.viditcam.app.VdbEditor;
import com.transee.viditcam.app.ViditImageButton;

abstract public class VdbEdit {

	// < 0: marked clip set
	// 0, 1, 2: playlist
	abstract public boolean requestMarkClip(int index);

	abstract public void requestDeleteClip();

	abstract public void requestDownload();

	abstract public void onMarkClipOk();

	abstract public void onInsertClipOk();

	private final BaseActivity mActivity;
	private final SlideView mSlideView;
	private final VdbEditor mEditor;

	private ViditImageButton mSelectButton;
	private ViditImageButton mDownloadButton;
	private ViditImageButton mMarkButton;
	private ViditImageButton mEditButton;
	private ViditImageButton mDeleteButton;

	private boolean mMarking; // waiting for marking result

	public VdbEdit(BaseActivity activity, VdbEditor editor, SlideView slideView, boolean bShowHint) {
		mActivity = activity;
		mSlideView = slideView;
		mEditor = editor;
	}

	// API
	public void onInitUI(View toolbar) {
		mSelectButton = (ViditImageButton)toolbar.findViewById(R.id.btnSelect);
		mSelectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickSelectButton();
			}
		});

		mDownloadButton = (ViditImageButton)toolbar.findViewById(R.id.btnDownload);
		mDownloadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickDownloadButton();
			}
		});

		mMarkButton = (ViditImageButton)toolbar.findViewById(R.id.btnMark);
		mMarkButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickMarkButton();
			}
		});

		mEditButton = (ViditImageButton)toolbar.findViewById(R.id.btnEdit);
		mEditButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickEditButton(v);
			}
		});

		mDeleteButton = (ViditImageButton)toolbar.findViewById(R.id.btnDelete);
		mDeleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickDeleteButton();
			}
		});
	}

	// API
	public void initButtonState() {
		if (mEditor.isEditingPlaylist()) {
			mDownloadButton.setVisibility(View.VISIBLE);
			mEditButton.setVisibility(View.VISIBLE);
		} else {
			hideButton(mDownloadButton);
			hideButton(mEditButton);
		}
		hideButton(mMarkButton);
		showDeleteButton();
	}

	// API
	public void disableSelect() {
		mSelectButton.setVisibility(View.INVISIBLE);
	}

	// API
	public void enableSelect() {
		mSelectButton.changeImages(R.drawable.btn_sel, R.drawable.btn_sel_pressed);
		mSelectButton.setVisibility(View.VISIBLE);
	}

	// API
	public void posterSelectionChanged() {
		if (mEditor.isEditingClip()) {
			if (mSlideView.isSelecting()) {
				mDownloadButton.setVisibility(View.VISIBLE);
				mMarkButton.setVisibility(View.VISIBLE);
			} else {
				mDownloadButton.setVisibility(View.INVISIBLE);
				mMarkButton.setVisibility(View.INVISIBLE);
			}
		}
	}

	// API
	public void onMarkClipResult(int error) {
		if (mMarking) {
			mMarking = false;
			if (error == 0) {
				onMarkClipOk();
				return;
			}
			int resId = R.string.msg_mark_clip_error;
			DialogBuilder builder = new DialogBuilder(mActivity);
			builder.setMsg(resId);
			builder.setButtons(DialogBuilder.DLG_OK);
			builder.show();
		}
	}

	// API
	public void onInsertClipResult(int error) {
		if (mMarking) {
			mMarking = false;
			int resId;
			switch (error) {
			default:
			case RemoteVdbClient.eInsertClip_Error:
				resId = R.string.msg_insert_clip_error;
				break;
			case RemoteVdbClient.eInsertClip_OK:
				onInsertClipOk();
				return;
			case RemoteVdbClient.eInsertClip_UnknownStream:
				resId = R.string.msg_insert_clip_unknown_stream;
				break;
			case RemoteVdbClient.eInsertClip_StreamNotMatch:
				resId = R.string.msg_insert_clip_stream_not_match;
				break;
			}
			DialogBuilder builder = new DialogBuilder(mActivity);
			builder.setMsg(resId);
			builder.setButtons(DialogBuilder.DLG_OK);
			builder.show();
		}
	}

	// API
	public void onClipChanged(Clip clip, boolean bFinished) {
		showDeleteButton();
	}

	// API
	public void onDownloadUrlFailed() {
		// TODO
	}

	static class DownloadState {
		DownloadInfoEx downloadInfo;
		int stream;
		Clip.StreamInfo si;
		DownloadRawDataBlock mAccData;
		DownloadRawDataBlock mGpsData;
		DownloadRawDataBlock mObdData;
	}

	private DownloadState mDownloadState;

	// API
	public void onDownloadedRawData(DownloadRawDataBlock block) {
		if (mDownloadState == null)
			return;
		if (block.header.mNumItems <= 0)
			return;
		switch (block.header.mDataType) {
		case VdbClient.RAW_DATA_GPS:
			mDownloadState.mGpsData = block;
			break;
		case VdbClient.RAW_DATA_ACC:
			mDownloadState.mAccData = block;
			break;
		case VdbClient.RAW_DATA_ODB:
			mDownloadState.mObdData = block;
			break;
		}
	}

	private void startDownload(DownloadInfoEx downloadInfo, int stream, Clip.StreamInfo si) {
		if (mDownloadState != null)
			return;
		boolean isEditingClip = mEditor.isEditingClip(downloadInfo.cid);
		if (isEditingClip || mEditor.isEditingPlaylist(downloadInfo.cid)) {
			mDownloadState = new DownloadState();
			mDownloadState.downloadInfo = downloadInfo;
			mDownloadState.stream = stream;
			mDownloadState.si = si;
			DownloadStreamInfo dsi = stream == 0 ? downloadInfo.main : downloadInfo.sub;
			dsi.clipDate += (int)(mEditor.getOffsetMs() / 1000);
			if (isEditingClip) {
				mEditor.requestClipDownloadUrl(dsi.clipTimeMs, dsi.clipTimeMs + dsi.lengthMs, false);
			} else {
				mEditor.requestPlaylistDownloadUrl(false);
			}
		}
	}

	private void startDownload2(DownloadInfoEx downloadInfo) {
		if (mDownloadState != null) {
			downloadVideo(mDownloadState);
			mDownloadState = null;
		}
	}

	// API
	public void onDownloadUrlReady(DownloadInfoEx downloadInfo, boolean bFirstLoop) {
		Clip.StreamInfo[] streamInfo = mEditor.getStreamInfo(downloadInfo);
		if (streamInfo != null) {
			if (bFirstLoop) {
				ConfirmDownload action = new ConfirmDownload(mActivity, mEditor, downloadInfo, streamInfo) {
					@Override
					public void onConfirmDownload(DownloadInfoEx downloadInfo, int stream, Clip.StreamInfo si) {
						startDownload(downloadInfo, stream, si);
					}
				};
				action.show();
			} else {
				startDownload2(downloadInfo);
			}
		}
	}

	private void downloadVideo(DownloadState ds) {
		DownloadStreamInfo dsi = ds.stream == 0 ? ds.downloadInfo.main : ds.downloadInfo.sub;
		int resId = RemuxHelper.checkSpace(dsi.size);
		if (resId != 0) {
			DialogBuilder builder = new DialogBuilder(mActivity);
			builder.setTitle(R.string.title_cannot_download);
			builder.setMsg(resId);
			builder.setButtons(DialogBuilder.DLG_OK);
			builder.show();
		} else {
			RemuxerParams params = new RemuxerParams();
			// clip params
			params.setClipDate(dsi.clipDate); // TODO
			params.setClipTimeMs(dsi.clipTimeMs); // TODO
			params.setClipLength(dsi.lengthMs);
			// stream info
			params.setStreamVersion(ds.si.version);
			params.setVideoCoding(ds.si.video_coding);
			params.setVideoFrameRate(ds.si.video_framerate);
			params.setVideoWidth(ds.si.video_width);
			params.setVideoHeight(ds.si.video_height);
			params.setAudioCoding(ds.si.audio_coding);
			params.setAudioNumChannels(ds.si.audio_num_channels);
			params.setAudioSamplingFreq(ds.si.audio_sampling_freq);
			// download params
			params.setInputFile(dsi.url + ",0,-1;");
			params.setInputMime("ts");
			params.setOutputFormat("mp4");
			params.setPosterData(ds.downloadInfo.posterData);
			params.setGpsData(ds.mGpsData != null ? ds.mGpsData.ack_data : null);
			params.setAccData(ds.mAccData != null ? ds.mAccData.ack_data : null);
			params.setObdData(ds.mObdData != null ? ds.mObdData.ack_data : null);
			params.setDurationMs(dsi.lengthMs);
			params.setDisableAudio(mEditor.bMuteAudio);
			params.setAudioFileName(mEditor.audioFileName);
			params.setAudioFormat("mp3");
			// add to queue
			RemuxHelper.remux(mActivity, params);
		}
	}

	private void showDeleteButton() {
		if (mSlideView.isSelecting() || mEditor.isEditingLiveClip()) {
			hideButton(mDeleteButton);
		} else {
			mDeleteButton.setVisibility(View.VISIBLE);
		}
	}

	private void hideButton(ViditImageButton button) {
		button.setVisibility(View.INVISIBLE);
	}

	private void onClickSelectButton() {
		mSlideView.toggleSelect();
		if (mSlideView.isSelecting()) {
			mSelectButton.changeImages(R.drawable.btn_sel_hilight, R.drawable.btn_sel_hilight_pressed);
		} else {
			mSelectButton.changeImages(R.drawable.btn_sel, R.drawable.btn_sel_pressed);
			mSlideView.showScroll(0);
		}
		mSlideView.invalidateSel();
		showDeleteButton();
	}

	private void onClickDownloadButton() {
		if (mEditor.isEditingClip() && mSlideView.getSelLength() <= 0) {
			noVideoSelected();
		} else {
			requestDownload();
		}
	}

	private void onClickMarkButton() {
		if (mSlideView.getSelLength() <= 0) {
			noVideoSelected();
			return;
		}
		SelectMarkType action = new SelectMarkType(mActivity, mEditor) {
			@Override
			protected void onSelectMarkType(int index) {
				mMarking = requestMarkClip(index);
			}
		};
		action.show();
	}

	private void onClickEditButton(View v) {
		PlaylistOperations action = new PlaylistOperations(mActivity, mEditor) {
			@Override
			public void onSelectAudio() {
				VdbEdit.this.onSelectAudio();
			}
		};
		action.show(v);
	}

	private void onSelectAudio() {
		SelectAudio action = new SelectAudio(mActivity) {
			@Override
			public void onMusicSelected(String url) {
				mEditor.setAudio(url);
			}
		};
		action.show(mEditButton);
	}

	private void onClickDeleteButton() {
		ConfirmDeleteClip action = new ConfirmDeleteClip(mActivity, mEditor) {
			@Override
			public void onConfirmed() {
				onDeleteClipConfirmed();
			}
		};
		action.show();
	}

	private void onDeleteClipConfirmed() {
		requestDeleteClip();
	}

	private void noVideoSelected() {
		DialogBuilder builder = new DialogBuilder(mActivity);
		builder.setMsg(R.string.info_no_selected_video);
		builder.setButtons(DialogBuilder.DLG_OK);
		builder.show();
	}

}
