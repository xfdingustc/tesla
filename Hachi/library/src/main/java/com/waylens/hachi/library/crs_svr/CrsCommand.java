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


}
