package com.waylens.hachi.ui.clips.player;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.library.vdb.ClipPos;
import com.waylens.hachi.library.snipe.VdbRequestQueue;
import com.waylens.hachi.glide_snipe_integration.SnipeGlideLoader;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/4/5.
 */
public class BannerAdapter extends PagerAdapter {
    private final Context mContext;


//    List<ClipPos> mClipPosList = new ArrayList<>();

    List<ImageView> mImageViewList = new ArrayList<>();
    private VdbRequestQueue mVdbRequestQueue = VdtCameraManager.getManager().getCurrentCamera().getRequestQueue();

    public BannerAdapter(Context context) {
        this.mContext = context;

    }

    public void addClipPos(ClipPos clipPos) {
//        mClipPosList.add(clipPos);
        ImageView imageView = new ImageView(mContext);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(params);
        Glide.with(mContext)
            .using(new SnipeGlideLoader(mVdbRequestQueue))
            .load(clipPos)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.icon_video_default_2)
            .crossFade()
            .into(imageView);
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
