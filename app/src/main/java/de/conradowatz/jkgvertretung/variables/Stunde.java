package de.conradowatz.jkgvertretung.variables;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.ArrayList;
import java.util.List;

@Table(database = AppDatabase.class)
public class Stunde extends BaseModel {

    public Stunde() {

    }

    @PrimaryKey(autoincrement = true)
    long id;

    @Column
    private int stunde;
    @Column
    private String fach;
    @Column
    private String lehrer;
    @Column
    private boolean isFachGeaendert;
    @Column
    private boolean isLehrerGeaendert;
    @Column
    private String raum;
    @Column
    private boolean isRaumGeaendert;
    @Column
    private String info;
    @ForeignKey(onDelete = ForeignKeyAction.CASCADE)
    private Klasse klasse;
    @ForeignKey(onDelete = ForeignKeyAction.SET_NULL)
    private Kurs kurs;
    @ForeignKey(onDelete = ForeignKeyAction.CASCADE)
    private OnlineTag onlineTag;

    public OnlineTag getOnlineTag() {
        return onlineTag;
    }

    public void setOnlineTag(OnlineTag onlineTag) {
        this.onlineTag = onlineTag;
    }

    public int getStunde() {
        return stunde;
    }

    public void setStunde(int stunde) {
        this.stunde = stunde;
    }

    public String getFach() {
        return fach;
    }

    public void setFach(String fach) {
        this.fach = fach;
    }

    public String getLehrer() {
        return lehrer;
    }

    public void setLehrer(String lehrer) {
        this.lehrer = lehrer;
    }

    public boolean isFachGeaendert() {
        return isFachGeaendert;
    }

    public void setFachGeaendert(boolean fachGeaendert) {
        isFachGeaendert = fachGeaendert;
    }

    public boolean isLehrerGeaendert() {
        return isLehrerGeaendert;
    }

    public void setLehrerGeaendert(boolean lehrerGeaendert) {
        isLehrerGeaendert = lehrerGeaendert;
    }

    public String getRaum() {
        return raum;
    }

    public void setRaum(String raum) {
        this.raum = raum;
    }

    public boolean isRaumGeaendert() {
        return isRaumGeaendert;
    }

    public void setRaumGeaendert(boolean raumGeaendert) {
        isRaumGeaendert = raumGeaendert;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Klasse getKlasse() {
        return klasse;
    }

    public void setKlasse(Klasse klasse) {
        this.klasse = klasse;
    }

    public Kurs getKurs() {
        return kurs;
    }

    public void setKurs(Kurs kurs) {
        this.kurs = kurs;
    }

    public static List<String> getAllRooms() {

        List<Stunde> stunden = SQLite.select(Stunde_Table.raum).from(Stunde.class).groupBy(Stunde_Table.raum).orderBy(Stunde_Table.raum, true).queryList();
        List<String> rooms = new ArrayList<>();
        for (Stunde s : stunden) rooms.add(s.getRaum());

        return rooms;
    }

    public static List<Stunde> getAllSelectedStunden() {

        Klasse selectedKlasse = Klasse.getSelectedKlasse();
        List<Integer> notSelectedKurse = Kurs.getNotSelectedKursNrn();
        return SQLite.select().from(Stunde.class).where(Stunde_Table.klasse_id.eq(selectedKlasse.getId())).and(Stunde_Table.kurs_nr.notIn(
                notSelectedKurse
        )).queryList();
    }
}
