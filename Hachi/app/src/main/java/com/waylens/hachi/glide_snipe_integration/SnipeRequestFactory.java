package com.waylens.hachi.glide_snipe_integration;

import com.xfdingustc.snipe.VdbRequest;
import com.xfdingustc.snipe.VdbRequestFuture;
import com.xfdingustc.snipe.vdb.ClipPos;


import java.io.InputStream;

/**
 * Created by Xiaofei on 2016/6/18.
 */
public interface SnipeRequestFactory {
    VdbRequest<InputStream> create(ClipPos clipPos, VdbRequestFuture<InputStream> future);
}
