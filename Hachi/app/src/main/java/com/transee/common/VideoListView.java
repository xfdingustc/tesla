package com.transee.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

public class VideoListView extends View {

	static final String TAG = "MyListView";

	static final int SEL_DELAY = 60;
	static final int TEXT_LINES = 3;

	public interface Callback {
		void prepareItems();

		int getTotalItems();

		void getItem(int index, Item item);

		void onClickItem(int index, int offset, int size);

		void onRulerPosChanged(int index, int offset, int size);
	}

	public static class Item {
		public Bitmap mPoster;
		public boolean mbSetBackground;
		public int mBackgroundColor;
		public Drawable mEmptyPoster;

		private String[] mTextLeft = new String[TEXT_LINES];
		private String[] mTextRight = new String[TEXT_LINES];
		private StringBuilder mSB = new StringBuilder();

		public void clear() {
			mPoster = null;
			mbSetBackground = false;
			mEmptyPoster = null;
			for (int i = 0; i < TEXT_LINES; i++) {
				mTextLeft[i] = null;
				mTextRight[i] = null;
			}
			mSB.setLength(0);
		}

		// API
		public void setTextLeft(int line, String text) {
			mTextLeft[line] = text;
		}

		// API
		public void setTextRight(int line, String text) {
			mTextRight[line] = text;
		}

		public void collectText() {
			mSB.setLength(0);

			for (int i = 0; i < TEXT_LINES; i++) {
				boolean bLine = false;
				if (mTextLeft[i] != null) {
					mSB.append(mTextLeft[i]);
					if (mTextRight[i] != null) {
						mSB.append(' ');
					}
					bLine = true;
				}
				if (mTextRight[i] != null) {
					mSB.append(mTextRight[i]);
					bLine = true;
				}
				if (bLine && i + 1 < TEXT_LINES) {
					mSB.append('\n');
				}
			}
		}
	}

	static final int POSTER_WIDTH = 128; // dp
	static final int POSTER_HEIGHT = 72; // dp

	static final int VIEW_TOP_MARGIN = 1;
	static final int ITEM_TOP_MARGIN = 1;
	static final int ITEM_BOTTOM_MARGIN = 1;
	static final int ITEM_LEFT_MARGIN = 2;
	static final int ITEM_RIGHT_MARGIN = 2;
	static final int POSTER_MARGIN = 2;
	static final int PREVIEW_TOP_MARGIN = 2;
	static final int PREVIEW_RIGHT_MARGIN = 2;
	static final int PREVIEW_BOTTOM_MARGIN = 2;

	static final int RULER_COLOR = 0xc000ff00;

	private int mPosterWidth;
	private int mPosterHeight;
	private int mItemHeight; // including margins
	private int mItemWidth; // including margins
	private int mViewTopMargin;

	private boolean mbEnableFastPreview;
	private int mPos; // y offset
	private int mRulerPos;
	private boolean mbPressRuler;
	private boolean mbMoved;
	private Item mDrawItem = new Item();
	private Rect mItemRect = new Rect();
	private Rect mDrawRect = new Rect();
	private Callback mCallback;

	private GestureDetector mGesture;
	private Scroller mScroller;
	private Timer mScrollTimer;

	private Paint mLinePaint;
	private TextPaint mTextPaint;

	private int mDownIndex = -1;
	private int mSetIndex = -1;
	private int mSelColor;

	private Bitmap mPreviewBitmap;
	private Rect mPreviewRect = new Rect(); // current preview rect

	private boolean mbLoading;
	private String mLoadingOrEmptyString;

	public VideoListView(Context context) {
		super(context);
		initView();
	}

	public VideoListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public VideoListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		setWillNotDraw(false);

		float density = Utils.getDensity(getContext());
		mPosterWidth = Utils.dp2px(density, POSTER_WIDTH);
		mPosterHeight = Utils.dp2px(density, POSTER_HEIGHT);
		mItemHeight = Utils.dp2px(density, ITEM_TOP_MARGIN + POSTER_HEIGHT + ITEM_BOTTOM_MARGIN);
		mItemWidth = Utils.dp2px(density, ITEM_LEFT_MARGIN + POSTER_WIDTH + POSTER_MARGIN);
		mViewTopMargin = Utils.dp2px(density, VIEW_TOP_MARGIN);

		mScroller = new Scroller(getContext());
		mGesture = new GestureDetector(getContext(), new MyGestureListener());

