package com.waylens.hachi.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.vdb.RawDataItem;


/**
 * Created by Xiaofei on 2016/4/12.
 */
public class RawDataItemDao {
    private static final String TAG = RawDataItemDao.class.getSimpleName();
    public static Context mSharedAppContext;
    private final RawDataItemDbOpenHelper mRawDataItemDbOpenHelper;
    private final String mDBName;

    public static void initialize(Context context) {
        mSharedAppContext = context;
    }


    private class RawDataItemDbOpenHelper extends SQLiteOpenHelper {
        private final static int VERSION = 1;

        public RawDataItemDbOpenHelper(String name) {
            super(mSharedAppContext, name, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Logger.t(TAG).d("Create database");

            String createTabAcc = "create table if not exists acc(" +
                "_id bigint primary key, " +
                "pts bigint," +
                "acc_x int, acc_y int, acc_z int, " +
                "gyro_x int, gyro_y int, gyro_z int, " +
                "magn_x int, magn_y int, magn_z int, " +
                "euler_heading int, euler_roll int, euler_pitch int,  " +
                "quaternion_w int, quaternion_x int, quaternion_y int, quaternion_z int)";

            db.execSQL(createTabAcc);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }


    public RawDataItemDao(String name) {
        mRawDataItemDbOpenHelper = new RawDataItemDbOpenHelper(name);
        this.mDBName = name;
    }


    public void addAccRawDataItem(RawDataItem item) {
        String command = "insert into acc(pts, acc_x, acc_y, acc_z, " +
            "gyro_x, gyro_y, gyro_z, " +
            "magn_x, magn_y, magn_z, " +
            "euler_heading, euler_roll, euler_pitch, " +
            "quaternion_w, quaternion_x, quaternion_y, quaternion_z) " +
            "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        RawDataItem.AccData accData = (RawDataItem.AccData) item.data;
        Object[] params = {item.getPtsMs(), accData.accX, accData.accY, accData.accZ,
            accData.gyro_x, accData.gyro_y, accData.gyro_z,
            accData.magn_x, accData.magn_y, accData.magn_z,
            accData.euler_heading, accData.euler_roll, accData.euler_pitch,
            accData.quaternion_w, accData.quaternion_x, accData.quaternion_y, accData.quaternion_z};
        exeSqlCommand(command, params);
    }

    private void exeSqlCommand(String command) {
        Logger.d("Execute SQL: " + command);

    }


    private void exeSqlCommand(String command, Object[] params) {
//        Logger.d("Execute SQL: " + command + "params: " + params);
        SQLiteDatabase database = mRawDataItemDbOpenHelper.getWritableDatabase();

        database.execSQL(command, params);

//        database.close();
    }
}
