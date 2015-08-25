package com.waylens.hachi.snipe.toolbox;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.widget.ImageView.ScaleType;

import com.orhanobut.logger.Logger;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipPos;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;

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
        Logger.t(TAG).d("RRRRRRRRRRRRRRRRRRRRRRRRRRRR " + mVdbCommand);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<Bitmap> parseVdbResponse(VdbAcknowledge response) {
        Logger.t(TAG).d("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        return null;
    }

    @Override
    protected void deliverResponse(Bitmap response) {

    }
}
