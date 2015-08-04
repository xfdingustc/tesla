package com.transee.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.waylens.hachi.R;


public class OverlayImageView extends ImageView {

	private Rect mOverlayRect = new Rect();
	private Drawable mOverlayDrawable;
	private boolean mbOverlayEnabled;
	private boolean mbOverlayVisible;
	private int mAlpha;
	public int mCounter; // for user

	public OverlayImageView(Context context) {
		super(context);
		initView(context, null);
	}

	public OverlayImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context, attrs);
	}

	public OverlayImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView(context, attrs);
	}

	private void initView(Context context, AttributeSet attrs) {
		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.OverlayImageView);
		mOverlayDrawable = array.getDrawable(R.styleable.OverlayImageView_overlayDrawable);
		mbOverlayEnabled = array.getBoolean(R.styleable.OverlayImageView_overlayEnabled, false);
		mbOverlayVisible = array.getBoolean(R.styleable.OverlayImageView_overlayVisible, false);
		mAlpha = array.getInteger(R.styleable.OverlayImageView_overlayAlpha, 255);
		calcOverlayRect();
		array.recycle();
	}

	private void calcOverlayRect() {
		if (mOverlayDrawable != null) {
			mOverlayRect.left = 0;
			mOverlayRect.top = 0;
			mOverlayRect.right = mOverlayDrawable.getIntrinsicWidth();
			mOverlayRect.bottom = mOverlayDrawable.getIntrinsicHeight();
			mOverlayDrawable.setBounds(mOverlayRect);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mbOverlayEnabled && mbOverlayVisible && mOverlayDrawable != null) {
			mOverlayDrawable.setAlpha(mAlpha);
			mOverlayDrawable.draw(canvas);
		}
	}

	// API
	public void setOverlayDrawable(Drawable drawable) {
		mOverlayDrawable = drawable;
		calcOverlayRect();
		invalidate();
	}

	// API
	public void setOverlayVisible(boolean bEnabled, boolean bVisible) {
		if (mbOverlayEnabled != bEnabled || mbOverlayVisible != bVisible) {
			mbOverlayEnabled = bEnabled;
			mbOverlayVisible = bVisible;
			invalidate(mOverlayRect);
		}
	}

	// API
	public void setOverlayAlpha(int alpha) {
		if (mAlpha != alpha) {
			mAlpha = alpha;
			invalidate(mOverlayRect);
		}
	}

	// API
	public int getOverlayAlpha() {
		return mAlpha;
	}

	// API
	public void enableOverlay(boolean bVisible) {
		setOverlayVisible(true, bVisible);
	}

	// API
	public void disableOverlay() {
		setOverlayVisible(false, false);
	}

	// API
	public final boolean isOverlayEnabled() {
		return mbOverlayEnabled;
	}

	// API
	public final boolean isOverlayVisible() {
		return mbOverlayVisible;
	}

}
