package scpp.globaleye.com.scppclient.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

import scpp.globaleye.com.scppclient.ISenzService;
import scpp.globaleye.com.scppclient.R;
import scpp.globaleye.com.scppclient.utils.ActivityUtils;
import scpp.globaleye.com.scppclient.utils.NetworkUtil;
import scpp.globaleye.com.scppclient.utils.NotificationUtils;
import scpp.globaleye.com.senzc.enums.enums.SenzTypeEnum;
import scpp.globaleye.com.senzc.enums.pojos.Senz;
import scpp.globaleye.com.senzc.enums.pojos.User;

public class UserSelect extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = UserSelect.class.getName();

    private TextView usernameLabel;
    private EditText reciverEditText;
    private Button shareButton;

    // use to track share timeout
    private SenzCountDownTimer senzCountDownTimer;
    private boolean isResponseReceived;

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
        setContentView(R.layout.activity_user_select);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userName= extras.getString("USER_NAME");
        }

        senzCountDownTimer = new SenzCountDownTimer(15000, 5000);
        isResponseReceived = false;
        initUi();
        bindConService();

    }




    /**
     * Initialize UI components,
     * Set country code text
     * set custom font for UI fields
     */

    private void initUi() {
        usernameLabel  = (TextView)findViewById(R.id.share_lb);
        reciverEditText = (EditText) findViewById(R.id.share_user_name);
        shareButton = (Button) findViewById(R.id.shareBtn);
        shareButton.setOnClickListener(UserSelect.this);

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



    @Override
    public void onClick(View v) {
        if(v== shareButton){
            if (reciverEditText.getText().toString().trim().isEmpty()) {
                Toast.makeText(UserSelect.this, "Empty username", Toast.LENGTH_LONG).show();
            } else if (reciverEditText.getText().toString().trim().equals(userName)){
                Toast.makeText(UserSelect.this, "Can't Send Coin to Your self", Toast.LENGTH_LONG).show();
            }else {
                if (NetworkUtil.isAvailableNetwork(UserSelect.this)) {
                    ActivityUtils.showProgressDialog(UserSelect.this, "Please wait...");
                    senzCountDownTimer.start();
                } else {
                    Toast.makeText(UserSelect.this, "No network connection available", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    /**
     * Share current sensor
     * Need to send share query to server via web socket
     */
    private void share() {
        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("COIN", "COIN");
            senzAttributes.put("S_ID","S_ID");
            senzAttributes.put("S_LOCATION","S_LOCATION");
            senzAttributes.put("MSG", "MSG");
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());

            senzAttributes.put("f","ctr"); //flag - coin transaction record
            senzAttributes.put("SENDER",userName);
            senzAttributes.put("RECIVER",reciverEditText.getText().toString().trim());
            senzAttributes.put("T_NO_COIN","T_NO_COIN");
            //senzAttributes.put("PROB_VALUE","PROB_VALUE");


            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.SHARE;
            User sender = new User("", userName);
            User receiver = new User("", reciverEditText.getText().toString().trim());
            //User base = new User("", "baseNode");

            //send quarry
            Senz senz = new Senz(id, signature, senzType, sender , receiver, senzAttributes);
            senzService.send(senz);

            //send_base_transaction_request
            //Senz base_senz = new Senz(id, signature, senzType, sender ,base, senzAttributes);
            //senzService.send(base_senz);



            User node1 = new User("", "node1");
            User node3 = new User("", "node3");

            //Log.d("Reciver text" ,reciverEditText.getText().toString().trim().equals("node1")+"");

            if(reciverEditText.getText().toString().trim().equals("node1")){
                senzAttributes.put("f","ctr");
                Senz node1_senz = new Senz(id, signature, senzType, sender ,node3, senzAttributes);
                senzService.send(node1_senz);

            }else if(reciverEditText.getText().toString().trim().equals("node3")){
                senzAttributes.put("f","ctr");
                Senz node1_senz = new Senz(id, signature, senzType, sender ,node1, senzAttributes);
                senzService.send(node1_senz);

            }else{
                senzAttributes.put("f","b_ct"); //flag-send share request to miners
                Senz node1_senz = new Senz(id, signature, senzType, sender ,node1, senzAttributes);
                senzService.send(node1_senz);

                Senz node3_senz = new Senz(id, signature, senzType, sender ,node3, senzAttributes);
                senzService.send(node3_senz);

            }
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
                share();
                //Log.d(TAG, "Response not received yet");
            }
        }

        @Override
        public void onFinish() {
            ActivityUtils.hideSoftKeyboard(UserSelect.this);
            ActivityUtils.cancelProgressDialog();

            // display message dialog that we couldn't reach the user
            if (!isResponseReceived) {
                String user = reciverEditText.getText().toString().trim();
                String message = "<font color=#000000>Seems we couldn't reach the user </font> <font color=#eada00>" + "<b>" + user + "</b>" + "</font> <font color=#000000> at this moment</font>";
                displayInformationMessageDialog("#Share Fail", message);
            }

        }
    }



    /**
     * Display message dialog when user request(click) to delete invoice
     *
     * @param message message to be display
     */
    public void displayInformationMessageDialog(String title, String message) {
        final Dialog dialog = new Dialog(UserSelect.this);

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
        //Log.d(TAG ,"MY ACTION" +action);

        //Toast.makeText(UserSelect.this, "Handle masage", Toast.LENGTH_LONG).show();
        Senz senz = intent.getExtras().getParcelable("SENZ");
        if (action.equalsIgnoreCase("scpp.globaleye.com.scppclient.PUT_SENZ")) {


            if (senz.getAttributes().containsKey("MSG")) {
                //uma msg response received
                isResponseReceived = true;
                ActivityUtils.cancelProgressDialog();
                senzCountDownTimer.cancel();
                String msg = senz.getAttributes().get("MSG");
                //Log.d("response masage" , msg);
                //Toast.makeText(UserSelect.this, "Successfully shared SenZ"+msg, Toast.LENGTH_LONG).show();
                if (msg != null && msg.equalsIgnoreCase("ShareDone")) {
                    onPostShare(senz);
                } else {
                    String user = reciverEditText.getText().toString().trim();
                    String message = "<font color=#000000>Seems we couldn't share the senz with </font> <font color=#eada00>" + "<b>" + user + "</b>" + "</font>";
                    displayInformationMessageDialog("#Share Fail", message);
                }
            }
        }else{

            Log.d("Tag", senz.getSender() + " : " + senz.getSenzType().toString());
            if (senz != null && senz.getSenzType() == SenzTypeEnum.SHARE) {
                NotificationUtils.showNotification(this, this.getString(R.string.new_senz), "Coin accept request" + senz.getSender().getUsername(),userName);
                String sender = senz.getSender().getUsername();
                User sen = new User("", sender);
                sendResponse(senzService, sen, true);
            }

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




    /**
     * Clear input fields and reset activity components
     */
    private void onPostShare(Senz senz) {

        //navigate
        Intent intent = new Intent(UserSelect.this, SendCoinPeer.class);
        intent.putExtra("USER_NAME", userName);
        intent.putExtra("RECIVER" , reciverEditText.getText().toString().trim());

        //reciverEditText.setText("");
        Toast.makeText(UserSelect.this, "user request coins", Toast.LENGTH_LONG).show();
        UserSelect.this.startActivity(intent);
        UserSelect.this.finish();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(UserSelect.this, Home.class);
        intent.putExtra("USER_NAME", userName);
        UserSelect.this.startActivity(intent);
        UserSelect.this.finish();
     }

    public void goHome(View v) {
        Intent intent = new Intent(UserSelect.this, Home.class);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
        UserSelect.this.finish();
    }
    public void goBack(View v) {
        Intent intent = new Intent(UserSelect.this, Home.class);
        intent.putExtra("USER_NAME", userName);
        UserSelect.this.startActivity(intent);
        UserSelect.this.finish();
    }

    public void goManual(View v){
        Uri uri = Uri.parse("http://scpp.netne.net/");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
    }

    public void logout(View v) {
        Intent intent = new Intent(UserSelect.this, Login.class);
        UserSelect.this.startActivity(intent);
        UserSelect.this.finish();
    }
}
