package com.waylens.hachi.library.crs_svr;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * CrsUserLogout
 * Created by Richard on 1/13/16.
 */
public class CrsUserLogout extends CrsCommand {
    String jid;

    public CrsUserLogout(String userid, String privateKey) {
        super(privateKey);
        jid = userid + "/" + WAYLENS_RESOURCE_TYPE_ANDROID;
    }

    @Override
    public void encode() throws IOException {
        write(jid, true);
    }

    @Override
    public CrsUserLogout decode(byte[] bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try {
            jid = readString(inputStream, 0);
            return this;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public CommandHead getCommandHeader() {
        return new CommandHead(CRS_C2S_LOGOUT);
    }

    @Override
    public EncodeCommandHeader getEncodeHeader() {
        return new EncodeCommandHeader();
    }

    @Override
    public String toString() {
        return String.format("jid[%s]", jid);
    }
}
