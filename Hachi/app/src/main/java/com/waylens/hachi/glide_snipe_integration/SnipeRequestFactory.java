package com.waylens.hachi.glide_snipe_integration;

import com.waylens.hachi.library.vdb.ClipPos;
import com.waylens.hachi.library.snipe.VdbRequest;
import com.waylens.hachi.library.snipe.VdbRequestFuture;


import java.io.InputStream;

/**
 * Created by Xiaofei on 2016/6/18.
 */
public interface SnipeRequestFactory {
    VdbRequest<InputStream> create(ClipPos clipPos, VdbRequestFuture<InputStream> future);
}
