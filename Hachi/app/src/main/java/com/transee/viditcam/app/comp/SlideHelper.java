package com.transee.viditcam.app.comp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;

import com.transee.common.DateTime;
import com.transee.common.HashCache;
import com.transee.common.Utils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RemoteClip;
import com.transee.vdb.SlideView;


public class SlideHelper {

	private static final int BUFFERED_CLIP_TIME_TEXT_COLOR = Color.rgb(232, 232, 232);
	private static final int MARKED_CLIP_TIME_TEXT_COLOR = Color.rgb(185, 245, 95);
	private static final int CLIP_LENGTH_TEXT_COLOR = Color.rgb(153, 217, 234);
	private static final int ACC_LINE_COLOR = Color.rgb(65, 105, 225);
	private static final int TEXT_BACK_COLOR = Color.BLACK;
	private static final int TEXT_BACK_ALPHA = 0x80;
	private static final int BACKGROUND_X_MARGIN = 8;
	private static final int BACKGROUND_Y_MARGIN = 6;

	static final int TEXT_SIZE = 14; // dp

	static final int ALIGN_LEFT = 0;
	static final int ALIGN_CENTER_X = 1;
	static final int ALIGN_RIGHT = 2;

	static final int ALIGN_TOP = 0;
	static final int ALIGN_CENTER_Y = 1;
	static final int ALIGN_BOTTOM = 2;

	private final Context mContext;
	private boolean mbInit;
	private Canvas mBitmapCanvas;
	private Paint mTextPaint;
	private final int mSlideWidth;
	// private final int mSlideHeight;
	private int mBmpWidth;
	private int mBmpHeight;
	private Paint mTextBackPaint;
	private Rect mDrawRect;

	private Paint mLinePaint;

	public SlideHelper(Context context, int slideWidth, int slideHeight) {
		mContext = context;
		mSlideWidth = slideWidth;
		// mSlideHeight = slideHeight;
	}

	// API
	public void drawClipTime(Clip.ID cid, long clipTimeMs, Bitmap bitmap) {
		init();

		mBitmapCanvas.setBitmap(bitmap);

		if (cid.cat == Clip.CAT_REMOTE && cid.type == RemoteClip.TYPE_BUFFERED) {
			mTextPaint.setColor(BUFFERED_CLIP_TIME_TEXT_COLOR);
		} else {
			mTextPaint.setColor(MARKED_CLIP_TIME_TEXT_COLOR);
		}

		// TODO
		String text = DateTime.secondsToString((int) (clipTimeMs / 1000));
		drawText(mBitmapCanvas, bitmap, ALIGN_LEFT, ALIGN_TOP, text);

		mBitmapCanvas.setBitmap(null);
	}

	// API
	public void drawClipLength(Clip clip, Bitmap bitmap) {

		init();

		mBitmapCanvas.setBitmap(bitmap);

		mTextPaint.setColor(CLIP_LENGTH_TEXT_COLOR);

		String text = DateTime.secondsToString(clip.clipLengthMs / 1000);
		drawText(mBitmapCanvas, bitmap, ALIGN_CENTER_X, ALIGN_TOP, text);

		mBitmapCanvas.setBitmap(null);
	}

	// API
	public boolean drawClipImage(HashCache.Item<Object, Bitmap, Object> item, Clip.ID cid, long clipTimeMs, int index,
			Bitmap bitmap, int frameLengthMs, SlideView.LayoutInfo layoutInfo) {

		init();

		// time info
		drawClipTime(cid, clipTimeMs, bitmap);

		// big bitmap
		Bitmap bmp = Bitmap.createBitmap(layoutInfo.width, layoutInfo.height, Config.ARGB_8888);
		mBitmapCanvas.setBitmap(bmp);

		mBitmapCanvas.drawColor(Color.TRANSPARENT); // TODO - optimize

		mDrawRect.left = layoutInfo.imageLeft;
		mDrawRect.top = layoutInfo.imageTop;
		mDrawRect.right = layoutInfo.imageLeft + layoutInfo.imageWidth;
		mDrawRect.bottom = layoutInfo.imageTop + layoutInfo.imageHeight;
		mBitmapCanvas.drawBitmap(bitmap, null, mDrawRect, null); // TODO - anti-alias

		if (item.tag != null) {
			drawClipRawData(index, (RawDataBlock)item.tag, frameLengthMs, layoutInfo);
			item.tag = null;
		}

		mBitmapCanvas.setBitmap(null);

		item.value = bmp;
		return true;
	}

