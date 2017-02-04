package scpp.globaleye.com.scppclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import scpp.globaleye.com.scppclient.R;
import scpp.globaleye.com.scppclient.exceptions.NoUserException;
import scpp.globaleye.com.senzc.enums.pojos.User;

/**
 * Created by umayanga on 6/15/16.
 */
public class PreferenceUtils {


    /**
     * Save user credentials in shared preference
     *
     * @param context application context
     * @param user    logged-in user
     */
    private static ArrayList<User> userDetail;
    private static User setUser;


    public static void saveUser(Context context, User user) {
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if (null == userDetail) {
            userDetail = new ArrayList<User>();
        }

        userDetail.add(user);
        editor.putString("User", new Gson().toJson(userDetail));
        editor.commit();
    }



    /**
     * Get user details from shared preference
     *
     * @param context application context
     * @return user object
     */
    public static User getUser(Context context) throws NoUserException {

        if(setUser!=null){
            return  setUser;
        }else {
            throw new NoUserException();
        }

    }



    /**
     * update user details from shared preference
     *
     * @param context application context
     * @return user object
     */
    public static User getUser(Context context ,String userName , String Password) throws NoUserException ,NullPointerException {

        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String userlist= preferences.getString("User","");

        if (!userName.equals(null)){
            Type collectionType = new TypeToken<List<User>>(){}.getType();
            userDetail = new Gson().fromJson(userlist, collectionType);
            for (User user : userDetail) {
                if(user.getUsername().equals(userName) && user.getPassword().equals(Password)){
                    setUser =user;
                    return user;
                }
            }
        }
        throw new NoUserException();
    }


    /**
     * update user details from shared preference
     *
     * @param context application context
     * @return user object
     */
    public static String updateUser(Context context ,String userName , String Password) {

        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String userlist= preferences.getString("User", "");

        if (!userName.equals(null)){
            Type collectionType = new TypeToken<List<User>>(){}.getType();
            userDetail = new Gson().fromJson(userlist, collectionType);
            for (User user : userDetail) {
                if(user.getUsername().equals(userName)){
                    SharedPreferences.Editor editor = preferences.edit();
                    userDetail.remove(user);
                    user =new User("0",userName,Password);

                    userDetail.add(user);
                    editor.putString("User", new Gson().toJson(userDetail));
                    editor.commit();

                    setUser =user;
                    return "update";
                }
            }
        }
        return "fail Update";
    }


    /**
     * Save public/private keys in shared preference,
     *
     * @param context application context
     * @param key     public/private keys(encoded key string)
     * @param keyType public_key, private_key, server_key
     *
     *                Context.MODE_MULTI_PROCESS ->change
     */
    public static void saveRsaKey(Context context, String key, String keyType) {
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_APPEND);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(keyType, key);
        editor.commit();
    }

    /**
     * Get saved RSA key string from shared preference
     *
     * @param context application context
     * @param keyType public_key, private_key, server_key
     * @return key string
     */
    public static String getRsaKey(Context context, String keyType) {
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        return preferences.getString(keyType, "");
    }
}
