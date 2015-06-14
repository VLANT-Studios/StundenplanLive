package de.conradowatz.jkgvertretung.variables;


public class Vertretung {

    private String klasse;
    private String stunde;
    private String fach;
    private String raum;
    private String info;

    public Vertretung(String klasse, String stunde, String fach, String raum, String info) {
        this.klasse = klasse;
        this.stunde = stunde;
        this.fach = fach;
        this.raum = raum;
        this.info = info;
    }

    public Vertretung() {

    }

    public String getKlasse() {
        return klasse;
    }

    public void setKlasse(String klasse) {
        this.klasse = klasse;
    }

    public String getStunde() {
        return stunde;
    }

    public void setStunde(String stunde) {
        this.stunde = stunde;
    }

    public String getFach() {
        return fach;
    }

    public void setFach(String fach) {
        this.fach = fach;
    }

    public String getRaum() {
        return raum;
    }

    public void setRaum(String raum) {
        this.raum = raum;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
