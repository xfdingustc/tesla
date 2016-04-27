package crs_svr.v2;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by Richard on 1/13/16.
 */
public class CrsClientTranData extends CrsCommand {
    String jidExt;
    long momentID;
    byte[] fileSha1;
    byte[] blockSha1;
    int dataType;
    int seqNum;
    int blockNum;
    short length;
    public byte[] buffer;

    public CrsClientTranData(String userID,
                             String guid,
                             long momentID,
                             byte[] fileSha1,
                             byte[] blockSha1,
                             int dataType,
                             int seqNum,
                             int blockNum,
                             short length,
                             byte[] buffer,
                             String privateKey) {
        super(privateKey);
        jidExt = userID + "/" + WAYLENS_RESOURCE_TYPE_ANDROID + "/" + guid;
        this.momentID = momentID;
        this.fileSha1 = fileSha1;
        this.blockSha1 = blockSha1;
        this.dataType = dataType;
        this.seqNum = seqNum;
        this.blockNum = blockNum;
        this.length = length;
        this.buffer = buffer;
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
        if (blockSha1 != null) {
            write(blockSha1);
        } else {
            write(new byte[20]);
        }
        write(dataType);
        write(seqNum);
        write(blockNum);
        write(length);
        write(buffer, 0, length);
    }

    @Override
    public CrsClientTranData decode(byte[] bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try {
            jidExt = readString(inputStream, 0);
            momentID = readLong(inputStream);
            fileSha1 = readByteArray(inputStream, 20);
            blockSha1 = readByteArray(inputStream, 20);
            dataType = readInt(inputStream);
            seqNum = readInt(inputStream);
            blockNum = readInt(inputStream);
            length = readShort(inputStream);
            buffer = readByteArray(inputStream, length);
            return this;
        } catch (IOException e) {
            Log.e("CrsClientTranData", "", e);
            return null;
        }
    }

    @Override
    public CommandHead getCommandHeader() {
        return new CommandHead(CRS_UPLOADING_DATA);
    }

    @Override
    public EncodeCommandHeader getEncodeHeader() {
        return new EncodeCommandHeader();
    }

}
