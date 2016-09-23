package com.waylens.hachi.ui.adapters;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.orhanobut.logger.Logger;
import java.util.ArrayList;

/**
 * Created by lshw on 16/9/22.
 */

public class ArrayAdapterSpinner<T> extends ArrayAdapter<T>{

    private Context mContext;
    private int mFieldId;
    private LayoutInflater mInflater;
    private int mResource;
    public ArrayAdapterSpinner(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource, 0, new ArrayList<T>());
        mContext = context;
        mFieldId = 0;
        mInflater = LayoutInflater.from(context);
        mResource = resource;
    }

    @Override
    public @NonNull View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final View view;
        final TextView text;

        view = mInflater.inflate(mResource, parent, false);

        try {
            if (mFieldId == 0) {
                //  If no custom field is assigned, assume the whole resource is a TextView
                text = (TextView) view;
            } else {
                //  Otherwise, find the TextView field within the layout
                text = (TextView) view.findViewById(mFieldId);

                if (text == null) {
                    throw new RuntimeException("Failed to find view with ID "
                            + mContext.getResources().getResourceName(mFieldId)
                            + " in item layout");
                }
            }
        } catch (ClassCastException e) {
            Logger.e("ArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "ArrayAdapter requires the resource ID to be a TextView", e);
        }

        final T item = getItem(position);
        if (item instanceof CharSequence) {
            text.setText((CharSequence) item);
        } else {
            text.setText(item.toString());
        }

        text.setWidth(400);


        return view;
    }
}
