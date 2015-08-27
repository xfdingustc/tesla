package com.waylens.hachi.vdb;

import android.net.Uri;

import com.waylens.hachi.vdb.LocalClip;

import java.io.File;

public class FileClip extends LocalClip {

	public final File file;
	public int clipCreateDate; // TODO

	public boolean has_gps;
	public boolean has_acc;
	public boolean has_obd;
	
	// subType: 0
	// extra: file
	// numStreams: 1

	public FileClip(File file) {
		super(TYPE_FILE, 0, file, 1);
		this.file = file;
		this.clipSize = file.length();
	}

	@Override
	public boolean contains(long timeMs) {
		return timeMs >= 0 && timeMs < clipLengthMs;
	}

	@Override
	public Uri getUri() {
		return Uri.fromFile(file);
	}

}
