package de.conradowatz.jkgvertretung.variables;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Event extends Termin implements Parcelable {

    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };
    private Date datum;
    private String title;
    private String description;
    private boolean deleteWhenElapsed;
    private String fachName; //only set when displaying
    private int fachIndex; //only set when displaying
    private List<Date> reminders;

    public Event(Date datum, String title, String description, boolean deleteWhenElapsed) {
        this.datum = datum;
        this.title = title;
        this.deleteWhenElapsed = deleteWhenElapsed;
        this.description = description;
        reminders = new ArrayList<>();
    }

    protected Event(Parcel in) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(in.readLong());
        datum = calendar.getTime();
        title = in.readString();
        description = in.readString();
        deleteWhenElapsed = in.readByte() != 0;
        fachName = in.readString();
        fachIndex = in.readInt();
        reminders = new ArrayList<>();
        for (long l : in.createLongArray()) {
            calendar.setTimeInMillis(l);
            reminders.add(calendar.getTime());
        }
    }

    public Event(Date datum, String title, String description, boolean deleteWhenElapsed, String fachName, List<Date> reminders) {
        this.datum = datum;
        this.title = title;
        this.description = description;
        this.deleteWhenElapsed = deleteWhenElapsed;
        this.fachName = fachName;
        this.reminders = reminders;

    }

    public boolean isDeleteWhenElapsed() {
        return deleteWhenElapsed;
    }

    public void setDeleteWhenElapsed(boolean deleteWhenElapsed) {
        this.deleteWhenElapsed = deleteWhenElapsed;
    }

    public List<Date> getReminders() {
        return reminders;
    }

    public String getFachName() {
        return fachName;
    }

    public void setFachName(String fachName) {
        this.fachName = fachName;
    }

    @Override
    public Date getDatum() {
        return datum;
    }

    public void setDatum(Date datum) {
        this.datum = datum;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getFachIndex() {
        return fachIndex;
    }

    public void setFachIndex(int fachIndex) {
        this.fachIndex = fachIndex;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(datum);
        parcel.writeLong(calendar.getTimeInMillis());
        parcel.writeString(title);
        parcel.writeString(description);
        parcel.writeByte((byte) (deleteWhenElapsed ? 1 : 0));
        parcel.writeString(fachName);
        parcel.writeInt(fachIndex);
        for (Date date : reminders) {
            calendar.setTime(date);
            parcel.writeLong(calendar.getTimeInMillis());
        }
    }
}
