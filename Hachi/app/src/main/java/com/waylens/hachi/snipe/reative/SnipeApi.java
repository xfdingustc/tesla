package com.waylens.hachi.snipe.reative;

import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipSet;

import java.util.concurrent.ExecutionException;

/**
 * Created by Xiaofei on 2016/10/11.
 */

public class SnipeApi {
    public static ClipSet getClipSet(int type, int flag, int attr) throws ExecutionException, InterruptedException {
        VdbRequestFuture<ClipSet> future = VdbRequestFuture.newFuture();
        ClipSetExRequest request = new ClipSetExRequest(type, flag, attr, future, future);
        VdtCameraManager.getManager().getCurrentVdbRequestQueue().add(request);
        return future.get();
    }

    public static Integer deleteClip(Clip.ID clipId) throws ExecutionException, InterruptedException {
        VdbRequestFuture<Integer> future = VdbRequestFuture.newFuture();
        ClipDeleteRequest request = new ClipDeleteRequest(clipId, future, future);
        VdtCameraManager.getManager().getCurrentVdbRequestQueue().add(request);
        return future.get();
    }
}
