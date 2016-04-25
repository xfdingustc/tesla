package com.waylens.hachi.ui.services.download;

import android.os.Environment;
import android.os.StatFs;

import com.waylens.hachi.utils.DateTime;
import com.waylens.hachi.R;
import com.waylens.hachi.app.DownloadManager;


import java.io.File;

public class RemuxHelper {

	static final String KEY_INPUT_FILE = "inputFile";
	static final String KEY_INPUT_MIME = "inputMime";
	static final String KEY_OUTPUT_FILE = "outputFile";
	static final String KEY_OUTPUT_FORMAT = "outputFormat";
	static final String KEY_POSTER_DATA = "posterData";
	static final String KEY_DURATION_MS = "durationMs";
	static final String KEY_DISABLE_AUDIO = "disableAudio";
	static final String KEY_AUDIO_FILENAME = "audioFileName";
	static final String KEY_AUDIO_FORMAT = "audioFormat";

	// API
	@SuppressWarnings("deprecation")
	public static final int checkSpace(long needed) {

		if (needed >= (1024 + 512) * 1024 * 1024) // 1.5 GB
			return R.string.msg_video_too_long;

		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			return R.string.msg_no_sdcard;

		File path = Environment.getExternalStorageDirectory();
		StatFs statfs = new StatFs(path.getPath());
		long blockSize = statfs.getBlockSize();
		long availBlocks = statfs.getAvailableBlocks();
		if (needed >= blockSize * availBlocks)
			return R.string.msg_no_space;

		return 0;
	}

	private static final String composeFileName(String dir, String fn, int i) {
		if (i == 0)
			return dir + fn + ".mp4";
		else
			return dir + fn + "-" + Integer.toString(i) + ".mp4";
	}

	// API
	public static String genDownloadFileName(int clipDate, long clipTimeMs) {
		try {
			String dir = DownloadManager.getManager().getVideoDownloadPath();
			if (dir == null) {
				return null;
			}
			String fn = DateTime.toFileName(clipDate, clipTimeMs);
			for (int i = 0;; i++) {
				String targetFile = composeFileName(dir, fn, i);
				File file = new File(targetFile);
				if (!file.exists())
					return targetFile;
			}
		} catch (Exception ex) {
			return null;
		}
	}


}
