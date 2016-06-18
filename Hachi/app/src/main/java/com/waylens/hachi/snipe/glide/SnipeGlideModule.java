package com.waylens.hachi.snipe.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.module.GlideModule;
import com.waylens.hachi.vdb.ClipPos;

import java.io.InputStream;

/**
 * Created by Xiaofei on 2016/6/18.
 */
public class SnipeGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {

    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        glide.register(ClipPos.class, InputStream.class, new SnipeGlideLoader.Factory(context));
    }
}
