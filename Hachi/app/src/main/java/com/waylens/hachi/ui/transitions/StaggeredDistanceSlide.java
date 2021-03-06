package com.waylens.hachi.ui.transitions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Keep;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;
import com.waylens.hachi.utils.TransitionUtils;

import java.util.List;

/**
 * Created by Xiaofei on 2016/10/19.
 */

public class StaggeredDistanceSlide extends Visibility {

    private static final String PROPNAME_SCREEN_LOCATION = "android:visibility:screenLocation";

    private int spread = 1;

    public StaggeredDistanceSlide() {
        super();
    }

    @Keep
    public StaggeredDistanceSlide(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a =
            context.obtainStyledAttributes(attrs, R.styleable.StaggeredDistanceSlide);
        spread = a.getInteger(R.styleable.StaggeredDistanceSlide_spread, spread);
        a.recycle();
    }

    public int getSpread() {
        return spread;
    }

    public void setSpread(int spread) {
        this.spread = spread;
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view,
                             TransitionValues startValues, TransitionValues endValues) {
        int[] position = (int[]) endValues.values.get(PROPNAME_SCREEN_LOCATION);
        return createAnimator(view, sceneRoot.getHeight() + (position[1] * spread), 0f);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view,
                                TransitionValues startValues, TransitionValues endValues) {
        int[] position = (int[]) endValues.values.get(PROPNAME_SCREEN_LOCATION);
        return createAnimator(view, 0f, sceneRoot.getHeight() + (position[1] * spread));
    }

    private Animator createAnimator(
        final View view, float startTranslationY, float endTranslationY) {
        view.setTranslationY(startTranslationY);
        final List<Boolean> ancestralClipping = TransitionUtils.setAncestralClipping(view, false);
        Animator transition = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, endTranslationY);
        transition.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                TransitionUtils.restoreAncestralClipping(view, ancestralClipping);
            }
        });
        return transition;
    }
}
