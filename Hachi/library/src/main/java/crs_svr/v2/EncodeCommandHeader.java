package crs_svr.v2;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * EncodeCommandHeader
 * Created by Richard on 1/13/16.
 */
public class EncodeCommandHeader extends CrsCommand {

    public static final int LENGTH = 4;

    public static final int SIZE_LENGTH = 2;

    public static short CURRENT_ENCODE_TYPE = ENCODE_TYPE_OPEN;

    short size;
    short encodeType;

    public EncodeCommandHeader() {
        super(null);
        this.encodeType = CURRENT_ENCODE_TYPE;
    }

    public void setSize(short size) {
       this.size = size;
    }

    @Override
    public void encode() throws IOException {
        write(size);
        write(encodeType);
    }

    @Override
    public EncodeCommandHeader decode(byte[] bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try {
            size = readShort(inputStream);
            encodeType = readShort(inputStream);
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
    public byte[] getEncodedCommand() throws IOException {
        return null;
    }

    @Override
    public String toString() {
        return String.format("size[%d], encodeType[%d]", size, encodeType);
    }
}
