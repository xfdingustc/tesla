package com.waylens.hachi.ui.clips.clipplay2;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.vdb.ClipPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/4/5.
 */
public class BannerAdapter extends PagerAdapter {
    private final Context mContext;
    private final VdbImageLoader mImageLoader;

//    List<ClipPos> mClipPosList = new ArrayList<>();

    List<ImageView> mImageViewList = new ArrayList<>();


    public BannerAdapter(Context context, VdbImageLoader imageLoader) {
        this.mContext = context;
        this.mImageLoader = imageLoader;
    }

    public void addClipPos(ClipPos clipPos) {
//        mClipPosList.add(clipPos);
        ImageView imageView = new ImageView(mContext);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(params);
        mImageLoader.displayVdbImage(clipPos, imageView);
        mImageViewList.add(imageView);
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(mImageViewList.get(position));
        return mImageViewList.get(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(mImageViewList.get(position));
    }



    @Override
    public int getCount() {
        return mImageViewList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }



}
