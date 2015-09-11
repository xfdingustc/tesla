package com.waylens.hachi.snipe.toolbox;

import android.os.Bundle;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.RawDataBlock;

/**
 * Created by Xiaofei on 2015/9/11.
 */
public class RawDataBlockRequest extends VdbRequest<RawDataBlock> {
    private final VdbResponse.Listener<RawDataBlock> mListener;
    private final Bundle mParameters;
    private final Clip mClip;

    public static final String PARAMETER_FOR_DOWNLOAD = "for_download";
    public static final String PARAMETER_CLIP_TIME_MS = "clip_time_ms";
    public static final String PARAMETER_LENGTH_MS = "length_ms";
    public static final String PARAMETER_DATA_TYPE = "data_type";

    public RawDataBlockRequest(Clip clip, Bundle parameters,
                               VdbResponse.Listener<RawDataBlock> listener,
                               VdbResponse.ErrorListener errorListener) {
        super(0, errorListener);
        this.mListener = listener;
        this.mParameters = parameters;
        this.mClip = clip;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        boolean forDownload = mParameters.getBoolean(PARAMETER_FOR_DOWNLOAD);
        long clipTimeMs = mParameters.getLong(PARAMETER_CLIP_TIME_MS);
        int lengthMs = mParameters.getInt(PARAMETER_LENGTH_MS);
        int dataType = mParameters.getInt(PARAMETER_DATA_TYPE);
        mVdbCommand = VdbCommand.Factory.createCmdGetRawDataBlock(mClip, forDownload, clipTimeMs,
            lengthMs, dataType);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<RawDataBlock> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            return null;
        }

        int clipType = response.readi32();
        int clipId = response.readi32();
        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, null);
        RawDataBlock.RawDataBlockHeader header = new RawDataBlock.RawDataBlockHeader(cid);
        header.mClipDate = response.readi32();
        header.mDataType = response.readi32();
        header.mRequestedTimeMs = response.readi64();
        header.mNumItems = response.readi32();
        header.mDataSize = response.readi32();


        RawDataBlock block = new RawDataBlock(header);

        int numItems = block.header.mNumItems;
        block.timeOffsetMs = new int[numItems];
        block.dataSize = new int[numItems];

        for (int i = 0; i < numItems; i++) {
            block.timeOffsetMs[i] = response.readi32();
            block.dataSize[i] = response.readi32();
        }

        block.data = response.readByteArray(block.header.mDataSize);


        return VdbResponse.success(block);
    }

    @Override
    protected void deliverResponse(RawDataBlock response) {
        mListener.onResponse(response);
    }
}
