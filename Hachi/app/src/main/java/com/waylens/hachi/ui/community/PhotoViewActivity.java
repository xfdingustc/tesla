package com.waylens.hachi.ui.community;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

import butterknife.BindView;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by Xiaofei on 2016/9/26.
 */

public class PhotoViewActivity extends BaseActivity {
    public static final String EXTRA_PHOTO_URL = "extra.photo.url";

    public String mPhotoUrl;

    public PhotoViewAttacher mAttacher;

    @BindView(R.id.iv_photo)
    PhotoView photoView;

    public static void launch(Activity activity, String url) {
        Intent intent = new Intent(activity, PhotoViewActivity.class);
        intent.putExtra(EXTRA_PHOTO_URL, url);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photoview);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mPhotoUrl = getIntent().getStringExtra(EXTRA_PHOTO_URL);

        mAttacher = new PhotoViewAttacher(photoView);


        Glide.with(this)
            .load(mPhotoUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .crossFade()
            .into(new SimpleTarget<GlideDrawable>() {
                @Override
                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                    photoView.setImageDrawable(resource);
                    mAttacher.update();
                }
            });
    }
}
