package com.waylens.hachi.bgjob.export.download;

import android.os.Bundle;
import android.support.annotation.Nullable;

public class RemuxerParams {

	static final String KEY_CLIP_DATE = "clipDate"; // first clip's date
	static final String KEY_CLIP_TIME_MS = "clipTimeMs";
	static final String KEY_CLIP_LENGTH = "clipLength";

	static final String KEY_STREAM_VERSION = "stream_version";

	static final String KEY_VIDEO_CODING = "video_coding";
	static final String KEY_VIDEO_FRAMERATE = "video_framerate";
	static final String KEY_VIDEO_WIDTH = "video_width";
	static final String KEY_VIDEO_HEIGHT = "video_height";

	static final String KEY_AUDIO_CODING = "audio_coding";
	static final String KEY_AUDIO_NUM_CHANNELS = "audio_num_channels";
	static final String KEY_AUDIO_SAMPLING_FREQ = "audio_sampling_req";

	static final String KEY_INPUT_FILE = "inputFile";
	static final String KEY_INPUT_MIME = "inputMime";
	static final String KEY_OUTPUT_FORMAT = "outputFormat";
	static final String KEY_POSTER_DATA = "posterData";
	static final String KEY_GPS_DATA = "gpsData";
	static final String KEY_ACC_DATA = "accData";
	static final String KEY_OBD_DATA = "obdData";
	static final String KEY_DURATION_MS = "durationMs";
	static final String KEY_DISABLE_AUDIO = "disableAudio";
	static final String KEY_AUDIO_FILENAME = "audioFileName";
	static final String KEY_AUDIO_FORMAT = "audioFormat";

	final Bundle mBundle;

	public RemuxerParams() {
		mBundle = new Bundle();
	}

	public RemuxerParams(Bundle bundle) {
		mBundle = bundle;
	}

	public Bundle getBundle() {
		return mBundle;
	}

	// clip date
	public final RemuxerParams setClipDate(int clipDate) {
		mBundle.putInt(KEY_CLIP_DATE, clipDate);
		return this;
	}

	public final int getClipDate() {
		return mBundle.getInt(KEY_CLIP_DATE);
	}

	// clip time ms
	public final RemuxerParams setClipTimeMs(long clipTimeMs) {
		mBundle.putLong(KEY_CLIP_TIME_MS, clipTimeMs);
		return this;
	}

	public final long getClipTimeMs() {
		return mBundle.getLong(KEY_CLIP_TIME_MS);
	}

	// clip length
	public final RemuxerParams setClipLength(int clipLength) {
		mBundle.putInt(KEY_CLIP_LENGTH, clipLength);
		return this;
	}

	public final int getClipLength() {
		return mBundle.getInt(KEY_CLIP_LENGTH);
	}

	// ------------------------------------------------------------------------

	// stream version
	public final RemuxerParams setStreamVersion(int version) {
		mBundle.putInt(KEY_STREAM_VERSION, version);
		return this;
	}

	public final int getStreamVersion() {
		return mBundle.getInt(KEY_STREAM_VERSION);
	}

	// video coding
	public final RemuxerParams setVideoCoding(int video_coding) {
		mBundle.putInt(KEY_VIDEO_CODING, video_coding);
		return this;
	}

	public final int getVideoCoding() {
		return mBundle.getInt(KEY_VIDEO_CODING);
	}

	// video framerate
	public final RemuxerParams setVideoFrameRate(int videoFrameRate) {
		mBundle.putInt(KEY_VIDEO_FRAMERATE, videoFrameRate);
		return this;
	}

	public final int getVideoFrameRate() {
		return mBundle.getInt(KEY_VIDEO_FRAMERATE);
	}

	// video width
	public final RemuxerParams setVideoWidth(int videoWidth) {
		mBundle.putInt(KEY_VIDEO_WIDTH, videoWidth);
		return this;
	}

	public final int getVideoWidth() {
		return mBundle.getInt(KEY_VIDEO_WIDTH);
	}

	// video height
	public final RemuxerParams setVideoHeight(int videoHeight) {
		mBundle.putInt(KEY_VIDEO_HEIGHT, videoHeight);
		return this;
	}

	public final int getVideoHeight() {
		return mBundle.getInt(KEY_VIDEO_HEIGHT);
	}

	// ------------------------------------------------------------------------

