package de.conradowatz.jkgvertretung.variables;


import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = AppDatabase.class)
public class Vertretung extends BaseModel{

    public Vertretung() {

    }

    @PrimaryKey(autoincrement = true)
    long id;
    @Column
    private String klasse;
    @Column
    private String stunde;
    @Column
    private String fach;
    @Column
    private String lehrer;
    @Column
    private boolean isLehrerGeaendert;
    @Column
    private String raum;
    @Column
    private boolean isRaumGeaendert;
    @Column
    private String info;
    @ForeignKey(onDelete = ForeignKeyAction.CASCADE)
    private OnlineTag onlineTag;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKlasse() {
        return klasse;
    }

    public void setKlasse(String klasse) {
        this.klasse = klasse;
    }

    public String getStunde() {
        return stunde;
    }

    public void setStunde(String stunde) {
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

    public OnlineTag getOnlineTag() {
        return onlineTag;
    }

    public void setOnlineTag(OnlineTag onlineTag) {
        this.onlineTag = onlineTag;
    }
}
