package com.waylens.hachi.snipe.glide;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by Xiaofei on 2016/6/18.
 */
public class GlideRequest extends VdbRequest<InputStream> {
    private static final String TAG = GlideRequest.class.getSimpleName();

    private final ClipPos mClipPos;

    private final VdbRequestFuture<InputStream> mFuture;

    public GlideRequest(ClipPos clipPos, VdbRequestFuture<InputStream> future) {
        super(0, future, future);
        this.mClipPos = clipPos;
        this.mFuture = future;
    }
    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdGetIndexPicture(mClipPos);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<InputStream> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("ackGetIndexPicture: failed");
            return null;
        }

        int clipType = response.readi32();
        int clipId = response.readi32();
        int clipDate = response.readi32();
        int type = response.readi32();
        boolean bIsLast = (type & ClipPos.F_IS_LAST) != 0;
        type &= ~ClipPos.F_IS_LAST;
        long timeMs = response.readi64();
        long clipStartTime = response.readi64();
        int clipDuration = response.readi32();

        int pictureSize = response.readi32();
        byte[] data = new byte[pictureSize];
        response.readByteArray(data, pictureSize);
        return VdbResponse.success((InputStream)new ByteArrayInputStream(data));

    }


}
