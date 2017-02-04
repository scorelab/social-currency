package scpp.globaleye.com.scppclient.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;

import scpp.globaleye.com.scppclient.ISenzService;
import scpp.globaleye.com.scppclient.exceptions.NoUserException;
import scpp.globaleye.com.scppclient.handlers.SenzHandler;
import scpp.globaleye.com.scppclient.recivers.AlarmReceiver;
import scpp.globaleye.com.scppclient.utils.NetworkUtil;
import scpp.globaleye.com.scppclient.utils.PreferenceUtils;
import scpp.globaleye.com.scppclient.utils.RSAUtils;
import scpp.globaleye.com.scppclient.utils.SenzParser;
import scpp.globaleye.com.senzc.enums.enums.SenzTypeEnum;
import scpp.globaleye.com.senzc.enums.pojos.Senz;
import scpp.globaleye.com.senzc.enums.pojos.User;


public class RemoteSenzService extends Service {


    private static final String TAG = RemoteSenzService.class.getName();

    // senz service host and port
    private static final String SENZ_HOST = "10.0.3.2";//genymotion emulator
    //private static final String SENZ_HOST = "192.168.1.2"; //localmachine port -real device
    //private static final String SENZ_HOST = "10.0.2.2"; //AVD manager emulator
    private static final int SENZ_PORT = 9090;

    // we are listing for UDP socket
    private DatagramSocket socket;

    // server address
    private InetAddress address;

    private String userName;
    private String password;

    // broadcast receiver to check network status changes
    private final BroadcastReceiver networkStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();

            //Log.d(TAG, "Network status changed");

            //should check null because in air plan mode it will be null
            if (netInfo != null && netInfo.isConnected()) {
                // send ping from here
                initUdpSender();
            }
        }
    };

    // broadcast receiver to send ping message
    private final BroadcastReceiver pingAlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(TAG, "Ping alarm received in senz service");
            sendPingMessage();
        }
    };

    // API end point of this service, we expose the endpoints define in ISenzService.aidl
    private final ISenzService.Stub apiEndPoints = new ISenzService.Stub() {

        @Override
        public void send(Senz senz) throws RemoteException {
            //Log.d(TAG, "Senz service call with senz " + senz.getId());
            sendSenzMessage(senz);
        }


        public String getUser() throws RemoteException {
            try {
                return PreferenceUtils.getUser(RemoteSenzService.this).getUsername();
            } catch (NoUserException e) {
                //e.printStackTrace();
                return null;
            }
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return   apiEndPoints;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        // Register network status receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStatusReceiver, filter);

        // register ping alarm receiver
        IntentFilter alarmFilter = new IntentFilter();
        alarmFilter.addAction("PING_ALARM");
        registerReceiver(pingAlarmReceiver, alarmFilter);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d(TAG, "Start service");
        initUdpSocket();
        initUdpListener();

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        //Log.d(TAG, "Destroyed");

        // unregister connectivity listener
        unregisterReceiver(networkStatusReceiver);

        // un register ping receiver
        unregisterReceiver(pingAlarmReceiver);

        // restart service again
        // its done via broadcast receiver
        Intent intent = new Intent("scpp.globaleye.com.scppclient.RESTART_SERVICE");
        sendBroadcast(intent);
    }

    /**
     * Initialize/Create UDP socket
     */
    private void initUdpSocket() {
        if (socket == null || socket.isClosed()) {
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Socket already initialized");
        }
    }

    /**
     * Start thread to send initial PING messages to server
     * We initialize UDP connection from here
     */
    private void initUdpSender() {
        if (socket != null) {
            new Thread(new Runnable() {
                public void run() {
                    sendPingMessage();
                }
            }).start();
        } else {
            Log.i(TAG, "Socket not connected");
        }
    }

    /**
     * Start thread to send PING message to server in every 28 minutes
     */
    private void initPingSender() {
        // register ping alarm receiver
        Intent intentAlarm = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intentAlarm, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
    }

    /**
     * Start thread for listen to UDP socket, all the incoming messages receives from
     * here, when message receives it should be broadcast or delegate to appropriate message
     * handler
     */
    private void initUdpListener() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] message = new byte[512];

                    while (true) {
                        // listen for senz
                        DatagramPacket receivePacket = new DatagramPacket(message, message.length);
                        socket.receive(receivePacket);
                        String senz = new String(message, 0, receivePacket.getLength());

                        //Log.d(TAG, "SenZ received: ");

                        SenzHandler.getInstance(RemoteSenzService.this).handleSenz(senz);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Send ping message to server
     */
    private void sendPingMessage() {
        new Thread(new Runnable() {
            public void run() {
                if (NetworkUtil.isAvailableNetwork(RemoteSenzService.this)) {
                    try {
                        String message;
                        PrivateKey privateKey = RSAUtils.getPrivateKey(RemoteSenzService.this);
                        User user = PreferenceUtils.getUser(RemoteSenzService.this);

                        // create senz attributes
                        HashMap<String, String> senzAttributes = new HashMap<>();
                        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());

                        // new senz object
                        Senz senz = new Senz();
                        senz.setSenzType(SenzTypeEnum.DATA);
                        senz.setSender(new User("", user.getUsername()));
                        senz.setReceiver(new User("", "mysensors"));
                        senz.setAttributes(senzAttributes);

                        // get digital signature of the senz
                        String senzPayload = SenzParser.getSenzPayload(senz);
                        String senzSignature = RSAUtils.getDigitalSignature(senzPayload.replaceAll(" ", ""), privateKey);
                        message = SenzParser.getSenzMessage(senzPayload, senzSignature);

                        //Log.d(TAG, "Ping to be send: " + message);

                        // send message
                        if (address == null) address = InetAddress.getByName(SENZ_HOST);
                        DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), address, SENZ_PORT);
                        socket.send(sendPacket);
                    } catch (IOException | NoSuchAlgorithmException | NoUserException | SignatureException | InvalidKeyException | InvalidKeySpecException e) {
                        //e.printStackTrace();
                        Log.e(TAG, "Wait For Registration" + e);
                    }
                } else {
                    Log.e(TAG, "Cannot send ping, No connection available");
                }
            }
        }).start();
    }

    /**
     * Send SenZ message to SenZ service via UDP socket
     * Separate thread used to send messages asynchronously
     *
     * @param senz senz message
     */
    public void sendSenzMessage(final Senz senz) {
        if (NetworkUtil.isAvailableNetwork(RemoteSenzService.this)) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        PrivateKey privateKey = RSAUtils.getPrivateKey(RemoteSenzService.this);

                        // if sender not already set find user(sender) and set it to senz first
                        if (senz.getSender() == null)
                            senz.setSender(PreferenceUtils.getUser(getApplicationContext()));

                        // get digital signature of the senz
                        String senzPayload = SenzParser.getSenzPayload(senz);
                        String senzSignature = RSAUtils.getDigitalSignature(senzPayload.replaceAll(" ", ""), privateKey);
                        String message = SenzParser.getSenzMessage(senzPayload, senzSignature);

                        //Log.d(TAG, "Senz to be send: " + message);

                        // send message
                        if (address == null) address = InetAddress.getByName(SENZ_HOST);
                        DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), address, SENZ_PORT);
                        socket.send(sendPacket);
                    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException | NoUserException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            Log.i(TAG, "Cannot send senz, No connection available");
        }
    }

}
