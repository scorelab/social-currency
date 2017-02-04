package scpp.globaleye.com.scppclient.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by umayanga on 6/15/16.
 */
public class NetworkUtil {

    /**
     * Check network connection availability
     * @param context need to access ConnectivityManager
     * @return availability of network
     */
    public static boolean isAvailableNetwork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }
}
