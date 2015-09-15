package com.waylens.hachi.comp;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.transee.common.BitmapView;
import com.transee.common.DateTime;
import com.transee.common.HashCache;
import com.transee.common.ViewAnimation;
import com.transee.common.ViewAnimation.Animation;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.transee.vdb.ImageDecoder;
import com.transee.vdb.Playlist;
import com.transee.vdb.PlaylistSet;
import com.transee.vdb.RemoteVdbClient;
import com.transee.vdb.SlideView;
import com.transee.vdb.SlideView.LayoutInfo;
import com.transee.viditcam.app.comp.SlideHelper;
import com.waylens.hachi.R;

import com.transee.viditcam.app.VdbEditor;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.vdb.RawDataBlock;

abstract public class VdbImageVideo {

	abstract public void onCursorPosChanged(SlideView slideView, int pos);

	abstract public void onSeekVideo(int pos_ms);

	abstract public void onBeginScroll(SlideView slideView);

	abstract public void onSelectionChanged(SlideView slideView);

	abstract public void onSaveCurrentImage(Bitmap bitmap);

	abstract public void onClick();

	public static final int SHOW_ANIM = (1 << 0);
	public static final int SHOW_MAP = (1 << 1);

	static final boolean DEBUG = false;
	static final String TAG = "VdbImageVide";

	final int mFrameLengthMs = 30 * 1000;

	private final BaseActivity mActivity;
	private final VdbEditor mEditor;
	private final boolean mbShowAcc;
	private final VdbPlayback mVdbPlayback;

	private BitmapView mImageView;
	private ProgressBar mProgressBar;
	private Button mSaveImageButton;
	private SlideView mSlideView;
	private SlideHelper mSlideHelper;

	private TextView mImageHintText;
	private ViewAnimation mImageHintAnim;

	private HashCache<Object, Bitmap, Object> mSlideBitmapCache;
	private Paint mPlaylistPaint;
	private Drawable mFadeClip;

	private Clip mTempClip; // for getPlaylistTimeMs

