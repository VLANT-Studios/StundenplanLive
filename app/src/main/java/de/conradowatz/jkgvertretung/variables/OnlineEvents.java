package de.conradowatz.jkgvertretung.variables;

import java.util.ArrayList;
import java.util.List;

public class OnlineEvents {

    public static int compatibleFileFormatVersion = 1;
    private List<Event> events;
    private List<Integer> uniqueEventNumbers;
    private int fileFormatVersion;

    public OnlineEvents(List<Event> events, List<Integer> uniqueEventNumbers, int fileFormatVersion) {
        this.events = events;
        this.uniqueEventNumbers = uniqueEventNumbers;
        this.fileFormatVersion = fileFormatVersion;
    }

    public OnlineEvents(int fileFormatVersion) {

        this.fileFormatVersion = fileFormatVersion;
        events = new ArrayList<>();
        uniqueEventNumbers = new ArrayList<>();
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public int getFileFormatVersion() {
        return fileFormatVersion;
    }

    public void setFileFormatVersion(int fileFormatVersion) {
        this.fileFormatVersion = fileFormatVersion;
    }

    public List<Integer> getUniqueEventNumbers() {
        return uniqueEventNumbers;
    }

    public void setUniqueEventNumbers(List<Integer> uniqueEventNumbers) {
        this.uniqueEventNumbers = uniqueEventNumbers;
    }
}
