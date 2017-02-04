package scpp.globaleye.com.scppclient.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import scpp.globaleye.com.scppclient.R;

/**
 * Created by umayanga on 8/11/16.
 */
public class ServicesView extends AppCompatActivity implements View.OnClickListener {

    private final int SPLASH_DISPLAY_LENGTH = 2000;
    private Button carPoolong;
    private Button buyItem;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services_view);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            userName= extras.getString("USER_NAME");
        }

        initUi();

    }


    private void initUi() {
        carPoolong = (Button) findViewById(R.id.btCarpooling);
        buyItem= (Button) findViewById(R.id.btBuyItem);

        carPoolong.setOnClickListener(ServicesView.this);
        buyItem.setOnClickListener(ServicesView.this);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(ServicesView.this, Home.class);
        intent.putExtra("USER_NAME", userName);
        ServicesView.this.startActivity(intent);
        ServicesView.this.finish();
    }

    @Override
    public void onClick(View v) {
        if (v == carPoolong) {
            navigateToMapView();
        }else{
            navigateToBuyItemView();
        }

    }

    private void navigateToMapView() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(ServicesView.this, MapsActivity.class);
                intent.putExtra("USER_NAME", userName);
                ServicesView.this.startActivity(intent);
                ServicesView.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);

    }

    private void navigateToBuyItemView() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(ServicesView.this, BuyItemActivity.class);
                intent.putExtra("USER_NAME", userName);
                ServicesView.this.startActivity(intent);
                ServicesView.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);

    }

    public void goHome(View v) {
        Intent intent = new Intent(ServicesView.this, Home.class);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
        ServicesView.this.finish();
    }
    public void goBack(View v) {
        Intent intent = new Intent(ServicesView.this, Home.class);
        intent.putExtra("USER_NAME", userName);
        ServicesView.this.startActivity(intent);
        ServicesView.this.finish();
    }

    public void goManual(View v){
        Uri uri = Uri.parse("http://scpp.netne.net/");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
    }

    public void logout(View v) {
        Intent intent = new Intent(ServicesView.this, Login.class);
        ServicesView.this.startActivity(intent);
        ServicesView.this.finish();
    }

}
