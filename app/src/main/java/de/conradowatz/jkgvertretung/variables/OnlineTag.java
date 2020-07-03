package de.conradowatz.jkgvertretung.variables;

import androidx.core.util.Pair;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.Method;
import com.raizlabs.android.dbflow.sql.language.OperatorGroup;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Table(database = AppDatabase.class)
public class OnlineTag extends BaseModel {

    public OnlineTag() {

    }

    @PrimaryKey(autoincrement = true)
    long id;
    @Column
    private Date date;
    @Column
    private String zeitStempel;
    @Column
    private String infotext;

    public OnlineTag(Date date) {

        this.date = date;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<Stunde> getStunden() {

        return SQLite.select().from(Stunde.class).where(Stunde_Table.onlineTag_id.eq(id)).queryList();
    }

    public List<Stunde> getSortedVertretung() {

        return SQLite.select().from(Stunde.class).innerJoin(Klasse.class).on(Stunde_Table.klasse_id.eq(Klasse_Table.id.withTable())).where(Stunde_Table.onlineTag_id.eq(id))
                .and(OperatorGroup.clause().orAll(
                        Stunde_Table.isFachGeaendert.eq(true), Stunde_Table.isLehrerGeaendert.eq(true), new Method("LENGTH", Stunde_Table.info).greaterThan(0), Stunde_Table.isRaumGeaendert.eq(true)
                )).orderBy(new Method("LENGTH", Klasse_Table.name), true).orderBy(Klasse_Table.name, true).orderBy(Stunde_Table.stunde, true).queryList();
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getZeitStempel() {
        return zeitStempel;
    }

    public void setZeitStempel(String zeitStempel) {
        this.zeitStempel = zeitStempel;
    }

    public String getInfotext() {
        return infotext;
    }

    public void setInfotext(String infotext) {
        this.infotext = infotext;
    }

    public static List<OnlineTag> getAllOnlineTagSorted() {

        return SQLite.select().from(OnlineTag.class).orderBy(OnlineTag_Table.date, true).queryList();
    }

    public Pair<List<String>, List<String>> getFreieZimmer() {

        if (SQLite.select().from(Stunde.class).where(Stunde_Table.onlineTag_id.eq(id)).querySingle()==null) return new Pair<>(null, null);

        //freie Zimmer herausfinden
        List<String> allRooms = Stunde.getAllRooms();
        List<String> roomsPerStunde = new ArrayList<>();
        for (int stunde=1; stunde<=9; stunde++) {
            List<String> rooms = new ArrayList<>(allRooms);
            List<Stunde> stunden = SQLite.select(Stunde_Table.raum).from(Stunde.class).where(Stunde_Table.stunde.eq(stunde)).groupBy(Stunde_Table.raum).queryList();
            for (Stunde s : stunden) if (rooms.contains(s.getRaum())) rooms.remove(s.getRaum());
            String roomsString = "";
            for (String r : rooms) roomsString += ", "+r;
            if (!roomsString.isEmpty()) {
                roomsPerStunde.add(roomsString.substring(2));
            } else {
                roomsPerStunde.add("");
            }
        }

        //gleiche Stunden zusammenfassen
        List<String> stunden = new ArrayList<>();
        stunden.add("1");
        int adding = 0;
        for (int i=1; i<roomsPerStunde.size(); i++) {
            if (roomsPerStunde.get(i).equals(roomsPerStunde.get(i-1))) {
                stunden.set(i-1, stunden.get(i-1)+"/"+(i+1+adding));
                roomsPerStunde.remove(i);
                i--;
                adding++;
            } else stunden.add(String.valueOf(i+1+adding));
        }

        return new Pair<>(stunden, roomsPerStunde);
    }

    public List<Stunde> getStunden(int stunde) {

        return SQLite.select().from(Stunde.class).where(Stunde_Table.onlineTag_id.eq(id)).and(Stunde_Table.stunde.eq(stunde)).queryList();
    }

    public List<Stunde> getSortedStunden(long klassenId) {

        return SQLite.select().from(Stunde.class).where(Stunde_Table.onlineTag_id.eq(id)).and(Stunde_Table.klasse_id.eq(klassenId)).orderBy(Stunde_Table.stunde, true).queryList();
    }

    public List<Stunde> getSelectedSortedStundenList() {

        Klasse selectedKlasse = Klasse.getSelectedKlasse();
        List<Integer> notSelectedKurse = Kurs.getNotSelectedKursNrn();
        return SQLite.select().from(Stunde.class).where(Stunde_Table.onlineTag_id.eq(id)).and(Stunde_Table.klasse_id.eq(selectedKlasse.getId())).and(
                OperatorGroup.clause().orAll(
                        Stunde_Table.kurs_nr.isNull(),
                        Stunde_Table.kurs_nr.notIn(notSelectedKurse)
        )).queryList();
    }

    public List<Stunde> getSelectedSortedVertretungList() {

        Klasse selectedKlasse = Klasse.getSelectedKlasse();
        List<Integer> notSelectedKurse = Kurs.getNotSelectedKursNrn();

        return SQLite.select().from(Stunde.class).where(Stunde_Table.onlineTag_id.eq(id)).and(Stunde_Table.klasse_id.eq(selectedKlasse.getId()))
                .and(OperatorGroup.clause().orAll(
                        Stunde_Table.isFachGeaendert.eq(true), Stunde_Table.isLehrerGeaendert.eq(true), new Method("LENGTH", Stunde_Table.info).greaterThan(0), Stunde_Table.isRaumGeaendert.eq(true)
                        )).and(OperatorGroup.clause().orAll(
                        Stunde_Table.kurs_nr.isNull(),
                        Stunde_Table.kurs_nr.notIn(notSelectedKurse))
                ).orderBy(Stunde_Table.stunde, true).queryList();
    }

    public static OnlineTag getOnlineTag(Date date) {

        return SQLite.select().from(OnlineTag.class).where(OnlineTag_Table.date.eq(date)).querySingle();
    }
}
