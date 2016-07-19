package com.waylens.hachi.library.crs_svr;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * CrsCommand
 * Created by Richard on 1/12/16.
 */
public abstract class CrsCommand {
    public static final short CRS_C2S_LOGIN = 0x1000;   //login server
    public static final short CRS_S2C_LOGIN_ACK = 0x1000;   //server response login

    public static final short CRS_C2S_LOGOUT = 0x1001;   //logout server

    public static final short CRS_C2S_START_UPLOAD = 0x1002;   //start upload
    public static final short CRS_S2C_STRAT_UPLOAD_ACK = 0x1002;   //server response upload

    public static final short CRS_UPLOADING_DATA = 0x1003;   //uploading data

    public static final short CRS_C2S_STOP_UPLOAD = 0x1004;   //stop upload
    public static final short CRS_S2C_STOP_UPLOAD_ACK = 0x1004;   //server response stop uplaod

    public static final short CRS_SEND_COMM_RESULT_TO_CLIENT = 0x1005;   //server send common result to client

    public static final short CRS_C2S_UPLOAD_MOMENT_DESC = 0x1006;
    public static final short CRS_S2C_UPLOAD_MOMENT_DESC_ACK = 0x1007;

    public static final short ENCODE_TYPE_OPEN = 0;
    public static final short ENCODE_TYPE_AES = 1;

    public static final byte DEVICE_VIDIT = 1;
    public static final byte DEVICE_OTHER = 2;

    //DEVICE_VIDIT
    public static final int VIDIT_RAW_DATA = 1;
    public static final int VIDIT_VIDEO_DATA = 2;
    public static final int VIDIT_PICTURE_DATA = 4;
    public static final int VIDIT_RAW_GPS = 8;
    public static final int VIDIT_RAW_OBD = 16;
    public static final int VIDIT_RAW_ACC = 32;
    public static final int VIDIT_VIDEO_DATA_LOW = 64;
    public static final int VIDIT_VIDEO_DATA_TRANSFER = 128;
    public static final int VIDIT_THUMBNAIL_JPG = 256;

    //DEVICE_OTHER
    public static final int RAW_GPS = 1;
    public static final int AUDIO_AAC = 2;
    public static final int AUDIO_MP3 = 3;
    public static final int MP4_DATA = 8;
    public static final int JPEG_DATA = 16;
    public static final int JPEG_AVATAR = 17;
    public static final int PNG_DATA = 32;
    public static final int GIF_DATA = 64;

    public static final int RESOURCE_TYPE_ANDROID = 1;
    public static final int RESOURCE_TYPE_IOS = 2;
    public static final int RESOURCE_TYPE_PC = 3;

    public static final String WAYLENS_RESOURCE_TYPE_ANDROID = "android";
    public static final String WAYLENS_RESOURCE_TYPE_IOS = "ios";
    public static final String WAYLENS_RESOURCE_TYPE_PC = "pc";

    public static final int RES_SLICE_TRANS_COMPLETE = 0x0001;
    public static final int RES_FILE_TRANS_COMPLETE = 0x0002;
    public static final int RES_STATE_CANCELLED = 0x0003;

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

    private static final String TAG = "CrsCommand";

    private ByteArrayOutputStream output;

    protected String privateKey;

    protected CrsCommand(String privateKey) {
        output = new ByteArrayOutputStream();
        this.privateKey = privateKey;
    }

    protected CrsCommand(int capacity, String privateKey) {
        output = new ByteArrayOutputStream(capacity);
        this.privateKey = privateKey;
    }

    public void write(byte value) throws IOException {
        output.write(value);
    }

    public void write(byte[] bytes) throws IOException {
        output.write(bytes);
    }

    public void write(byte[] bytes, int offset, int count) throws IOException {
        output.write(bytes, offset, count);
    }

    public void write(String value, boolean requireLength) throws IOException {
        byte[] bytes = value.getBytes("iso-8859-1");
        int length = bytes.length;
        if (requireLength) {
            output.write((byte) bytes.length);
            length++;
        }
        output.write(bytes);

        int zeroCount = (4 - length % 4) % 4;
        for (int i = 0; i < zeroCount; i++) {
            output.write(0);
        }
    }

    public void write(short value) throws IOException {
        output.write((byte) (value >> 8));
        output.write((byte) value);
    }

    public void write(char value) throws IOException {
        output.write((byte) (value >> 8));
        output.write((byte) value);
    }

