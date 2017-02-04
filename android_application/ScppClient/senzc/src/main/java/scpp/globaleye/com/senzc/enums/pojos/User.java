package scpp.globaleye.com.senzc.enums.pojos;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by umayanga on 6/15/16.
 * Altered by Iresha on 6/8/2016.
 */
public class User implements Parcelable {

    String id;
    String username;
    String password;


    public User() {
    }

    public User(String id) {
        this.id = id;
    }

    public User(String id, String username) {
        this.id = id;
        this.username = username;
    }

    public User(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    /**
     * Use when reconstructing User object from parcel
     * This will be used only by the 'CREATOR'
     * @param in a parcel to read this object
     */
    public User(Parcel in) {
        this.id = in.readString();
        this.username = in.readString();
    }



    /**
     * Define the kind of object that you gonna parcel,
     * You can use hashCode() here
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Actual object serialization happens here, Write object content
     * to parcel one by one, reading should be done according to this write order
     * @param dest parcel
     * @param flags Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(username);
    }

    /**
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays
     *
     * If you don’t do that, Android framework will through exception
     * Parcelable protocol requires a Parcelable.Creator object called CREATOR
     */
    public static final Creator<User> CREATOR = new Creator<User>() {

        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };




    public String getId() {
        return id;
    }

    public void setId(String id) {this.id = id;}

    public String getUsername() {return username;}

    public void setUsername(String username) {
        this.username = username;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof User) {
            User toCompare = (User) obj;
            return (this.username.equalsIgnoreCase(toCompare.getUsername()));
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (this.getUsername()).hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
