package com.waylens.hachi.ui.adapters;

import android.graphics.Rect;
import android.view.View;

import com.transee.common.VideoListView;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipPos;
import com.transee.vdb.ImageDecoder;

public abstract class VideoListAdapter {

	public abstract void setListView(VideoListView listView, boolean bEnableFastPreview);

	public abstract void onVdbUnmounted();

	public abstract void notifyDataSetChanged();

	public abstract boolean decodeImage(ImageDecoder decoder, ClipPos clipPos, byte[] data);

	// for clip set, cid is valid; for playlist, plistId is valid
	public abstract Rect getThumbnailRect(Clip.ID cid, int plistId, View other);

	public abstract void clear();
}
