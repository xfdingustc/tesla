package com.transee.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.waylens.hachi.R;

public class ViewfinderView extends View {

	Rect mRect;
	Paint mPaint;
	int mBorderColor;
	int mBorderSizeX;
	int mBorderSizeY;

	public ViewfinderView(Context context) {
		this(context, null);
	}

	public ViewfinderView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ViewfinderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (attrs != null) {
			TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView);
			mBorderColor = typedArray.getColor(R.styleable.ViewfinderView_borderColor, Color.rgb(0, 0, 0));
			// mBorderSizeX = typedArray.getDimensionPixelSize(R.styleable.ViewfinderView_borderSizeX, 16);
			// mBorderSizeY = typedArray.getDimensionPixelSize(R.styleable.ViewfinderView_borderSizeY, 16);
			typedArray.recycle();
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		mBorderSizeX = (right - left) / 4;
		mBorderSizeY = (bottom - top) / 4;
	}

	// API
	public void getRect(Rect rect) {
		rect.left = mBorderSizeX;
		rect.top = mBorderSizeY;
		rect.right = getWidth() - mBorderSizeX;
		rect.bottom = getHeight() - mBorderSizeY;
		rect.offset(getLeft(), getTop());
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (mRect == null) {
			mRect = new Rect();
		}
		if (mPaint == null) {
			mPaint = new Paint();
		}

		int width = getWidth();
		int height = getHeight();

		int left = mBorderSizeX;
		int right = width - mBorderSizeX;
		int top = mBorderSizeY;
		int bottom = height - mBorderSizeY;

		mPaint.setColor(mBorderColor);
		mRect.set(0, 0, width, top);
		canvas.drawRect(mRect, mPaint);
		mRect.set(0, top, left, bottom);
		canvas.drawRect(mRect, mPaint);
		mRect.set(right, top, width, bottom);
		canvas.drawRect(mRect, mPaint);
		mRect.set(0, bottom, width, height);
		canvas.drawRect(mRect, mPaint);
	}
}
