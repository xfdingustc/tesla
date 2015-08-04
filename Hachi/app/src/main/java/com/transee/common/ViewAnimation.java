package com.transee.common;

import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;

import java.util.ArrayList;

public class ViewAnimation {

	static final boolean DEBUG = false;
	static final String TAG = "ViewAnimation";

	protected void onAnimationStart(ViewAnimation animation, boolean bReverse) {
	}

	protected void onAnimationDone(ViewAnimation animation, boolean bReverse) {
	}

	protected void onAnimationUpdated(ViewAnimation animation, boolean bReverse) {
	}

	// simple animation
	static public class Animation {
		protected int mStartLengthMs;

		public void setStartLengthMs(int startLengthMs) {
			mStartLengthMs = startLengthMs;
		}

		public void updateView(Animation anim, View view, boolean bReverse) {
		}

		public void startAnimation(Animation anim, boolean bReverse) {
		}

		public void endAnimation(Animation anim, boolean bReverse) {
		}

		public void finished(Animation anim, boolean bReverse) {
		}

		public void calculate(Animation anim, boolean bReverse, int duration, int elapsed) {
		}
	}

	// translate animation
	static public class TranslateAnimation extends Animation {

		public static final int ANCHOR_LEFT_TOP = 0;
		public static final int ANCHOR_RIGHT_BOTTOM = 1;

		public static final int TYPE_LINEAR = 0;
		public static final int TYPE_ACCELERATED = 1;

		public Rect mStartRect = new Rect();
		public Rect mEndRect = new Rect();
		public Rect mRect = new Rect();
		public int mAnchor = ANCHOR_LEFT_TOP;
		public int mType = TYPE_LINEAR;

		@Override
		public void updateView(Animation anim, View view, boolean bReverse) {
			MarginLayoutParams lp = (MarginLayoutParams)view.getLayoutParams();
			Rect rect = mRect;
			lp.width = rect.width();
			lp.height = rect.height();
			if (mAnchor == ANCHOR_LEFT_TOP) {
				if (DEBUG) {
					Log.d(TAG, "ANCHOR_LEFT_TOP: " + rect.left + "," + rect.top);
				}
				lp.setMargins(rect.left, rect.top, 0, 0);
			} else {
				int right = mRect.left - mEndRect.left;
				int bottom = mRect.top - mEndRect.top;
				if (DEBUG) {
					Log.d(TAG, "ANCHOR_RIGHT_BOTTOM: " + right + "," + bottom);
				}
				lp.setMargins(0, 0, right, bottom);
			}
			view.setLayoutParams(lp);
		}

		@Override
		public void startAnimation(Animation anim, boolean bReverse) {
			mRect.set(bReverse ? mEndRect : mStartRect);
		}

		@Override
		public void endAnimation(Animation anim, boolean bReverse) {
			mRect.set(bReverse ? mStartRect : mEndRect);
		}

		@Override
		public void calculate(Animation anim, boolean bReverse, int duration, int elapsed) {
			Rect rect = mRect;
			Rect from;
			Rect to;

			if (mType == TYPE_LINEAR) {
				from = mStartRect;
				to = mEndRect;
				if (bReverse) {
					elapsed = duration - elapsed;
				}
				rect.left = from.left + (int)((long)elapsed * (to.left - from.left) / duration);
				rect.top = from.top + (int)((long)elapsed * (to.top - from.top) / duration);
				rect.right = from.right + (int)((long)elapsed * (to.right - from.right) / duration);
				rect.bottom = from.bottom + (int)((long)elapsed * (to.bottom - from.bottom) / duration);
			} else {
				if (!bReverse) {
					from = mEndRect;
					to = mStartRect;
				} else {
					from = mStartRect;
					to = mEndRect;
				}
				elapsed = duration - elapsed;
				double x_from = Math.sqrt(duration * duration - elapsed * elapsed) / (double)duration;
				double x_to = 1.0d - x_from;
				rect.left = (int)(from.left * x_from + to.left * x_to);
				rect.top = (int)(from.top * x_from + to.top * x_to);
				rect.right = (int)(from.right * x_from + to.right * x_to);
				rect.bottom = (int)(from.bottom * x_from + to.bottom * x_to);
			}
		}

		// API
		public void setAnimationType(int type) {
			mType = type;
		}

	}

