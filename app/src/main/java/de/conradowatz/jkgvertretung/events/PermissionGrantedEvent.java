package de.conradowatz.jkgvertretung.events;

public class PermissionGrantedEvent {

    private int requestCode;

    public PermissionGrantedEvent(int requestCode) {

        this.requestCode = requestCode;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }
}
