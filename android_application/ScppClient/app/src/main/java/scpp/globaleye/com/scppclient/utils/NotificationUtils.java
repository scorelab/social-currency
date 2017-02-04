package scpp.globaleye.com.scppclient.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import scpp.globaleye.com.scppclient.R;
import scpp.globaleye.com.scppclient.ui.WalletInfo;

/**
 * Created by umayanga on 6/19/16.
 */
public class NotificationUtils {

    // notification Id
    public static final int SERVICE_NOTIFICATION_ID = 1;
    public static final int MESSAGE_NOTIFICATION_ID = 2;

    /**
     * Get notification to create/ update
     * We need to create or update notification in different scenarios
     *
     * @param context context
     * @return notification
     */
    public static Notification getNotification(Context context, int icon, String title, String message,String userName) {
        // set up pending intent
        Intent intent = new Intent(context, WalletInfo.class);
        //Log.d("check notify user name", userName);
        intent.putExtra("USER_NAME", userName);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(icon)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent);

        return builder.build();


    }

    /**
     * Display notification from here
     *
     * @param context context
     * @param title   notification title
     * @param message message to be display
     */
    public static void showNotification(Context context, String title, String message,String userName) {
        // display notification
        Notification notification = NotificationUtils.getNotification(context, R.drawable.notification, title, message ,userName);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NotificationUtils.MESSAGE_NOTIFICATION_ID, notification);
    }

    /**
     * Create and update notification when query receives from server
     * No we have two notifications regarding Sensor application
     *
     * @param message incoming query
     */
    public static void updateNotification(Context context, String message,String userName) {
        Notification notification = getNotification(context, R.drawable.logo_green,  context.getString(R.string.new_senz), message,userName);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(MESSAGE_NOTIFICATION_ID, notification);
    }

    /**
     * Cancel notification
     * need to cancel when disconnect from web socket
     */
    public static void cancelNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(MESSAGE_NOTIFICATION_ID);
        notificationManager.cancel(SERVICE_NOTIFICATION_ID);
    }
}
