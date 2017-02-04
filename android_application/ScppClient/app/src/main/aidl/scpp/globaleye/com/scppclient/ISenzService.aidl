// ISenzService.aidl
package scpp.globaleye.com.scppclient;

//import scpp.globaleye.com.senzc.enums.pojos.Senz;
import scpp.globaleye.com.senzc.enums.pojos.Senz;

// Declare any non-default types here with import statements

interface ISenzService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */

        // send senz messages to service via this function
        void send(in Senz senz);

        // get registered user via this function
        //String getUser();

}
