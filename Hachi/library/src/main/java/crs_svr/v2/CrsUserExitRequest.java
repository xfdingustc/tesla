package crs_svr.v2;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by Richard on 1/13/16.
 */
public class CrsUserExitRequest extends CrsCommand {
    String jidExt;

    public CrsUserExitRequest(String userID, String guid, String privateKey) {
        super(privateKey);
        jidExt = userID + "/" + WAYLENS_RESOURCE_TYPE_ANDROID + "/" + guid;
    }

    @Override
    public void encode() throws IOException {
        write(jidExt, true);
    }

    @Override
    public CrsUserExitRequest decode(byte[] bytes) {
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
        return null;
    }

    @Override
    public EncodeCommandHeader getEncodeHeader() {
        return null;
    }

    @Override
    public String toString() {
        return String.format("jidExt[%s]", jidExt);
    }
}
