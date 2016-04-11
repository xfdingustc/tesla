package com.waylens.hachi.ui.views.cliptrimmer;

import android.content.Context;
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
    public BookmarkView(Context context) {
        super(context);
        init();
    }

    private void init() {
//        setOrientation(HORIZONTAL);
    }


    public void addBookmark(Clip.ID cid, LayoutParams params) {
        View view = findViewById(cid.hashCode());
        if (view == null) {
            view = new View(getContext());
            view.setBackgroundColor(0xFF7AD502);
            view.setAlpha(0.3f);
            view.setId(cid.hashCode());

            addView(view, params);
        } else {
            view.setLayoutParams(params);
        }

    }


}
