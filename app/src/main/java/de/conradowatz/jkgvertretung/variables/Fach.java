package de.conradowatz.jkgvertretung.variables;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Fach {

    private String name;
    private List<Integer> sonstigeNoten;
    private List<Integer> klausurenNoten;
    private List<Event> events;
    private boolean[][] aStunden;
    private boolean[][] bStunden;

    public Fach(String name) {

        this.name = name;
        sonstigeNoten = new ArrayList<>();
        klausurenNoten = new ArrayList<>();
        events = new ArrayList<>();
        aStunden = new boolean[5][9];
        bStunden = new boolean[5][9];
    }

    public Double getSonstigeAverage() {

        if (sonstigeNoten.isEmpty()) return null;
        int sum = 0;
        for (Integer note : sonstigeNoten) {
            sum += note;
        }
        return ((double) sum) / sonstigeNoten.size();

    }

    public Double getKlausurenAverage() {

        if (klausurenNoten.isEmpty()) return null;
        int sum = 0;
        for (Integer note : klausurenNoten) {
            sum += note;
        }
        return ((double) sum) / klausurenNoten.size();

    }

    public Double getNotenAverage() {

        if (sonstigeNoten.isEmpty() && klausurenNoten.isEmpty()) return null;
        Double sontsige = getSonstigeAverage();
        Double klausuren = getKlausurenAverage();
        if (sontsige == null) return klausuren;
        if (klausuren == null) return sontsige;
        return (sontsige + klausuren) / 2;

    }

    public List<Integer> getKlausurenNoten() {
        return klausurenNoten;
    }

    public void setKlausurenNoten(List<Integer> klausurenNoten) {
        this.klausurenNoten = klausurenNoten;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getSonstigeNoten() {
        return sonstigeNoten;
    }

    public void setSonstigeNoten(List<Integer> sonstigeNoten) {
        this.sonstigeNoten = sonstigeNoten;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public boolean[][] getaStunden() {
        return aStunden;
    }

    public void setaStunden(boolean[][] aStunden) {
        this.aStunden = aStunden;
    }

    public boolean[][] getStunden(boolean isaWoche) {
        return (isaWoche ? aStunden : bStunden);
    }

    public boolean[][] getbStunden() {
        return bStunden;
    }

    public void setbStunden(boolean[][] bStunden) {
        this.bStunden = bStunden;
    }

    public void sortEvents() {

        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return e1.getDatum().compareTo(e2.getDatum());
            }
        });

    }

    public boolean hasStunden() {
        for (boolean[] anAStunden : aStunden) {
            for (boolean anAnAStunden : anAStunden) {
                if (anAnAStunden) return true;
            }
        }
        for (boolean[] aBStunden : bStunden) {
            for (boolean anABStunden : aBStunden) {
                if (anABStunden) return true;
            }
        }
        return false;
    }
}
