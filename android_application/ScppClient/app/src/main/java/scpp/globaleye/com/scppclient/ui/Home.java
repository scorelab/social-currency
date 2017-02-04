package scpp.globaleye.com.scppclient.ui;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.HashMap;

import scpp.globaleye.com.scppclient.ISenzService;
import scpp.globaleye.com.scppclient.R;
import scpp.globaleye.com.scppclient.utils.NotificationUtils;
import scpp.globaleye.com.senzc.enums.enums.SenzTypeEnum;
import scpp.globaleye.com.senzc.enums.pojos.Senz;
import scpp.globaleye.com.senzc.enums.pojos.User;

/**
 * Created by umayanga on 8/11/16.
 */
public class Home extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = BuyItemActivity.class.getName();

    private final int SPLASH_DISPLAY_LENGTH = 2000;
    private long backPressedTime = 0;
    private ImageButton profileimgButton;
    private ImageButton transctionimgButton;
    private ImageButton walletimgButton;
    private ImageButton serviceimgButton;


    // custom font
    private Typeface typeface;
    private String userName;

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
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userName= extras.getString("USER_NAME");
        }

        initUi();
        bindConService();

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
        registerReceiver(senzMessageReceiver, new IntentFilter("scpp.globaleye.com.scppclient.SHARE_SENZ"));

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            unbindService(senzServiceConnection);
        }
        unregisterReceiver(senzMessageReceiver);
    }


    private void initUi() {
        profileimgButton = (ImageButton) findViewById(R.id.imageButtonProfile);
        transctionimgButton = (ImageButton) findViewById(R.id.imageButtontransaction);
        walletimgButton = (ImageButton) findViewById(R.id.imageButtonwallet);
        serviceimgButton = (ImageButton) findViewById(R.id.imageButtonService);

        profileimgButton.setOnClickListener(Home.this);
        transctionimgButton.setOnClickListener(Home.this);
        walletimgButton.setOnClickListener(Home.this);
        serviceimgButton.setOnClickListener(Home.this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }


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
        //Log.d("Tag", senz.getSender() + " : " + senz.getSenzType().toString());
        if (senz != null && senz.getSenzType() == SenzTypeEnum.SHARE) {
            NotificationUtils.showNotification(this, this.getString(R.string.new_senz), "Coin accept request from" + senz.getSender().getUsername(), userName);
            String sender = senz.getSender().getUsername();
            User sen = new User("", sender);
            sendResponse(senzService, sen, true);
        }

    }


    private void sendResponse(ISenzService senzService, User receiver, boolean isDone) {
        //Log.d(TAG, "send response");
        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            if (isDone){
                senzAttributes.put("MSG", "ShareDone");
            }else{
                senzAttributes.put("MSG", "ShareFail");
            }

            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.PUT;
            User sender = new User("", userName);
            Senz senz = new Senz(id, signature, senzType, sender, receiver, senzAttributes);
            senzService.send(senz);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        if (v == profileimgButton) {
            navigateToPrfileUpdateView();
        } else if (v == transctionimgButton) {
            navigateToTransaction();
        } else if (v == walletimgButton) {
            navigateToWallte();
        } else if (v == serviceimgButton) {
            navgateToServicesListView();
        }

    }

    /**
     * Home page navigation functions
     *
     */
    private void navigateToTransaction() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Home.this, UserSelect.class);
                intent.putExtra("USER_NAME", userName);
                Home.this.startActivity(intent);
                Home.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    private void navigateToPrfileUpdateView() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Home.this, UpdateProfile.class);
                intent.putExtra("USER_NAME", userName);
                Home.this.startActivity(intent);
                Home.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);

    }


    private void navgateToServicesListView() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Home.this, ServicesView.class);
                intent.putExtra("USER_NAME", userName);
                Home.this.startActivity(intent);
                Home.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    private void navigateToWallte() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Home.this,WalletInfo.class);
                intent.putExtra("USER_NAME", userName);
                Home.this.startActivity(intent);
                Home.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }



    public void logout(View v) {
        Intent intent = new Intent(Home.this, Login.class);
        intent.putExtra("USER_NAME", "");
        Home.this.startActivity(intent);
        Home.this.finish();
    }


    public void goHome(View v) {

    }

    public void goManual(View v){
        Uri uri = Uri.parse("http://scpp.netne.net/");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
    }

    public void goBack(View v) {

    }


    @Override
    public void onBackPressed() {        // to prevent irritating accidental logout
        long t = System.currentTimeMillis();
        if (t - backPressedTime > 2000) {    // 2 secs
            backPressedTime = t;
            Toast.makeText(this, "Press back again to logout",
                    Toast.LENGTH_SHORT).show();

        } else {    // this guy is serious
            // clean up
            Home.this.finish();
            super.onBackPressed();       // bye
        }
    }


}
