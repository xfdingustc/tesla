package com.transee.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class MapLayout extends RelativeLayout {

	public interface OnTouchEventListener {
		public void onTouchEvent(MotionEvent ev, boolean bHandled);
	}

	private OnTouchEventListener mOnTouchEventListener;

	public MapLayout(Context context) {
		super(context);
		initView();
	}

	public MapLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public MapLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
	}

	// API
	public void setOnTouchEventListener(OnTouchEventListener listener) {
		mOnTouchEventListener = listener;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		boolean result = super.dispatchTouchEvent(ev);
		if (mOnTouchEventListener != null) {
			mOnTouchEventListener.onTouchEvent(ev, result);
		}
		return result;
	}
}