		mLinePaint = new Paint();
		mLinePaint.setColor(RULER_COLOR);
		mLinePaint.setStrokeWidth(3);

		mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setTextSize(Utils.dp2px(getContext(), 14));
	}

	// pos is relative
	private final int getItemIndex(int pos) {
		return (pos + mPos - mViewTopMargin) / mItemHeight;
	}

	private final int getItemTop(int index) {
		return index * mItemHeight - mPos + mViewTopMargin;
	}

	// pos is relative
	private final int getItemOffset(int pos, int index) {
		return (pos + mPos - mViewTopMargin) - index * mItemHeight;
	}

	private final int _getTotalHeight() {
		return mCallback.getTotalItems() * mItemHeight + mViewTopMargin;
	}

	// API
	public void setCallback(Callback callback, boolean bEnableFastPreview) {
		mCallback = callback;
		mbEnableFastPreview = bEnableFastPreview;
	}

	// API
	public void setSelColor(int color) {
		mSelColor = color;
	}

	// API
	public void startLoading(String str) {
		mbLoading = true;
		mLoadingOrEmptyString = str;
		invalidate();
	}

	// API
	public void finishLoading(String str) {
		mbLoading = false;
		mLoadingOrEmptyString = str;
		invalidate();
	}

	private int calcItemIndex(int pos) {
		if (mCallback == null)
			return -1;
		int index = getItemIndex(pos);
		if (index < 0)
			return -1;
		int total = mCallback.getTotalItems();
		return index < total ? index : total - 1;
	}

	// API
	public int getFirstVisiblePosition() {
		return calcItemIndex(0);
	}

	// API
	public int getLastVisiblePosition() {
		return calcItemIndex(getHeight() - 1);
	}

	// API
	public void update() {
		invalidate();
	}

	// API
	public void update(int index) {
		int top = getItemTop(index);
		invalidate(0, top, getWidth(), top + mItemHeight);
	}

	// API
	public final int getPosterWidth() {
		return mPosterWidth;
	}

	// API
	public final int getPosterHeight() {
		return mPosterHeight;
	}

	// API
	public final void getPosterRect(int index, Rect rect) {
		float density = Utils.getDensity(getContext());
		rect.left += Utils.dp2px(density, ITEM_LEFT_MARGIN);
		rect.top += getItemTop(index) + Utils.dp2px(density, ITEM_TOP_MARGIN);
		rect.right = rect.left + mPosterWidth;
		rect.bottom = rect.top + mPosterHeight;
	}

	// API
	public final void setPreviewBitmap(Bitmap bitmap) {
		clearPreview();
		mPreviewBitmap = bitmap;
		getPreviewRect(mDrawRect);
		invalidate(mDrawRect);
	}

	// API
	public final void clearPreview() {
		if (mPreviewBitmap != null) {
			mPreviewBitmap = null;
			if (!mPreviewRect.isEmpty()) {
				invalidate(mPreviewRect);
				mPreviewRect.set(0, 0, 0, 0);
			}
		}
	}

	private void getPreviewRect(Rect rect) {
		float density = Utils.getDensity(getContext());
		int topMargin = (int)Utils.dp2px(density, PREVIEW_TOP_MARGIN);
		int rightMargin = (int)Utils.dp2px(density, PREVIEW_RIGHT_MARGIN);
		int bottomMargin = (int)Utils.dp2px(density, PREVIEW_BOTTOM_MARGIN);

		int width = getWidth() - mItemWidth - rightMargin;
		int height = getHeight() - topMargin - bottomMargin;

		int bmpWidth = mPreviewBitmap.getWidth();
		int bmpHeight = mPreviewBitmap.getHeight();

		if (width * bmpHeight > height * bmpWidth) {
			width = height * bmpWidth / bmpHeight;
		} else {
			height = width * bmpHeight / bmpWidth;
		}

		if (getWidth() > getHeight()) {
			rect.top = topMargin;
		} else {
			rect.top = mRulerPos - height / 2;
			if (rect.top < topMargin)
				rect.top = topMargin;
		}

		rect.left = mItemWidth;
		rect.right = rect.left + width;
		rect.bottom = rect.top + height;
	}

	private final Runnable mPrepareAction = new Runnable() {
		@Override
		public void run() {
			if (mCallback != null) {
				mCallback.prepareItems();
			}
		}
	};

	private final Runnable mSetSelAction = new Runnable() {
		@Override
		public void run() {
			if (mSetIndex >= 0) {
				setSelIndex(mSetIndex);
				mSetIndex = -1;
			}
		}
	};

	private final boolean prepareItems() {
		if (mCallback != null) {
			post(mPrepareAction);
			return true;
		}
		return false;
	}

	private final void setSelIndex(int index) {
		if (index != mDownIndex) {
			update(mDownIndex);
			mDownIndex = index;
			update(index);
		}
	}

	private final void clearSel() {
		mSetIndex = -1;
		setSelIndex(-1);
	}

	private void drawBackground(Canvas canvas, int color) {
		int saved = mLinePaint.getColor();
		mLinePaint.setColor(color);
		canvas.drawRect(mItemRect, mLinePaint);
		mLinePaint.setColor(saved);
	}

	private void drawString(Canvas canvas) {
		if (mLoadingOrEmptyString == null)
			return;
		StaticLayout layout = new StaticLayout(mLoadingOrEmptyString, mTextPaint, getWidth(), Alignment.ALIGN_CENTER,
				1.2f, 0.0f, false);
		int top = (getHeight() - layout.getHeight()) / 2;
		canvas.translate(0, top);
		layout.draw(canvas);
		canvas.translate(0, -top);
	}

	// TODO: optimize, draw dirty rect only
	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {

		if (mbLoading) {
			drawString(canvas);
			return;
		}

		if (mCallback != null && mCallback.getTotalItems() == 0) {
			drawString(canvas);
			return;
		}

		if (!prepareItems())
			return;

		int index = getItemIndex(0);
		int count = mCallback.getTotalItems();

		mItemRect.left = 0;
		mItemRect.right = getWidth();
		mItemRect.top = getItemTop(index);
		mItemRect.bottom = mItemRect.top + mItemHeight;

		mDrawRect.top = mItemRect.top + (int)Utils.dp2px(getContext(), ITEM_TOP_MARGIN);
		mDrawRect.bottom = mDrawRect.top + mPosterHeight;
		mDrawRect.left = (int)Utils.dp2px(getContext(), ITEM_LEFT_MARGIN);
		mDrawRect.right = mDrawRect.left + mPosterWidth;

		for (int i = index; i < count; i++) {
			mCallback.getItem(i, mDrawItem);

			// draw background
			if (mDrawItem.mbSetBackground) {
				drawBackground(canvas, mDrawItem.mBackgroundColor);
			} else if (i == mDownIndex) {
				drawBackground(canvas, mSelColor);
			}

			// draw poster
			if (mDrawItem.mPoster != null) {
				canvas.drawBitmap(mDrawItem.mPoster, null, mDrawRect, null);
			} else if (mDrawItem.mEmptyPoster != null) {
				mDrawItem.mEmptyPoster.setBounds(mDrawRect);
				mDrawItem.mEmptyPoster.draw(canvas);
			}

			// draw text
			mDrawItem.collectText();
			StaticLayout layout = new StaticLayout(mDrawItem.mSB.toString(), mTextPaint, getWidth() - mItemWidth,
					Alignment.ALIGN_NORMAL, 1.2f, 0.0f, false);
			int top = mDrawRect.top + (int)Utils.dp2px(getContext(), 2);
			canvas.translate(mItemWidth, top);
			layout.draw(canvas);
			canvas.translate(-mItemWidth, -top);

			mDrawItem.clear();

			mItemRect.top += mItemHeight;
			mItemRect.bottom += mItemHeight;
			if (mItemRect.top >= getHeight())
				break;

			mDrawRect.top += mItemHeight;
			mDrawRect.bottom += mItemHeight;
		}

		if (mbEnableFastPreview) {
			canvas.drawLine(0, mRulerPos, mItemWidth, mRulerPos, mLinePaint);
		}

		if (mPreviewBitmap != null) {
			getPreviewRect(mPreviewRect);
			canvas.drawBitmap(mPreviewBitmap, null, mPreviewRect, null);
		}
	}

	private boolean setPos(int pos) {
		if (mCallback == null)
			return false;

		int height = getHeight();
		int height2 = height / 2;
		int total = _getTotalHeight();
		int rulerPos = mRulerPos;

		if (mbPressRuler) {
			int delta = pos - mPos;
			pos -= delta;
			rulerPos = mRulerPos + delta;

			if (delta > 0) {
				if (rulerPos > height2) {
					delta = rulerPos - height2;
					pos += delta;
					rulerPos -= delta;
				}
			} else {
				if (rulerPos < height2) {
					delta = height2 - rulerPos;
					pos -= delta;
					rulerPos += delta;
				}
			}
		}

		if (pos + height2 > total) {
			if (mbPressRuler) {
				rulerPos += pos + height2 - total;
				if (rulerPos > height2) {
					rulerPos = height2;
				}
			}
			pos = total - height2;
		}

		if (pos < 0) {
			if (mbPressRuler) {
				rulerPos += pos;
				if (rulerPos < 0) {
					rulerPos = 0;
				}
			}
			pos = 0;
		}

		if (pos == mPos && rulerPos == mRulerPos)
			return false;

		mPos = pos;
		mRulerPos = rulerPos;
		invalidate();

		reportRulerPos();

		return true;
	}

	private void reportRulerPos() {
		if (mCallback == null)
			return;
		if (mbEnableFastPreview && mbPressRuler) {
			int index = getItemIndex(mRulerPos);
			int offset = getItemOffset(mRulerPos, index);
			if (offset < 0) {
				offset = 0;
			}
			mCallback.onRulerPosChanged(index, offset, mItemHeight);
		}
	}

	private void computeScrollOnTimer() {
		if (!mScroller.isFinished()) {
			if (mScroller.computeScrollOffset()) {
				setPos(mScroller.getCurrY());
				invalidate();
				if (!mScroller.isFinished()) {
					mScrollTimer.run(10);
				}
			}
		}
	}

	private void startScroll() {
		if (mScrollTimer == null) {
			mScrollTimer = new Timer() {
				@Override
				public void onTimer(Timer timer) {
					computeScrollOnTimer();
				}
			};
		}
		mScrollTimer.run(10);
	}

	private void stopScroll() {
		if (!mScroller.isFinished()) {
			mScroller.forceFinished(true);
			mScrollTimer.cancel();
		}
	}

	@Override
	protected int computeVerticalScrollExtent() {
		return getWidth();
	}

	@Override
	protected int computeVerticalScrollOffset() {
		return 0; // TODO
	}

	@Override
	protected int computeVerticalScrollRange() {
		return 0; // TODO
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		boolean handled = super.dispatchTouchEvent(ev);
		handled |= mGesture.onTouchEvent(ev);
		int action = ev.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mbMoved = false;
			if (mbEnableFastPreview) {
				int x = (int)ev.getX();
				mbPressRuler = x < mPosterWidth;
			}
			int index;
			if (mbPressRuler) {
				index = -1;
				reportRulerPos();
			} else {
				index = getItemIndex((int)ev.getY());
				clearPreview();
			}
			mSetIndex = index;
			postDelayed(mSetSelAction, SEL_DELAY);
			break;
		default:
			clearSel();
			break;
		}
		return handled;
	}

	private boolean onDown(MotionEvent e) {
		stopScroll();
		return true;
	}

	private boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (mCallback != null) {
			int total = _getTotalHeight();
			int height = getHeight();
			// TODO: more calculation
			mScroller.fling(0, mPos, 0, -(int)velocityY, 0, 0, -height / 2, total);
			startScroll();
		}
		return true;
	}

	private boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (!mbMoved) {
			if (Math.abs(distanceX) > Math.abs(distanceY)) {
				return false;
			}
		}
		mbMoved = true;
		setPos(mPos + (int)distanceY);
		return true;
	}

	private void onSingleTapUp(MotionEvent e) {
		if (mCallback == null)
			return;
		int index = getItemIndex((int)e.getY());
		if (index >= 0 && index < mCallback.getTotalItems()) {
			clearPreview();
			int offset = 0;
			if (mbPressRuler) {
				int rulerIndex = getItemIndex(mRulerPos);
				if (rulerIndex == index) {
					offset = getItemOffset(mRulerPos, index);
				}
			}
			if (offset < 0) {
				offset = 0;
			}
			mCallback.onClickItem(index, offset, mItemHeight);
		}
	}

	private class MyGestureListener implements GestureDetector.OnGestureListener {

		@Override
		public boolean onDown(MotionEvent e) {
			return VideoListView.this.onDown(e);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return VideoListView.this.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return VideoListView.this.onScroll(e1, e2, distanceX, distanceY);
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			VideoListView.this.onSingleTapUp(e);
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}
	}

}
