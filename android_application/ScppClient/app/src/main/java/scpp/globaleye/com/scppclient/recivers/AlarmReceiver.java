package scpp.globaleye.com.scppclient.recivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by umayanga on 6/15/16.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.d(TAG, "Alarm received");
        Intent alarmIntent = new Intent("PING_ALARM");
        context.sendBroadcast(alarmIntent);
    }
}
