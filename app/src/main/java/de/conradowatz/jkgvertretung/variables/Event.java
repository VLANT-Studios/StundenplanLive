package de.conradowatz.jkgvertretung.variables;

import android.os.Parcel;
import android.os.Parcelable;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.InheritedColumn;
import com.raizlabs.android.dbflow.annotation.InheritedPrimaryKey;
import com.raizlabs.android.dbflow.annotation.OneToMany;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.Model;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Table(database = AppDatabase.class, inheritedColumns = {@InheritedColumn(column = @Column, fieldName = "date")})
public class Event extends Termin implements Parcelable {

    public Event() {

    }

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

    @PrimaryKey(autoincrement = true)
    long id;
    @Column
    private Date date;
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private boolean deleteWhenElapsed;
    @ForeignKey(onDelete = ForeignKeyAction.CASCADE)
    private Fach fach;

    public List<Erinnerung> getErinnerungen() {

        return SQLite.select().from(Erinnerung.class).where(Erinnerung_Table.event_id.eq(id)).queryList();
    }

    public Fach getFach() {
        return fach;
    }

    public void setFach(Fach fach) {
        this.fach = fach;
    }

    protected Event(Parcel in) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(in.readLong());
        setDate(calendar.getTime());
        name = in.readString();
        description = in.readString();
        deleteWhenElapsed = in.readByte() != 0;

    }

    public boolean isDeleteWhenElapsed() {
        return deleteWhenElapsed;
    }

    public void setDeleteWhenElapsed(boolean deleteWhenElapsed) {
        this.deleteWhenElapsed = deleteWhenElapsed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public void setDate(Date date) {
        this.date = date;
    }

    public static Event getEvent(long id) {

        return SQLite.select().from(Event.class).where(Event_Table.id.eq(id)).querySingle();
    }

    public void deleteErinnerungen() {

        SQLite.delete().from(Erinnerung.class).where(Erinnerung_Table.event_id.eq(id)).execute();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getDate());
        parcel.writeLong(calendar.getTimeInMillis());
        parcel.writeString(name);
        parcel.writeString(description);
        parcel.writeByte((byte) (deleteWhenElapsed ? 1 : 0));
    }
}
