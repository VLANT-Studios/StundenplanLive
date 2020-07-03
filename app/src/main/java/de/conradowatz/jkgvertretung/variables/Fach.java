package de.conradowatz.jkgvertretung.variables;


import android.content.Context;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.Method;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;

@Table(database = AppDatabase.class)
public class Fach extends BaseModel {

    public Fach() {

    }

    public static final int MODE_5050 = 0;
    public static final int MODE_7030 = 1;
    public static final int MODE_6040 = 2;
    public static final int MODE_2LK = 3;

    @PrimaryKey(autoincrement = true)
    long id;

    @Column
    private String name;
    @Column
    private boolean isLeistungskurs;
    @ForeignKey(onDelete = ForeignKeyAction.SET_NULL)
    private Kurs kurs;
    @Column
    private int assKursNr;
    @Column
    private int notenModus;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<Zensur> getZensuren() {

        return SQLite.select().from(Zensur.class).where(Zensur_Table.fach_id.eq(id)).queryList();
    }

    public List<UnterrichtsZeit> getUnterrichtsZeiten() {

        return SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.fach_id.eq(id)).queryList();
    }

    public Fach(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLeistungskurs() {
        return isLeistungskurs;
    }

    public void setLeistungskurs(boolean leistungskurs) {
        isLeistungskurs = leistungskurs;
    }

    public Kurs getKurs() {
        return kurs;
    }

    public void setKurs(Kurs kurs) {
        this.kurs = kurs;
    }

    public int getNotenModus() {
        return notenModus;
    }

    public void setNotenModus(int notenModus) {
        this.notenModus = notenModus;
    }

    public int getAssKursNr() {
        return assKursNr;
    }

    public void setAssKursNr(int assKursNr) {
        this.assKursNr = assKursNr;
    }

    public Double getZensurenDurchschnitt() {

        if (notenModus==MODE_2LK) {

            //return SQLite.select(Method.avg(Zensur_Table.zensur.as("avgZensur"))).from().where(Zensur_Table.fach_id.eq(id)).queryCustomSingle(AverageZensur.class).avgZensur;

            List <Zensur> zensuren = SQLite.select().from(Zensur.class).where(Zensur_Table.fach_id.eq(id)).queryList();
            double sum = 0;
            int count = zensuren.size();
            for (Zensur z : zensuren) {
                sum+=z.getZensur();
                if (z.isKlausur()) {
                    sum+=z.getZensur();
                    count++;
                }
            }
            return sum==0?null:(sum/count);

        }

        Double klausuren = getKlausurenDurchschnitt();
        Double tests = getTestDurchschnitt();

        if (klausuren==null || tests==null) {
            if (klausuren==null && tests==null) return null;
            if (klausuren==null) return tests;
            else return klausuren;
        }

        switch (notenModus) {
            case MODE_5050:
                return 0.5*klausuren+0.5*tests;
            case MODE_6040:
                return 0.4*klausuren+0.6*tests;
            case MODE_7030:
                return 0.3*klausuren+0.7*tests;
            default:
                return null;
        }

    }

    public List<Zensur> getTests() {

        return SQLite.select().from(Zensur.class).where(Zensur_Table.fach_id.eq(id)).and(Zensur_Table.isKlausur.eq(false)).queryList();
    }

    public List<Zensur> getKlausuren() {

        return SQLite.select().from(Zensur.class).where(Zensur_Table.fach_id.eq(id)).and(Zensur_Table.isKlausur.eq(true)).queryList();
    }

    public Double getTestDurchschnitt() {

        //return SQLite.select(Method.avg(Zensur_Table.zensur).as("avgZensur")).from(Zensur.class).where(Zensur_Table.fach_id.eq(id)).and(Zensur_Table.isKlausur.eq(false)).queryCustomSingle(AverageZensur.class).avgZensur;

        List <Zensur> zensuren = getTests();
        double sum = 0;
        for (Zensur z : zensuren) sum+=z.getZensur();
        return sum==0?null:(sum/zensuren.size());
    }

    public Double getKlausurenDurchschnitt() {

        List <Zensur> zensuren = getKlausuren();
        double sum = 0;
        for (Zensur z : zensuren) sum+=z.getZensur();
        return sum==0?null:(sum/zensuren.size());
    }

    public static List<Fach> getFaecherWithZensuren() {

        List<Zensur> zensuren = SQLite.select(Zensur_Table.fach_id).from(Zensur.class).groupBy(Zensur_Table.fach_id).queryList();
        List<Fach> faecher = new ArrayList<>();
        for (Zensur z : zensuren) faecher.add(z.getFach());

        final boolean isOberstufe = LocalData.isOberstufe();
        Collections.sort(faecher, new Comparator<Fach>() {
            @Override
            public int compare(Fach f1, Fach f2) {
                return isOberstufe ? f2.getZensurenDurchschnitt().compareTo(f1.getZensurenDurchschnitt()) : f1.getZensurenDurchschnitt().compareTo(f2.getZensurenDurchschnitt());
            }
        });

        return faecher;
    }

    public static List<Fach> getSortedFaecherList() {

        return SQLite.select().from(Fach.class).orderBy(Fach_Table.name, true).queryList();
    }

    public static Fach getFach(long id) {

        return SQLite.select().from(Fach.class).where(Fach_Table.id.eq(id)).querySingle();
    }

    public static boolean exists(String name) {

        return SQLite.select().from(Fach.class).where(Fach_Table.name.eq(name)).querySingle()!=null;
    }

    public boolean[][] getAStunden() {

        List<UnterrichtsZeit> unterricht = SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.fach_id.eq(id)).and(UnterrichtsZeit_Table.aWoche.eq(true)).queryList();
        boolean[][] aStunden = new boolean[5][13];
        for (UnterrichtsZeit u : unterricht) aStunden[u.getWochentag()-1][u.getStunde()] = true;
        return aStunden;

    }

    public boolean[][] getBStunden() {

        List<UnterrichtsZeit> unterricht = SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.fach_id.eq(id)).and(UnterrichtsZeit_Table.aWoche.eq(false)).queryList();
        boolean[][] bStunden = new boolean[5][13];
        for (UnterrichtsZeit u : unterricht) bStunden[u.getWochentag()-1][u.getStunde()] = true;
        return bStunden;

    }

    public boolean[][] getBelegteAStunden() {

        List<UnterrichtsZeit> unterricht = SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.aWoche.eq(true)).and(UnterrichtsZeit_Table.fach_id.notEq(id)).queryList();
        boolean[][] belegt = new boolean[5][13];
        for (UnterrichtsZeit u : unterricht) belegt[u.getWochentag()-1][u.getStunde()] = true;
        return belegt;
    }

    public boolean[][] getBelegteBStunden() {

        List<UnterrichtsZeit> unterricht = SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.aWoche.eq(false)).and(UnterrichtsZeit_Table.fach_id.notEq(id)).queryList();
        boolean[][] belegt = new boolean[5][13];
        for (UnterrichtsZeit u : unterricht) belegt[u.getWochentag()-1][u.getStunde()] = true;
        return belegt;
    }

    public boolean[][] getBelegteStunden() {

        List<UnterrichtsZeit> unterricht = SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.fach_id.notEq(id)).queryList();
        boolean[][] belegt = new boolean[5][13];
        for (UnterrichtsZeit u : unterricht) belegt[u.getWochentag()-1][u.getStunde()] = true;
        return belegt;
    }

    public boolean hasStunden() {
        return SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.fach_id.eq(id)).hasData();
    }

    public List<Date> getUnterrichtDaten(Date startDate) {

        boolean[] wochenTage = new boolean[5];
        for (int i=0; i<5; i++) wochenTage[i] = SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.fach_id.eq(id)).and(UnterrichtsZeit_Table.wochentag.eq(i+1)).hasData();

        if (VertretungsAPI.isntSchoolDay(startDate)) startDate = VertretungsAPI.nextSchoolDay(startDate);
        List<Date> dates = new ArrayList<>();
        for (int i=0; i<10; i++) {
            if (wochenTage[Utilities.getDayOfWeek(startDate)-1]) dates.add(startDate);
            startDate = VertretungsAPI.nextSchoolDay(startDate);
        }
        return dates;
    }

    public List<Event> getEventList(Date date) {

        return SQLite.select().from(Event.class).where(Event_Table.fach_id.eq(id)).and(Event_Table.date.eq(date)).queryList();
    }

    public List<Event> getEventList() {

        return SQLite.select().from(Event.class).where(Event_Table.fach_id.eq(id)).queryList();
    }

    public UnterrichtsZeit getUnterrichtsZeit(int wochentag, int stunde, boolean awoche) {

        return SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.fach_id.eq(id)).and(UnterrichtsZeit_Table.wochentag.eq(wochentag)).and(UnterrichtsZeit_Table.stunde.eq(stunde)).and(UnterrichtsZeit_Table.aWoche.eq(awoche)).querySingle();
    }

    public static boolean hasFaecher() {

        return SQLite.select().from(Fach.class).querySingle()!=null;
    }

    public static Fach getFachByKursNr(int kursNr) {

        return SQLite.select().from(Fach.class).where(Fach_Table.kurs_nr.eq(kursNr)).querySingle();
    }

    public boolean hasUnterrichtsZeit(int stunde, int wochenTag, boolean aWoche) {

        return SQLite.select().from(UnterrichtsZeit.class).where(UnterrichtsZeit_Table.fach_id.eq(id))
                .and(UnterrichtsZeit_Table.stunde.eq(stunde))
                .and(UnterrichtsZeit_Table.wochentag.eq(wochenTag))
                .and(UnterrichtsZeit_Table.aWoche.eq(aWoche))
                .querySingle()!=null;
    }
}
