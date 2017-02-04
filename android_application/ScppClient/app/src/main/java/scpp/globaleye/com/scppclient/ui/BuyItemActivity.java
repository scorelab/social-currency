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
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


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

public class BuyItemActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {


    private static final String TAG = BuyItemActivity.class.getName();

    private Button btbuyItem;
    private EditText etBillId;
    private EditText etBillAmount;
    private Spinner shopNameSpiner;

    // use to track share timeout
    private SenzCountDownTimer senzCountDownTimer;
    private boolean isResponseReceived;

    // custom font
    private Typeface typeface;
    private  String userName;

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
        setContentView(R.layout.activity_buy_item);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        senzCountDownTimer = new SenzCountDownTimer(15000, 5000);
        isResponseReceived = false;


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

    private void initUi() {
        btbuyItem = (Button) findViewById(R.id.btBuyItem);
        etBillId = (EditText) findViewById(R.id.etBillNo);
        etBillAmount = (EditText) findViewById(R.id.etBillAount);
        shopNameSpiner = (Spinner) findViewById(R.id.shop_name_spinner);

        shopNameSpiner.setOnItemSelectedListener(BuyItemActivity.this);
        btbuyItem.setOnClickListener(BuyItemActivity.this);

    }

    @Override
    public void onClick(View v) {
        if (v == btbuyItem) {
            if (NetworkUtil.isAvailableNetwork(BuyItemActivity.this)) {
                btbuyItem.setEnabled(false);
                ActivityUtils.showProgressDialog(BuyItemActivity.this, "Please wait...");
                senzCountDownTimer.start();

            } else {
                Toast.makeText(BuyItemActivity.this, "No network connection available", Toast.LENGTH_LONG).show();
            }
        }

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(parent.getContext(),
                "OnItemSelectedListener : " + parent.getItemAtPosition(position).toString(),
                Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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
            ActivityUtils.hideSoftKeyboard(BuyItemActivity.this);
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
            senzAttributes.put("S_ID","2");
            senzAttributes.put("f","cc");
            senzAttributes.put("S_PARA",etBillAmount.getText().toString());
            senzAttributes.put("COIN","COIN");
            senzAttributes.put("TIME", ((Long) (System.currentTimeMillis() / 1000)).toString());

            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.SHARE;
            User sender = new User("", userName);
            User receiver = new User("", "node1");
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
        final Dialog dialog = new Dialog(BuyItemActivity.this);

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

                //Log.d("para", cv + " : " + format_date + " :" + userName + " : " + etBillAmount.getText().toString());
                String coin_para = etBillAmount.getText().toString()+""+format_date+""+userName;
                //write hash function
                try {
                    String s = AeSimpleSHA1.SHA1(coin_para);
                    //Log.d("RE_GENERATED_HASH" , s);  //ea6793c63c46b4432abc1e9c078970eb09bc4e85
                    if(cv.equals(s) && cv!= null){
                        onPostShare(senz);
                        sendResponse(senzService);
                    }else{
                        String message = "<font color=#000000>Seems we couldn't take coin contact with miners in this moment </font> <font color=#eada00>" + "<b>" + "</font>";
                        displayInformationMessageDialog("Coin Mining Fail", message);
                        // implement roll back protocol
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
     * UNSHARE #S_ID #f #S_PARA  #COIN  @node1
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
        String format_date = senz.getAttributes().get("TIME");
        String s_para=etBillAmount.getText().toString();

        Toast.makeText(BuyItemActivity.this, "Recived New Coin " + cv, Toast.LENGTH_LONG).show();

        SenzorsDbSource dbSource = new SenzorsDbSource(BuyItemActivity.this);
        String dbState= dbSource.addCoin(cv,"2",userName ,shopNameSpiner.getSelectedItem().toString());
        dbSource.addVerifyCoin(cv,s_para ,"2",userName,format_date,"verify_coin");

        Toast.makeText(BuyItemActivity.this, dbState, Toast.LENGTH_LONG).show();
        btbuyItem.setEnabled(true);
        //Log.d("DB_State", dbState);



    }

    /**
     *
     * navigation functions back ,home page, manual and logout
     *
     */

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(BuyItemActivity.this, Home.class);
        intent.putExtra("USER_NAME", userName);
        BuyItemActivity.this.startActivity(intent);
        BuyItemActivity.this.finish();
    }

    public void goHome(View v) {
        Intent intent = new Intent(BuyItemActivity.this, Home.class);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
        BuyItemActivity.this.finish();
    }
    public void goBack(View v) {
        Intent intent = new Intent(BuyItemActivity.this, Home.class);
        intent.putExtra("USER_NAME", userName);
        BuyItemActivity.this.startActivity(intent);
        BuyItemActivity.this.finish();
    }


    public void goManual(View v){
        Uri uri = Uri.parse("http://scpp.netne.net/");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
    }


    public void logout(View v) {
        Intent intent = new Intent(BuyItemActivity.this, Login.class);
        BuyItemActivity.this.startActivity(intent);
        BuyItemActivity.this.finish();
    }



}
