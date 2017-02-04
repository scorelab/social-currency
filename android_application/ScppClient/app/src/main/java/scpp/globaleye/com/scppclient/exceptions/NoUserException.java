package scpp.globaleye.com.scppclient.exceptions;

/**
 * Created by umayanga on 6/15/16.
 * using session validation
 */
public class NoUserException extends  Exception{

    private static final long serialVersionID=1L;

    @Override
    public String toString(){
        return "No logged in user";
    }


}
