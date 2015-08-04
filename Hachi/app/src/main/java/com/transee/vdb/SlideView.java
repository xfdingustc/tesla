package com.transee.vdb;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import com.transee.common.Timer;
import com.transee.common.Utils;

public class SlideView extends View {

	static final boolean DEBUG = false;
	static final String TAG = "SlideView";

	public interface Callback {
		void prepareDrawItems(SlideView slideView);

		boolean isSmallBitmap(SlideView slideView);

		Bitmap getBitmap(SlideView slideView, int index);

		void beginScroll(SlideView slideView);

		void cursorPosChanged(SlideView slideView, int pos);

		void drawItem(SlideView slideView, int index, Canvas canvas, Rect rect, Bitmap bitmap);

		void selectionChanged(SlideView slideView);
	}

	public static class LayoutInfo {
		public int width;
		public int height;
		public int imageLeft;
		public int imageTop;
		public int imageWidth;
		public int imageHeight;
		public int curveBase;
		public int curveHeight;
	}

	static final int CURSOR_COLOR = Color.rgb(0, 136, 34);
	static final int CURSOR_COLOR_2 = 0xff00ff40;

	static final int IMAGE_WIDTH = 96;
	static final int IMAGE_HEIGHT = 54;
	static final int CURVE_HEIGHT = 16;

	private int mImageWidth;
	private int mImageHeight;
	private int mCurveHeight;

	private int mLeftMargin = 0;
	private int mRightMargin = 0;
	private int mTopMargin = 4;
	private int mBottomMargin = 4;

	private int mFrameWidth;
	private int mFrameHeight;

	boolean mbCanSelect;
	private int mRangePx = 1; // exclusive
	private int mCurrPosPx = 0;
	private Rect mDrawRect = new Rect();
	private Paint mLinePaint = new Paint();
	private int mSelColor = 0x4000ff40;
	private int mSelBorderColor = 0xff00ff40;

	private Callback mCallback;
	private GestureDetector mGesture;
	private Scroller mTouchScroller;
	private Scroller mLinearScroller;
	private Scroller mScroller;
	private Timer mScrollTimer;
	private int mScrollSpeed;

	private int mSelStart;
	private int mSelLength;
	private boolean mbSelecting;
	private Paint mSelPaint;

	private RectF mHintRect = new RectF();
	private Drawable mScrollHintDrawable;

	private final LayoutInfo mLayoutInfo = new LayoutInfo();

	public SlideView(Context context) {
		super(context);
		initView();
	}

	public SlideView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public SlideView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		float density = Utils.getDensity(getContext());
		mImageWidth = Utils.dp2px(density, IMAGE_WIDTH);
		mImageHeight = Utils.dp2px(density, IMAGE_HEIGHT);
		mCurveHeight = Utils.dp2px(density, CURVE_HEIGHT);
		mFrameWidth = mLeftMargin + mImageWidth + mRightMargin;
		mFrameHeight = mTopMargin + mImageHeight + mBottomMargin;

		mTouchScroller = new Scroller(getContext());
		mLinearScroller = new Scroller(getContext(), new LinearInterpolator());
		mGesture = new GestureDetector(getContext(), new MyGestureListener());

