package de.conradowatz.jkgvertretung.variables;

import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = AppDatabase.class)
public class Klasse_Kurs extends BaseModel {

    @PrimaryKey(autoincrement = true)
    long _id;
    @ForeignKey(saveForeignKeyModel = false, onDelete = ForeignKeyAction.CASCADE)
    Kurs kurs;
    @ForeignKey(saveForeignKeyModel = false, onDelete = ForeignKeyAction.CASCADE)
    Klasse klasse;

    public final long getId() {
        return _id;
    }

    public final Kurs getKurs() {
        return kurs;
    }

    public final void setKurs(Kurs param) {
        kurs = param;
    }

    public final Klasse getKlasse() {
        return klasse;
    }

    public final void setKlasse(Klasse param) {
        klasse = param;
    }
}
