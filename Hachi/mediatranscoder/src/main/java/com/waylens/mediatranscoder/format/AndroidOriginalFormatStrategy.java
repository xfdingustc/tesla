/*
 * Copyright (C) 2014 Yuya Tanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.waylens.mediatranscoder.format;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

class AndroidOriginalFormatStrategy implements MediaFormatStrategy {
    private static final String TAG = "720pFormatStrategy";
    private static final int DEFAULT_BITRATE = 8000 * 1000; // From Nexus 4 Camera in 720p
    private int mMaxWidth = -1;
    private int mMaxHeight = -1;
    private final int mBitRate;

    public AndroidOriginalFormatStrategy() {
        mBitRate = DEFAULT_BITRATE;
    }

    public AndroidOriginalFormatStrategy(int bitRate) {
        mBitRate = bitRate;
    }

    public AndroidOriginalFormatStrategy(int maxWidth, int maxHeight, int bitRate) {
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
        mBitRate = bitRate;
    }

    @Override
    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
        int width = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        int longer, shorter, outWidth, outHeight;
        if (width >= height) {
            longer = width;
            shorter = height;
            outWidth = longer;
            outHeight = shorter;
        } else {
            shorter = width;
            longer = height;
            outWidth = shorter;
            outHeight = longer;
        }
        if (longer * 9 != shorter * 16) {
            throw new OutputFormatUnavailableException("This video is not 16:9, and is not able to transcode. (" + width + "x" + height + ")");
        }

        if (mMaxHeight > 0) {
            outWidth = Math.min(outWidth, mMaxWidth);
            outHeight = Math.min(outHeight, mMaxHeight);
        }
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", outWidth, outHeight);
        // From Nexus 4 Camera in 720p
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        return format;
    }

    @Override
    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        return null;
    }
}
