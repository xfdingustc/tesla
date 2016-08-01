package com.waylens.hachi.ui.entities;

import android.support.annotation.NonNull;
import android.util.Log;


import com.xfdingustc.snipe.SnipeError;
import com.xfdingustc.snipe.VdbRequestQueue;
import com.xfdingustc.snipe.VdbResponse;
import com.xfdingustc.snipe.toolbox.ClipExtentGetRequest;
import com.xfdingustc.snipe.vdb.Clip;
import com.xfdingustc.snipe.vdb.ClipExtent;
import com.xfdingustc.snipe.vdb.ClipSet;

import java.util.ArrayList;
import java.util.List;

/**
 * SharableClip
 * Created by Richard on 1/17/16.
 */
public class SharableClip {
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



    public int getSelectedLength() {
        return (int) (selectedEndValue - selectedStartValue);
    }

    void getClipExtent(VdbRequestQueue vdbRequestQueue) {
        vdbRequestQueue.add(new ClipExtentGetRequest(clip, new VdbResponse.Listener<ClipExtent>() {
            @Override
            public void onResponse(ClipExtent clipExtent) {
                if (clipExtent != null) {
                    calculateExtension(clipExtent);
                }
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e("test", "", error);
            }
        }));
    }

    public void calculateExtension(ClipExtent clipExtent) {
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

//        bufferedCid = clipExtent.bufferedCid;
//        realCid = clipExtent.realCid;

        if (clipExtent.bufferedCid != null) {
            bufferedCid = clipExtent.bufferedCid;
        }

        if (clipExtent.realCid != null) {
            realCid = clipExtent.realCid;
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
