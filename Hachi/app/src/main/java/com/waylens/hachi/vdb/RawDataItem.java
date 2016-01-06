package com.waylens.hachi.vdb;

import android.util.Log;

import com.transee.common.ByteStream;
import com.transee.common.Utils;

/**
 * Created by Xiaofei on 2016/1/6.
 */
public class RawDataItem {
    public static final int RAW_DATA_NULL = 0;
    public static final int RAW_DATA_GPS = 1;
    public static final int RAW_DATA_ACC = 2;
    public static final int RAW_DATA_ODB = 3;

    private final int mType;
    private final long mPtsMs;
    public Object object; // GPSRawData for RAW_DATA_GPS

    public RawDataItem(int type, long ptsMs) {
        this.mType = type;
        this.mPtsMs = ptsMs;
    }

    public int getType() {
        return mType;
    }

    public long getPtsMs() {
        return mPtsMs;
    }

    public static class GPSRawData {

        public static final int GPS_F_LATLON = (1 << 0);
        public static final int GPS_F_ALTITUDE = (1 << 1);
        public static final int GPS_F_SPEED = (1 << 2);
        public static final int GPS_F_TIME = (1 << 3);
        public static final int GPS_F_TRACK = (1 << 4);

        public static class Coord {
            public double lat;
            public double lng;
            public double lat_orig;
            public double lng_orig;

            public void set(Coord other) {
                this.lat = other.lat;
                this.lng = other.lng;
                this.lat_orig = other.lat_orig;
                this.lng_orig = other.lng_orig;
            }
        }

        public int flags;
        public float speed;
        public double altitude;

        public int utc_time;
        public float track;
        public float accuracy;

        public final Coord coord = new Coord();

        public final boolean hasLatLng() {
            return (flags & GPS_F_LATLON) != 0;
        }

        public final boolean hasAltitude() {
            return (flags & GPS_F_ALTITUDE) != 0;
        }

        public final boolean hasSpeed() {
            return (flags & GPS_F_SPEED) != 0;
        }

        public final boolean hasTime() {
            return (flags & GPS_F_TIME) != 0;
        }

        public final boolean hasTrack() {
            return (flags & GPS_F_TRACK) != 0;
        }

        static public GPSRawData translate(byte[] data) {
            GPSRawData result = new GPSRawData();

            result.flags = ByteStream.readI32(data, 0);
            result.speed = ByteStream.readFloat(data, 4);
            result.coord.lat = result.coord.lat_orig = ByteStream.readDouble(data, 8);
            result.coord.lng = result.coord.lng_orig = ByteStream.readDouble(data, 16);
            result.altitude = ByteStream.readDouble(data, 24);

            result.utc_time = ByteStream.readI32(data, 32);
            result.track = ByteStream.readFloat(data, 36);
            result.accuracy = ByteStream.readFloat(data, 40);

            result.GMS84ToGCJ02();

            return result;
        }

        // ===========================================================================

        public static final double ECa = 6378245.0;
        public static final double ECee = 0.00669342162296594323;
        public static final double pi = 3.14159265358979324;

        public static boolean outOfChina(double lat, double lng) {
            if (lng < 73.3 || lng > 135.17)
                return true;
            if (lat < 3.5 || lat > 53.6)
                return true;
            if (lat < 39.8 && lat > 124.3)// Korea & Japan
                return true;
            if (lat < 25.4 && lat > 120.3)// Taiwan
                return true;
            if (lat < 24 && lat > 119)// Taiwan
                return true;
            if (lat < 21 && lat < 108.1)// SouthEastAsia
                return true;
            if (lng < 108 && (lng + lat < 107))
                return true;
            if (lat < 26.8 && lat < 97)// India
                return true;
            return false;
        }

        public static double transformLat(double x, double y) {
            double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
            ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
            ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
            ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
            return ret;
        }

        public static double transformLng(double x, double y) {
            double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
            ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
            ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
            ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;
            return ret;
        }

        public void GMS84ToGCJ02() {
            GMS84ToGCJ02(this.coord);
        }

        // coord.lat, lng -> new lat, lng
        static public void GMS84ToGCJ02(Coord coord) {
            double lat = coord.lat;
            double lng = coord.lng;

            if (outOfChina(lat, lng)) {
                return;
            }

            double x = lng - 105.0;
            double y = lat - 35.0;

            double dLat = transformLat(x, y);
            double dLng = transformLng(x, y);

            double radLat = lat / 180.0 * pi;
            double magic = Math.sin(radLat);
            magic = 1 - ECee * magic * magic;
            double sqrtMagic = Math.sqrt(magic);
            dLat = (dLat * 180.0) / ((ECa * (1 - ECee)) / (magic * sqrtMagic) * pi);
            dLng = (dLng * 180.0) / (ECa / sqrtMagic * Math.cos(radLat) * pi);

            coord.lat = lat + dLat;
            coord.lng = lng + dLng;
        }

    }

