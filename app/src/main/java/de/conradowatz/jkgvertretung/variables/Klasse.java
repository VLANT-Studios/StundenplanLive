package de.conradowatz.jkgvertretung.variables;

import android.content.Context;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.ManyToMany;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.Method;
import com.raizlabs.android.dbflow.sql.language.OrderBy;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.ArrayList;
import java.util.List;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;

@Table(database = AppDatabase.class)
public class Klasse extends BaseModel {

    public Klasse() {

    }

    @PrimaryKey(autoincrement = true)
    long id;
    @Column
    private String name;

    public List<Stunde> getStunden() {

        return SQLite.select().from(Stunde.class).where(Stunde_Table.klasse_id.eq(id)).queryList();
    }

    public Klasse(String name) {
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

    public List<Kurs> getSortedKurse() {

        return SQLite.select().from(Kurs.class).innerJoin(Klasse_Kurs.class).on(Klasse_Kurs_Table.kurs_nr.eq(Kurs_Table.nr)).where(Klasse_Kurs_Table.klasse_id.eq(id)).orderBy(Kurs_Table.fachName, true).orderBy(Kurs_Table.bezeichnung, true).queryList();
    }

    public List<Kurs> getAuswaehlbareKurseSorted() {

        return SQLite.select().from(Kurs.class).innerJoin(Klasse_Kurs.class).on(Klasse_Kurs_Table.kurs_nr.eq(Kurs_Table.nr)).where(Klasse_Kurs_Table.klasse_id.eq(id)).and(Kurs_Table.isAuswaehlbar.eq(true)).orderBy(Kurs_Table.fachName, true).orderBy(Kurs_Table.bezeichnung, true).queryList();
    }

    public static List<Klasse> getAllKlassenSorted() {

        return SQLite.select().from(Klasse.class).orderBy(new Method("LENGTH", Klasse_Table.name), true).orderBy(Klasse_Table.name, true).queryList();
    }

    public static Klasse getKlasse(String name) {

        return SQLite.select().from(Klasse.class).where(Klasse_Table.name.eq(name)).querySingle();
    }

    public static Klasse getKlasse(long id) {

        return SQLite.select().from(Klasse.class).where(Klasse_Table.id.eq(id)).querySingle();
    }

    public static Klasse getSelectedKlasse() {

        String selectedKlasseString = PreferenceHelper.readStringFromPreferences(MyApplication.getAppContext(), "selectedKlasse", null);
        return selectedKlasseString==null?null:getKlasse(selectedKlasseString);
    }

    public static void setSelectedKlasse(String klassenName) {

        PreferenceHelper.saveStringToPreferences(MyApplication.getAppContext(), "selectedKlasse", klassenName);
    }
}
