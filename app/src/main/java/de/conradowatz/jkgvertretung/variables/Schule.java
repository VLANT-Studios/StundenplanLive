package de.conradowatz.jkgvertretung.variables;

public class Schule {

    public String name;
    public boolean hasAuth;
    public String baseUrl;

    public Schule(String name, boolean hasAuth, String baseUrl) {
        this.name = name;
        this.hasAuth = hasAuth;
        this.baseUrl = baseUrl;
    }
}
