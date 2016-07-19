package com.waylens.hachi.snipe.glide;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.waylens.hachi.library.vdb.ClipPos;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.utils.DigitUtils;


import java.io.InputStream;

/**
 * Created by Xiaofei on 2016/6/18.
 */
public class SnipeStreamFetcher implements DataFetcher<InputStream> {
    private static final String TAG = SnipeStreamFetcher.class.getSimpleName();
    public static final SnipeRequestFactory DEFAULT_REQUEST_FACTORY = new SnipeRequestFactory() {

        @Override
        public VdbRequest<InputStream> create(ClipPos clipPos, VdbRequestFuture<InputStream> future) {
            return new GlideRequest(clipPos, future);
        }

    };

    private final VdbRequestQueue mVdbRequestQueue;
    private final ClipPos mClipPos;
    private final SnipeRequestFactory requestFactory;
    private VdbRequestFuture<InputStream> requestFuture;


    public SnipeStreamFetcher(VdbRequestQueue requestQueue, ClipPos url) {
        this(requestQueue, url,  null);
    }

    public SnipeStreamFetcher(VdbRequestQueue requestQueue, ClipPos url,
                               VdbRequestFuture<InputStream> requestFuture) {
        this(requestQueue, url, requestFuture, DEFAULT_REQUEST_FACTORY);
    }

    public SnipeStreamFetcher(VdbRequestQueue requestQueue, ClipPos url,
                              VdbRequestFuture<InputStream> requestFuture, SnipeRequestFactory requestFactory) {
        this.mVdbRequestQueue = requestQueue;
        this.mClipPos = url;
        this.requestFactory = requestFactory;
        this.requestFuture = requestFuture;
        if (requestFuture == null) {
            this.requestFuture = VdbRequestFuture.newFuture();
        }
    }


    @Override
    public InputStream loadData(Priority priority) throws Exception {
        VdbRequest<InputStream> request = requestFactory.create(
            mClipPos, requestFuture);

        requestFuture.setRequest(mVdbRequestQueue.add(request));

        return requestFuture.get();

    }

    @Override
    public void cleanup() {

    }

    @Override
    public String getId() {
        String clipId = mClipPos.vdbId == null ? String.valueOf(mClipPos.cid.hashCode()) : mClipPos.vdbId;
        //Log.e("test", String.format("====== clipId[%s],clipTime[%d], w[%d], h[%d], scale[%d]",
        //        clipId, clipPos.getClipTimeMs(), maxWidth, maxHeight, scaleType.ordinal()));
//        return DigitUtils.md5(clipId
//                + "#T" + clipPos.getClipTimeMs()
//                + "#W" + maxWidth
//                + "#H" + maxHeight
//                + "#S" + scaleType.ordinal());
        return DigitUtils.md5(clipId + "#T" + mClipPos.getClipTimeMs());

    }

    @Override
    public void cancel() {

    }
}


