package scpp.globaleye.com.scppclient.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.text.SimpleDateFormat;
import java.util.Calendar;



/**
 * Created by umayanga on 6/23/16.
 */
public class SenzorsDbSource {

    private static final String TAG = SenzorsDbSource.class.getName();
    private static Context context;
    private SQLiteDatabase db;
    public static final String[] ALL_KEYS = new String[] {SenzorsDbContract.WalletCoins._ID,
            SenzorsDbContract.WalletCoins.COLUMN_NAME_COIN,SenzorsDbContract.WalletCoins.COLUMN_NAME_S_ID,
            SenzorsDbContract.WalletCoins.COLUMN_NAME_S_LOCATION,
            SenzorsDbContract.WalletCoins.COLUMN_NAME_TIME};


    public static final String[] Verify_ALL_KEYS = new String[] {SenzorsDbContract.VerifyCoins._ID,
            SenzorsDbContract.VerifyCoins.COLUMN_NAME_COIN,
            SenzorsDbContract.VerifyCoins.COLUMN_NAME_SPARA,
            SenzorsDbContract.VerifyCoins.COLUMN_NAME_GENERETED_DATE,
            SenzorsDbContract.VerifyCoins.COLUMN_NAME_S_ID,
            SenzorsDbContract.VerifyCoins.COLUMN_NAME_CREATER,
            SenzorsDbContract.VerifyCoins.COLUMN_NAME_VerifyState};



    /**
     * Init db helper
     *
     * @param context application context
     */
    public SenzorsDbSource(Context context) {
        //Log.d(TAG, "Init: db source");
        this.context = context;
    }



