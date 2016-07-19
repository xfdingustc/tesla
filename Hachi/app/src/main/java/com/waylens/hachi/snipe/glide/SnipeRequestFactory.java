package com.waylens.hachi.snipe.glide;

import com.waylens.hachi.library.vdb.ClipPos;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbRequestFuture;


import java.io.InputStream;

/**
 * Created by Xiaofei on 2016/6/18.
 */
public interface SnipeRequestFactory {
    VdbRequest<InputStream> create(ClipPos clipPos, VdbRequestFuture<InputStream> future);
}