	// alpha animation
	public static class AlphaAnimation extends Animation {

		public final int mStartAlpha;
		public final int mEndAlpha;

		public int mColor = 0;
		public int mAlpha;

		public AlphaAnimation(int startAlpha, int endAlpha) {
			mStartAlpha = startAlpha;
			mEndAlpha = endAlpha;
		}

		@Override
		public void updateView(Animation anim, View view, boolean bReverse) {
			int color = (mAlpha << 24) | mColor;
			view.setBackgroundColor(color);
		}

		@Override
		public void startAnimation(Animation anim, boolean bReverse) {
			mAlpha = bReverse ? mEndAlpha : mStartAlpha;
		}

		@Override
		public void endAnimation(Animation anim, boolean bReverse) {
			mAlpha = bReverse ? mStartAlpha : mEndAlpha;
		}

		@Override
		public void calculate(Animation anim, boolean bReverse, int duration, int elapsed) {
			if (bReverse) {
				elapsed = duration - elapsed;
			}
			mAlpha = mStartAlpha + elapsed * (mEndAlpha - mStartAlpha) / duration;
		}

	}

	// View alpha animation
	public static class ViewAlphaAnimation extends Animation {

		public final float mStartAlpha;
		public final float mEndAlpha;

		public float mAlpha;

		public ViewAlphaAnimation(float startAlpha, float endAlpha) {
			mStartAlpha = startAlpha;
			mEndAlpha = endAlpha;
		}

		@Override
		public void updateView(Animation anim, View view, boolean bReverse) {
			view.setAlpha(mAlpha);
		}

		@Override
		public void startAnimation(Animation anim, boolean bReverse) {
			mAlpha = bReverse ? mEndAlpha : mStartAlpha;
		}

		@Override
		public void endAnimation(Animation anim, boolean bReverse) {
			mAlpha = bReverse ? mStartAlpha : mEndAlpha;
		}

		@Override
		public void calculate(Animation anim, boolean bReverse, int duration, int elapsed) {
			if (bReverse) {
				elapsed = duration - elapsed;
			}
			mAlpha = mStartAlpha + elapsed * (mEndAlpha - mStartAlpha) / duration;
		}
	}

	// alpha translate animation
	public static class AlphaTranslateAnimation extends Animation {

		public final AlphaAnimation mAlphaAnimation;
		public final TranslateAnimation mTranslateAnimation;

		public AlphaTranslateAnimation(AlphaAnimation alphaAnimation, TranslateAnimation translateAnimation) {
			mAlphaAnimation = alphaAnimation;
			mTranslateAnimation = translateAnimation;
		}

		@Override
		public void updateView(Animation anim, View view, boolean bReverse) {
			mAlphaAnimation.updateView(mAlphaAnimation, view, bReverse);
			mTranslateAnimation.updateView(mTranslateAnimation, view, bReverse);
		}

		@Override
		public void startAnimation(Animation anim, boolean bReverse) {
			mAlphaAnimation.startAnimation(mAlphaAnimation, bReverse);
			mTranslateAnimation.startAnimation(mTranslateAnimation, bReverse);
		}

		@Override
		public void endAnimation(Animation anim, boolean bReverse) {
			mAlphaAnimation.endAnimation(mAlphaAnimation, bReverse);
			mTranslateAnimation.endAnimation(mTranslateAnimation, bReverse);
		}

		@Override
		public void calculate(Animation anim, boolean bReverse, int duration, int elapsed) {
			mAlphaAnimation.calculate(mAlphaAnimation, bReverse, duration, elapsed);
			mTranslateAnimation.calculate(mTranslateAnimation, bReverse, duration, elapsed);
		}

	}

	static class AnimationItem {
		final View mView;
		final Animation mAnimation;

		public AnimationItem(View view, Animation animation) {
			mView = view;
			mAnimation = animation;
		}
	}

	private ArrayList<AnimationItem> mAnimationList = new ArrayList<AnimationItem>();
	private Timer mTimer;
	private long mStartTime;
	private int mDuration;
	private int mAnimStep;
	private boolean mbReverse;

	// API
	public void addAnimation(View view, Animation anim) {
		AnimationItem item = new AnimationItem(view, anim);
		mAnimationList.add(item);
	}

