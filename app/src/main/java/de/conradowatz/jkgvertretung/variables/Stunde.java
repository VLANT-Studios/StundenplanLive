package de.conradowatz.jkgvertretung.variables;

public class Stunde {

    private String stunde;
    private String fach;
    private String kurs;
    private boolean fachg;
    private String raum;
    private boolean raumg;
    private String info;

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

    public String getKurs() {
        return kurs;
    }

    public void setKurs(String kurs) {
        this.kurs = kurs;
    }

    public boolean isFachg() {
        return fachg;
    }

    public void setFachg(boolean fachg) {
        this.fachg = fachg;
    }

    public String getRaum() {
        return raum;
    }

    public void setRaum(String raum) {
        this.raum = raum;
    }

    public boolean isRaumg() {
        return raumg;
    }

    public void setRaumg(boolean raumg) {
        this.raumg = raumg;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Stunde(String stunde, String fach, String kurs, boolean fachg, String raum, boolean raumg, String info) {

        this.stunde = stunde;
        this.fach = fach;
        this.kurs = kurs;
        this.fachg = fachg;
        this.raum = raum;
        this.raumg = raumg;
        this.info = info;
    }

    public Stunde() {

    }

}
