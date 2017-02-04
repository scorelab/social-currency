package scpp.globaleye.com.scppclient.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import java.util.HashMap;

import scpp.globaleye.com.scppclient.ISenzService;
import scpp.globaleye.com.scppclient.R;
import scpp.globaleye.com.scppclient.exceptions.NoUserException;
import scpp.globaleye.com.scppclient.services.RemoteSenzService;
import scpp.globaleye.com.scppclient.utils.ActivityUtils;
import scpp.globaleye.com.scppclient.utils.PreferenceUtils;
import scpp.globaleye.com.scppclient.utils.RSAUtils;
import scpp.globaleye.com.senzc.enums.enums.SenzTypeEnum;
import scpp.globaleye.com.senzc.enums.pojos.Senz;
import scpp.globaleye.com.senzc.enums.pojos.User;

public class Login extends AppCompatActivity implements View.OnClickListener{

    private final int SPLASH_DISPLAY_LENGTH = 2000;

    private static final String TAG = Login.class.getName();

    private long backPressedTime = 0;
    private EditText loginTextUsername;
    private EditText passwordTextPasword;
    private Button loginButton;
    private Button registraionButton;


    private String username;
    private String password;
    private User user;

    // use to track registration timeout
    private SenzCountDownTimer senzCountDownTimer;
    private boolean isResponseReceived;

    // service interface
    private ISenzService senzService = null;
    private boolean isServiceBound = false;

    // service connection
    private ServiceConnection senzServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Connected with senz service");
            isServiceBound = true;
            senzService = ISenzService.Stub.asInterface(service);

            isResponseReceived = false;
            senzCountDownTimer.start();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d("TAG", "Disconnected from senz service");

            senzService = null;
            isServiceBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        senzCountDownTimer = new SenzCountDownTimer(30000, 5000); //16000
        initUi();
        registerReceiver(senzMessageReceiver, new IntentFilter("scpp.globaleye.com.scppclient.DATA_SENZ"));

    }

    /**
     * {@inheritDoc}
     */
    public void onClick(View v) {
        if (v == loginButton) {
            checkUserExit();
           //initNavigation();

            //Intent intent = new Intent(Login.this, Home.class);
            //Login.this.startActivity(intent);
            //Login.this.finish();

        }else if(v ==registraionButton){
            navigateToRegistration();
        }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound){
            unbindService(senzServiceConnection);
        }
        unregisterReceiver(senzMessageReceiver);
    }

    /**
     * Initialize UI components
     */
    private void initUi() {
        //change label font ...etc
        loginTextUsername = (EditText) findViewById(R.id.luserName_txt);
        passwordTextPasword = (EditText)findViewById(R.id.lpasswordTxt);
        loginButton = (Button)findViewById(R.id.lbutton);
        registraionButton=(Button)findViewById(R.id.lrButton);

        loginButton.setOnClickListener(Login.this);
        registraionButton.setOnClickListener(Login.this);

        initSenzService();

    }


    private void checkUserExit(){
        try {

            username = loginTextUsername.getText().toString().trim();
            password = passwordTextPasword.getText().toString().trim();

            user = PreferenceUtils.getUser(this, username, password);

            if(user.getPassword().equals(password)){
                Toast.makeText(this, "loading App", Toast.LENGTH_LONG).show();
                onClickLogin();
            }else{
                Toast.makeText(this, "Invalid Password", Toast.LENGTH_LONG).show();
            }

        } catch (NoUserException e) {
            // no user means navigate to login
            Toast.makeText(this, "Invalied User Name", Toast.LENGTH_LONG).show();
        }catch (NullPointerException e){
            Toast.makeText(this, "Please ,Register ", Toast.LENGTH_LONG).show();
        }



    }



    /**
     * Initialize senz service
     */
    private void initSenzService() {
        // start service from here
        Intent serviceIntent = new Intent(Login.this, RemoteSenzService.class);
        startService(serviceIntent);
    }



    /**
     * Sign-up button action,
     * create user and validate fields from here
     */
    private void onClickLogin() {
        ActivityUtils.hideSoftKeyboard(this);
        ActivityUtils.showProgressDialog(Login.this, "Please wait...");
        doPreLogin();

    }

    /**
     *
     * start service
     * bind service
     */
    private void doPreLogin() {
            if (!isServiceBound) {
                // bind to service from here as well
                Intent bindIntent = new Intent();
                bindIntent.setClassName("scpp.globaleye.com.scppclient", "scpp.globaleye.com.scppclient.services.RemoteSenzService");
                bindService(bindIntent, senzServiceConnection, Context.BIND_AUTO_CREATE);
            } else {
                // start to send senz to server form here
                isResponseReceived = false;
                senzCountDownTimer.start();
            }


    }



    /**
     * Create register senz
     * Send register senz to senz service via service binder
     */
    private void doLogin() {
        try {
            // first create create senz
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            senzAttributes.put("pubkey", PreferenceUtils.getRsaKey(this, RSAUtils.PUBLIC_KEY));

            // new senz
            String id = "_ID";
            String signature = "";
            SenzTypeEnum senzType = SenzTypeEnum.SHARE;
            User sender = new User("", loginTextUsername.getText().toString().trim());
            User receiver = new User("", "mysensors");
            Senz senz = new Senz(id, signature, senzType, sender, receiver, senzAttributes);
            senzService.send(senz);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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
                doLogin();
                Log.d(TAG, "Response not received yet");
            }
        }

        @Override
        public void onFinish() {
            ActivityUtils.hideSoftKeyboard(Login.this);
            ActivityUtils.cancelProgressDialog();

            // display message dialog that we couldn't reach the user
            if (!isResponseReceived) {
                Toast.makeText(Login.this, "Couldn't reach server this moment", Toast.LENGTH_LONG).show();
            }
        }

    }



    private BroadcastReceiver senzMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service" + intent.getAction());
            handleMessage(intent);
        }
    };


    /**
     * Handle broadcast message receives
     * Need to handle registration success failure here
     *
     * @param intent intent
     */
    private void handleMessage(Intent intent) {
        Senz senz = intent.getExtras().getParcelable("SENZ");

        Log.d(TAG, "Login broad cast handeler");
        if (senz != null && senz.getSenzType() == SenzTypeEnum.DATA) {
            if (senz.getAttributes().containsKey("msg")) {
                // msg response received
                ActivityUtils.cancelProgressDialog();
                isResponseReceived = true;
                senzCountDownTimer.cancel();

                String msg = senz.getAttributes().get("msg");
                if (msg != null && msg.equalsIgnoreCase("ALREADY_REGISTERED") || msg.equalsIgnoreCase("REGISTRATION_FAIL")) {
                    navigateToHome();
                } else {
                    Toast.makeText(this, "Please , Registers First", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Navigate to Register activity
     */
    private void navigateToRegistration() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Login.this, Registration.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Login.this.startActivity(intent);
                Login.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    /**
     * Switch to home activity
     * This method will be call after successful login
     */
    private void navigateToHome() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Login.this, Home.class);
                intent.putExtra("USER_NAME", username);
                Login.this.startActivity(intent);
                Login.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);

    }

    @Override
    public void onBackPressed() {        // to prevent irritating accidental exits
        long t = System.currentTimeMillis();
        if (t - backPressedTime > 2000) {    // 2 secs
            backPressedTime = t;
            Toast.makeText(this, "Press back again to exit",
                    Toast.LENGTH_SHORT).show();
        } else {
            // clean up
            super.onBackPressed();
            Login.this.finish();

        }
    }
}