	// API
	static public TranslateAnimation createTranslateAnimation(View view, View viewFrom, View viewTo) {
		TranslateAnimation anim = new TranslateAnimation();
		Utils.getViewRectForView(viewFrom, view, anim.mStartRect);
		Utils.getViewRectForView(viewTo, view, anim.mEndRect);
		return anim;
	}

	// API
	static public TranslateAnimation createTranslateAnimationP(View view, View viewTo, int fromXpercent,
			int fromYpercent, int toXpercent, int toYpercent) {

		TranslateAnimation anim = new TranslateAnimation();

		Utils.getViewRectForView(viewTo, view, anim.mRect);
		int width = anim.mRect.width();
		int height = anim.mRect.height();

		int xoff = width * fromXpercent / 100;
		int yoff = height * fromYpercent / 100;
		anim.mStartRect.set(anim.mRect);
		anim.mStartRect.offset(xoff, yoff);

		xoff = width * toXpercent / 100;
		yoff = height * toYpercent / 100;
		anim.mEndRect.set(anim.mRect);
		anim.mEndRect.offset(xoff, yoff);

		return anim;
	}

	// API
	static public TranslateAnimation createTranslateAnimationD(View view, View viewTo, int fromXdelta, int fromYdelta,
			int toXdelta, int toYdelta) {

		TranslateAnimation anim = new TranslateAnimation();

		Utils.getViewRectForView(viewTo, view, anim.mRect);

		anim.mStartRect.set(anim.mRect);
		anim.mStartRect.offset(fromXdelta, fromYdelta);

		anim.mEndRect.set(anim.mRect);
		anim.mEndRect.offset(toXdelta, toYdelta);

		anim.mAnchor = TranslateAnimation.ANCHOR_RIGHT_BOTTOM; // TODO

		return anim;
	}

	// API
	static public TranslateAnimation createTranslateAnimation(Rect rectFrom, Rect rectTo) {
		TranslateAnimation anim = new TranslateAnimation();
		anim.mStartRect.set(rectFrom);
		anim.mEndRect.set(rectTo);
		return anim;
	}

	// API
	static public ViewAlphaAnimation createAlphaAnimation(float startAlpha, float endAlpha) {

		ViewAlphaAnimation anim = new ViewAlphaAnimation(startAlpha, endAlpha);

		return anim;
	}

	// ---------------------------------------------------------------------------

	// API
	public void stopAnimation() {
		if (mTimer != null) {
			mTimer.cancel();
		}
	}

	// API
	public void startAnimation(int duration, int step, boolean bReverse) {
		stopAnimation();

		mStartTime = System.currentTimeMillis();
		mbReverse = bReverse;
		mDuration = duration;
		mAnimStep = step;

		if (mTimer == null) {
			mTimer = new Timer() {
				@Override
				public void onTimer(Timer timer) {
					onAnimationTimer();
				}
			};
		}

		onAnimationStart(this, mbReverse);

		for (int i = 0; i < mAnimationList.size(); i++) {
			AnimationItem item = mAnimationList.get(i);
			item.mAnimation.startAnimation(item.mAnimation, bReverse);
			update(item);
		}

		mTimer.run(mAnimStep);
	}

	private final void update(AnimationItem item) {
		item.mAnimation.updateView(item.mAnimation, item.mView, mbReverse);
	}

	private void onAnimationTimer() {
		int elapsed = (int)(System.currentTimeMillis() - mStartTime);

		if (elapsed >= mDuration) {

			for (int i = 0; i < mAnimationList.size(); i++) {
				AnimationItem item = mAnimationList.get(i);
				item.mAnimation.endAnimation(item.mAnimation, mbReverse);
				update(item);
			}

			for (int i = 0; i < mAnimationList.size(); i++) {
				AnimationItem item = mAnimationList.get(i);
				item.mAnimation.finished(item.mAnimation, mbReverse);
			}

			onAnimationDone(this, mbReverse);

		} else {

			mTimer.run(mAnimStep);

			for (int i = 0; i < mAnimationList.size(); i++) {
				AnimationItem item = mAnimationList.get(i);
				int startLengthMs = item.mAnimation.mStartLengthMs;
				if (elapsed > startLengthMs) {
					elapsed -= startLengthMs;
					item.mAnimation.calculate(item.mAnimation, mbReverse, mDuration - startLengthMs, elapsed);
					update(item);
				}
			}

			onAnimationUpdated(this, mbReverse);
		}
	}
}
