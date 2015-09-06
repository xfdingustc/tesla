package com.waylens.hachi.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.waylens.hachi.skin.Skin;
import com.waylens.hachi.skin.SkinManager;

/**
 * Created by Xiaofei on 2015/9/6.
 */
public class DashboardView extends View {

    private Skin mSkin = SkinManager.getManager().getSkin();

    public DashboardView(Context context) {
        this(context, null, 0);
    }

    public DashboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DashboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }



    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DashboardView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
    }
}
