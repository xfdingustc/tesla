package com.waylens.hachi.library.crs_svr.v2;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by Richard on 1/13/16.
 */
public class CrsCommandResponse extends CrsCommand {
    public static final int RES_STATE_UNKNOWN = 0x1000;

    public static final int RES_STATE_OK = 0x0000;    //OK
    public static final int RES_STATE_FAIL = -0x0001;    //failed
    public static final int RES_STATE_NO_DEVICE = -0x0002;    //no device
    public static final int RES_STATE_NO_PERMISSION = -0x0003;    //no permission
    public static final int RES_STATE_NO_SPACE = -0x0004;
    public static final int RES_STATE_WRITE_ERR = -0x0005;
    public static final int RES_STATE_NO_CLIPS = -0x0006;
    public static final int RES_STATE_INVALID_HTTP_REQUEST = -0x0007;
    public static final int RES_CRS_DEVICE_ID_INVALID = -0x0008;
    public static final int RES_STATE_MULTI_DEVICE_UPLOADING = -0x0009;
    public static final int RES_STATE_TOO_MUCH_UNFINSIH_MOMENT  = -0x000A;
    public static final int RES_STATE_INVALID_MOMENT_ID         = -0x000B;
    public static final int RES_STATE_INVALID_RESOLUTION        = -0x000C;

    public String jidExt;
    public long momentID;
    public long offset;
    public short responseCode;

    public CrsCommandResponse() {
        super(null);
        responseCode = RES_STATE_UNKNOWN;
    }

    @Override
    public void encode() throws IOException {
        write(jidExt, true);
        write(momentID);
        write(offset);
        write(responseCode);
    }

    @Override
    public CrsCommandResponse decode(byte[] bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try {
            jidExt = readString(inputStream, 0);
            momentID = readLong(inputStream);
            offset = readLong(inputStream);
            responseCode = readShort(inputStream);
            return this;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public CommandHead getCommandHeader() {
        return null;
    }

    @Override
    public EncodeCommandHeader getEncodeHeader() {
        return null;
    }

    public boolean isSuccessful() {
        return responseCode == RES_STATE_OK;
    }
}
