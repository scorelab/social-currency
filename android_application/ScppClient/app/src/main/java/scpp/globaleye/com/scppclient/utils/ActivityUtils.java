package scpp.globaleye.com.scppclient.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import scpp.globaleye.com.scppclient.exceptions.InvalidInputFieldsException;
import scpp.globaleye.com.senzc.enums.pojos.User;

/**
 * Created by umayanga on 6/16/16.
 */
public class ActivityUtils {

    private static ProgressDialog progressDialog;

    /**
     * Hide keyboard
     * Need to hide soft keyboard in following scenarios
     * 1. When starting background task
     * 2. When exit from activity
     * 3. On button submit
     */
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getApplicationContext().getSystemService(activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * Create and show custom progress dialog
     * Progress dialogs displaying on background tasks
     * <p/>
     * So in here
     * 1. Create custom layout for message dialog
     * 2, Set messages to dialog
     *
     * @param context activity context
     * @param message message to be display
     */
    public static void showProgressDialog(Context context, String message) {
        progressDialog = ProgressDialog.show(context, null, message, true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    /**
     * Cancel progress dialog when background task finish
     */
    public static void cancelProgressDialog() {
        if (progressDialog != null) {
            progressDialog.cancel();
        }
    }

    /**
     * Validate input fields of registration form,
     * Need to have
     * 1. non empty valid phone no
     * 2. non empty username
     * 3. non empty passwords
     * 4. two passwords should be match
     *
     * @param user User object
     * @return valid or not
     */
    public static boolean isValidRegistrationFields(User user) throws InvalidInputFieldsException {
        if (user.getUsername().isEmpty()) {
            throw new InvalidInputFieldsException();
        }
        return true;
    }

    /**
     * validate input fields of login form
     *
     * @param user login user
     * @return valid of not
     */
    public static boolean isValidLoginFields(User user) {
        return !(user.getUsername().isEmpty());
    }

    /**
     * Create custom text view for tab view
     * Set custom typeface to the text view as well
     *
     * @param context application context
     * @param title   tab title
     * @return text view
     */
    public static TextView getCustomTextView(Context context, String title) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        TextView textView = new TextView(context);
        textView.setText(title);
        textView.setTextColor(Color.parseColor("#4a4a4a"));
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(18);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

}
