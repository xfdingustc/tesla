package com.transee.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class ViewHolder extends View {

	private View mView;

	public ViewHolder(Context context) {
		super(context);
		initView();
	}

	public ViewHolder(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public ViewHolder(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private final void initView() {
	}

	public void setView(View view) {
		mView = view;
		requestLayout();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (mView != null) {
			mView.measure(widthMeasureSpec, heightMeasureSpec);
			setMeasuredDimension(mView.getMeasuredWidth(), mView.getMeasuredHeight());
		} else {
			int minWidth = getSuggestedMinimumWidth();
			int minHeight = getSuggestedMinimumHeight();
			setMeasuredDimension(myDefaultSize(minWidth, widthMeasureSpec), myDefaultSize(minHeight, heightMeasureSpec));
		}
	}

	public static int myDefaultSize(int size, int measureSpec) {
		int result = size;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		switch (specMode) {
		case MeasureSpec.UNSPECIFIED:
			result = size;
			break;
		case MeasureSpec.AT_MOST:
			result = 100; // different from getDefaultSize()
			break;
		case MeasureSpec.EXACTLY:
			result = specSize;
			break;
		}
		return result;
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed && mView != null) {
			post(new Runnable() {
				@Override
				public void run() {
					Utils.positionViewTo(mView, ViewHolder.this);
				}
			});
			// Utils.positionViewTo(mView, this);
		}
	}
}
