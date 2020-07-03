package de.conradowatz.jkgvertretung.variables;

import android.os.Parcel;
import android.os.Parcelable;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.InheritedColumn;
import com.raizlabs.android.dbflow.annotation.InheritedPrimaryKey;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.Where;
import com.raizlabs.android.dbflow.structure.Model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.conradowatz.jkgvertretung.tools.Utilities;

@Table(database = AppDatabase.class, inheritedColumns = {@InheritedColumn(column = @Column, fieldName = "date")})
public class Ferien extends Termin implements Parcelable {

    public Ferien() {

    }

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

    @PrimaryKey(autoincrement = true)
    long id;
    @Column
    private Date startDate;
    @Column
    private Date endDate;
    @Column
    private String name;

    public Ferien(Date startDate, Date endDate, String name) {
        setDate(startDate);
        this.endDate = endDate;
        this.name = name;
    }

    private Ferien(Parcel in) {
        Calendar c = Calendar.getInstance();
        name = in.readString();
        c.setTimeInMillis(in.readLong());
        setDate(c.getTime());
        c.setTimeInMillis(in.readLong());
        endDate = c.getTime();
    }

    @Override
    public Date getDate() {
        return startDate;
    }

    @Override
    public void setDate(Date date) {
        startDate = date;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getStartDate() {
        return getDate();
    }

    public void setStartDate(Date startDate) {
        setDate(startDate);
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

    public String getDateString() {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MMMM yyyy", Locale.GERMAN);
        if (Utilities.compareDays(getDate(), endDate) == 0)
            return String.format("am %s", dateFormat.format(getDate()));
        else
            return String.format("vom %s\n\tbis %s", dateFormat.format(getDate()), dateFormat.format(endDate));
    }

    public static boolean isFerien(Date date) {
        return getFerien(date)!=null;
    }

    public static Ferien getFerien(Date date) {
        return SQLite.select().from(Ferien.class).where(Ferien_Table.startDate.lessThanOrEq(date)).and(Ferien_Table.endDate.greaterThanOrEq(date)).querySingle();
    }

    public static Ferien getFerien(long id) {
        return SQLite.select().from(Ferien.class).where(Ferien_Table.id.eq(id)).querySingle();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        Calendar c = Calendar.getInstance();
        c.setTime(getDate());
        parcel.writeLong(c.getTimeInMillis());
        c.setTime(endDate);
        parcel.writeLong(c.getTimeInMillis());
    }
}
