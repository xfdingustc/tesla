package com.waylens.hachi.bgjob.export.statejobqueue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

/**
 * Created by lshw on 16/11/17.
 */

public class DbOpenHelper extends SQLiteOpenHelper{
    private static final int DB_VERSION = 12;
    public static final String TAG = DbOpenHelper.class.getSimpleName();

    public static final String DATABASE_NAME = "job_queue";

    private SQLiteDatabase db = null;

    private static final Property primary_key = new Property("insertId", "integer");
    private static final Property job_id = new Property("jobId", "text");
    private static final Property job_state = new Property("jobState", "integer");
    private static final Property job_temp_file = new Property("jobTemp", "text");

    public static final String LOAD_ALL_IDS_QUERY = "SELECT " + job_id.propertyName + " FROM " + DATABASE_NAME;
    public static final String FIND_BY_ID_QUERY = "SELECT * FROM " + DATABASE_NAME + " WHERE " + job_id.propertyName + " = ?";
    public static final String LOAD_ALL_JOBS = "select * from " +  DATABASE_NAME;
    public static final String COUNT_ALL_JOBS = "select count(*) from " + DATABASE_NAME;

    public DbOpenHelper(Context context, String name) {
        super(context, name, null, DB_VERSION);
        db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createQuery = getCreateStatement();
        sqLiteDatabase.execSQL(createQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }


    public static String getCreateStatement() {
        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        builder.append(DATABASE_NAME).append(" (");
        builder.append(primary_key.propertyName).append(" ");
        builder.append(primary_key.propertyType);
        builder.append(" primary key ");
        builder.append(" autoincrement ");
        builder.append(", `");
        builder.append(job_id.propertyName);
        builder.append("` ");
        builder.append(job_id.propertyType);

        builder.append(", `");
        builder.append(job_state.propertyName);
        builder.append("` ");
        builder.append(job_state.propertyType);

        builder.append(", `");
        builder.append(job_temp_file.propertyName);
        builder.append("` ");
        builder.append(job_temp_file.propertyType);

        builder.append(" );");
        return builder.toString();
    }

    public static String getDropStatement() {
        StringBuffer builder = new StringBuffer();
        builder.append("drop table if exists " + DATABASE_NAME);
        return  builder.toString();
    }

    public SQLiteStatement getInsertStatement() {
        StringBuffer builder = new StringBuffer();
        builder.append("insert into").append(DATABASE_NAME);
        builder.append("?");
        builder.append(",");
        builder.append("?");
        builder.append(",");
        builder.append("?");
        SQLiteStatement insertStatement = db.compileStatement(builder.toString());
        return insertStatement;
    }

    public SQLiteStatement getDeleteStatement() {
        SQLiteStatement deleteStatement = db.compileStatement("delete from " + DATABASE_NAME + " where "
                    + job_id.propertyName + " = ?");
        return deleteStatement;
    }

    public SQLiteStatement getSelectAllStatement() {
        SQLiteStatement selectAllStatement = db.compileStatement("select * from " + DATABASE_NAME);
        return selectAllStatement;
    }

    public static class Property {
        public String propertyName;
        public String propertyType;
        public Property(String name, String type) {
            this.propertyName = name;
            this.propertyType = type;
        }
    }
}
