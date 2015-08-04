package com.transee.vdb;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.IBinder;
import android.provider.MediaStore;

import com.transee.common.ByteStream;
import com.transee.common.GPSRawData;
import com.transee.viditcam.app.ThisApp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class LocalVdbClient extends VdbClient {

	private WorkerThread mThread;
	private MediaMetadataRetriever mMetaDataRetriever; // used by thread
	private File mCurrentFile; // used by thread

	private final Context mContext;
	private DownloadService mDownloadService;
	private boolean mDownloadServiceBound;

	protected static final int SUB_CMD_Null = 0;
	protected static final int SUB_CMD_GetDownloadInfo = 1;

	private Clip.ID mCurrClipId;
	private LocalRawData mCurrRawData = new LocalRawData();

	// constructor
	public LocalVdbClient(Context context, Callback callback) {
		super(callback);
		mContext = context;
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	// API
	public void start() {
		if (mThread == null) {
			mThread = new WorkerThread();
			mThread.start();
		}
		bindDownloadService();
	}

	// Override
	public void stop() {
		unbindDownloadService();
		if (mThread != null) {
			mThread.interrupt();
			mThread = null;
		}
	}

	private void bindDownloadService() {
		if (!mDownloadServiceBound) {
			Intent intent = new Intent(mContext, DownloadService.class);
			mContext.bindService(intent, mConnection, 0);
			mDownloadServiceBound = true;
		}
	}

	private void unbindDownloadService() {
		if (mDownloadServiceBound) {
			mContext.unbindService(mConnection);
			mDownloadServiceBound = false;
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			DownloadService.LocalBinder binder = (DownloadService.LocalBinder)service;
			mDownloadService = binder.getService();
			mDownloadService.addCallback(mServiceCallback);
			Request request = new Request(CMD_Null, SUB_CMD_GetDownloadInfo);
			request.param = mDownloadService;
			mQueue.addRequest(request);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mDownloadService.removeCallback(mServiceCallback);
			mDownloadService = null;
		}

	};

	private DownloadService.Callback mServiceCallback = new DownloadService.Callback() {
		@Override
		public void onStateChangedAsync(final DownloadService service, final int reason, final int state,
				final DownloadService.Item item, final int progress) {
			switch (reason) {
			case DownloadService.REASON_ITEM_ADDED:
				break;
			case DownloadService.REASON_ITEM_PROGRESS:
				mCallback.onDownloadProgress(item.id, progress);
				break;
			case DownloadService.REASON_DOWNLOAD_ERROR:
				mCallback.onDownloadError(item.id);
				break;
			case DownloadService.REASON_DOWNLOAD_STARTED:
				mCallback.onDownloadStarted(item.id);
				break;
			case DownloadService.REASON_DOWNLOAD_FINISHED:
				mCallback.onDownloadFinished(item.id, item.outputFile);
				break;
			}
		}
	};

	class WorkerThread extends Thread {
		@Override
		public void run() {
			try {
				cmdLoop(this);
			} catch (IOException ex) {

			} catch (InterruptedException ex) {

			}
			closeMMR();
		}
	}

	private MediaMetadataRetriever getMMR(File file) {
		if (mMetaDataRetriever != null && mCurrentFile.equals(file)) {
			return mMetaDataRetriever;
		}
		mCurrentFile = file;
		if (mMetaDataRetriever == null) {
			mMetaDataRetriever = new MediaMetadataRetriever();
		}
		mMetaDataRetriever.setDataSource(file.getPath());
		return mMetaDataRetriever;
	}

	private void closeMMR() {
		if (mMetaDataRetriever != null) {
			mMetaDataRetriever.release();
			mMetaDataRetriever = null;
			mCurrentFile = null;
		}
	}

	// before the file is deleted
	private void checkMMR(File file) {
		if (mMetaDataRetriever != null && mCurrentFile.equals(file)) {
			closeMMR();
		}
	}

	private DownloadingClip createClip(SimpleClipSet clipSet, DownloadService.Item item) {
		if (item == null)
			return null;

		DownloadingClip clip = new DownloadingClip(item.id);
		RemuxerParams params = item.params;

		clip.clipDate = params.getClipDate();
		clip.clipLengthMs = params.getClipLength();
		clip.posterData = params.getPosterData();

		Clip.StreamInfo streamInfo = clip.streams[0];
		streamInfo.version = params.getStreamVersion();
		streamInfo.video_coding = (byte)params.getVideoCoding();
		streamInfo.video_framerate = (byte)params.getVideoFrameRate();
		streamInfo.video_width = (short)params.getVideoWidth();
		streamInfo.video_height = (short)params.getVideoHeight();
		streamInfo.audio_coding = (byte)params.getAudioCoding();
		streamInfo.audio_num_channels = (byte)params.getAudioNumChannels();
		streamInfo.audio_sampling_freq = params.getAudioSamplingFreq();

		clipSet.addClip(clip);
		return clip;
	}

	private void getDownloadInfo(DownloadService service) {
		DownloadService.DownloadInfo downloadInfo = new DownloadService.DownloadInfo();
		service.getDownloadInfo(downloadInfo);
		if (downloadInfo.item != null || downloadInfo.list.size() > 0) {
			SimpleClipSet clipSet = new SimpleClipSet(Clip.CAT_LOCAL, LocalClip.TYPE_DOWNLOADING);
			DownloadingClip clip = createClip(clipSet, downloadInfo.item);
			if (clip != null) {
				clip.progress = downloadInfo.percent;
			}
			for (int i = 0; i < downloadInfo.list.size(); i++) {
				createClip(clipSet, downloadInfo.list.get(i));
			}
			mCallback.onClipSetInfoAsync(clipSet);
		}
	}

	@Override
	protected void cmdNull(int sub_cmd, Object param) throws IOException {
		switch (sub_cmd) {
		case SUB_CMD_GetDownloadInfo:
			getDownloadInfo((DownloadService)param);
			break;
		default:
			break;
		}
	}

	@Override
	protected void cmdGetVersionInfo() throws IOException {
	}

	@Override
	protected void cmdGetClipSetInfo(int type) throws IOException {
		SimpleClipSet clipSet = new SimpleClipSet(Clip.CAT_LOCAL, LocalClip.TYPE_FILE);

		String path = ThisApp.getVideoDownloadPath();
		File[] fileList = (new File(path)).listFiles();
		if (fileList == null) {
			mCallback.onClipSetInfoAsync(clipSet);
			return;
		}

		Arrays.sort(fileList, new FileComparator());

		Thread thread = Thread.currentThread();
		for (int i = 0; i < fileList.length; i++) {
			File file = fileList[i];
			if (thread.isInterrupted()) {
				return;
			}
			FileClip clip = new FileClip(file);
			Mp4Info info = new Mp4Info();
			if (info.readInfo(file.getPath()) == 0) {
				clip.clipDate = info.clip_date;
				clip.clipLengthMs = info.clip_length_ms;
				clip.clipCreateDate = info.clip_created_date;
				Clip.StreamInfo stream = clip.streams[0];
				stream.version = info.stream_version;
				stream.video_coding = (byte)info.video_coding;
				stream.video_framerate = (byte)info.video_frame_rate;
				stream.video_width = (short)info.video_width;
				stream.video_height = (short)info.video_height;
				stream.audio_coding = (byte)info.audio_coding;
				stream.audio_num_channels = (byte)info.audio_num_channels;
				stream.audio_sampling_freq = info.audio_sampling_freq;
				clip.has_gps = info.has_gps;
				clip.has_acc = info.has_acc;
				clip.has_obd = info.has_obd;
				clipSet.addClip(clip);
			}
		}

		mCallback.onClipSetInfoAsync(clipSet);
	}

	private static class FileComparator implements Comparator<File> {
		@Override
		public int compare(File lhs, File rhs) {
			return rhs.toString().compareTo(lhs.toString());
		}
	}

	private Bitmap resizeBitmap(Bitmap bitmap, int width, int height) {
		Matrix matrix = new Matrix();
		matrix.postScale((float)width / bitmap.getWidth(), (float)height / bitmap.getHeight());
		Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
		bitmap.recycle();
		return newBitmap;
	}

	@Override
	protected void cmdGetIndexPicture(GetClipImageRequest param) throws IOException {
		ClipPos clipPos = param.clipPos;
		if (clipPos.cid.type == LocalClip.TYPE_FILE) {
			File file = (File)clipPos.cid.extra;
			if (clipPos.getType() == ClipPos.TYPE_POSTER) {
				byte[] data = Mp4Info.readPoster(file.getPath());
				if (data != null) {
					clipPos.setRealTimeMs(clipPos.getClipTimeMs());
					mCallback.onImageDataAsync(clipPos, data);
				}
			} else {
				MediaMetadataRetriever mmr = getMMR(file);
				Bitmap bitmap = mmr.getFrameAtTime((clipPos.getClipTimeMs() + 500) * 1000,
						MediaMetadataRetriever.OPTION_PREVIOUS_SYNC);
				if (bitmap != null) {
					if (param.width > 0 && param.height > 0) {
						bitmap = resizeBitmap(bitmap, param.width, param.height);
					}
					clipPos.setRealTimeMs(clipPos.getClipTimeMs());
					mCallback.onBitmapDataAsync(clipPos, bitmap);
				}
				if (clipPos.isDiscardable()) {
					mQueue.animationDataReceived();
				}
			}
		} else if (clipPos.cid.type == LocalClip.TYPE_DOWNLOADING) {
			DownloadingClip clip = (DownloadingClip)param.clip;
			byte[] data = clip.posterData;
			if (clipPos.getType() == ClipPos.TYPE_POSTER) {
				if (data != null) {
					clipPos.setRealTimeMs(clipPos.getClipTimeMs());
					mCallback.onImageDataAsync(clipPos, data);
				}
			} else {
				if (data != null) {
					clipPos.setRealTimeMs(clipPos.getClipTimeMs());
					mCallback.onImageDataAsync(clipPos, data);
				}
				if (clipPos.isDiscardable()) {
					mQueue.animationDataReceived();
				}
			}
		}
	}

	@Override
	protected void cmdGetPlaybackUrl(PlaybackUrlRequest param) throws IOException {
		File file = (File)param.cid.extra;
		PlaybackUrl playbackUrl = new PlaybackUrl(param.cid);
		playbackUrl.stream = 0;
		playbackUrl.urlType = param.mUrlType;
		playbackUrl.realTimeMs = param.mClipTimeMs;
		playbackUrl.lengthMs = (int)(param.mClipLengthMs - param.mClipTimeMs);
		if (playbackUrl.lengthMs < 0)
			playbackUrl.lengthMs = 0;
		playbackUrl.bHasMore = false;
		playbackUrl.url = file.getPath();
		playbackUrl.offsetMs = (int)param.mClipTimeMs;

		mCallback.onPlaybackUrlReadyAsync(playbackUrl);
	}

	@Override
	protected void cmdMarkClip(MarkClipRequest param) throws IOException {
	}

	@Override
	protected void cmdDeleteClip(DeleteClipRequest param) throws IOException {
		int result = -1;
		File file = (File)param.cid.extra;
		checkMMR(file);
		if (file.delete()) {
			result = 0;
			mCallback.onClipRemovedAsync(param.cid);
			try {
				mContext.getContentResolver().delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
						MediaStore.Video.Media.DATA + "='" + file.getPath() + "'", null);
			} catch (Exception e) {

			}
		}
		mCallback.onDeleteClipResultAsync(result);
	}

	private void loadRaw(Clip.ID cid) {
		if (mCurrClipId == null || !mCurrClipId.equals(cid)) {
			if (cid.type != LocalClip.TYPE_FILE) {
				if (mCurrClipId != null) {
					mCurrRawData.unload();
					mCurrClipId = null;
				}
				return;
			}
			File file = (File)cid.extra;
			if (mCurrRawData.load(file.getPath())) {
				mCurrClipId = cid;
			} else {
				mCurrClipId = null;
			}
		}
	}

	@Override
	protected void cmdGetRawData(RawDataRequest param) throws IOException {
		loadRaw(param.cid);
		if (mCurrClipId != null) {
			byte[] ack = mCurrRawData.read((int)param.mClipTimeMs, param.mTypes);
			if (ack != null) {
				RawDataResult result = new RawDataResult(param.cid);

				int offset = 0;
				offset += 4; // clip_type
				offset += 4; // clip_id

				result.clipDate = ByteStream.readI32(ack, offset);
				offset += 4;

				while (true) {
					int dataType = ByteStream.readI32(ack, offset);
					offset += 4;
					if (dataType == 0)
						break;

					long clipTimeMs = ByteStream.readI64(ack, offset);
					offset += 8;
					int size = ByteStream.readI32(ack, offset);
					offset += 4;

					if (size > 0) {
						RawDataItem item = new RawDataItem();
						item.dataType = dataType;
						item.clipTimeMs = clipTimeMs;

						byte[] item_data = new byte[size];
						System.arraycopy(ack, offset, item_data, 0, size);
						offset += size;

						if (dataType == RAW_DATA_GPS) {
							item.object = GPSRawData.translate(item_data);
						}

						if (result.items == null) {
							result.items = new ArrayList<RawDataItem>();
						}

						result.items.add(item);
					}
				}

				if (result.items != null) {
					mCallback.onRawDataResultAsync(result);
				}
			}
		}
	}

	@Override
	protected void cmdSetRawDataOption(Integer rawDataTypes) throws IOException {
	}

	@Override
	protected void cmdGetRawDataBlock(RawDataBlockRequest param) throws IOException {
		loadRaw(param.cid);
		if (mCurrClipId != null) {
			byte[] ack = mCurrRawData.readBlock((int)param.mClipTimeMs, param.mLengthMs, param.mDataType);
			if (ack != null) {
				RawDataBlockHeader header = new RawDataBlockHeader(param.cid);

				int offset = 0;
				offset += 4; // clip_type
				offset += 4; // clip_id

				header.mClipDate = ByteStream.readI32(ack, offset);
				offset += 4;

				header.mDataType = ByteStream.readI32(ack, offset);
				offset += 4;

				header.mRequestedTimeMs = ByteStream.readI64(ack, offset);
				offset += 8;

				header.mNumItems = ByteStream.readI32(ack, offset);
				offset += 4;

				header.mDataSize = ByteStream.readI32(ack, offset);
				offset += 4;

				RawDataBlock block = new RawDataBlock(header);

				int numItems = block.header.mNumItems;
				block.timeOffsetMs = new int[numItems];
				block.dataSize = new int[numItems];

				for (int i = 0; i < numItems; i++) {
					block.timeOffsetMs[i] = ByteStream.readI32(ack, offset);
					offset += 4;
					block.dataSize[i] = ByteStream.readI32(ack, offset);
					offset += 4;
				}

				block.data = new byte[block.header.mDataSize];
				System.arraycopy(ack, offset, block.data, 0, block.header.mDataSize);

				mCallback.onRawDataBlockAsync(block);
			}
		}
	}

	@Override
	protected void cmdGetDownloadUrlEx(DownloadUrlExRequest param) throws IOException {
	}

	@Override
	protected void cmdGetPlaylistSet(GetPlaylistSetInfo request) throws IOException {
	}

	@Override
	protected void cmdGetPlaylistIndexPicture(GetPlaylistImageRequest request) throws IOException {
	}

	@Override
	protected void cmdClearPlaylist(ClearPlaylistRequest request) throws IOException {
	}

	@Override
	protected void cmdInsertClip(InsertClipRequest request) throws IOException {
	}

	@Override
	protected void cmdMoveClip(MoveClipRequest request) throws IOException {
	}

	@Override
	protected void cmdGetPlaylistPlaybackUrl(GetPlaylistPlaybackUrlRequest request) throws IOException {
	}

	@Override
	protected void cmdGetAllClipSetInfo() throws IOException {
	}

}
