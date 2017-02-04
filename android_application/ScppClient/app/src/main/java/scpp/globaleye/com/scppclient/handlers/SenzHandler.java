package scpp.globaleye.com.scppclient.handlers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import scpp.globaleye.com.scppclient.utils.SenzParser;
import scpp.globaleye.com.senzc.enums.pojos.Senz;

/**
 * Created by umayanga on 6/15/16.
 * protocol handler main class
 */
public class SenzHandler {

    private static final String TAG = SenzHandler.class.getName();

    private static Context context;

    private static SenzHandler instance;

    private SenzHandler() {
    }

    public static SenzHandler getInstance(Context context) {
        if (instance == null) {
            instance = new SenzHandler();
            SenzHandler.context = context;
        }

        return instance;
    }

    /**
     *
     * @param senzMessage
     * This method using to take protocol request and according to request type ,it
     * switch protocol to relevant method.
     */
    public void handleSenz(String senzMessage) {
        // parse and verify senz
        Senz senz = SenzParser.parse(senzMessage);
        //Log.d(TAG,"Masage"+senzMessage);
        switch (senz.getSenzType()) {
            case PING:
                //Log.d(TAG, "PING received");
                broadcastSenz(senz, new Intent("scpp.globaleye.com.scppclient.PING_SENZ"));
                break;
            case SHARE:
                //Log.d(TAG, "SHARE received");
                broadcastSenz(senz, new Intent("scpp.globaleye.com.scppclient.SHARE_SENZ"));
                break;
            case GET:
                //Log.d(TAG, "GET received");
                broadcastSenz(senz, new Intent("scpp.globaleye.com.scppclient.GET_SENZ"));
                break;
            case DATA:
                //Log.d(TAG, "DATA received");
                broadcastSenz(senz, new Intent("scpp.globaleye.com.scppclient.DATA_SENZ"));
                break;
            case PUT:
                //Log.d(TAG, "PUT received");
                broadcastSenz(senz, new Intent("scpp.globaleye.com.scppclient.PUT_SENZ"));
                break;
            case UNSHARE:
                //Log.d(TAG, "UNSHARE received");
                broadcastSenz(senz, new Intent("scpp.globaleye.com.scppclient.UNSHARE_SENZ"));
                break;
        }
    }

    /**
     *
     * Broadcast receiving protocol to relevant interface.
     * @param senz
     * @param intent
     */
    private void broadcastSenz(Senz senz, Intent intent) {
        intent.putExtra("SENZ", senz);
        Log.d(TAG,"brodcast" + senz.getAttributes());
        context.sendBroadcast(intent);
    }

}