	public VdbImageVideo(BaseActivity activity, View videoControl, View posterControl, VdbEditor editor,
			boolean bShowAcc, VdbPlayback vdbPlayback) {

		mActivity = activity;
		mEditor = editor;
		mbShowAcc = bShowAcc;
		mVdbPlayback = vdbPlayback;

		mImageView = (BitmapView)videoControl.findViewById(R.id.bitmapView1);
		mImageView.setCallback(new BitmapView.Callback() {
			@Override
			public void onSingleTapUp() {
				VdbImageVideo.this.onSingleTapUp();
			}

			@Override
			public void onDown() {
				VdbImageVideo.this.onDown();
			}

			@Override
			public void onDoubleClick() {
				VdbImageVideo.this.onDoubleClick();
			}
		});

		mProgressBar = (ProgressBar)videoControl.findViewById(R.id.progressBar2);
		mSaveImageButton = (Button)videoControl.findViewById(R.id.btnSave);
		mSaveImageButton.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		mSaveImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveCurrentImage();
			}
		});

		mImageHintText = (TextView)videoControl.findViewById(R.id.textView4);
		mImageHintText.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

		mSlideView = (SlideView)posterControl.findViewById(R.id.slideView1);
		SlideView.LayoutInfo info = mSlideView.getLayoutInfo();
		mSlideHelper = new SlideHelper(mActivity, info.imageWidth, info.imageHeight);
	}

	private final void showSaveImageButton() {
		mSaveImageButton.setVisibility(View.VISIBLE);
		updateControlPos(mSaveImageButton);
	}

	private final void hideSaveImageButton() {
		if (mSaveImageButton.getVisibility() == View.VISIBLE) {
			mSaveImageButton.setVisibility(View.GONE);
		}
	}

	// API
	// when clicked the image view or the video view
	public void onSingleTapUp() {
		onClick();
	}

	// API
	public final boolean isShowSaveImageButtonVisible() {
		return mSaveImageButton.getVisibility() == View.VISIBLE;
	}

	private final boolean imageReady() {
		return mImageView.getVisibility() == View.VISIBLE && mProgressBar.getVisibility() != View.VISIBLE;
	}

	// API
	public void toggleShowSaveImageButton() {
		if (mSaveImageButton.getVisibility() != View.VISIBLE) {
			if (imageReady() || mVdbPlayback.imageReady()) {
				showSaveImageButton();
			}
		} else {
			hideSaveImageButton();
		}
	}

	// API
	public void onDown() {
	}

	// API
	public void onDoubleClick() {
		hideSaveImageButton();
	}

	// API todo - remove
	public BitmapView getImageView() {
		return mImageView;
	}

	// API todo - remove
	public SlideView getSlideView() {
		return mSlideView;
	}

	// API
	public void editClip(Bitmap bitmap, Clip clip, int offset, int size) {
		int range = calcClipRange(clip);
		int pos = (int)((long)offset * range / size);
		beginEdit(bitmap, calcClipRange(clip), pos, true);
	}

	// API
	public void editPlaylist(Bitmap bitmap, Playlist playlist) {
		beginEdit(bitmap, calcPlaylistRange(playlist), 0, false);
	}

	// API
	public final void onSetupUI() {
		if (mActivity.isPortrait()) {
			mImageView.setThumbnailScale(3);
			mImageView.setBackgroundColor(mActivity.getResources().getColor(R.color.imageVideoBackground));
		} else {
			mImageView.setThumbnailScale(4);
			mImageView.setBackgroundColor(Color.BLACK);
		}
	}

	// API
	public final void onLayoutChanged() {
		updateProgressPos(false);
		if (mSaveImageButton.getVisibility() == View.VISIBLE) {
			updateControlPos(mSaveImageButton);
		}
	}

	private void beginEdit(Bitmap bitmap, int range, int pos, boolean bCanSelect) {
		mSlideBitmapCache = new SlideBitmapCache();
		mSlideView.setCallback(new SlideViewCallback());
		mSlideView.setRange(range, pos, bCanSelect);

		// init the animation with bitmap and request new image
		setBitmap(null, bitmap);
		onCursorPosChanged(mSlideView, pos);
	}

	private void onPlaylistChanged() {
		if (mEditor.isEditingPlaylist()) {
			int range = mEditor.mPlaylist.numClips * mSlideView.getFrameWidth();
			mSlideView.updateRange(range);
		}
	}

	// API
	public void onClipMoved() {
		onPlaylistChanged();
	}

	// API
	public void onClipRemoved(Clip.ID cid) {
		onPlaylistChanged();
	}

	// API
	public void onHideView() {
		if (!mActivity.isRotating()) {
			mSlideBitmapCache.clear();
			setBitmap(null, null);
		}
	}

	private int calcClipRange(Clip clip) {
		int range = (int)((long)clip.clipLengthMs * mSlideView.getFrameWidth() / mFrameLengthMs);
		return range + 2;
	}

	private int calcPlaylistRange(Playlist playlist) {
		int range = playlist.numClips * mSlideView.getFrameWidth();
		return range + 1;
	}

	private long getClipTimeMs(int pos) {
		return mEditor.mClip.getStartTime() + (long)pos * mFrameLengthMs / mSlideView.getFrameWidth();
	}

	private int getPlaylistTimeMs(int pos) {
		int numClips = mEditor.mPlaylist.numClips;
		if (numClips == 0)
			return -1;
		int index = mSlideView.getIndexAt(pos);
		if (index < 0) {
			index = 0;
		} else if (index >= numClips) {
			index = numClips - 1;
		}
		int timeMs = 0;
		for (int i = 0; i < index; i++) {
			Clip clip = mEditor.mPlaylist.getClip(i);
			timeMs += clip.clipLengthMs;
		}
		Clip clip = mEditor.mPlaylist.getClip(index);
		timeMs += mSlideView.getCurrIndexOffset(index) * clip.clipLengthMs / mSlideView.getFrameWidth();
		return timeMs;
	}

	// mTempClip
	private int getClipTimeMsInPlaylist(int pos) {
		mTempClip = null;
		int numClips = mEditor.mPlaylist.numClips;
		if (numClips == 0)
			return -1;
		int index = mSlideView.getIndexAt(pos);
		if (index < 0) {
			index = 0;
		} else if (index >= numClips) {
			index = numClips - 1;
		}
		mTempClip = mEditor.mPlaylist.getClip(index);
		int timeMs = mSlideView.getCurrIndexOffset(index) * mTempClip.clipLengthMs / mSlideView.getFrameWidth();
		return timeMs;
	}

	long getRelativeTimeMs(int pos) {
		if (mEditor.isEditingClip()) {
			return (long)pos * mFrameLengthMs / mSlideView.getFrameWidth();
		}
		if (mEditor.isEditingPlaylist()) {
			return getPlaylistTimeMs(pos);
		}
		return 0;
	}

	long getTotalTimeMs() {
		if (mEditor.isEditingClip()) {
			return mEditor.mClip.clipLengthMs;
		}
		if (mEditor.isEditingPlaylist()) {
			return mEditor.mPlaylist.mTotalLengthMs;
		}
		return 0;
	}

	// API
	public void onStopActivity() {
		mSlideView.cancelScroll();
		stopHintAnim();
	}

	// API
	public int requestPlayback() {
		mSlideView.cancelScroll();
		int pos = mSlideView.getPos();
		return pos;
	}

	// API
	public void preparePlayback() {
	}

	// API
	public void playbackStarted(int pos) {
		mSlideView.adjustSelStart(pos);
		mImageView.setVisibility(View.INVISIBLE);
	}

	// API
	public void beforeStopPlayback() {
		mImageView.setVisibility(View.VISIBLE);
	}

	// API
	public void playbackEnded() {
		// refresh animation
		onCursorPosChanged(mSlideView, mSlideView.getPos());
		if (!mEditor.mbVideoOnly) {
			mImageView.setVisibility(View.VISIBLE);
		}
	}

	// API
	public void onStartAnimation() {
		hideSaveImageButton();
	}

	// API
	public void onStopAnimation() {
		hideSaveImageButton();
		stopHintAnim();
	}

	private void stopHintAnim() {
		if (mImageHintAnim != null) {
			mImageHintAnim.stopAnimation();
			mImageHintAnim = null;
			mImageHintText.setVisibility(View.GONE);
		}
	}

	@SuppressLint("NewApi")
	private void updateControlPos(View view) {
		View parent = (View)view.getParent();
		Rect rect = mImageView.getBitmapRect();

		int left = rect.left >= 0 ? rect.left : 0;
		int top = rect.top >= 0 ? rect.top : 0;
		int right = rect.right <= parent.getWidth() ? rect.right : parent.getWidth();
		int bottom = rect.bottom <= parent.getHeight() ? rect.bottom : parent.getHeight();

		int w = view.getMeasuredWidth();
		int h = view.getMeasuredHeight();

		MarginLayoutParams lp = (MarginLayoutParams)view.getLayoutParams();
		lp.leftMargin = left + (right - left - w) / 2;
		lp.topMargin = top + (bottom - top - h) / 2;
		if (android.os.Build.VERSION.SDK_INT >= 17) {
			lp.setMarginStart(lp.leftMargin);
		}
		lp.width = w;
		lp.height = h;

		view.setLayoutParams(lp);
	}

	// API
	@SuppressLint("NewApi")
	public void updateProgressPos(boolean bShow) {
		if (bShow || mProgressBar.getVisibility() == View.VISIBLE) {
			View parent = (View)mProgressBar.getParent();
			Rect rect = mImageView.getBitmapRect();
			int left = rect.left >= 0 ? rect.left : 0;
			int top = rect.top >= 0 ? rect.top : 0;
			int right = rect.right <= parent.getWidth() ? rect.right : parent.getWidth();
			int bottom = rect.bottom <= parent.getHeight() ? rect.bottom : parent.getHeight();
			Rect prect = new Rect();
			mProgressBar.getBackground().getPadding(prect);
			Drawable d = mActivity.getResources().getDrawable(R.drawable.loading0);
			int w = d.getIntrinsicWidth() + prect.left + prect.right;
			int h = d.getIntrinsicHeight() + prect.top + prect.bottom;
			MarginLayoutParams lp = (MarginLayoutParams)mProgressBar.getLayoutParams();
			lp.leftMargin = left + (right - left - w) / 2;
			lp.topMargin = top + (bottom - top - h) / 2;
			if (android.os.Build.VERSION.SDK_INT >= 17) {
				lp.setMarginStart(lp.leftMargin);
			}
			lp.width = w;
			lp.height = h;
			mProgressBar.setLayoutParams(lp);
		}
		if (bShow) {
			mProgressBar.setVisibility(View.VISIBLE);
			hideSaveImageButton();
		}
	}

	// API
	public void hideProgress() {
		mProgressBar.setVisibility(View.GONE);
	}

	// API
	public boolean requestPlaybackUrl() {
		int pos = requestPlayback();
		if (mEditor.isEditingClip()) {
			mEditor.requestClipPlaybackUrl(getClipTimeMs(pos));
			return true;
		} else if (mEditor.isEditingPlaylist()) {
			int playlistTimeMs = getPlaylistTimeMs(pos);
			if (playlistTimeMs >= 0) {
				mEditor.requestPlaylistPlaybackUrl(playlistTimeMs);
				return true;
			}
		}
		return false;
	}

	// API
	public void requestDownload() {
		if (mEditor.isEditingClip()) {
			int selStart = mSlideView.getSelStart();
			int selEnd = mSlideView.getSelEnd();
			if (selEnd > selStart) {
				long startMs = getClipTimeMs(selStart);
				long endMs = getClipTimeMs(selEnd);
				mEditor.requestClipDownloadUrl(startMs, endMs, true);
			}
		} else if (mEditor.isEditingPlaylist()) {
			mEditor.requestPlaylistDownloadUrl(true);
		}
	}

	// API
	public void updateAnimation(int flags) {
		showAnimation(mSlideView.getPos(), flags);
	}

	// API
	public long getCurrentDate() {
		if (mEditor.isEditingClip()) {
			long clipTimeMs = getClipTimeMs(mSlideView.getPos());
			return ((int)mEditor.mClip.clipDate) * 1000 + clipTimeMs;
		} else if (mEditor.isEditingPlaylist()) {
			long clipTimeMs = getClipTimeMsInPlaylist(mSlideView.getPos());
			Clip clip = mTempClip;
			if (clip != null) {
				mTempClip = null;
				return ((int)clip.clipDate) * 1000 + clipTimeMs;
			}
		}
		return 0;
	}

	// API
	// flags - SHOW_ANIM | SHOW_MAP
	public void showAnimation(int pos, int flags) {
		if (mEditor.isEditingClip()) {
			long clipTimeMs = getClipTimeMs(pos);
			if (mEditor.mbVideoOnly) {
				if (mProgressBar.getVisibility() != View.VISIBLE) {
					onSeekVideo((int)(clipTimeMs - mEditor.mClip.getStartTime()));
				}
			} else {
				if ((flags & SHOW_ANIM) != 0) {
					mEditor.requestClipAnimationImage(clipTimeMs);
				}
			}
			if ((flags & SHOW_MAP) != 0) {
				mEditor.requestClipGPSData(clipTimeMs);
			}
		} else if (mEditor.isEditingPlaylist()) {
			int index = mSlideView.getCurrIndex(mEditor.mPlaylist.numClips);
			if (mEditor.mPlaylist.contains(index)) {
				Clip clip = mEditor.mPlaylist.getClip(index);
				if (clip != null) {
					int offset = mSlideView.getCurrIndexOffset(index);
					long clipTimeMs = offset * clip.clipLengthMs / mSlideView.getFrameWidth();
					mEditor.mPlaylistClipIndex = index;
					if ((flags & SHOW_ANIM) != 0) {
						mEditor.requestPlaylistAnimationImage(clip, clipTimeMs);
					}
					if ((flags & SHOW_MAP) != 0) {
						mEditor.requestPlaylistGPSData(clip, clipTimeMs);
					}
				}
			}
		}
	}

	// API
	// -1: marked clip set
	// 0, 1, 2: playlist
	public boolean markClipTo(int index) {
		long startTimeMs = getClipTimeMs(mSlideView.getSelStart());
		long endTimeMs = getClipTimeMs(mSlideView.getSelEnd());
		if (index < 0) {
			mEditor.requestMarkClip(startTimeMs, endTimeMs);
			return true;
		} else {
			PlaylistSet playlistSet = mEditor.mVdb.getPlaylistSet();
			if (playlistSet != null && index < playlistSet.mPlaylists.size()) {
				Playlist playlist = playlistSet.mPlaylists.get(index);
				mEditor.requestInsertClip(startTimeMs, endTimeMs, playlist.plistId);
				return true;
			}
		}
		return false;
	}

	// API
	public void updatePosInfo(TextView textView, TextView textView2) {
		SlideView slideView = mSlideView;
		long timeMs = getRelativeTimeMs(slideView.getPos());
		long durationMs = getTotalTimeMs();
		if (timeMs > durationMs) {
			timeMs = durationMs;
		}
		String text = DateTime.secondsToString((int) (timeMs / 1000)) + "/"
				+ DateTime.secondsToString((int) (durationMs / 1000));
		textView.setText(text);
		if (slideView.isSelecting()) {
			timeMs = getRelativeTimeMs(slideView.getSelLength());
			text = DateTime.secondsToString((int) (timeMs / 1000));
			textView2.setText(text);
			textView2.setVisibility(View.VISIBLE);
		} else {
			textView2.setVisibility(View.GONE);
		}
	}

	// API
	public void onImageSaved() {
		mImageHintAnim = new ViewAnimation() {
			@Override
			protected void onAnimationStart(ViewAnimation animation, boolean bReverse) {
				mImageHintText.setVisibility(View.VISIBLE);
				updateControlPos(mImageHintText);
			}

			@Override
			protected void onAnimationDone(ViewAnimation animation, boolean bReverse) {
				mImageHintText.setVisibility(View.GONE);
			}
		};
		Animation anim = ViewAnimation.createAlphaAnimation(1.0f, 0.0f);
		anim.setStartLengthMs(2000);
		mImageHintAnim.addAnimation(mImageHintText, anim);
		mImageHintAnim.startAnimation(2500, 10, false);
	}

	private void saveCurrentImage() {
		hideSaveImageButton();
		Handler handler = mImageView.getHandler();
		if (handler != null) {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (mImageView.getVisibility() == View.VISIBLE) {
						onSaveCurrentImage(mImageView.getBitmap());
					} else {
						onSaveCurrentImage(null);
					}
				}
			}, 10);
		}
	}

	private final ImageDecoder.Callback mDecoderCallback = new ImageDecoder.Callback() {
		@Override
		public void onDecodeDoneAsync(final Bitmap bitmap, final Object tag) {
			mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onBitmapDecoded((ClipPos)tag, bitmap);
				}
			});
		}
	};

	private final Object indexToKey(int index) {
		if (mEditor.isEditingClip()) {
			return index < 0 ? null : index;
		} else if (mEditor.isEditingPlaylist()) {
			Clip clip = mEditor.mPlaylist.getClip(index);
			return clip == null ? null : clip.cid;
		} else {
			return null;
		}
	}

	// API
	public void decodeImage(ImageDecoder decoder, ClipPos clipPos, byte[] data) {
		if (clipPos.getType() == ClipPos.TYPE_SLIDE) {
			if (mEditor.isEditingClip()) {
				int index = (int)(clipPos.getClipTimeMs() - mEditor.mClip.getStartTime()) / mFrameLengthMs;
				if (mSlideBitmapCache.getItem(indexToKey(index)) != null) {
					decoder.decode(data, mSlideView.getImageWidth(), mSlideView.getImageHeight(), 0, clipPos,
							mDecoderCallback);
				}
			} else if (mEditor.isEditingPlaylist()) {
				int index = mEditor.mPlaylist.getClipIndex(clipPos.cid);
				if (mSlideBitmapCache.getItem(indexToKey(index)) != null) {
					decoder.decode(data, mSlideView.getImageWidth(), mSlideView.getImageHeight(), 0, clipPos,
							mDecoderCallback);
				}
			}
		} else if (clipPos.getType() == ClipPos.TYPE_ANIMATION) {
			decoder.decode(data, 0, 0, 0, clipPos, mDecoderCallback);
		}
	}

	public void onBitmapDecoded(ClipPos clipPos, Bitmap bitmap) {
		if (clipPos.getType() == ClipPos.TYPE_SLIDE) {
			if (mEditor.isEditingClip()) {
				int index = (int)(clipPos.getClipTimeMs() - mEditor.mClip.getStartTime()) / mFrameLengthMs;
				HashCache.Item<Object, Bitmap, Object> item = mSlideBitmapCache.getItem(indexToKey(index));
				if (item != null) {
					if (mSlideHelper.drawClipImage(item, clipPos.cid, clipPos.getClipTimeMs()
							- mEditor.mClip.getStartTime(), index, bitmap, mFrameLengthMs, mSlideView.getLayoutInfo())) {
						mSlideView.refreshItem(index);
					}
				}
			} else if (mEditor.isEditingPlaylist()) {
				int index = mEditor.mPlaylist.getClipIndex(clipPos.cid);
				if (index >= 0 && mSlideBitmapCache.setValue(indexToKey(index), bitmap)) {
					Clip clip = mEditor.mPlaylist.getClip(index);
					mSlideHelper.drawClipLength(clip, bitmap);
					mSlideView.refreshItem(index);
				}
			}
		} else if (clipPos.getType() == ClipPos.TYPE_ANIMATION) {
			setBitmap(clipPos, bitmap);
		}
	}

	// API
	public void onRawDataBlock(RawDataBlock block) {
		if (block.header.mDataType == RawDataBlock.RAW_DATA_ACC) {
			if (mEditor.isEditingClip(block.header.cid)) {
				int index = (int)(block.header.mRequestedTimeMs - mEditor.mClip.getStartTime()) / mFrameLengthMs;
				HashCache.Item<Object, Bitmap, Object> item = mSlideBitmapCache.getItem(indexToKey(index));
				if (item != null) {
					if (mSlideHelper.drawClipRawData(item, index, block, mFrameLengthMs, mSlideView.getLayoutInfo())) {
						mSlideView.refreshItem(index);
					}
				}
			}
		}
	}

	// API
	public int onPlaybackPosChanged(long startTimeMs, int posMs) {
		long timeMs = startTimeMs + posMs;
		int posPx = 0;

		if (mEditor.isEditingClip()) {
			if (timeMs <= mEditor.mClip.getStartTime() || mEditor.mClip.clipLengthMs <= 0) {
				posPx = 0;
			} else {
				posPx = (int)((timeMs - mEditor.mClip.getStartTime()) * mSlideView.getRange() / mEditor.mClip.clipLengthMs);
			}
			mSlideView.setNewPos(posPx, true);
		} else if (mEditor.isEditingPlaylist()) {
			int lengthMs = 0;
			int index = 0;
			ClipSet clipSet = mEditor.mPlaylist.clipSet;
			int totalClips = clipSet.getCount();
			for (int i = 0; i < totalClips; i++) {
				Clip clip = clipSet.getClip(i);
				int nextLengthMs = lengthMs + clip.clipLengthMs;
				if (timeMs < nextLengthMs) {
					posPx = index * mSlideView.getFrameWidth();
					posPx += (timeMs - lengthMs) * mSlideView.getFrameWidth() / clip.clipLengthMs;
					break;
				}
				lengthMs = nextLengthMs;
				index++;
			}
			if (index >= mEditor.mPlaylist.numClips) {
				posPx = mEditor.mPlaylist.numClips * mSlideView.getFrameWidth();
			}
			mSlideView.setNewPos(posPx, true);
		}

		return posPx;
	}

	// API
	public final void setBitmap(ClipPos clipPos, Bitmap bitmap) {
		if (clipPos == null || mEditor.checkAnimationPos(clipPos)) {
			mEditor.updateAnimationPos(clipPos);
			mImageView.setBitmap(bitmap);
		}
	}

	private Object requestSlideImage(int index, Object key) {
		if (index >= 0) {
			if (mEditor.isEditingClip()) {
				int max = (mEditor.mClip.clipLengthMs + mFrameLengthMs - 1) / mFrameLengthMs;
				if (index < max) {
					int posMs = index * mFrameLengthMs;
					int rawFlags = mbShowAcc ? RawDataBlock.F_RAW_DATA_ACC : 0;
					mEditor.requestClipSlideImage(posMs, rawFlags, mFrameLengthMs, mSlideView.getImageWidth(),
							mSlideView.getImageHeight());
				}
				return mEditor.mClip;
			} else if (mEditor.isEditingPlaylist()) {
				mEditor.requestPlaylistSlideImage(index, mSlideView.getImageWidth(), mSlideView.getImageHeight());
				return mEditor.mPlaylist;
			}
		}
		return null;
	}

	private void onDrawItem(int index, Canvas canvas, Rect rect, Bitmap bitmap) {
		if (mEditor.isEditingClip()) {
			if (bitmap != null && index >= 0) {
				LayoutInfo info = mSlideView.getLayoutInfo();
				int max = mSlideView.getRange();
				if ((index + 1) * info.width > max) {
					if (mFadeClip == null) {
						mFadeClip = mActivity.getResources().getDrawable(R.drawable.fade_clip);
					}
					mFadeClip.setBounds(rect.left + (max - index * info.width), rect.top + info.imageTop, rect.right,
							info.imageTop + info.imageHeight);
					mFadeClip.draw(canvas);
				}
			}
		} else if (mEditor.isEditingPlaylist()) {
			if (mEditor.mPlaylist.contains(index) && index == mSlideView.getCurrIndex(mEditor.mPlaylist.numClips)) {
				if (mPlaylistPaint == null) {
					mPlaylistPaint = new Paint();
					mPlaylistPaint.setStyle(Style.STROKE);
					mPlaylistPaint.setColor(Color.rgb(0x73, 0xbb, 0x0c));
					mPlaylistPaint.setStrokeWidth(5.0F);
				}
				canvas.drawRect(rect, mPlaylistPaint);
			}
		}
	}

	class SlideViewCallback implements SlideView.Callback {

		@Override
		public void prepareDrawItems(SlideView slideView) {
			mSlideBitmapCache.update();
		}

		@Override
		public boolean isSmallBitmap(SlideView slideView) {
			return mEditor.isEditingPlaylist();
		}

		@Override
		public Bitmap getBitmap(SlideView slideView, int index) {
			return mSlideBitmapCache.getValue(indexToKey(index));
		}

		@Override
		public void cursorPosChanged(SlideView slideView, int pos) {
			hideSaveImageButton();
			onCursorPosChanged(slideView, pos);
		}

		@Override
		public void drawItem(SlideView slideView, int index, Canvas canvas, Rect rect, Bitmap bitmap) {
			VdbImageVideo.this.onDrawItem(index, canvas, rect, bitmap);
		}

		@Override
		public void beginScroll(SlideView slideView) {
			onBeginScroll(slideView);
		}

		@Override
		public void selectionChanged(SlideView slideView) {
			onSelectionChanged(slideView);
		}

	}

	class SlideBitmapCache extends HashCache<Object, Bitmap, Object> {

		@Override
		public int getStartIndex() {
			return mSlideView.getFirstIndex();
		}

		@Override
		public int getEndIndex() {
			return mSlideView.getLastIndex();
		}

		@Override
		public Object getItemKey(int index) {
			return indexToKey(index);
		}

		@Override
		public Object requestValue(int index, Object key) {
			return requestSlideImage(index, key);
		}

		@Override
		public void itemReleased(HashCache.Item<Object, Bitmap, Object> item) {
		}

	}

}
