package crs_svr.v2;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import crs_svr.HashUtils;

/**
 * CrsUserLogin
 * Created by Richard on 1/12/16.
 */
public class CrsUserLogin extends CrsCommand {
    String jid;
    long momentID;
    public String hash;
    byte deviceType;



    public CrsUserLogin(String userID, long momentID, String privateKey, byte deviceType) {
        super(privateKey);
        jid = userID + "/" + WAYLENS_RESOURCE_TYPE_ANDROID;
        this.momentID = momentID;
        this.deviceType = deviceType;
        hash = HashUtils.MD5String(jid + momentID + deviceType + privateKey);
    }

    public void encode() throws IOException {
        write(jid, true);
        write(momentID);
        write(deviceType);
        write(hash, false);
    }

    @Override
    public CrsUserLogin decode(byte[] bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try {
            jid = readString(inputStream, 0);
            momentID = readLong(inputStream);
            deviceType = readByte(inputStream);
            hash = readString(inputStream, 32);
            return this;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public CommandHead getCommandHeader() {
        return new CommandHead(CRS_C2S_LOGIN);
    }

    @Override
    public EncodeCommandHeader getEncodeHeader() {
        return new EncodeCommandHeader();
    }

    @Override
    public String toString() {
        return String.format("jid[%s], momentId[%d], deviceType[%d], hash[%s]",
                jid, momentID, deviceType, hash);
    }
}
