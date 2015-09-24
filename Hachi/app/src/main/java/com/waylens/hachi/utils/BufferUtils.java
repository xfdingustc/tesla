package com.waylens.hachi.utils;

/**
 * Created by Richard on 9/23/15.
 */
public class BufferUtils {

    public static final int VDB_DATA_VIDEO = 1;
    public static final int VDB_DATA_JPEG = 2;
    public static final int RAW_DATA_GPS = 3;
    public static final int RAW_DATA_OBD = 4;
    public static final int RAW_DATA_ACC = 5;
    public static final int RAW_DATA_END = 0xffffffff;

    public static final int HZ1K = 1000;

    public static final int UPLOAD_HEADER_SIZE = 32;

    public static int writeByte(byte[] buffer, byte value, int index) {
        buffer[index] = value;
        return index + 1;
    }

    public static int writei16(byte[] buffer, short value, int index) {
        for (int i = 0; i < 2; i++) {
            buffer[index + i] = (byte) (value >> (8 * i));
        }
        return index + 2;
    }

    public static int writei32(byte[] buffer, int value, int index) {
        for (int i = 0; i < 4; i++) {
            buffer[index + i] = (byte) (value >> (8 * i));
        }
        return index + 4;
    }

    public static int writei64(byte[] buffer, long value, int index) {
        int newIndex = writei32(buffer, (int) value, index);
        newIndex = writei32(buffer, (int) (value >> 32), newIndex);
        return newIndex;
    }

    public static byte[] buildUploadHeader(int dataType, int dataSize, long timeStamp, int stream, int duration) {
        long processedTimeStamp;
        int processedDuration;
        switch (dataType) {
            case VDB_DATA_VIDEO:
                processedTimeStamp = timeStamp * HZ1K * 90;
                processedDuration = duration * HZ1K * 90;
                break;
            case VDB_DATA_JPEG:
                processedTimeStamp = timeStamp * HZ1K * 90;
                processedDuration = 0;
                break;
            case RAW_DATA_ACC:
            case RAW_DATA_GPS:
            case RAW_DATA_OBD:
                processedTimeStamp = timeStamp * HZ1K;
                processedDuration = 0;
                break;
            default:
                processedTimeStamp = 0;
                processedDuration = 0;
                break;
        }

        byte[] buffer = new byte[UPLOAD_HEADER_SIZE];
        int index = 0;
        index = writei32(buffer, dataType, index);
        index = writei32(buffer, dataSize, index);
        index = writei64(buffer, processedTimeStamp, index);
        index = writei32(buffer, stream, index);
        index = writei32(buffer, processedDuration, index);
        index = writeByte(buffer, (byte) 1, index);
        index = writeByte(buffer, (byte) 0, index);
        index = writei32(buffer, 0, index);
        return buffer;
    }

    public static byte[] buildUploadTail(int dataSize) {
        byte[] buffer = new byte[UPLOAD_HEADER_SIZE];
        int index = 0;
        index = writei32(buffer, RAW_DATA_END, index);
        index = writei32(buffer, dataSize, index);
        index = writei64(buffer, 0, index);
        index = writei64(buffer, 0, index);
        index = writei64(buffer, 0, index);
        return buffer;
    }


}
