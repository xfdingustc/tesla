package com.transee.vdb;

public class Mp4Info {

	static {
		LibLoader.load();
	}

	// set to native before native_write_info()
	public int clip_date;
	public int clip_length_ms;
	public int clip_created_date;
	public int stream_version;
	public int video_coding;
	public int video_frame_rate;
	public int video_width;
	public int video_height;
	public int audio_coding;
	public int audio_num_channels;
	public int audio_sampling_freq;

	// valid only after readInfo()
	public boolean has_gps;
	public boolean has_acc;
	public boolean has_obd;

	public int writeInfo(String filename, byte[] jpg_data, byte[] gps_ack_data, byte[] acc_ack_data, byte[] obd_ack_data) {
		return native_write_info(filename, jpg_data, gps_ack_data, acc_ack_data, obd_ack_data);
	}

	public int readInfo(String filename) {
		return native_read_info(filename);
	}

	public static byte[] readPoster(String filename) {
		return native_read_poster(filename);
	}

	private native void native_init();

	private native int native_write_info(String filename, byte[] jpg_data, byte[] gps_ack_data, byte[] acc_ack_data,
			byte[] obd_ack_data);

	private native int native_read_info(String filename);

	private static native byte[] native_read_poster(String filename);
}
