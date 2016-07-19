package com.waylens.hachi.library.crs_svr;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by Richard on 1/13/16.
 */
public class CrsUserStopUpload extends CrsCommand {
    String jidExt;

    public CrsUserStopUpload(String userID, String guid, String privateKey) {
        super(privateKey);
        jidExt = userID + "/" + WAYLENS_RESOURCE_TYPE_ANDROID + "/" + guid;
    }

    @Override
    public void encode() throws IOException {
        write(jidExt, true);
    }

    @Override
    public CrsUserStopUpload decode(byte[] bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try {
            jidExt = readString(inputStream, 0);
            return this;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public CommandHead getCommandHeader() {
        return new CommandHead(CRS_C2S_STOP_UPLOAD);
    }

    @Override
    public EncodeCommandHeader getEncodeHeader() {
        return new EncodeCommandHeader();
    }

    @Override
    public String toString() {
        return String.format("jidExt[%s]", jidExt);
    }
}
