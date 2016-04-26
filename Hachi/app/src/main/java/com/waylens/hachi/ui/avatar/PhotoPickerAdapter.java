package com.waylens.hachi.ui.avatar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ToggleButton;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.avatar.serializables.Photo;
import com.waylens.hachi.utils.ThumbnailsUtil;

import java.util.List;

/**
 * Created by Xiaofei on 2015/6/29.
 */
public class PhotoPickerAdapter extends BaseAdapter {
    private static final String TAG = PhotoPickerAdapter.class.getSimpleName();

    protected ImageLoader imageLoader = ImageLoader.getInstance();

    private Context mContext;
    private List<Photo> dataList;

    public PhotoPickerAdapter(Context context, List<Photo> dataList) {
        this.mContext = context;
        this.dataList = dataList;
    }


    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    private class ViewHolder {
        public ImageView imageView;
        public ToggleButton tgButton;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.grid_item_img, parent, false);
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image_view);
            viewHolder.tgButton = (ToggleButton) convertView.findViewById(R.id.toggle_button);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Photo item = null;
        if (dataList != null && dataList.size() > position) {
            item = dataList.get(position);
        }
        if (item == null) {
            viewHolder.imageView.setImageResource(R.drawable.defaultpic);
        } else {
            String displayItemUri = ThumbnailsUtil.MapgetHashValue(item.getImageId(), item.getUrl());
            imageLoader.displayImage(displayItemUri, viewHolder.imageView);
        }

        viewHolder.tgButton.setVisibility(View.GONE);
        convertView.setTag(viewHolder);

        return convertView;
    }

}