	// audio coding
	public final RemuxerParams setAudioCoding(int audioCoding) {
		mBundle.putInt(KEY_AUDIO_CODING, audioCoding);
		return this;
	}

	public final int getAudioCoding() {
		return mBundle.getInt(KEY_AUDIO_CODING);
	}

	// audio num channels
	public final RemuxerParams setAudioNumChannels(int audioNumChannels) {
		mBundle.putInt(KEY_AUDIO_NUM_CHANNELS, audioNumChannels);
		return this;
	}

	public final int getAudioNumChannels() {
		return mBundle.getInt(KEY_AUDIO_NUM_CHANNELS);
	}

	// audio sampling freq
	public final RemuxerParams setAudioSamplingFreq(int audioSamplingFreq) {
		mBundle.putInt(KEY_AUDIO_SAMPLING_FREQ, audioSamplingFreq);
		return this;
	}

	public final int getAudioSamplingFreq() {
		return mBundle.getInt(KEY_AUDIO_SAMPLING_FREQ);
	}

	// ------------------------------------------------------------------------

	// input file
	public final RemuxerParams setInputFile(String inputFile) {
		mBundle.putString(KEY_INPUT_FILE, inputFile);
		return this;
	}

	public final String getInputFile() {
		return mBundle.getString(KEY_INPUT_FILE);
	}

	// input mime
	public final RemuxerParams setInputMime(String inputMime) {
		mBundle.putString(KEY_INPUT_MIME, inputMime);
		return this;
	}

	public final String getInputMime() {
		return mBundle.getString(KEY_INPUT_MIME);
	}

	// output Format
	public final RemuxerParams setOutputFormat(String outputFormat) {
		mBundle.putString(KEY_OUTPUT_FORMAT, outputFormat);
		return this;
	}

	public final String getOutputFormat() {
		return mBundle.getString(KEY_OUTPUT_FORMAT);
	}

	// poster data
	public final RemuxerParams setPosterData(byte[] posterData) {
		mBundle.putByteArray(KEY_POSTER_DATA, posterData);
		return this;
	}

	// gps data
	public final RemuxerParams setGpsData(byte[] ackData) {
		mBundle.putByteArray(KEY_GPS_DATA, ackData);
		return this;
	}

	// acc data
	public final RemuxerParams setAccData(byte[] ackData) {
		mBundle.putByteArray(KEY_ACC_DATA, ackData);
		return this;
	}

	// obd data
	public final RemuxerParams setObdData(byte[] ackData) {
		mBundle.putByteArray(KEY_OBD_DATA, ackData);
		return this;
	}

	@Nullable
	public final byte[] getPosterData() {
		return mBundle.getByteArray(KEY_POSTER_DATA);
	}

	@Nullable
	public final byte[] getGspData() {
		return mBundle.getByteArray(KEY_GPS_DATA);
	}

	@Nullable
	public final byte[] getAccData() {
		return mBundle.getByteArray(KEY_ACC_DATA);
	}

	@Nullable
	public final byte[] getObdData() {
		return mBundle.getByteArray(KEY_OBD_DATA);
	}

	// duration ms
	public final RemuxerParams setDurationMs(int duration_ms) {
		mBundle.putInt(KEY_DURATION_MS, duration_ms);
		return this;
	}

	public final int getDurationMs() {
		return mBundle.getInt(KEY_DURATION_MS);
	}

	// disable audio
	public final RemuxerParams setDisableAudio(boolean bDisableAudio) {
		mBundle.putBoolean(KEY_DISABLE_AUDIO, bDisableAudio);
		return this;
	}

	public final boolean getDisableAudio() {
		return mBundle.getBoolean(KEY_DISABLE_AUDIO);
	}

	// audio file name
	public final RemuxerParams setAudioFileName(String audioFileName) {
		mBundle.putString(KEY_AUDIO_FILENAME, audioFileName);
		return this;
	}

	public final String getAudioFileName() {
		return mBundle.getString(KEY_AUDIO_FILENAME);
	}

	// audio format
	public final RemuxerParams setAudioFormat(String audioFormat) {
		mBundle.putString(KEY_AUDIO_FORMAT, audioFormat);
		return this;
	}

	public final String getAudioFormat() {
		return mBundle.getString(KEY_AUDIO_FORMAT);
	}

}
