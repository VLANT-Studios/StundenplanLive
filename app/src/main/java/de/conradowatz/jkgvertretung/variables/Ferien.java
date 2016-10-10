package de.conradowatz.jkgvertretung.variables;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;
import java.util.Date;

public class Ferien implements Parcelable {

    public static final Creator<Ferien> CREATOR = new Creator<Ferien>() {
        @Override
        public Ferien createFromParcel(Parcel in) {
            return new Ferien(in);
        }

        @Override
        public Ferien[] newArray(int size) {
            return new Ferien[size];
        }
    };
    private Date startDate;
    private Date endDate;
    private String name;

    public Ferien(Date startDate, Date endDate, String name) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.name = name;
    }

    private Ferien(Parcel in) {
        Calendar c = Calendar.getInstance();
        name = in.readString();
        c.setTimeInMillis(in.readLong());
        startDate = c.getTime();
        c.setTimeInMillis(in.readLong());
        endDate = c.getTime();
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        parcel.writeLong(c.getTimeInMillis());
        c.setTime(endDate);
        parcel.writeLong(c.getTimeInMillis());
    }
}
