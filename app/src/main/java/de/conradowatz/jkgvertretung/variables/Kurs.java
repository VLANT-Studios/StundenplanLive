package de.conradowatz.jkgvertretung.variables;


import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.ArrayList;
import java.util.List;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;

@Table(database = AppDatabase.class)
public class Kurs extends BaseModel implements Parcelable {

    public Kurs() {

    }

    @PrimaryKey
    private int nr;

    @Column
    private String fachName;
    @Column
    private String bezeichnung;
    @Column
    private boolean isAuswaehlbar;
    @ForeignKey(onDelete = ForeignKeyAction.SET_NULL)
    private Lehrer lehrer;

    protected Kurs(Parcel in) {
        fachName = in.readString();
        bezeichnung = in.readString();
        isAuswaehlbar = in.readByte() != 0;
        nr = in.readInt();
    }

    public static final Creator<Kurs> CREATOR = new Creator<Kurs>() {
        @Override
        public Kurs createFromParcel(Parcel in) {
            return new Kurs(in);
        }

        @Override
        public Kurs[] newArray(int size) {
            return new Kurs[size];
        }
    };

    public List<Stunde> getStunden() {

        return SQLite.select().from(Stunde.class).where(Stunde_Table.kurs_nr.eq(nr)).queryList();
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public void setBezeichnung(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public boolean isAuswaehlbar() {
        return isAuswaehlbar;
    }

    public void setAuswaehlbar(boolean auswaehlbar) {
        isAuswaehlbar = auswaehlbar;
    }

    public int getNr() {
        return nr;
    }

    public void setNr(int nr) {
        this.nr = nr;
    }

    public List<Klasse> getKlassen() {

        return SQLite.select().from(Klasse.class).innerJoin(Klasse_Kurs.class).on(Klasse_Kurs_Table.klasse_id.eq(Klasse_Table.id)).where(Klasse_Kurs_Table.kurs_nr.eq(nr)).queryList();
    }

    public String getFachName() {
        return fachName;
    }

    public void setFachName(String fachName) {
        this.fachName = fachName;
    }

    public Lehrer getLehrer() {
        return lehrer;
    }

    public void setLehrer(Lehrer lehrer) {
        this.lehrer = lehrer;
    }

    public Kurs(String fachName, Lehrer lehrer) {

        this.fachName = fachName;
        this.lehrer = lehrer;
    }

    public Fach getFach() {
        return SQLite.select().from(Fach.class).where(Fach_Table.assKursNr.eq(nr)).querySingle();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(fachName);
        parcel.writeString(bezeichnung);
        parcel.writeByte((byte) (isAuswaehlbar ? 1 : 0));
        parcel.writeInt(nr);
    }

    public static void setSelectedKurse(List<Integer> kursNrn) {

        PreferenceHelper.saveIntListToPreferences(MyApplication.getAppContext(), "notSelectedKurse", kursNrn);
    }

    public static List<Integer> getNotSelectedKursNrn() {

        return PreferenceHelper.readIntListFromPreferences(MyApplication.getAppContext(), "notSelectedKurse");

    }

    public static List<Kurs> getAllSelectedKurse() {

        Klasse selectedKlasse = Klasse.getSelectedKlasse();
        List<Integer> notSelectedKurse = Kurs.getNotSelectedKursNrn();
        return SQLite.select().from(Kurs.class).innerJoin(Klasse_Kurs.class).on(Klasse_Kurs_Table.kurs_nr.eq(Kurs_Table.nr)).where(Klasse_Kurs_Table.klasse_id.eq(selectedKlasse.getId())).and(Kurs_Table.nr.notIn(
                notSelectedKurse
        )).queryList();
    }
}
