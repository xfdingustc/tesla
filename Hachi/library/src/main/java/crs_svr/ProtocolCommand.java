
package crs_svr;

public class ProtocolCommand {
    //=======================================================================
    //CRS server
    //=======================================================================
    public static final int CRS_C2S_LOGIN = 0x1000;    //login server
    public static final int CRS_S2C_LOGIN_ACK = 0x1000;    //server response login

    public static final int CRS_C2S_LOGOUT = 0x1001;    //logout server

    public static final int CRS_C2S_START_UPLOAD = 0x1002;    //start upload

    public static final int CRS_S2C_STRAT_UPLOAD_ACK = 0x1002;    //server response upload

    public static final int CRS_UPLOADING_DATA = 0x1003;    //uploading data

    public static final int CRS_C2S_STOP_UPLOAD = 0x1004;    //stop upload
    public static final int CRS_S2C_STOP_UPLOAD_ACK = 0x1004;    //server response stop uplaod

    public static final int CRS_SEND_COMM_RESULT_TO_CLIENT = 0x1005; //server send common result to client
}
