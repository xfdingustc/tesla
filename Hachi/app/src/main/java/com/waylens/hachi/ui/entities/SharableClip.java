package com.waylens.hachi.ui.entities;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipExtentGetRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * SharableClip
 * Created by Richard on 1/17/16.
 */
public class SharableClip{
    private static final int MAX_EXTENSION = 1000 * 30;
    public Clip.ID bufferedCid;
    public Clip.ID realCid;
    public final Clip clip;
    public long minExtensibleValue;
    public long maxExtensibleValue;
    public long selectedStartValue;
    public long selectedEndValue;
    public long currentPosition;

    public SharableClip(Clip clip) {
        this.clip = clip;
        minExtensibleValue = clip.getStartTimeMs();
        maxExtensibleValue = clip.getStartTimeMs() + clip.getDurationMs();
        selectedStartValue = minExtensibleValue;
        selectedEndValue = maxExtensibleValue;
        bufferedCid = clip.cid;
        realCid = clip.cid;
    }

    /**
     * This operation involves network operation, and
     * should NOT be called in MAIN thread.
     */
    public void checkExtension(VdbRequestQueue vdbRequestQueue) {
        if (vdbRequestQueue == null) {
            return;
        }
        if (clip.cid.type == Clip.TYPE_MARKED) {
            getClipExtent(vdbRequestQueue);
        }
    }

    public ClipPos getThumbnailClipPos(long timeMs) {
        return new ClipPos(clip.getVdbId(),
                realCid, clip.clipDate, timeMs, ClipPos.TYPE_POSTER, false);
    }

    public int getSelectedLength() {
        return (int) (selectedEndValue - selectedStartValue);
    }

    void getClipExtent(VdbRequestQueue vdbRequestQueue) {
        final CountDownLatch latch = new CountDownLatch(1);
        vdbRequestQueue.add(new ClipExtentGetRequest(clip, new VdbResponse.Listener<ClipExtent>() {
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
            selectedStartValue = clipExtent.clipStartTimeMs;
            selectedEndValue = clipExtent.clipEndTimeMs;

            bufferedCid = clipExtent.bufferedCid;
            realCid = clipExtent.realCid;

        } else {
            minExtensibleValue = clipExtent.clipStartTimeMs;
            maxExtensibleValue = clipExtent.clipEndTimeMs;
            selectedStartValue = minExtensibleValue;
            selectedEndValue = maxExtensibleValue;
            bufferedCid = clipExtent.cid;
            realCid = clipExtent.cid;
        }
    }

    public static List<SharableClip> processClipSet(@NonNull ClipSet clipSet, VdbRequestQueue
            requestQueue) {
        ArrayList<SharableClip> sharableClips = new ArrayList<>();
        for (Clip clip : clipSet.getClipList()) {
            SharableClip sharableClip = new SharableClip(clip);
            sharableClip.checkExtension(requestQueue);
            sharableClips.add(sharableClip);

        }
        return sharableClips;
    }
}
