package de.conradowatz.jkgvertretung.variables;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = AppDatabase.class)
public class Zensur extends BaseModel {

    public Zensur() {

    }

    @PrimaryKey(autoincrement = true)
    long id;
    @Column
    private int zensur;
    @Column
    private boolean isKlausur;
    @ForeignKey(onDelete = ForeignKeyAction.CASCADE)
    private Fach fach;

    public Zensur(int zensur, boolean isKlausur, Fach fach) {
        this.zensur = zensur;
        this.isKlausur = isKlausur;
        this.fach = fach;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getZensur() {
        return zensur;
    }

    public void setZensur(int zensur) {
        this.zensur = zensur;
    }

    public boolean isKlausur() {
        return isKlausur;
    }

    public void setKlausur(boolean klausur) {
        isKlausur = klausur;
    }

    public Fach getFach() {
        return fach;
    }

    public void setFach(Fach fach) {
        this.fach = fach;
    }
}
