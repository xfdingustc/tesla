package com.transee.viditcam.app;

import android.content.Context;

import com.waylens.hachi.hardware.vdtcamera.CameraState;
import com.waylens.hachi.vdb.Clip.StreamInfo;
import com.transee.vdb.StdMedia;
import com.waylens.hachi.R;

public class MediaHelper {

	static public String formatVideoInfo(Context context, StreamInfo si) {
		int index = -1;
		if (si.video_width == 1920 && si.video_height == 1080) {
			switch (si.video_framerate) {
			case StdMedia.FrameRate_59_94:
			case StdMedia.FrameRate_60:
				index = CameraState.VIDEO_RESOLUTION_1080P60;
				break;
			case StdMedia.FrameRate_29_97:
			case StdMedia.FrameRate_30:
				index = CameraState.VIDEO_RESOLUTION_1080P30;
				break;
			default:
				break;
			}
		} else if (si.video_width == 1280 && si.video_height == 720) {
			switch (si.video_framerate) {
			case StdMedia.FrameRate_59_94:
			case StdMedia.FrameRate_60:
				index = CameraState.VIDEO_RESOLUTION_720P60;
				break;
			case StdMedia.FrameRate_29_97:
			case StdMedia.FrameRate_30:
				index = CameraState.VIDEO_RESOLUTION_720P30;
				break;
			case StdMedia.FrameRate_120:
				index = CameraState.VIDEO_RESOLUTION_720P120;
				break;
			default:
				break;
			}
		} else if (si.video_width == 720 && si.video_height == 480) {
			switch (si.video_framerate) {
			case StdMedia.FrameRate_59_94:
			case StdMedia.FrameRate_60:
				index = CameraState.VIDEO_RESOLUTION_480P60;
				break;
			case StdMedia.FrameRate_29_97:
			case StdMedia.FrameRate_30:
				index = CameraState.VIDEO_RESOLUTION_480P30;
				break;
			default:
				break;
			}
		} else if (si.video_width == 3840 && si.video_height == 2160) {
			switch (si.video_framerate) {
			case StdMedia.FrameRate_59_94:
			case StdMedia.FrameRate_60:
				index = CameraState.VIDEO_RESOLUTION_4KP60;
				break;
			case StdMedia.FrameRate_29_97:
			case StdMedia.FrameRate_30:
				index = CameraState.VIDEO_RESOLUTION_4KP30;
				break;
			default:
				break;
			}
		}
		if (index >= 0) {
			index++;
			String[] resNames = context.getResources().getStringArray(R.array.resolution);
			if (index < resNames.length) {
				return "  " + resNames[index];
			}
		}
		return "";
	}

}
