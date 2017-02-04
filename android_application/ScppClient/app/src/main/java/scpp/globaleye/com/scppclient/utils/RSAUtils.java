package scpp.globaleye.com.scppclient.utils;

import android.content.Context;
import android.util.Base64;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by umayanga on 6/15/16.
 */
public class RSAUtils {

    public static final String PUBLIC_KEY = "PUBLIC_KEY";
    public static final String PRIVATE_KEY = "PRIVATE_KEY";

    // size of RSA keys
    public static final int KEY_SIZE = 1024;

    public static KeyPair initKeys(Context context) throws NoSuchProviderException, NoSuchAlgorithmException {
        // generate keypair
        KeyPairGenerator keyPairGenerator;
        keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // save keys in shared preferences
        savePublicKey(context, keyPair);
        savePrivateKey(context, keyPair);

        return keyPair;
    }

    private static void savePublicKey(Context context, KeyPair keyPair) {
        // get public key from keypair
        byte[] keyContent = keyPair.getPublic().getEncoded();
        String publicKey = Base64.encodeToString(keyContent, Base64.DEFAULT).replaceAll("\n", "").replaceAll("\r", "");

        // save public key in shared preference
        PreferenceUtils.saveRsaKey(context, publicKey, RSAUtils.PUBLIC_KEY);
    }

    private static void savePrivateKey(Context context, KeyPair keyPair) {
        // get public key from keypair
        byte[] keyContent = keyPair.getPrivate().getEncoded();
        String privateKey = Base64.encodeToString(keyContent, Base64.DEFAULT).replaceAll("\n", "").replaceAll("\r", "");

        // save private key in shared preference
        PreferenceUtils.saveRsaKey(context, privateKey, RSAUtils.PRIVATE_KEY);
    }

    public static PublicKey getPublicKey(Context context) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        // get key string from shared preference
        String keyString = PreferenceUtils.getRsaKey(context, RSAUtils.PUBLIC_KEY);

        // convert to string key public key
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(keyString, Base64.DEFAULT));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = kf.generatePublic(spec);

        return publicKey;
    }

    public static PrivateKey getPrivateKey(Context context) throws InvalidKeySpecException, NoSuchAlgorithmException {
        // get key string from shared preference
        String keyString = PreferenceUtils.getRsaKey(context, RSAUtils.PRIVATE_KEY);

        // convert to string key public key
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.decode(keyString, Base64.DEFAULT));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(spec);

        return privateKey;
    }

    public static String getDigitalSignature(String payload, PrivateKey privateKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(payload.getBytes());

        // Base64 encoded string
        return Base64.encodeToString(signature.sign(), Base64.DEFAULT).replaceAll("\n", "").replaceAll("\r", "");
    }

    public static boolean verifyDigitalSignature(String payload, String signedPayload, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(payload.getBytes());

        byte[] signedPayloadContent = Base64.decode(signedPayload, Base64.DEFAULT);

        return signature.verify(signedPayloadContent);
    }
}
