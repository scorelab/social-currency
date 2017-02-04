package scpp.globaleye.com.senzc.enums.pojos;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;

import scpp.globaleye.com.senzc.enums.enums.SenzTypeEnum;

/**
 * Created by umayanga on 6/15/16.
 */
public class Senz implements Parcelable {

    private String id;
    private String signature;
    private SenzTypeEnum senzType;
    private User sender;
    private User receiver;
    private HashMap<String, String> attributes;

    public Senz() {
    }

    public Senz(String id, String signature, SenzTypeEnum senzType, User sender, User receiver, HashMap<String, String> attributes) {
        this.id = id;
        this.signature = signature;
        this.senzType = senzType;
        this.sender = sender;
        this.receiver = receiver;
        this.attributes = attributes;
    }

    /**
     * Use when reconstructing User object from parcel
     * This will be used only by the 'CREATOR'
     *
     * @param in a parcel to read this object
     */
    public Senz(Parcel in) {
        this.id = in.readString();
        this.signature = in.readString();
        this.senzType = SenzTypeEnum.valueOf(in.readString());
        this.sender = in.readParcelable(User.class.getClassLoader());
        this.receiver = in.readParcelable(User.class.getClassLoader());

        // read attribute map from parcel
        this.attributes = new HashMap<>();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            this.attributes.put(in.readString(), in.readString());
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(signature);
        dest.writeString(senzType.name());
        dest.writeParcelable(sender, flags);
        dest.writeParcelable(receiver, flags);

        // write attribute map to parcel
        dest.writeInt(attributes.size());
        for (String s : attributes.keySet()) {
            dest.writeString(s);
            dest.writeString(attributes.get(s));
        }
    }

    /**
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays
     * <p/>
     * If you don’t do that, Android framework will through exception
     * Parcelable protocol requires a Parcelable.Creator object called CREATOR
     */
    public static final Creator<Senz> CREATOR = new Creator<Senz>() {

        public Senz createFromParcel(Parcel in) {
            return new Senz(in);
        }

        public Senz[] newArray(int size) {
            return new Senz[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public SenzTypeEnum getSenzType() {
        return senzType;
    }

    public void setSenzType(SenzTypeEnum senzType) {
        this.senzType = senzType;
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(HashMap<String, String> attributes) {
        this.attributes = attributes;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            Senz toCompare = (Senz) obj;
            return (this.id.equalsIgnoreCase(toCompare.getId()));
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (this.getId()).hashCode();
    }


}
