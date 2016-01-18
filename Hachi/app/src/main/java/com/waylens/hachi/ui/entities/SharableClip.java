package com.waylens.hachi.ui.entities;

import android.util.Log;

import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipExtentGetRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.RemoteClip;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Richard on 1/17/16.
 */
public class SharableClip {
    private static final int MAX_EXTENSION = 1000 * 30;
    public Clip.ID bufferedCid;
    public Clip.ID realCid;
    public long minExtensibleValue;
    public long maxExtensibleValue;
    public final Clip clip;
    long offset;

    VdbRequestQueue mVdbRequestQueue;

    public SharableClip(Clip clip, VdbRequestQueue vdbRequestQueue) {
        this.clip = clip;
        mVdbRequestQueue = vdbRequestQueue;
    }

    /**
     * This operation involves network operation, and
     * should NOT be called in MAIN thread.
     */
    public void checkExtension() {
        if (mVdbRequestQueue == null || clip == null) {
            return;
        }
        if (clip.cid.type == RemoteClip.TYPE_MARKED) {
            getClipExtent();
        } else {
            minExtensibleValue = clip.getStartTimeMs();
            maxExtensibleValue = clip.getStartTimeMs() + clip.getDurationMs();
            bufferedCid = clip.cid;
            realCid = clip.cid;
        }
    }

    void getClipExtent() {
        final CountDownLatch latch = new CountDownLatch(1);
        mVdbRequestQueue.add(new ClipExtentGetRequest(clip, new VdbResponse.Listener<ClipExtent>() {
            @Override
            public void onResponse(ClipExtent clipExtent) {
                if (clipExtent != null) {
                    calculateExtension(clipExtent);
                }
                latch.countDown();
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e("test", "", error);
                latch.countDown();
            }
        }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e("test", "", e);
        }
    }

    void calculateExtension(ClipExtent clipExtent) {
        if (clipExtent.bufferedCid != null) {
            minExtensibleValue = clipExtent.clipStartTimeMs - MAX_EXTENSION;
            if (minExtensibleValue < clipExtent.minClipStartTimeMs) {
                minExtensibleValue = clipExtent.minClipStartTimeMs;
            }
            maxExtensibleValue = clipExtent.clipEndTimeMs + MAX_EXTENSION;
            if (maxExtensibleValue > clipExtent.maxClipEndTimeMs) {
                maxExtensibleValue = clipExtent.maxClipEndTimeMs;
            }
            bufferedCid = clipExtent.bufferedCid;
            realCid = clipExtent.realCid;
        } else {
            minExtensibleValue = clipExtent.clipStartTimeMs;
            maxExtensibleValue = clipExtent.clipEndTimeMs;
            bufferedCid = clipExtent.cid;
            realCid = clipExtent.cid;
        }
    }

}
