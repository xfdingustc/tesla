package com.waylens.hachi.ui.clips.cliptrimmer;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;


import com.waylens.hachi.utils.Utils;
import com.xfdingustc.snipe.vdb.Clip;


/**
 * Created by Xiaofei on 2016/4/11.
 */
public class BookmarkView extends FrameLayout {
    private static final String TAG = BookmarkView.class.getSimpleName();
    public BookmarkView(Context context) {
        super(context);
        init();
    }

    private void init() {
//        setOrientation(VERTICAL);
    }


    public void addBookmark(Clip clip, LayoutParams params, boolean isSelected, OnClickListener listener) {
        int viewId = Math.abs(clip.cid.hashCode());
        FrameLayout bookmarkContainer = (FrameLayout)findViewById(viewId);
        if (bookmarkContainer == null) {
            bookmarkContainer = new FrameLayout(getContext());
//            bookmarkContainer.setOrientation(LinearLayout.VERTICAL);
            //bookmarkContainer.setAlpha(0.3f);
            bookmarkContainer.setId(viewId);
            addView(bookmarkContainer, params);

            int maringHeight = (int)Utils.dp2Px(4);

            View topView = new View(getContext());
            FrameLayout.LayoutParams paramsTop = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maringHeight);

            bookmarkContainer.addView(topView, paramsTop);

            View middleView = new View(getContext());
            FrameLayout.LayoutParams paramsMiddle = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            paramsMiddle.setMargins(0, maringHeight, 0, maringHeight);
            middleView.setAlpha(0.3f);
            bookmarkContainer.addView(middleView, paramsMiddle);

            View bottomView = new View(getContext());
            FrameLayout.LayoutParams paramsBottom = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maringHeight);
            paramsBottom.gravity = Gravity.BOTTOM;
            bookmarkContainer.addView(bottomView, paramsBottom);

        } else {
            bookmarkContainer.setLayoutParams(params);
        }

        for (int i = 0; i < bookmarkContainer.getChildCount(); i++) {
            View childView = bookmarkContainer.getChildAt(i);
            int backgroundColor = isSelected ? 0xFFFE5000 : 0xFF7AD502;
            childView.setBackgroundColor(backgroundColor);
        }


        bookmarkContainer.setTag(clip);
        bookmarkContainer.setOnClickListener(listener);

    }


}
