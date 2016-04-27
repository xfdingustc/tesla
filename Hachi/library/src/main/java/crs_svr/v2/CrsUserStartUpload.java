package crs_svr.v2;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by Richard on 1/13/16.
 */
public class CrsUserStartUpload extends CrsCommand {
    String jidExt;
    long momentID;
    byte[] fileSha1;
    int dataType;
    long fileSize;
    long startTime;
    long offset;
    int duration;

    public CrsUserStartUpload(String userID,
                       String guid,
                       long momentID,
                       byte[] fileSha1,
                       int dataType,
                       long fileSize,
                       long startTime,
                       long offset,
                       int duration,
                       String privateKey) {
        super(privateKey);
        jidExt = userID + "/" + WAYLENS_RESOURCE_TYPE_ANDROID + "/" + guid;
        this.momentID = momentID;
        this.fileSha1 = fileSha1;
        this.dataType = dataType;
        this.fileSize = fileSize;
        this.startTime = startTime;
        this.offset = offset;
        this.duration = duration;
    }

    @Override
    public void encode() throws IOException {
        write(jidExt, true);
        write(momentID);
        if (fileSha1 != null) {
            write(fileSha1);
        } else {
            write(new byte[20]);
        }
        write(dataType);
        write(fileSize);
        write(startTime);
        write(offset);
        write(duration);
    }

    @Override
    public CrsUserStartUpload decode(byte[] bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try {
            jidExt = readString(inputStream, 0);
            momentID = readLong(inputStream);
            fileSha1 = readByteArray(inputStream, 20);
            dataType = readInt(inputStream);
            fileSize = readLong(inputStream);
            startTime = readLong(inputStream);
            offset = readLong(inputStream);
            duration = readInt(inputStream);
            return this;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public CommandHead getCommandHeader() {
        return new CommandHead(CRS_C2S_START_UPLOAD);
    }

    @Override
    public EncodeCommandHeader getEncodeHeader() {
        return new EncodeCommandHeader();
    }
}
