package de.conradowatz.jkgvertretung.events;

public class AnalyticsEventEvent {

    private String category;
    private String action;

    public AnalyticsEventEvent(String category, String action) {
        this.category = category;
        this.action = action;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
