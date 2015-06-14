package de.conradowatz.jkgvertretung.variables;


import java.util.ArrayList;

public class StuPlaKlasse {

    private ArrayList<Stunde> stundenList;
    private String name;

    public StuPlaKlasse(ArrayList<Stunde> stundenList, String name) {
        this.stundenList = stundenList;
        this.name = name;
    }

    public StuPlaKlasse() {

    }

    public ArrayList<Stunde> getStundenList() {
        return stundenList;
    }

    public void setStundenList(ArrayList<Stunde> stundenList) {
        this.stundenList = stundenList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
