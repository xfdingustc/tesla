package com.waylens.hachi.VdbImageLoader;

import com.transee.vdb.Clip;
import com.transee.vdb.ClipPos;
import com.transee.vdb.Vdb;
import com.waylens.hachi.VdbImageLoader.core.imageaware.VdbImageAware;

/**
 * Created by Xiaofei on 2015/8/14.
 */
public class VdbImageLoadingInfo {
    private final Vdb mVdb;
    private final Clip mClip;
    private final ClipPos mClipPos;
    private final VdbImageAware mImageAware;

    public VdbImageLoadingInfo(Vdb vdb, Clip clip, ClipPos clipPos, VdbImageAware imageAware) {
        this.mVdb = vdb;
        this.mClip = clip;
        this.mClipPos = clipPos;
        this.mImageAware = imageAware;
    }
}
