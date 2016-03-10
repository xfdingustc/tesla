package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.waylens.hachi.utils.ViewUtils;

/**
 * Created by Richard on 3/9/16.
 */
public class IconSpinnerAdapter extends ArrayAdapter<CharSequence> {

    Drawable[] mDrawables;

    int mPadding;

    public IconSpinnerAdapter(Context context, int resource, CharSequence[] strings, Drawable[] drawables, int padding) {
        super(context, resource, strings);
        mDrawables = drawables;
        mPadding = padding;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setCompoundDrawablePadding(mPadding);
            textView.setCompoundDrawablesWithIntrinsicBounds(mDrawables[position], null, null, null);
        }
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setCompoundDrawablePadding(mPadding);
            textView.setCompoundDrawablesWithIntrinsicBounds(mDrawables[position], null, null, null);
        }
        return view;
    }
}
