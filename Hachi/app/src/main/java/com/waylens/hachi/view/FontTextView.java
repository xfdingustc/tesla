package com.waylens.hachi.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import com.waylens.hachi.R;
import com.waylens.hachi.utils.FontUtil;

/**
 * Created by Xiaofei on 2016/10/19.
 */

public class FontTextView extends TextView {


    public FontTextView(Context context) {
        super(context);
        init(context, null);
    }

    public FontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FontTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FontTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FontTextView);

        if (a.hasValue(R.styleable.FontTextView_android_textAppearance)) {
            final int textAppearanceId = a.getResourceId(R.styleable
                    .FontTextView_android_textAppearance,
                android.R.style.TextAppearance);
            TypedArray atp = getContext().obtainStyledAttributes(textAppearanceId,
                R.styleable.FontTextAppearance);
            if (atp.hasValue(R.styleable.FontTextAppearance_font)) {
                setFont(atp.getString(R.styleable.FontTextAppearance_font));
            }
            atp.recycle();
        }

        if (a.hasValue(R.styleable.FontTextView_font)) {
            setFont(a.getString(R.styleable.FontTextView_font));
        }
        a.recycle();
    }

    public void setFont(String font) {
        setPaintFlags(getPaintFlags() | Paint.ANTI_ALIAS_FLAG);
        setTypeface(FontUtil.get(getContext(), font));
    }
}
