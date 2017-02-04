package scpp.globaleye.com.scppclient.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;


import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import scpp.globaleye.com.scppclient.ISenzService;
import scpp.globaleye.com.scppclient.R;
import scpp.globaleye.com.scppclient.db.SenzorsDbSource;
import scpp.globaleye.com.scppclient.utils.ActivityUtils;
import scpp.globaleye.com.scppclient.utils.AeSimpleSHA1;
import scpp.globaleye.com.scppclient.utils.NetworkUtil;
import scpp.globaleye.com.senzc.enums.enums.SenzTypeEnum;
import scpp.globaleye.com.senzc.enums.pojos.Senz;
import scpp.globaleye.com.senzc.enums.pojos.User;

public class MapsActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback,View.OnClickListener {


    private static final String TAG = BuyItemActivity.class.getName();


    GoogleMap googleMap;
    double start_lat,stop_lat,start_lng,stop_lng,lat,lng;
    Criteria criteria;
    LocationManager locationManager;
    String provider;
    Location location;
    Button start_btn,stop_btn;
    TextView tv;
    float[] distance = new float[1];


    // use to track share timeout
    private SenzCountDownTimer senzCountDownTimer;
    private boolean isResponseReceived;


    // custom font
    private Typeface typeface;
    private  String userName;
    private  double dis;



    // service interface
    private ISenzService senzService = null;
    private boolean isServiceBound = false;

