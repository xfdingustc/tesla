package com.waylens.hachi.ui.views.cliptrimmer;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.vdb.Clip;

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
//        setOrientation(HORIZONTAL);
    }


    public void addBookmark(Clip clip, LayoutParams params, boolean isSelected, OnClickListener listener) {
        int viewId = Math.abs(clip.cid.hashCode());
        View view = findViewById(viewId);
        if (view == null) {
            view = new View(getContext());

            view.setAlpha(0.3f);
            view.setId(viewId);
            addView(view, params);
        } else {
            view.setLayoutParams(params);
        }

        if (isSelected) {
            view.setBackgroundColor(0xFFFE5000);
        } else {
            view.setBackgroundColor(0xFF7AD502);
        }
        view.setTag(clip);
        view.setOnClickListener(listener);

    }


}
