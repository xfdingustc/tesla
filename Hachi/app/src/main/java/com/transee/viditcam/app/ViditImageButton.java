package com.transee.viditcam.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.waylens.hachi.R;

public class ViditImageButton extends Button {

	public interface OnStateChangedListener {
		public void onStateChanged(ViditImageButton button, boolean bPressed);
	}

	public interface OnPressedListener {
		public void onPressed(ViditImageButton button);
	}

	final static boolean DEBUG = false;
	final static String TAG = "ViditImageButton";

	private boolean mbPressed;
	private boolean mbChecked;
	private Drawable mNormalImage;
	private Drawable mPressedImage;
	private OnStateChangedListener mOnStateChangedListener;
	private OnPressedListener mOnPressedListener;

	public ViditImageButton(Context context) {
		super(context);
		initView(context, null);
	}

	public ViditImageButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context, attrs);
	}

	public ViditImageButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView(context, attrs);
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("ClickableViewAccessibility")
	private void initView(Context context, AttributeSet attrs) {
		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ViditImageView);
		mNormalImage = array.getDrawable(R.styleable.ViditImageView_image);
		mPressedImage = array.getDrawable(R.styleable.ViditImageView_pressedImage);
		boolean bShowText = array.getBoolean(R.styleable.ViditImageView_showText, false);
		boolean bNoBackground = array.getBoolean(R.styleable.ViditImageView_noBackground, false);
		array.recycle();

		setFocusable(true);
		if (!bShowText) {
			this.setLines(0);
		}
		if (!bNoBackground) {
			setBackgroundDrawable(context.getResources().getDrawable(R.drawable.vidit_button));
		} else {
			setBackgroundDrawable(null);
		}
		setOnTouchListener(mOnTouched);
		update();
	}

	// API
	public void setOnStateChangedListener(OnStateChangedListener listener) {
		mOnStateChangedListener = listener;
	}

	// API
	public void setOnPressedListener(OnPressedListener listener) {
		mOnPressedListener = listener;
	}

	// API
	public void setChecked(boolean bChecked) {
		if (mbChecked != bChecked) {
			mbChecked = bChecked;
			update();
		}
	}

	// API
	public void changeImages(int normalImage, int pressedImage) {
		Resources res = getContext().getResources();
		mPressedImage = res.getDrawable(pressedImage);
		mNormalImage = res.getDrawable(normalImage);
		update();
	}

	private void update() {
		if (mbPressed || mbChecked) {
			this.setCompoundDrawablesWithIntrinsicBounds(null, mPressedImage, null, null);
		} else {
			this.setCompoundDrawablesWithIntrinsicBounds(null, mNormalImage, null, null);
		}
	}

	private void onTouched(MotionEvent event) {
		if (isClickable()) {
			boolean bPressed;
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				bPressed = true;
				break;
			case MotionEvent.ACTION_UP:
				bPressed = false;
				break;
			case MotionEvent.ACTION_CANCEL:
				bPressed = false;
				break;
			default:
				return;
			}
			if (mbPressed != bPressed) {
				mbPressed = bPressed;
				update();
				if (mOnStateChangedListener != null) {
					mOnStateChangedListener.onStateChanged(this, mbPressed);
				}
			}
			if (bPressed && mOnPressedListener != null) {
				mOnPressedListener.onPressed(this);
			}
		}
	}

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
	}

	@SuppressLint("ClickableViewAccessibility")
	static final View.OnTouchListener mOnTouched = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			ViditImageButton self = (ViditImageButton)v;
			self.onTouched(event);
			return false;
		}
	};
}