    public void write(int value) {
        output.write((byte) (value >> 24));
        output.write((byte) (value >> 16));
        output.write((byte) (value >> 8));
        output.write((byte) value);
    }

    public void write(long value) {
        write((int) (value >> 32));
        write((int) value);
    }

    public void write(double value) {
        long longBits = Double.doubleToLongBits(value);
        write(longBits);
    }

    public abstract void encode() throws IOException;

    public abstract CrsCommand decode(byte[] bytes);

    public abstract CommandHead getCommandHeader();

    public abstract EncodeCommandHeader getEncodeHeader();

    /**
     * Returns the encoded content, without including headers.
     */
    public byte[] getEncodedContent() throws IOException {
        output.reset();
        encode();
        return output.toByteArray();
    }

    /**
     * @return the encoded content, including headers.
     */
    public byte[] getEncodedCommand() throws IOException {
        byte[] content = getEncodedContent();
        CommandHead commandHead = getCommandHeader();
        commandHead.setSize((short) (CommandHead.LENGTH + content.length));
        EncodeCommandHeader encodeHeader = getEncodeHeader();
        if (encodeHeader.encodeType == ENCODE_TYPE_OPEN) {
            encodeHeader.setSize((short) (EncodeCommandHeader.LENGTH + commandHead.size));
            output.reset();
            write(encodeHeader.getEncodedContent());
            write(commandHead.getEncodedContent());
            write(content);
        } else {
            output.reset();
            write(commandHead.getEncodedContent());
            write(content);
            byte[] plainPackage = output.toByteArray();
            byte[] encryptedPackage = PTUntil.encrypt(plainPackage, 0, plainPackage.length, privateKey);
            encodeHeader.setSize((short) (EncodeCommandHeader.LENGTH + encryptedPackage.length));
            output.reset();
            write(encodeHeader.getEncodedContent());
            write(encryptedPackage);
        }
        return output.toByteArray();
    }

    public CrsCommand decodeCommand(byte[] bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try {
            byte[] content = readByteArray(inputStream, EncodeCommandHeader.LENGTH);
            EncodeCommandHeader encodeHeader = new EncodeCommandHeader().decode(content);
            if (encodeHeader.encodeType == ENCODE_TYPE_OPEN) {
                content = readByteArray(inputStream, CommandHead.LENGTH);
                CommandHead commandHead = new CommandHead().decode(content);
                content = readByteArray(inputStream, commandHead.size - CommandHead.LENGTH);
                return decode(content);
            } else {
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    public byte readByte(InputStream inputStream) throws IOException {
        return (byte) (inputStream.read() & 0xFF);
    }

    public byte[] readByteArray(InputStream inputStream, int length) throws IOException {
        byte[] bytes = new byte[length];
        int realLength = inputStream.read(bytes);
        if (realLength == length) {
            return bytes;
        } else {
            throw new IOException("Buffer length does not match.");
        }
    }

    public short readShort(InputStream inputStream) throws IOException {
        int result = (inputStream.read() & 0xFF) << 8;
        result |= inputStream.read() & 0xFF;
        return (short) result;
    }

    public int readInt(InputStream inputStream) throws IOException {
        int result = (inputStream.read() & 0xFF) << 24;
        result |= (inputStream.read() & 0xFF) << 16;
        result |= (inputStream.read() & 0xFF) << 8;
        result |= inputStream.read() & 0xFF;
        return result;
    }

    public long readLong(InputStream inputStream) throws IOException {
        long high = readInt(inputStream);
        long low = readInt(inputStream);
        return (high << 32) | (low & 0xFFFFFFFFL);
    }

    public double readDouble(InputStream inputStream) throws IOException {
        long longBits = readLong(inputStream);
        return Double.longBitsToDouble(longBits);
    }

    public String readString(InputStream inputStream, int length) throws IOException {
        byte strLength;
        int byteLength;
        if (length > 0) {
            strLength = (byte) length;
            byteLength = 0;
        } else {
            strLength = readByte(inputStream);
            byteLength = 1;
        }
        byte[] bytes = new byte[strLength];
        int readLength = inputStream.read(bytes);
        if (readLength == strLength) {
            byteLength += readLength;
            int zeroCount = (4 - byteLength % 4) % 4;

            for (int i = 0; i < zeroCount; i++) {
                inputStream.read();
            }
            return new String(bytes, "iso-8859-1");
        } else {
            throw new IOException("String length does not match.");
        }
    }
}