    /**
     * Check Coin if exists in the database, other wise store a coin in db
     *
     * @param
     * @return Senz
     */
    public String addCoin(String coin,String s_id,String userName,String s_location) {

        // get matching user if exists
        db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        Cursor cursor = db.query(SenzorsDbContract.WalletCoins.TABLE_NAME, // table
                null, SenzorsDbContract.WalletCoins.COLUMN_NAME_COIN + "=?", // constraint
                new String[]{coin}, // prams
                null, // order by
                null, // group by
                null); // join

        if (cursor.moveToFirst()) {
            // have matching user
            // so get user data
            // we return id as password since we no storing users password in database
            String _id = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.WalletCoins._ID));
            String _coin = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.WalletCoins.COLUMN_NAME_COIN));
            String _time = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.WalletCoins.COLUMN_NAME_TIME));
            String _location = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.WalletCoins.COLUMN_NAME_S_LOCATION));
            String _user_name = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.WalletCoins.COLUMN_NAME_USER_NAME));


            // clear
            cursor.close();
            db.close();

            return "Coin Already Exist in Wallet";
        } else {
            // no matching user
            // so create user
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateFormat = df.format(c.getTime());

            ContentValues values = new ContentValues();
            values.put(SenzorsDbContract.WalletCoins.COLUMN_NAME_COIN, coin);
            values.put(SenzorsDbContract.WalletCoins.COLUMN_NAME_S_ID, s_id);
            values.put(SenzorsDbContract.WalletCoins.COLUMN_NAME_S_LOCATION,s_location);
            values.put(SenzorsDbContract.WalletCoins.COLUMN_NAME_TIME, dateFormat);
            values.put(SenzorsDbContract.WalletCoins.COLUMN_NAME_USER_NAME, userName);

            // inset data
            long id = db.insert(SenzorsDbContract.WalletCoins.TABLE_NAME, SenzorsDbContract.WalletCoins.COLUMN_NAME_COIN, values);
            cursor.close();
            db.close();
            return "Save Coin To wallet Successfully";
        }
    }


    public Cursor getAllMiningDteail(String userName){
        // get matching user if exists
        db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        String where = SenzorsDbContract.WalletCoins.COLUMN_NAME_USER_NAME + "=?";

        Cursor cursor = db.query(SenzorsDbContract.WalletCoins.TABLE_NAME, ALL_KEYS, where, new String[] { userName }, null, null, null);

        return cursor;
    }



    // Get a specific row (by rowId)
    public Cursor getMiningRow(long rowId) {
        db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        String where = SenzorsDbContract.WalletCoins._ID + "=" + rowId;
        Cursor c = 	db.query(true, SenzorsDbContract.WalletCoins.TABLE_NAME, ALL_KEYS,
                where, null, null, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }


    // Delete a row from the database, by rowId (primary key)
    public boolean deleteRow(long rowId) {
        db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        String where = SenzorsDbContract.WalletCoins._ID + "=" + rowId;
        return db.delete(SenzorsDbContract.WalletCoins.TABLE_NAME, where, null) != 0;

    }


    /**
     *
     * not used this function but we corded it to be future used.
     * @param rowId
     * @param coin
     * @param s_id
     * @return
     */
     public boolean updateRow(long rowId, String coin,String s_id , String s_location) {
         db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
         String where = SenzorsDbContract.WalletCoins._ID + "=" + rowId;

         Calendar c = Calendar.getInstance();
         SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         String dateFormat = df.format(c.getTime());

         ContentValues newValues = new ContentValues();
         newValues.put(SenzorsDbContract.WalletCoins.COLUMN_NAME_COIN, coin);
         newValues.put(SenzorsDbContract.WalletCoins.COLUMN_NAME_S_ID, s_id);
         newValues.put(SenzorsDbContract.WalletCoins.COLUMN_NAME_S_LOCATION, s_location);
         newValues.put(SenzorsDbContract.WalletCoins.COLUMN_NAME_TIME, dateFormat);

         // Insert it into the database.
         return db.update(SenzorsDbContract.WalletCoins.TABLE_NAME, newValues, where, null) != 0;
    }


    /**
     * Check Coin verification before add coin to wallet
     *
     * @param
     * @return Senz
     */
    public String addVerifyCoin(String coin,String s_para,String s_id,String create_date,String userName,String state) {

        // get matching user if exists
        db = SenzorsDbHelper.getInstance(context).getWritableDatabase();
        Cursor cursor = db.query(SenzorsDbContract.VerifyCoins.TABLE_NAME, // table
                null, SenzorsDbContract.VerifyCoins.COLUMN_NAME_COIN + "=?", // constraint
                new String[]{coin}, // prams
                null, // order by
                null, // group by
                null); // join

        if (cursor.moveToFirst()) {
            // have matching user
            // so get user data
            // we return id as password since we no storing users password in database
            String _id = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.VerifyCoins._ID));
            String _coin = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.VerifyCoins.COLUMN_NAME_COIN));
            String _para = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.VerifyCoins.COLUMN_NAME_SPARA));
            String _s_id = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.VerifyCoins.COLUMN_NAME_S_ID));
            String _generated_date = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.VerifyCoins.COLUMN_NAME_GENERETED_DATE));
            String _creator = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.VerifyCoins.COLUMN_NAME_CREATER));
            String _state = cursor.getString(cursor.getColumnIndex(SenzorsDbContract.VerifyCoins.COLUMN_NAME_VerifyState));


            // clear
            cursor.close();
            db.close();
            return "Coin Already Already Verified";

        } else {
            // no matching details

            ContentValues values = new ContentValues();
            values.put(SenzorsDbContract.VerifyCoins.COLUMN_NAME_COIN, coin);
            values.put(SenzorsDbContract.VerifyCoins.COLUMN_NAME_SPARA, s_para);
            values.put(SenzorsDbContract.VerifyCoins.COLUMN_NAME_S_ID, s_id);
            values.put(SenzorsDbContract.VerifyCoins.COLUMN_NAME_GENERETED_DATE,create_date);
            values.put(SenzorsDbContract.VerifyCoins.COLUMN_NAME_CREATER, userName);
            values.put(SenzorsDbContract.VerifyCoins.COLUMN_NAME_VerifyState, state);

            // inset data
            long id = db.insert(SenzorsDbContract.VerifyCoins.TABLE_NAME, SenzorsDbContract.VerifyCoins.COLUMN_NAME_COIN, values);
            cursor.close();
            db.close();
            return "Save Verified Successfully";
        }
    }


}
