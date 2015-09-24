package crs_svr;

public class ProtocolConstMsg
{
	//COMM DEFINE
	public static final int WAYLENS_VERSION 		= 0x0101; //version 
	public static final	int MAX_DEVICE_ID_SIZE 		= 64; //define max device_id size
	public static final int MAX_DEVICE_MODEL_SIZE 	= 256; //define max device model size
	public static final	int CRS_HASH_VALUE_SIZE 	= 33; //the fixed length of hash_value
	public static final int CAM_TRAN_BLOCK_SIZE 	= 4096; //the block size
	public static final int MAX_TOKEN_SIZE 			= 64; //define max token size
	public static final	int UPLOAD_DATA_SHA1_SIZE 	= 20; //the fixed length of SHA1
	public static final int FILE_EACH_BLOCK_SIZE	= 4 * 1024 * 1024; //define each block size while the file for block transmission

	//COMM_IID_ERR
	public static final int RES_SLICE_TRANS_COMPLETE	= 0x0001;
	public static final int RES_FILE_TRANS_COMPLETE		= 0x0002;

	public static final int RES_STATE_OK				=  0x0000;	//OK
	public static final int RES_STATE_FAIL				= -0x0001;	//failed
	public static final int RES_STATE_NO_DEVICE			= -0x0002;	//no device
	public static final int RES_STATE_NO_PERMISSION 	= -0x0003;	//no permission
	public static final int RES_STATE_NO_SPACE			= -0x0004;
	public static final int RES_STATE_WRITE_ERR			= -0x0005;
	public static final int RES_STATE_NO_CLIPS			= -0x0006;
	public static final int RES_STATE_INVALIDE_HTTP_REQUEST = -0x0007;
	public static final int RES_CRS_DEVICE_ID_INCALIDE		= -0x0008;


	//VIDT_DATA_TYPE
	public static final int	VIDIT_RAW_DATA		= 1;
	public static final int VIDIT_VIDEO_DATA		= 2;
	public static final int	VIDIT_PICTURE_DATA	= 4;
    public static final int VIDIT_RAW_GPS       = 8;
    public static final int VIDIT_RAW_OBD       = 16;
    public static final int VIDIT_RAW_ACC       = 32;
    public static final int VIDIT_VIDEO_DATA_LOW= 64;

	//OTHER_DATA_TYPE
	public static final int RAW_GPS			= 1;
	public static final int AUDIO_AAC       = 2;
	public static final int AUDIO_MP3       = 3;
	public static final int MP4_DATA		= 8;
	public static final int JPEG_DATA		= 16;
	public static final int JPEG_AVATAR     = 17;
	public static final int PNG_DATA		= 32;
	public static final int GIF_DATA		= 64;

	//CLIENT_UPLOAD_STREAM_TYPE
	public static final int LIVE_DATA		= 1;
	public static final int	HISTORY_DATA	= 2;

	//WAYLENS_DEVICE_TYPE
	public static final int	DEVICE_VIDIT	= 1;
	public static final int DEVICE_OTHER	= 2;

	//CRS_ENCODE_TYPE
	public static final int	ENCODE_TYPE_OPEN	= 0;
	public static final int ENCODE_TYPE_AES		= 1;
}
