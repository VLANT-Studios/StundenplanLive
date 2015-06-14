package de.conradowatz.jkgvertretung.variables;

import java.util.ArrayList;

/**
 * Created by conrad on 09.06.15.
 */
public class Klasse {

    private ArrayList<Kurs> kurse;
    private String name;

    public Klasse(String name, ArrayList<Kurs> kurse) {
        this.name = name;
        this.kurse = kurse;
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

    public ArrayList<Kurs> getKurse() {

        return kurse;
    }

    public void setKurse(ArrayList<Kurs> kurse) {
        this.kurse = kurse;
    }
}
