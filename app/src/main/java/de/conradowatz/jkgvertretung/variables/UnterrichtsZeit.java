package de.conradowatz.jkgvertretung.variables;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.List;

import de.conradowatz.jkgvertretung.fragments.FachStundenFragment;

@Table(database = AppDatabase.class)
public class UnterrichtsZeit extends BaseModel {

    public UnterrichtsZeit() {

    }

    @PrimaryKey(autoincrement = true)
    long id;
    @Column
    private int wochentag;
    @Column
    private int stunde;
    @Column
    private boolean aWoche;
    @ForeignKey(onDelete = ForeignKeyAction.CASCADE, onUpdate = ForeignKeyAction.CASCADE)
    private Fach fach;

    public UnterrichtsZeit(int wochentag, int stunde, boolean aWoche, Fach fach) {
        this.wochentag = wochentag;
        this.stunde = stunde;
        this.aWoche = aWoche;
        this.fach = fach;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getWochentag() {
        return wochentag;
    }

    public void setWochentag(int wochentag) {
        this.wochentag = wochentag;
    }

    public int getStunde() {
        return stunde;
    }

    public void setStunde(int stunde) {
        this.stunde = stunde;
    }

    public boolean isAWoche() {
        return aWoche;
    }

    public void setAWoche(boolean aWoche) {
        this.aWoche = aWoche;
    }

    public Fach getFach() {
        return fach;
    }

    public void setFach(Fach fach) {
        this.fach = fach;
    }

    public static List<UnterrichtsZeit> getSortedUnterrichtList(int wochentag, boolean aWoche) {

        return SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.wochentag.eq(wochentag)).and(UnterrichtsZeit_Table.aWoche.eq(aWoche)).orderBy(UnterrichtsZeit_Table.stunde, true).queryList();
    }

    public static List<UnterrichtsZeit> getBelegtenUnterricht(long fachId, int wochenTag, int stunde, int abwoche) {

        if (abwoche== FachStundenFragment.STATE_IMMER) {
            return SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.fach_id.notEq(fachId)).and(UnterrichtsZeit_Table.wochentag.eq(wochenTag)).and(UnterrichtsZeit_Table.stunde.eq(stunde)).queryList();
        } else {
            return SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.fach_id.notEq(fachId)).and(UnterrichtsZeit_Table.wochentag.eq(wochenTag)).and(UnterrichtsZeit_Table.stunde.eq(stunde)).and(UnterrichtsZeit_Table.aWoche.eq(abwoche==FachStundenFragment.STATE_AWOCHE)).queryList();
        }
    }

    public static void deleteBelegtenUnterricht(long fachId, int wochenTag, int stunde, int abwoche) {

        if (abwoche== FachStundenFragment.STATE_IMMER) {
            SQLite.delete().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.fach_id.notEq(fachId)).and(UnterrichtsZeit_Table.wochentag.eq(wochenTag)).and(UnterrichtsZeit_Table.stunde.eq(stunde)).execute();
        } else {
            SQLite.delete().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.fach_id.notEq(fachId)).and(UnterrichtsZeit_Table.wochentag.eq(wochenTag)).and(UnterrichtsZeit_Table.stunde.eq(stunde)).and(UnterrichtsZeit_Table.aWoche.eq(abwoche==FachStundenFragment.STATE_AWOCHE)).execute();
        }

    }
}