    // service connection
    private ServiceConnection senzServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            //Log.d("TAG", "Connected with senz service");
            senzService = ISenzService.Stub.asInterface(service);

        }

        public void onServiceDisconnected(ComponentName className) {
            senzService = null;
            //Log.d("TAG", "Disconnected from senz service");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        senzCountDownTimer = new SenzCountDownTimer(15000, 5000);
        isResponseReceived = false;


        SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        fm.getMapAsync(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userName= extras.getString("USER_NAME");
        }

        init();
        bindConService();
    }


    private void init() {
        //////button
        start_btn = (Button) findViewById(R.id.button1);
        stop_btn = (Button) findViewById(R.id.button2);
        tv = (TextView) findViewById(R.id.textView);

        stop_btn.setEnabled(false);
        start_btn.setOnClickListener(this);
        stop_btn.setOnClickListener(this);

    }

    /**
     * {@inheritDoc}
     */
    public void bindConService() {

        // bind with senz service
        // bind to service from here as well
        Intent intent = new Intent();
        intent.setClassName("scpp.globaleye.com.scppclient", "scpp.globaleye.com.scppclient.services.RemoteSenzService");
        bindService(intent, senzServiceConnection, Context.BIND_AUTO_CREATE);
        isServiceBound=true;
        registerReceiver(senzMessageReceiver, new IntentFilter("scpp.globaleye.com.scppclient.PUT_SENZ"));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        //Log.d("unregister" , "call on destrot");
        super.onDestroy();
        if (isServiceBound) {
            unbindService(senzServiceConnection);
            //Log.d("unbind" , "call on destroy");
        }
        unregisterReceiver(senzMessageReceiver);
    }

    @Override
    public void onClick(View v) {
        if(v==start_btn){
            if (tv != null) {
                tv.setText("");
            }
            stop_btn.setEnabled(true);
            start_btn.setEnabled(false);
            getStarrtLoc(location);

        }else if(v==stop_btn){
            if (NetworkUtil.isAvailableNetwork(MapsActivity.this)) {
                stop_btn.setEnabled(false);
                getStoptLoc(location);
                ActivityUtils.showProgressDialog(MapsActivity.this, "Please wait...");
                senzCountDownTimer.start();
            } else {
                Toast.makeText(MapsActivity.this, "No network connection available", Toast.LENGTH_LONG).show();
            }
        }

    }

    @Override
    public void onLocationChanged(Location loc) {
        location = loc;
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public Location getLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //return;
            Toast.makeText(this,"NO PERMISSION", Toast.LENGTH_LONG).show();
        }
        // Enabling MyLocation Layer of Google Map
        googleMap.setMyLocationEnabled(true);

        // Getting LocationManager object from System Service LOCATION_SERVICE
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Creating a criteria object to retrieve provider
        criteria = new Criteria();

        // Getting the name of the best provider
        provider = locationManager.getBestProvider(criteria, true);

        // Getting Current Location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //return;
            Toast.makeText(this,"NO PERMISSION", Toast.LENGTH_LONG).show();
        }
        location = locationManager.getLastKnownLocation(provider);

        if(location!=null){
            Toast.makeText(this,"GPS FIXED", Toast.LENGTH_LONG).show();
            lat=location.getLatitude();
            lng=location.getLongitude();
        }
        locationManager.requestLocationUpdates(provider,0,0, this);
        return location;
    }

    public void getStarrtLoc(Location start_loc){
        start_lat = start_loc.getLatitude();
        start_lng = start_loc.getLongitude();

    }

    public void getStoptLoc(Location stop_loc){

        stop_lat = stop_loc.getLatitude();
        stop_lng = stop_loc.getLongitude();
        calcDistance();

        dis = distance[0]/1000;

        tv.setText("Distance :" + (distance[0] / 1000) + " Km");

    }

    public void calcDistance(){
        Location.distanceBetween(start_lat, start_lng, stop_lat, stop_lng, distance);
    }


    @Override
    public void onMapReady(GoogleMap map) {
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.setTrafficEnabled(true);
        map.setIndoorEnabled(true);
        map.setBuildingsEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        googleMap = map;
        getLocation();
    }

    /**
     * Keep track with share response timeout
     */
    private class SenzCountDownTimer extends CountDownTimer {

        public SenzCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            // if response not received yet, resend share
            if (!isResponseReceived) {
                miningCoin();
                //Log.d(TAG, "Response not received yet");
            }
        }

        @Override
        public void onFinish() {
            ActivityUtils.hideSoftKeyboard(MapsActivity.this);
            ActivityUtils.cancelProgressDialog();

            // display message dialog that we couldn't reach the user
            if (!isResponseReceived) {
                String message = "<font color=#000000>Seems we couldn't reach the SCPP Minner </font> <font color=#eada00>" + "<b>"+ "</font> <font color=#000000> at this moment</font>";
                displayInformationMessageDialog("#Request Fail", message);
            }
        }
    }

    /**
     *
     * SHARE #S_ID 2 #f cc #S_PARA 3km #COIN
     *
     *
     */
    private void miningCoin() {
        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("S_ID","1");
            senzAttributes.put("f","cc");
            senzAttributes.put("S_PARA",String.valueOf(dis));
            senzAttributes.put("COIN","COIN");
            senzAttributes.put("TIME", ((Long) (System.currentTimeMillis() / 1000)).toString());


            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.SHARE;
            User sender = new User("", userName);
            User receiver = new User("", "node3");
            //send quarry
            Senz senz = new Senz(id, signature, senzType,sender , receiver, senzAttributes);

            senzService.send(senz);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Display message dialog when user request(click) to delete invoice
     *
     * @param message message to be display
     */
    public void displayInformationMessageDialog(String title, String message) {
        final Dialog dialog = new Dialog(MapsActivity.this);

        //set layout for dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.information_message_dialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        // set dialog texts
        TextView messageHeaderTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_header_text);
        TextView messageTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_text);
        messageHeaderTextView.setText(title);
        messageTextView.setText(Html.fromHtml(message));

        // set custom font
        messageHeaderTextView.setTypeface(typeface);
        messageTextView.setTypeface(typeface);

        //set ok button
        Button okButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_ok_button);
        okButton.setTypeface(typeface);
        okButton.setTypeface(null, Typeface.BOLD);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        dialog.show();
    }


    //recived messege
    private BroadcastReceiver senzMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(TAG, "Got message from Senz service");
            handleMessage(intent);
        }
    };

    /**
     * Handle broadcast message receives
     * Need to handle share success failure here
     *
     * @param intent intent
     */
    private void handleMessage(Intent intent) {
        String action = intent.getAction();
        Senz senz = intent.getExtras().getParcelable("SENZ");

        if(action.equalsIgnoreCase("scpp.globaleye.com.scppclient.PUT_SENZ")) {
            boolean a =senz.getAttributes().containsKey("COIN");

            if (senz.getAttributes().containsKey("COIN")) {

                ActivityUtils.cancelProgressDialog();
                isResponseReceived = true;
                senzCountDownTimer.cancel();

                String cv = senz.getAttributes().get("COIN");
                String format_date = senz.getAttributes().get("TIME");
                String coin_para = String.valueOf(dis)+""+format_date+""+userName;

                try {
                    String s = AeSimpleSHA1.SHA1(coin_para);
                    if (cv.equals(s) && cv != null) {
                        onPostShare(senz);
                        sendResponse(senzService);
                    } else {
                        String message = "<font color=#000000>Seems we couldn't take coin contact with miners in this moment </font> <font color=#eada00>" + "<b>" + "</font>";
                        displayInformationMessageDialog("Coin Mining Fail", message);
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *
     *
     *
     * UNSHARE #S_ID #f #S_PARA  #COIN  @mysensors
     */
    private void sendResponse(ISenzService senzService) {
        Log.d(TAG, "send response");
        // create senz attributes
        try {
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("S_ID","S_ID");
            senzAttributes.put("f","f");
            senzAttributes.put("S_PARA","S_PARA");
            senzAttributes.put("COIN","COIN");
            String id = "_ID";
            String signature = "";
            SenzTypeEnum senzType = SenzTypeEnum.UNSHARE;
            User sender = new User("", userName);
            User receiver = new User("", "mysensors");
            //send quarry
            Senz senz = new Senz(id, signature, senzType,sender , receiver, senzAttributes);

            senzService.send(senz);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * Clear input fields and reset activity components
     */
    private void onPostShare(Senz senz) {

        isResponseReceived = false;
        String cv = senz.getAttributes().get("COIN");
        Toast.makeText(MapsActivity.this, "Recived New Coin " + cv, Toast.LENGTH_LONG).show();

        SenzorsDbSource dbSource = new SenzorsDbSource(MapsActivity.this);
        String dbState= dbSource.addCoin(cv,"2",userName ,"Ride " + tv.getText().toString());
        Toast.makeText(MapsActivity.this, dbState, Toast.LENGTH_LONG).show();
        start_btn.setEnabled(true);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(MapsActivity.this, ServicesView.class);
        intent.putExtra("USER_NAME", userName);
        MapsActivity.this.startActivity(intent);
        MapsActivity.this.finish();
    }

}
