package com.waylens.hachi.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.skin.Element;
import com.waylens.hachi.skin.ElementStaticImage;
import com.waylens.hachi.skin.PanelGforce;

import java.util.List;

/**
 * Created by Xiaofei on 2015/9/8.
 */
public class PanelGforceView extends ViewGroup {
    private static final String TAG = PanelGforceView.class.getSimpleName();
    private final PanelGforce mPanel;

    public PanelGforceView(Context context, PanelGforce panel) {
        super(context);
        this.mPanel = panel;
        init();
    }

    private void init() {
        addElements();
    }

    private void addElements() {
        List<Element> elementList = mPanel.getElementList();
        for (Element element : elementList) {
            if (element instanceof ElementStaticImage) {
                addStaticImageElement((ElementStaticImage)element);
            }
        }
    }

    private void addStaticImageElement(ElementStaticImage element) {
        StaticImageView imageView = new StaticImageView(getContext().getApplicationContext(), element);
        //imageView.setBackgroundColor(getResources().getColor(R.color.material_red_600));
        addView(imageView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mPanel.getWidth(), mPanel.getHeight());
        //super.onMeasure();
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final int childHeight = child.getMeasuredHeight();
            final int childWidth = child.getMeasuredWidth();
            child.layout(l, t, l + childWidth, t + childHeight);
        }
    }
}