		mLayoutInfo.width = mFrameWidth;
		mLayoutInfo.height = mFrameHeight + mCurveHeight;
		mLayoutInfo.imageLeft = mLeftMargin;
		mLayoutInfo.imageTop = mTopMargin;
		mLayoutInfo.imageWidth = mImageWidth;
		mLayoutInfo.imageHeight = mImageHeight;
		int scrollbarHeight = getHorizontalScrollbarHeight();
		mLayoutInfo.curveBase = mLayoutInfo.height - scrollbarHeight;
		mLayoutInfo.curveHeight = mCurveHeight;
	}

	// API
	public final LayoutInfo getLayoutInfo() {
		return mLayoutInfo;
	}

	// API
	public void setRange(int rangePx, int posPx, boolean bCanSelect) {
		mbSelecting = false;
		mbCanSelect = bCanSelect;
		mRangePx = rangePx;
		setNewPos(posPx, false);
		cancelScroll();
		showScroll(0);
		invalidate();
	}

	// API
	public void updateRange(int rangePx) {
		if (rangePx != mRangePx) {
			mRangePx = rangePx;
			if (mCurrPosPx >= rangePx) {
				setNewPos(rangePx - 1, false);
			}
		}
		cursorPosChanged();
		invalidate();
	}

	// API
	public int getImageWidth() {
		return mImageWidth;
	}

	// API
	public int getImageHeight() {
		return mImageHeight;
	}

	// API
	public int getRange() {
		return mRangePx;
	}

	// API
	public int getPos() {
		return mCurrPosPx;
	}

	// API
	public int getCurrIndex(int max) {
		int index = getIndexAt(mCurrPosPx);
		if (index < 0) {
			index = 0;
		} else if (index >= max) {
			index = max - 1;
		}
		return index;
	}

	// API
	public int getCurrIndexOffset(int index) {
		int posPx = index * mFrameWidth;
		return mCurrPosPx - posPx;
	}

	// API
	public int getFirstIndex() {
		int x = mCurrPosPx - getWidth() / 2;
		return getIndexAt(x);
	}

	// API
	public int getLastIndex() {
		int x = mCurrPosPx + (getWidth() + 1) / 2 - 1;
		return getIndexAt(x);
	}

	// API
	public int getIndexAt(int x) {
		if (x < 0)
			x -= mFrameWidth - 1;
		return x / mFrameWidth;
	}

	// API
	public int getFrameWidth() {
		return mFrameWidth;
	}

	// API
	public void setCallback(Callback callback) {
		mCallback = callback;
	}

	// API
	public void cancelScroll() {
		mTouchScroller.forceFinished(true);
		mLinearScroller.forceFinished(true);
		if (mScrollTimer != null) {
			mScrollTimer.cancel();
			performSetKeepScreenOn(false);
		}
		mScroller = null;
		mScrollSpeed = 0;
	}

	// API
	public boolean isScrolling() {
		return mScroller != null && !mScroller.isFinished();
	}

	// API
	public void toggleSelect() {
		if (mbSelecting && mSelLength != 0) {
			invalidate();
		}
		mbSelecting = !mbSelecting;
		mSelStart = mCurrPosPx;
		mSelLength = 0;
		selectionChanged();
	}

	// API
	public void adjustSelStart(int pos) {
		if (mbSelecting && mSelLength == 0) {
			mSelStart = pos;
			selectionChanged();
		}
	}

	// API
	public boolean isSelecting() {
		return mbSelecting;
	}

	// API
	public int getSelStart() {
		return mSelLength >= 0 ? mSelStart : mSelStart + mSelLength;
	}

	// API
	public int getSelLength() {
		return mSelLength >= 0 ? mSelLength : -mSelLength;
	}

	// API
	public int getSelEnd() {
		return mSelLength >= 0 ? mSelStart + mSelLength : mSelStart;
	}

	// API, rate: 2x, 3x, ...
	public void scrollToRight(int scrollLengthMs, int speed) {
		int remain = mRangePx - mCurrPosPx;
		int duration = (int)((long)scrollLengthMs * remain / mRangePx);
		doScroll(remain, duration, speed);
	}

	// API
	public void scrollToLeft(int scrollLengthMs, int speed) {
		int remain = mRangePx;
		int duration = (int)((long)scrollLengthMs * remain / mRangePx);
		doScroll(-remain, duration, speed);
	}

	// API
	public int getScrollSpeed() {
		return mScrollSpeed;
	}

	// API
	public void showScroll(int scrollHintDrawable) {
		if (scrollHintDrawable == 0) {
			if (mScrollHintDrawable != null) {
				invalidate();
			}
			mScrollHintDrawable = null;
		} else {
			mScrollHintDrawable = getContext().getResources().getDrawable(scrollHintDrawable);
			invalidate();
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

	private void computeScrollOnTimer() {
		if (mScroller != null && !mScroller.isFinished()) {
			if (mScroller.computeScrollOffset()) {
				int x = mScroller.getCurrX();
				setNewPos(x, true);
				invalidate();
				if (mScroller.isFinished()) {
					cancelScroll();
					hideScrollBar();
				} else {
					mScrollTimer.run(10);
				}
			}
		}
	}

	private void performSetKeepScreenOn(boolean bSet) {
		if (getKeepScreenOn() != bSet) {
			setKeepScreenOn(bSet);
		}
	}

	// scrollToLeft, scrollToRight
	private void doScroll(int dx, int duration, int speed) {
		mLinearScroller.startScroll(mCurrPosPx, 0, dx, 0, duration);
		mScroller = mLinearScroller;
		mScrollSpeed = speed;
		showScrollBar();
		beginScroll();
		performSetKeepScreenOn(true);
		startScroll();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = mFrameHeight + mCurveHeight;
		setMeasuredDimension(width, height);
	}

	private final Runnable mPrepareAction = new Runnable() {
		@Override
		public void run() {
			if (mCallback != null) {
				mCallback.prepareDrawItems(SlideView.this);
			}
		}
	};

	@Override
	protected void onDraw(Canvas canvas) {
		post(mPrepareAction);

		int viewWidth = getWidth();
		int startIndex = getFirstIndex();
		int endIndex = getLastIndex();
		int left = startIndex * mFrameWidth - (mCurrPosPx - viewWidth / 2);

		boolean isSmall = mCallback != null ? mCallback.isSmallBitmap(this) : true;
		if (isSmall) {
			mDrawRect.top = mTopMargin;
			mDrawRect.bottom = mTopMargin + mImageHeight;
			mDrawRect.left = left + mLeftMargin;
		} else {
			mDrawRect.top = 0;
			mDrawRect.bottom = mLayoutInfo.height;
			mDrawRect.left = left;
		}

		for (int i = startIndex; i <= endIndex; i++) {
			mDrawRect.right = mDrawRect.left + mImageWidth;

			if (mCallback != null) {
				Bitmap bitmap = mCallback.getBitmap(this, i);
				if (bitmap != null) {
					canvas.drawBitmap(bitmap, null, mDrawRect, null);
				}
				mCallback.drawItem(this, i, canvas, mDrawRect, bitmap);
			}

			mDrawRect.left += mFrameWidth;
		}

		if (mbSelecting && mSelLength != 0) {
			if (mSelPaint == null) {
				mSelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			}

			mDrawRect.left = viewWidth / 2 + (getSelStart() - mCurrPosPx);
			mDrawRect.right = viewWidth / 2 + (getSelEnd() - mCurrPosPx);
			mDrawRect.top = mTopMargin;
			mDrawRect.bottom = mTopMargin + mImageHeight;

			mSelPaint.setStyle(Style.FILL);
			mSelPaint.setColor(mSelColor);
			canvas.drawRect(mDrawRect, mSelPaint);

			mSelPaint.setStyle(Style.STROKE);
			mSelPaint.setColor(mSelBorderColor);
			canvas.drawRect(mDrawRect, mSelPaint);
		}

		int color = mLinePaint.getColor();
		int bottom = mFrameHeight - mBottomMargin;

		if (mbSelecting) {
			mLinePaint.setColor(CURSOR_COLOR_2);
			canvas.drawLine(viewWidth / 2, mTopMargin, viewWidth / 2, bottom, mLinePaint);
			mLinePaint.setColor(CURSOR_COLOR);
			canvas.drawLine(viewWidth / 2 - 1, mTopMargin, viewWidth / 2 - 1, bottom, mLinePaint);
			canvas.drawLine(viewWidth / 2 + 1, mTopMargin, viewWidth / 2 + 1, bottom, mLinePaint);
		} else {
			mLinePaint.setColor(Color.WHITE);
			canvas.drawLine(viewWidth / 2, mTopMargin, viewWidth / 2, bottom, mLinePaint);
		}
		mLinePaint.setColor(color);

		if (mScrollHintDrawable != null) {
			drawScroll(canvas);
		}
	}

	// API
	public void invalidateSel() {
		int viewWidth = getWidth();
		mDrawRect.left = viewWidth - 1;
		mDrawRect.right = viewWidth + 2;
		mDrawRect.top = mTopMargin;
		mDrawRect.bottom = mFrameHeight - mBottomMargin;
		invalidate(mDrawRect);
	}

	private void drawScroll(Canvas canvas) {
		int width = mScrollHintDrawable.getIntrinsicWidth();
		int height = mScrollHintDrawable.getIntrinsicHeight();
		int left = getWidth() / 2 - width / 2;
		int top = mTopMargin + (mImageHeight - height) / 2;
		mHintRect.set(left, top, left + width, top + height);

		if (mSelPaint == null) {
			mSelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		}

		mSelPaint.setStyle(Style.FILL);
		mSelPaint.setColor(0x60000000);
		canvas.drawRoundRect(mHintRect, 5.0f, 5.0f, mSelPaint);

		mScrollHintDrawable.setBounds(left, top, left + width, top + height);
		mScrollHintDrawable.draw(canvas);
	}

	private final void cursorPosChanged() {
		if (mCallback != null) {
			mCallback.cursorPosChanged(this, mCurrPosPx);
		}
	}

	private final void selectionChanged() {
		showScroll(0);
		if (mCallback != null) {
			mCallback.selectionChanged(this);
		}
	}

	public void refreshItem(int index) {
		int left = index * mFrameWidth - (mCurrPosPx - getWidth() / 2);
		mDrawRect.top = 0;
		mDrawRect.bottom = mFrameHeight + mCurveHeight;
		mDrawRect.left = left;
		mDrawRect.right = left + mFrameWidth;
		invalidate(mDrawRect);
	}

	@Override
	protected int computeHorizontalScrollExtent() {
		return getWidth();
	}

	@Override
	protected int computeHorizontalScrollOffset() {
		return mCurrPosPx;
	}

	@Override
	protected int computeHorizontalScrollRange() {
		return mRangePx + getWidth();
	}

	public void setNewPos(int posPx, boolean bNotify) {
		if (posPx < 0) {
			posPx = 0;
		} else if (posPx >= mRangePx) {
			posPx = mRangePx - 1;
		}
		if (posPx != mCurrPosPx) {
			mCurrPosPx = posPx;
			if (mbSelecting) {
				mSelLength = mCurrPosPx - mSelStart;
			}
			invalidate();
			if (bNotify) {
				cursorPosChanged();
			}
			if (mbSelecting) {
				selectionChanged();
			}
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		boolean handled = super.dispatchTouchEvent(ev);
		handled |= mGesture.onTouchEvent(ev);
		int action = ev.getAction();
		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			if (mScroller != null && mScroller.isFinished()) {
				cancelScroll();
			}
			if (mScroller != mTouchScroller) {
				hideScrollBar();
			}
		}
		return handled;
	}

	private void showScrollBar() {
		awakenScrollBars(Integer.MAX_VALUE, true);
	}

	private void hideScrollBar() {
		awakenScrollBars(500, false);
	}

	private boolean onDown(MotionEvent e) {
		cancelScroll();
		showScrollBar();
		return true;
	}

	private void beginScroll() {
		if (mCallback != null) {
			mCallback.beginScroll(this);
		}
	}

	private boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		mTouchScroller.fling(mCurrPosPx, 0, (int)-velocityX, 0, 0, mRangePx, 0, 0);
		mScroller = mTouchScroller;
		beginScroll();
		startScroll();
		return true;
	}

	private boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		beginScroll();
		setNewPos(mCurrPosPx + (int)distanceX, true);
		showScrollBar();
		return true;
	}

	private boolean onSingleTapUp(MotionEvent e) {
		return true;
	}

	private class MyGestureListener implements GestureDetector.OnGestureListener {

		@Override
		public boolean onDown(MotionEvent e) {
			return SlideView.this.onDown(e);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return SlideView.this.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return SlideView.this.onScroll(e1, e2, distanceX, distanceY);
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return SlideView.this.onSingleTapUp(e);
		}

		@Override
		public void onLongPress(MotionEvent e) {
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}
	}
}