    public static class OBDData {
        public static final int OBD_VERSION_1 = 1;
        public static final int OBD_VERSION_2 = 2;

        public int speed;
        public int temperature;
        public int rpm;

        public static final int OFF_revision = 0;
        public static final int OFF_total_size = 4;
        public static final int OFF_pid_info_size = 8;
        public static final int OFF_pid_data_size = 12;
        public static final int OFF_HEAD = 40;


        public static final int PID_TEMP = 0x05;        //temperature
        public static final int PID_RPM = 0x0C;         //RPM
        public static final int PID_SPEED = 0x0D;       //Speed
        public static final int PID_THROTTLE = 0x11;    // Throttle Position
        public static final int PID_BP = 0x33;          // - barometric pressure
        public static final int PID_IMP = 0x0B;         // - intake manifold absolute pressure
        public static final int PID_AEPT = 0x62;    // - Actual engine percent torque
        public static final int PID_ERT = 0x63;    // - Engine reference torque

        private static final int[] g_pid_data_size_table = new int[]{
            4, 4, 2, 2, 1, 1, 2, 2,        // 00 - 07
            2, 2, 1, 1, 2, 1, 1, 1,        // 08 - 0F

            2, 1, 1, 1, 2, 2, 2, 2,        // 10 - 17
            2, 2, 2, 2, 1, 1, 1, 2,        // 18 - 1F

            4, 2, 2, 2, 4, 4, 4, 4,        // 20 - 27
            4, 4, 4, 4, 1, 1, 1, 1,        // 28 - 2F

            1, 2, 2, 1, 4, 4, 4, 4,        // 30 - 37
            4, 4, 4, 4, 2, 2, 2, 2,        // 38 - 3F

            4, 4, 2, 2, 2, 1, 1, 1,        // 40 - 47
            1, 1, 1, 1, 1, 2, 2, 4,        // 48 - 4F

            4, 1, 1, 2, 2, 2, 2, 2,        // 50 - 57
            2, 2, 1, 1, 1, 2, 2, 1,        // 58 - 5F

            4, 1, 1, 2, 5, 2, 5, 3,        // 60 - 67
            7, 7, 5, 5, 5, 6, 5, 3,        // 68 - 6F

            9, 5, 5, 5, 5, 7, 7, 5,        // 70 - 77
            9, 9, 7, 7, 9, 1, 1, 13,       // 78 - 7F
        };


        public OBDData(int speed, int temperature, int rpm) {
            this.speed = speed;
            this.temperature = temperature;
            this.rpm = rpm;
        }

        public String toString() {
            return String.format("Speed[%d], Temperature[%d], RPM[%d]", speed, temperature, rpm);
        }


        public static OBDData parse(byte[] data) {
            if (data == null) {
                Log.e("OBDData", "Invalid OBD data.");
                return null;
            }

            int revisionCode = Utils.readi32(data, OFF_revision);
            if (revisionCode == OBD_VERSION_1) {
                return parseVersion1(data);
            } else {
                return parseVersion2(data);
            }
        }

        private static OBDData parseVersion2(byte[] data) {
            int speed = 0;
            int rpm = 0;
            int temperature = 0;
            int index = 1;
            while (index < data.length) {
                int pid = data[index];
                if (pid == 0) {
                    break;
                }
                index++;
                int len = g_pid_data_size_table[pid];
                switch (pid) {
                    case PID_SPEED:
                        speed = data[index] & 0x00FF;
                        break;
                    case PID_TEMP:
                        temperature = data[index] - 40;
                        break;
                    case PID_RPM:
                        rpm = data[index] & 0x000000FF;
                        rpm <<= 8;
                        rpm |= (data[index + 1] & 0x000000FF);
                        rpm >>= 2;
                        break;
                    default:
                        //Log.e("test", "PID is not supported yet: " + pid);
                        break;
                }
                index += len;
            }
            //Log.e("test", String.format("speed[%d], t[%d], rpm[%d]", speed, temperature, rpm));
            return new OBDData(speed, temperature, rpm);
        }

