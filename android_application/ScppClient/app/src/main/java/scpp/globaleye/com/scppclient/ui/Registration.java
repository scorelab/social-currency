package scpp.globaleye.com.scppclient.ui;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import scpp.globaleye.com.scppclient.R;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;

import scpp.globaleye.com.scppclient.ISenzService;
import scpp.globaleye.com.scppclient.exceptions.InvalidInputFieldsException;
import scpp.globaleye.com.scppclient.utils.ActivityUtils;
import scpp.globaleye.com.scppclient.utils.NetworkUtil;
import scpp.globaleye.com.scppclient.utils.PreferenceUtils;
import scpp.globaleye.com.scppclient.utils.RSAUtils;
import scpp.globaleye.com.senzc.enums.enums.SenzTypeEnum;
import scpp.globaleye.com.senzc.enums.pojos.Senz;
import scpp.globaleye.com.senzc.enums.pojos.User;

public class Registration extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = Registration.class.getName();

    // registration deal with User object
    private User registeringUser;

    // UI fields
    private EditText editTextUsername;
    private EditText editTextPasword;
    private EditText editTextConfrimPasword;
    private Button signUpButton;
    private Typeface typeface;

    // use to track registration timeout
    private SenzCountDownTimer senzCountDownTimer;
    private boolean isResponseReceived;

    // service interface
    private ISenzService senzService = null;
    private boolean isServiceBound = false;

    // service connection
    private ServiceConnection senzServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            //Log.d(TAG, "Connected with senz service");
            isServiceBound = true;
            senzService = ISenzService.Stub.asInterface(service);
            isResponseReceived = false;
            senzCountDownTimer.start();
        }

        public void onServiceDisconnected(ComponentName className) {
            //Log.d("TAG", "Disconnected from senz service");
            senzService = null;
            isServiceBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        typeface = Typeface.createFromAsset(getAssets(), "fonts/vegur_2.otf");
        senzCountDownTimer = new SenzCountDownTimer(20000, 5000); //16000
        initUi();
        registerReceiver(senzMessageReceiver, new IntentFilter("scpp.globaleye.com.scppclient.DATA_SENZ"));
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
     * Initialize UI components,
     * Set country code text
     * set custom font for UI fields
     */

    private void initUi() {
        editTextUsername = (EditText) findViewById(R.id.tfUpdateUserName);
        editTextPasword = (EditText)findViewById(R.id.etUpdatpasswordtxt);
        editTextConfrimPasword =(EditText)findViewById(R.id.etUPConfirmPasswordeditText);
        signUpButton = (Button) findViewById(R.id.rbutton);
        signUpButton.setOnClickListener(Registration.this);
        editTextUsername.setTypeface(typeface, Typeface.NORMAL);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        if (v == signUpButton) {
            if (NetworkUtil.isAvailableNetwork(this)) {
                onClickRegister();
            } else {
                Toast.makeText(this, "No network connection available", Toast.LENGTH_LONG).show();
            }
        }

    }

    /**
     * Sign-up button action,
     * create user and validate fields from here
     */
    private void onClickRegister() {
        ActivityUtils.hideSoftKeyboard(this);
        // crate user
        String username = editTextUsername.getText().toString().trim();
        String password =editTextPasword.getText().toString().trim();
        String confirmpassword =editTextConfrimPasword.getText().toString().trim();

        if(confirmpassword.equals(password)){
            registeringUser = new User("0", username ,password);
            try {
                ActivityUtils.isValidRegistrationFields(registeringUser);
                String confirmationMessage = "<font color=#000000>Are you sure you want to register on SenZ with </font> <font color=#306d97>" + "<b>" + registeringUser.getUsername() + "</b>" + "</font>";
                displayConfirmationMessageDialog(confirmationMessage);
            } catch (InvalidInputFieldsException e) {
                Toast.makeText(this, "Invalid username", Toast.LENGTH_LONG).show();
                //e.printStackTrace();
            }
        }else{
            Toast.makeText(this, "password and confirm password not equal", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Create user
     * First initialize key pair
     * start service
     * bind service
     */
    private void doPreRegistration() {

        try{
            if (!isServiceBound) {
                // init keys
                RSAUtils.initKeys(this);

                // bind to service from here as well
                Intent bindIntent = new Intent();
                bindIntent.setClassName("scpp.globaleye.com.scppclient", "scpp.globaleye.com.scppclient.services.RemoteSenzService");
                bindService(bindIntent, senzServiceConnection, Context.BIND_AUTO_CREATE);
            } else {
                // start to send senz to server form here
                isResponseReceived = false;
                senzCountDownTimer.start();
            }
        }catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create register senz
     * Send register senz to senz service via service binder
     */
    private void doRegistration() {
        try {
            // first create create senz
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            senzAttributes.put("pubkey", PreferenceUtils.getRsaKey(this, RSAUtils.PUBLIC_KEY));

            // new senz
            String id = "_ID";
            String signature = "";
            SenzTypeEnum senzType = SenzTypeEnum.SHARE;
            User sender = new User("", registeringUser.getUsername());
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
                doRegistration();
                //Log.d(TAG, "Response not received yet");
            }
        }

        @Override
        public void onFinish() {
            ActivityUtils.hideSoftKeyboard(Registration.this);
            ActivityUtils.cancelProgressDialog();

            // display message dialog that we couldn't reach the user
            if (!isResponseReceived) {
                String message = "<font color=#000000>Seems we couldn't reach the senz service at this moment</font>";
                displayInformationMessageDialog("#Registration Fail", message);
            }
        }

    }



    private BroadcastReceiver senzMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
                //Log.d(TAG, "Got message from Senz service" + intent.getAction());
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

        Log.d(TAG, "Register broad cast handeler");
        if (senz != null && senz.getSenzType() == SenzTypeEnum.DATA) {
            if (senz.getAttributes().containsKey("msg")) {
                // msg response received
                ActivityUtils.cancelProgressDialog();
                isResponseReceived = true;
                senzCountDownTimer.cancel();

                String msg = senz.getAttributes().get("msg");
                if (msg != null && msg.equalsIgnoreCase("REGISTRATION_DONE")) {
                    Toast.makeText(this, "Successfully registered", Toast.LENGTH_LONG).show();

                    // init keys
                    // save user
                    // navigate home

                    PreferenceUtils.saveUser(getApplicationContext(), registeringUser);
                    navigateToLogin();
                } else {
                    String informationMessage = "<font color=#4a4a4a>Seems username </font> <font color=#eada00>" + "<b>" + registeringUser.getUsername() + "</b>" + "</font> <font color=#4a4a4a> already obtained by some other user, try SenZ with different username</font>";
                    displayInformationMessageDialog("Registration fail", informationMessage);
                }
            }
        }
    }

    /**
     * Switch to home activity
     * This method will be call after successful login
     */
    private void navigateToLogin() {
        Intent intent = new Intent(Registration.this, Login.class);
        Registration.this.startActivity(intent);
        Registration.this.finish();
    }

    /**
     * Display message dialog with registration status
     *
     * @param message message to be display
     */
    public void displayInformationMessageDialog(String title, String message) {
        final Dialog dialog = new Dialog(Registration.this);

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
        //messageHeaderTextView.setTypeface(typeface);
       // messageTextView.setTypeface(typeface);

        //set ok buttons
        Button okButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_ok_button);
       // okButton.setTypeface(typeface);
        okButton.setTypeface(null, Typeface.BOLD);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }


    /**
     * Display message dialog when user request(click) to register
     *
     * @param message message to be display
     */
    public void displayConfirmationMessageDialog(String message) {
        final Dialog dialog = new Dialog(Registration.this);

        //set layout for dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.share_confirm_message_dialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        // set dialog texts
        TextView messageHeaderTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_header_text);
        TextView messageTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_text);
        messageHeaderTextView.setText("Confirm username");
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
                ActivityUtils.showProgressDialog(Registration.this, "Please wait...");
                doPreRegistration();
            }
        });

        // cancel button
        Button cancelButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_cancel_button);
        cancelButton.setTypeface(typeface);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        dialog.show();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Registration.this, Login.class);
        Registration.this.startActivity(intent);
        Registration.this.finish();
    }
}
