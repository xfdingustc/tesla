package com.waylens.hachi.snipe.toolbox;

import android.os.Bundle;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSegment;
import com.waylens.hachi.vdb.rawdata.RawDataBlock;

/**
 * Created by Xiaofei on 2015/9/11.
 */
public class DownloadRawDataBlockRequest extends VdbRequest<RawDataBlock.DownloadRawDataBlock> {
    private static final String TAG = DownloadRawDataBlockRequest.class.getSimpleName();
    private final Bundle mParameters;
    private final ClipSegment mClipSegment;

    public static final String PARAMETER_CLIP_TIME_MS = "clip_time_ms";
    public static final String PARAMETER_LENGTH_MS = "length_ms";
    public static final String PARAMETER_DATA_TYPE = "data_type";


    public DownloadRawDataBlockRequest(ClipSegment clipSegment, Bundle parameters,
                                       VdbResponse.Listener<RawDataBlock.DownloadRawDataBlock> listener,
                                       VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        this.mParameters = parameters;
        this.mClipSegment = clipSegment;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        long clipTimeMs = mParameters.getLong(PARAMETER_CLIP_TIME_MS);
        int lengthMs = mParameters.getInt(PARAMETER_LENGTH_MS);
        int dataType = mParameters.getInt(PARAMETER_DATA_TYPE);
        mVdbCommand = VdbCommand.Factory.createCmdGetRawDataBlock(mClipSegment.getClip().cid, true, dataType, clipTimeMs, lengthMs);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<RawDataBlock.DownloadRawDataBlock> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            return null;
        }

        int clipType = response.readi32();
        int clipId = response.readi32();
        Clip.ID cid = new Clip.ID(clipType, clipId, null);
        RawDataBlock.RawDataBlockHeader header = new RawDataBlock.RawDataBlockHeader(cid);
        header.mClipDate = response.readi32();
        header.mDataType = response.readi32();
        header.mRequestedTimeMs = response.readi64();
        header.mNumItems = response.readi32();
        header.mDataSize = response.readi32();

        // downloaded raw data for remuxing into mp4 file

        RawDataBlock.DownloadRawDataBlock block = new RawDataBlock.DownloadRawDataBlock(header);

        int size = 32;
        response.skip(-size);
        block.ack_data = response.readByteArray(size + header.mNumItems * 8 + header.mDataSize);

        return VdbResponse.success(block);
    }
}
