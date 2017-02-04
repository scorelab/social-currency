package scpp.globaleye.com.scppclient.utils;

import android.util.Log;

import java.util.HashMap;

import scpp.globaleye.com.senzc.enums.enums.SenzTypeEnum;
import scpp.globaleye.com.senzc.enums.pojos.Senz;
import scpp.globaleye.com.senzc.enums.pojos.User;

/**
 * Created by umayanga on 6/15/16.
 */
public class SenzParser {

    public static Senz parse(String senzMessage) {

        //Log.d("Parser Masages" , senzMessage);

        // init sez with
        Senz senz = new Senz();
        senz.setAttributes(new HashMap<String, String>());

        // parse senz
        String[] tokens = senzMessage.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (i == 0) {
                // query type at first (PING, SHARE, GET, DATA)
                senz.setSenzType(SenzTypeEnum.valueOf(token.toUpperCase()));

                // if query type is PING we breakup from here :)
                if (senz.getSenzType() == SenzTypeEnum.PING)
                    return senz;
            } else if (i == tokens.length - 1) {
                // signature at the end
                senz.setSignature(token);
            } else if (tokens[i].startsWith("@")) {
                // @0775432012
                senz.setReceiver(new User("", token.substring(1)));
            } else if (token.startsWith("^")) {
                // ^mysensors, ^0775432015
                senz.setSender(new User("", token.substring(1)));
            } else if (token.startsWith("#")) {
                // we remove # from token and store as a key
                String key = token.substring(1);
                if (token.equals("#time") || token.equals("#pubkey")) {
                    // #time 2453234, #pubkey ac23edf432fdg
                    senz.getAttributes().put(key, tokens[i + 1]);
                    i++;
                } else {
                    if (senz.getSenzType() == SenzTypeEnum.DATA) {
                        // #lat 3.2343 #lon 4.3434
                        senz.getAttributes().put(key, tokens[i + 1]);
                        i++;
                    }else if(senz.getSenzType() == SenzTypeEnum.PUT){
                        senz.getAttributes().put(key, tokens[i + 1]);
                        i++;
                    }else {
                        // #lat #lon
                        senz.getAttributes().put(key, "");
                    }
                }
            }
        }

//        System.out.println(senz.getSender());
//        System.out.println(senz.getReceiver());
//        System.out.println(senz.getSenzType());
//        System.out.println(senz.getSignature());
//        System.out.println(senz.getAttributes().entrySet());
//        System.out.println("------------");

        return senz;
    }

    public static String getSenzPayload(Senz senz) {
        // add senz type to payload
        String payload = senz.getSenzType().toString();
        //Log.e("PAYLOAD", "payload" + senz.getAttributes().keySet()+"");
        // add attributes to payload
        for (String key : senz.getAttributes().keySet()) {
            //Log.e("PAYLOAD", "key" + key.equalsIgnoreCase(senz.getAttributes().get(key)));
            if (key.equalsIgnoreCase(senz.getAttributes().get(key))) {
                // GET or SHARE query
                // param and value equal since no value to store (SHARE #lat #lon)
                payload = payload.concat(" ").concat("#").concat(senz.getAttributes().get(key));
                //Log.e("PAYLOAD", "GETor SHARE" + senz.getAttributes().keySet()+"");
            } else {
                // DATA query
                //Log.e("PAYLOAD", "DAAT" + senz.getAttributes().keySet()+"");
                payload = payload.concat(" ").concat("#").concat(key).concat(" ").concat(senz.getAttributes().get(key));
            }
        }

        // add sender and receiver
        payload = payload.concat(" ").concat("@").concat(senz.getReceiver().getUsername());
        payload = payload.concat(" ").concat("^").concat(senz.getSender().getUsername());

        return payload;
    }

    public static String getSenzMessage(String payload, String signature) {
        String senzMessage = payload + " " + signature;
        //System.out.println((senzMessage.replaceAll("\n", "").replaceAll("\r", "")).getBytes().length);
        return senzMessage.replaceAll("\n", "").replaceAll("\r", "");
    }

   /* public static void main(String args[]) {
        String senzMessage1 = "DATA" + " " +
                "#pubkey" + " " + "keyyyyy" + " " +
                "#time" + " " + "timestamp" + " " +
                "@senz" + " " +
                "^0775432015" + " " +
                "signatureeee";

        String senzMessage2 = "SHARE" + " " +
                "#lat" + " " +
                "#lon" + " " +
                "#time" + " " + "timestamp" + " " +
                "@senz" + " " +
                "^0775432015" + " " +
                "signatureeee";

        String senzMessage3 = "PUT" + " " +
                "#COIN_VALUE" + " " +
                "#time" + " " + "timestamp" + " " +
                "@senz" + " " +
                "^0775432015" + " " +
                "signatureeee";

        parse(senzMessage1);
        parse(senzMessage2);

//        Senz senz = new Senz();
//        senz.setSender("03452");
//        senz.setReceiver("mysen");
//        senz.setSenzType(SenzTypeEnum.SHARE);
//
//        HashMap<String, String> senzAttributes = new HashMap<>();
//        senzAttributes.put("pubkey", "public_key");
//        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
//        senz.setAttributes(senzAttributes);

        String senzMessage4 = "DATA " +
                "#msg UserCreated " +
                "#pubkey sd23453451234sfsdfd==  " +
                "#time 1441806897.71 " +
                "^mysensors " +
                "v50I88VzgvBvubCjGitTMO9";

        Senz parse = parse(senzMessage1);
//        System.out.print(parse.toString());

        Senz senz = new Senz();
        senz.setSender(new User("", "222"));
        senz.setReceiver(new User("", "111"));
        senz.setSenzType(SenzTypeEnum.SHARE);

        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put("lat", "lat");
        senzAttributes.put("lat", "lon");
        senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
        senz.setAttributes(senzAttributes);

        String senzPaylod = getSenzPayload(senz);
        String signature = "digsig";
        String senzMessage = getSenzMessage(senzPaylod, signature);
        System.out.println(senzPaylod);
        System.out.println(senzMessage);
    } */
}
