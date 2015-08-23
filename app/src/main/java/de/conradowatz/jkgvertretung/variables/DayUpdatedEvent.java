package de.conradowatz.jkgvertretung.variables;

public class DayUpdatedEvent {

    private int position;

    public DayUpdatedEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
