package de.conradowatz.jkgvertretung.variables;


import android.content.ClipData;
import android.os.Parcel;
import android.os.Parcelable;

public class Kurs implements Parcelable {
    private String name;
    private String lehrer;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLehrer() {
        return lehrer;
    }

    public void setLehrer(String lehrer) {
        this.lehrer = lehrer;
    }

    public Kurs(String name, String lehrer) {

        this.name = name;
        this.lehrer = lehrer;
    }

    public Kurs(Parcel in) {

        name = in.readString();
        lehrer = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(name);
        dest.writeString(lehrer);
    }

    public static final Parcelable.Creator CREATOR =
            new Parcelable.Creator() {
                public Kurs createFromParcel(Parcel in) {
                    return new Kurs(in);
                }

                public Kurs[] newArray(int size) {
                    return new Kurs[size];
                }
            };
}
