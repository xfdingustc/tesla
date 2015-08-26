package com.waylens.hachi.snipe.toolbox;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView.ScaleType;


import com.orhanobut.logger.Logger;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipPos;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.events.GetIndexPictureEvent;

/**
 * Created by Xiaofei on 2015/8/25.
 */
public class VdbImageRequest extends VdbRequest<Bitmap> {
    private final static String TAG = VdbImageRequest.class.getSimpleName();

    private final VdbResponse.Listener<Bitmap> mListener;
    private final Config mDecoderConfig;
    private final int mMaxWidth;
    private final int mMaxHeight;
    private final ScaleType mScaleType;
    private final Clip mClip;
    private final ClipPos mClipPos;

    public VdbImageRequest(Clip clip, ClipPos clipPos, VdbResponse.Listener<Bitmap> listener, int
        maxWidth, int maxHeight, ScaleType scaleType, Config decodeConfig, VdbResponse
                               .ErrorListener errorListener) {
        super(0, errorListener);
        this.mClip = clip;
        this.mClipPos = clipPos;
        this.mListener = listener;
        this.mDecoderConfig = decodeConfig;
        this.mMaxWidth = maxWidth;
        this.mMaxHeight = maxHeight;
        this.mScaleType = scaleType;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdGetIndexPicture(mClip, mClipPos);
        return mVdbCommand;
    }


    @Override
    protected VdbResponse<Bitmap> parseVdbResponse(VdbAcknowledge response) {

        if (response.getRetCode() != 0) {
            Log.e(TAG, "ackGetIndexPicture: failed");
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
        Logger.t(TAG).d("Picture size = " + pictureSize);
        response.readByteArray(data, pictureSize);

        String vdbId = null;

        vdbId = response.readStringAligned();

        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, vdbId);
        ClipPos clipPos = new ClipPos(vdbId, cid, clipDate, timeMs, type, bIsLast);
        clipPos.setRealTimeMs(clipStartTime);
        clipPos.setDuration(clipDuration);

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap = null;
        if (mMaxWidth == 0 && mMaxHeight == 0) {
            decodeOptions.inPreferredConfig = mDecoderConfig;
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        }


        if (bitmap != null) {
            return VdbResponse.success(bitmap);
        } else {
            return null;
        }

    }

    @Override
    protected void deliverResponse(Bitmap response) {

    }
}
