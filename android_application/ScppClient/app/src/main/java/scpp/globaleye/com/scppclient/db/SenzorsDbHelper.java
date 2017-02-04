package scpp.globaleye.com.scppclient.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

/**
 * Created by umayanga on 6/23/16.
 */
public class SenzorsDbHelper  extends SQLiteOpenHelper {

    private static final String TAG = SenzorsDbHelper.class.getName();

    // we use singleton database
    private static SenzorsDbHelper senzorsDbHelper;

    // If you change the database schema, you must increment the database version
    private static final int DATABASE_VERSION = 20;
    private static final String DATABASE_NAME = "scppAndroidClient.db";

    // data types, keywords and queries
    private static final String TEXT_TYPE = " TEXT";


    private static final String SQL_CREATE_MINING_DETAIL =
            "CREATE TABLE " + SenzorsDbContract.WalletCoins.TABLE_NAME + " (" +
                    SenzorsDbContract.WalletCoins._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + "," +
                    SenzorsDbContract.WalletCoins.COLUMN_NAME_COIN + TEXT_TYPE + " UNIQUE NOT NULL" + "," +
                    SenzorsDbContract.WalletCoins.COLUMN_NAME_TIME + TEXT_TYPE +","+
                    SenzorsDbContract.WalletCoins.COLUMN_NAME_S_ID + TEXT_TYPE +","+
                    SenzorsDbContract.WalletCoins.COLUMN_NAME_S_LOCATION + TEXT_TYPE +","+
                    SenzorsDbContract.WalletCoins.COLUMN_NAME_USER_NAME + TEXT_TYPE +
                    " )";


    private static final String SQL_CREATE_VERIFY_COINS =
            "CREATE TABLE " + SenzorsDbContract.VerifyCoins.TABLE_NAME + " (" +
                    SenzorsDbContract.VerifyCoins._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + "," +
                    SenzorsDbContract.VerifyCoins.COLUMN_NAME_COIN + TEXT_TYPE + " UNIQUE NOT NULL" + "," +
                    SenzorsDbContract.VerifyCoins.COLUMN_NAME_SPARA + TEXT_TYPE +","+
                    SenzorsDbContract.VerifyCoins.COLUMN_NAME_S_ID + TEXT_TYPE +","+
                    SenzorsDbContract.VerifyCoins.COLUMN_NAME_GENERETED_DATE + TEXT_TYPE +","+
                    SenzorsDbContract.VerifyCoins.COLUMN_NAME_CREATER + TEXT_TYPE +","+
                    SenzorsDbContract.VerifyCoins.COLUMN_NAME_VerifyState + TEXT_TYPE +
                    " )";


    /**
     * Init context
     * Init database
     * @param context application context
     */
    public SenzorsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * We are reusing one database instance in all over the app for better memory usage
     * @param context application context
     * @return db helper instance
     */
    synchronized static SenzorsDbHelper getInstance(Context context) {
        if (senzorsDbHelper == null) {
            senzorsDbHelper = new SenzorsDbHelper(context.getApplicationContext());
        }
        return (senzorsDbHelper);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //Log.d(TAG, "OnCreate: creating db helper, db version - " + DATABASE_VERSION);
        //Log.d(TAG, SQL_CREATE_MINING_DETAIL);
        db.execSQL(SQL_CREATE_MINING_DETAIL);
        db.execSQL(SQL_CREATE_VERIFY_COINS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // enable foreign key constraint here
        //Log.d(TAG, "OnConfigure: Enable foreign key constraint");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(true);
        }
    }
}
