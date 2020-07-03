package de.conradowatz.jkgvertretung.variables;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.annotation.Unique;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.List;

@Table(database = AppDatabase.class)
public class Lehrer extends BaseModel {

    public Lehrer() {

    }

    @PrimaryKey(autoincrement = true)
    long id;

    @Column
    @Unique
    private String name;

    public List<Kurs> getKurse() {
        return SQLite.select().from(Kurs.class).where(Kurs_Table.lehrer_id.eq(id)).queryList();
    }

    public Lehrer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public static Lehrer getLehrerByName(String lehrerName) {

        return SQLite.select().from(Lehrer.class).where(Lehrer_Table.name.eq(lehrerName)).querySingle();
    }
}