	// API
	public boolean drawClipRawData(HashCache.Item<Object, Bitmap, Object> item, int index, RawDataBlock block,
			int frameLengthMs, SlideView.LayoutInfo layoutInfo) {

		if (item.value == null) {
			// bitmap not decoded
			item.tag = block;
			return false;
		}
		item.tag = block;

		init();

		mBitmapCanvas.setBitmap(item.value);
		drawClipRawData(index, block, frameLengthMs, layoutInfo);
		mBitmapCanvas.setBitmap(null);

		return true;
	}

	private void drawClipRawData(int index, RawDataBlock block, int frameLengthMs, SlideView.LayoutInfo layoutInfo) {
		float[] coords = new float[block.header.mNumItems * 4];
		int address = 0;

		int n = block.header.mNumItems;
		for (int i = 0; i < n; i++) {
			if (block.dataSize[i] == 12) {
				float x = (float)block.timeOffsetMs[i] * layoutInfo.width;
				x /= frameLengthMs;

				int ax = Utils.readi32(block.data, address);
				int ay = Utils.readi32(block.data, address + 4);
				int az = Utils.readi32(block.data, address + 8);
				address += block.dataSize[i];

				double value = Math.sqrt(ax * ax + ay * ay + az * az);
				float y = (float)(layoutInfo.curveBase - value * layoutInfo.curveHeight / 250);

				int j = i * 4;
				coords[j] = x;
				coords[j + 1] = layoutInfo.curveBase;
				coords[j + 2] = x;
				coords[j + 3] = y;
			}
		}

		if (mLinePaint == null) {
			mLinePaint = new Paint();
			mLinePaint.setColor(ACC_LINE_COLOR);
		}

		mBitmapCanvas.drawLines(coords, mLinePaint);
	}

	private final void init() {
		if (!mbInit) {
			mbInit = true;

			mBitmapCanvas = new Canvas();
			mDrawRect = new Rect();

			mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mTextPaint.setStyle(Style.FILL);

			mTextBackPaint = new Paint();
			mTextBackPaint.setColor(TEXT_BACK_COLOR);
			mTextBackPaint.setAlpha(TEXT_BACK_ALPHA);
		}
	}

	private final void drawText(Canvas canvas, Bitmap bitmap, int x_align, int y_align, String text) {

		int bmpWidth = bitmap.getWidth();
		int bmpHeight = bitmap.getHeight();

		if (bmpWidth != mBmpWidth || bmpHeight != mBmpHeight) {
			mBmpWidth = bmpWidth;
			mBmpHeight = bmpHeight;
			float textSize = Utils.sp2px(mContext, TEXT_SIZE);
			textSize = textSize * bmpWidth / mSlideWidth;
			mTextPaint.setTextSize(textSize);
		}

		Rect rect = mDrawRect;
		mTextPaint.getTextBounds(text, 0, text.length(), rect);

		int baseline = rect.height() + BACKGROUND_Y_MARGIN;
		rect.right += rect.left + (BACKGROUND_X_MARGIN * 2);
		rect.left = 0;
		rect.bottom = rect.bottom * 3 - rect.top + (BACKGROUND_Y_MARGIN * 2);
		rect.top = 0;

		int temp;
		switch (x_align) {
		default:
		case ALIGN_LEFT:
			break;
		case ALIGN_CENTER_X:
			temp = (bmpWidth - rect.width()) / 2;
			rect.left += temp;
			rect.right += temp;
			break;
		case ALIGN_RIGHT:
			temp = bmpWidth - rect.width();
			rect.left += temp;
			rect.right += temp;
			break;
		}

		switch (y_align) {
		default:
		case ALIGN_TOP:
			break;
		case ALIGN_CENTER_Y:
			temp = (bmpHeight - rect.height()) / 2;
			rect.top += temp;
			rect.bottom += temp;
			break;
		case ALIGN_BOTTOM:
			temp = bmpHeight - rect.height();
			rect.top += temp;
			rect.bottom += temp;
			break;
		}

		mBitmapCanvas.drawRect(rect, mTextBackPaint);
		mBitmapCanvas.drawText(text, rect.left + BACKGROUND_X_MARGIN, rect.top + baseline, mTextPaint);
	}

}
