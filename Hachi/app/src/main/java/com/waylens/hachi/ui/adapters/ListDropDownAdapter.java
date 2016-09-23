package com.waylens.hachi.ui.adapters;

/**
 * Created by lshw on 16/9/22.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.waylens.hachi.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class ListDropDownAdapter extends BaseAdapter {

    private Object mLock = new Object();
    private Context context;
    private List<String> list;
    private int checkItemPosition = 0;

    public void setCheckItem(int position) {
        checkItemPosition = position;
        notifyDataSetChanged();
    }

    public ListDropDownAdapter(Context context){
        this(context, new ArrayList<String>() {
        });
    }

    public ListDropDownAdapter(Context context, List<String> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < getCount()) {
            return list.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView != null) {
            viewHolder = (ViewHolder) convertView.getTag();
        } else {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_default_drop_down, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        fillValue(position, viewHolder);
        return convertView;
    }

    public void add(String item) {
        synchronized (mLock) {
            list.add(item);
        }
        notifyDataSetChanged();
    }

    public void insert(String item, int index) {
        synchronized (mLock) {
            list.add(index, item);
        }
        notifyDataSetChanged();
    }

    public void addAll(Collection<? extends String> collection) {
        synchronized (mLock) {
            list.addAll(collection);
        }
        notifyDataSetChanged();
    }

    private void fillValue(int position, ViewHolder viewHolder) {
        viewHolder.mText.setText(list.get(position));
        if (checkItemPosition != -1) {
            if (checkItemPosition == position) {
                viewHolder.mText.setTextColor(context.getResources().getColor(R.color.app_color_control_activated));
                viewHolder.mText.setBackgroundResource(R.color.settings_item_bg_pressed);
            } else {
                viewHolder.mText.setTextColor(context.getResources().getColor(R.color.app_color_control_activated));
                viewHolder.mText.setBackgroundResource(R.color.settings_item_bg_normal);
            }
        }
    }

    static class ViewHolder {
        @BindView(R.id.text)
        TextView mText;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}