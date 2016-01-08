package de.conradowatz.jkgvertretung.tools;

import java.util.ArrayList;
import java.util.Date;

import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Tag;

public class VertretungsData {

    private static VertretungsData sInstance = null;

    private ArrayList<Klasse> klassenList;
    private ArrayList<Date> freieTageList;
    private ArrayList<Tag> tagList;

    private VertretungsData() {

    }

    public static VertretungsData getInstance() {

        if (sInstance == null) sInstance = new VertretungsData();
        return sInstance;
    }

    public static void setInstance(VertretungsData vertretungsData) {

        sInstance = vertretungsData;
    }

    public ArrayList<Klasse> getKlassenList() {
        return klassenList;
    }

    public void setKlassenList(ArrayList<Klasse> klassenList) {
        this.klassenList = klassenList;
    }

    public ArrayList<Date> getFreieTageList() {
        return freieTageList;
    }

    public void setFreieTageList(ArrayList<Date> freieTageList) {
        this.freieTageList = freieTageList;
    }

    public ArrayList<Tag> getTagList() {
        return tagList;
    }

    public void setTagList(ArrayList<Tag> tagList) {
        this.tagList = tagList;
    }

    public boolean isReady() {

        return (klassenList != null && freieTageList != null && tagList != null);
    }
}
