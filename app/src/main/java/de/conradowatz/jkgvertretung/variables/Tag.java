package de.conradowatz.jkgvertretung.variables;

import java.util.ArrayList;
import java.util.Date;

public class Tag {

    private Date datum;
    private String datumString;
    private String zeitStempel;
    private ArrayList<StuPlaKlasse> stuplaKlasseList;
    private ArrayList<Vertretung> vertretungsList;

    public Tag(Date datum, String datumString, String zeitStempel, ArrayList<StuPlaKlasse> stuplaKlasseList, ArrayList<Vertretung> vertretungsList) {

        this.datum = datum;
        this.datumString = datumString;
        this.zeitStempel = zeitStempel;
        this.stuplaKlasseList = stuplaKlasseList;
        this.vertretungsList = vertretungsList;
    }

    public Tag() {

    }

    public Date getDatum() {
        return datum;
    }

    public void setDatum(Date datum) {
        this.datum = datum;
    }

    public String getDatumString() {
        return datumString;
    }

    public void setDatumString(String datumString) {
        this.datumString = datumString;
    }

    public String getZeitStempel() {
        return zeitStempel;
    }

    public void setZeitStempel(String zeitStempel) {
        this.zeitStempel = zeitStempel;
    }

    public ArrayList<StuPlaKlasse> getStuplaKlasseList() {
        return stuplaKlasseList;
    }

    public void setStuplaKlasseList(ArrayList<StuPlaKlasse> stuplaKlasseList) {
        this.stuplaKlasseList = stuplaKlasseList;
    }

    public ArrayList<Vertretung> getVertretungsList() {
        return vertretungsList;
    }

    public void setVertretungsList(ArrayList<Vertretung> vertretungsList) {
        this.vertretungsList = vertretungsList;
    }
}