        private static OBDData parseVersion1(byte[] data) {
            int totalSize = Utils.readi32(data, OFF_total_size);
            int pidInfoSize = Utils.readi32(data, OFF_pid_info_size);
            int pidDataSize = Utils.readi32(data, OFF_pid_data_size);

            int INDEX_INFO_START = OFF_HEAD;
            int INDEX_DATA_START = OFF_HEAD + pidInfoSize;

            int flag = Utils.read16(data, INDEX_INFO_START + PID_TEMP * 4);
            int temperature = 0;
            if ((flag & 0x1) == 1) {
                int offsetTemp = Utils.read16(data, INDEX_INFO_START + PID_TEMP * 4 + 2);
                temperature = (data[INDEX_DATA_START + offsetTemp] & 0x00FF) - 40;
            }
            flag = Utils.read16(data, INDEX_INFO_START + PID_SPEED * 4);
            int speed = 0;
            if ((flag & 0x1) == 1) {
                int offsetSpeed = Utils.read16(data, INDEX_INFO_START + PID_SPEED * 4 + 2);
                speed = data[INDEX_DATA_START + offsetSpeed] & 0x00FF;
            }

            int rpm = 0;
            flag = Utils.read16(data, INDEX_INFO_START + PID_RPM * 4);
            if ((flag & 0x1) == 1) {
                int offsetRMP = Utils.read16(data, INDEX_INFO_START + PID_RPM * 4 + 2);
                rpm = data[INDEX_DATA_START + offsetRMP] & 0x000000FF;
                rpm <<= 8;
                rpm |= (data[INDEX_DATA_START + offsetRMP + 1] & 0x000000FF);
                rpm >>= 2;
            }
            return new OBDData(speed, temperature, rpm);
        }
    }

    public static class AccData {
        //// accel : g x 1000 = mg
        public int accX;
        public int accY;
        public int accZ;

        //---------------------------------------------------
        public int version;   // IIO_VERSION
        int size;      // sizeof(iio_raw_data_s)
        public int flags;     // IIO_F_ACCEL etc
        //---------------------------------------------------

        // gyro : Dps x 1000 = mDps
        public int gyro_x;
        public int gyro_y;
        public int gyro_z;

        // magn : uT x 1000000
        public int magn_x;
        public int magn_y;
        public int magn_z;

        // Orientation
        // Euler : Degrees x 1000 = mDegrees
        public int euler_heading;
        public int euler_roll;
        public int euler_pitch;

        // Quaternion : Raw, no unit
        public int quaternion_w;
        public int quaternion_x;
        public int quaternion_y;
        public int quaternion_z;

        // Pressure: Pa x 1000
        public int pressure;

        public static final int ACC_DATA_LENGTH_V0 = 12;
        private int mPos;

        @Override
        public String toString() {
            return String.format("AccX[%d], AccY[%d], AccZ[%d], EulerRoll[%d]", accX, accY, accZ,
                euler_roll);
        }

        public static AccData parse(byte[] data) {
            AccData accData = new AccData();
            accData.parseData(data);
            return accData;
        }

        void parseData(byte[] data) {
            mPos = 0;
            accX = readi32(data);
            accY = readi32(data);
            accZ = readi32(data);

            if (data.length == ACC_DATA_LENGTH_V0) {
                return;
            }

            version = readi16(data);
            size = readi16(data);
            if (size != data.length) {
                version = 0;
                return;
            }
            flags = readi32(data);

            gyro_x = readi32(data);
            gyro_y = readi32(data);
            gyro_z = readi32(data);

            magn_x = readi32(data);
            magn_y = readi32(data);
            magn_z = readi32(data);

            euler_heading = readi32(data);
            euler_roll = readi32(data);
            euler_pitch = readi32(data);

            quaternion_w = readi32(data);
            quaternion_x = readi32(data);
            quaternion_y = readi32(data);
            quaternion_z = readi32(data);

            pressure = readi32(data);
        }

        int readi32(byte[] data) {
            int result = (int) data[mPos] & 0xFF;
            result |= ((int) data[mPos + 1] & 0xFF) << 8;
            result |= ((int) data[mPos + 2] & 0xFF) << 16;
            result |= ((int) data[mPos + 3] & 0xFF) << 24;
            mPos += 4;
            return result;
        }

        int readi16(byte[] data) {
            int result = (int) data[mPos] & 0xFF;
            result |= ((int) data[mPos + 1] & 0xFF) << 8;
            mPos += 2;
            return result;
        }
    }
}
