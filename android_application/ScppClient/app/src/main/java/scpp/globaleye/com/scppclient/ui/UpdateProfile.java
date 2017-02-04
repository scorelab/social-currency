package scpp.globaleye.com.scppclient.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import scpp.globaleye.com.scppclient.R;
import scpp.globaleye.com.scppclient.exceptions.NoUserException;
import scpp.globaleye.com.scppclient.utils.PreferenceUtils;
import scpp.globaleye.com.senzc.enums.pojos.User;

/**
 * Created by umayanga on 8/11/16.
 */
public class UpdateProfile extends AppCompatActivity implements View.OnClickListener  {

    private final int SPLASH_DISPLAY_LENGTH = 2000;
    private Button updateProfile;
    private EditText updateUserName;
    private EditText updatePassword;
    private EditText confirmPassword;
    private String userName;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_update_view);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userName= extras.getString("USER_NAME");
        }

        try {
            user = PreferenceUtils.getUser(this);
        } catch (NoUserException e) {
            Toast.makeText(this, "Invalid USER", Toast.LENGTH_LONG).show();
        }

        initUi();

    }


    private void initUi() {
        updateProfile = (Button) findViewById(R.id.btUpdateProfile);
        updateUserName = (EditText) findViewById(R.id.etUpdateUserName);
        updatePassword = (EditText) findViewById(R.id.etUpdatpasswordtxt);
        confirmPassword =(EditText) findViewById(R.id.etUPConfirmPasswordeditText);
        updateProfile.setOnClickListener(UpdateProfile.this);

        updateUserName.setText(user.getUsername());
        updatePassword.setText(user.getPassword());
        updateUserName.setEnabled(false);


    }

    @Override
    public void onClick(View v) {
        if (v == updateProfile) {
            if(updatePassword.getText().toString().trim().equals(confirmPassword.getText().toString().trim())){
                updateProfile();
            }else{
                Toast.makeText(UpdateProfile.this, "password and confirm Password Not Match" , Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void updateProfile() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        String msg= PreferenceUtils.updateUser(getApplicationContext(), user.getUsername(), updatePassword.getText().toString().trim());
                        if(msg.equals("update")){
                            Toast.makeText(UpdateProfile.this, "update profile successfully" , Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(UpdateProfile.this, "update failed" , Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure  ,you want to change password ?").setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();


    }

    public void goHome(View v) {
        Intent intent = new Intent(UpdateProfile.this, Home.class);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
        UpdateProfile.this.finish();
    }

    public void goBack(View v) {
        Intent intent = new Intent(UpdateProfile.this, Home.class);
        intent.putExtra("USER_NAME", userName);
        UpdateProfile.this.startActivity(intent);
        UpdateProfile.this.finish();
    }

    public void goManual(View v){
        Uri uri = Uri.parse("http://scpp.netne.net/");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
    }

    public void logout(View v) {
        Intent intent = new Intent(UpdateProfile.this, Login.class);
        UpdateProfile.this.startActivity(intent);
        UpdateProfile.this.finish();
    }
}
