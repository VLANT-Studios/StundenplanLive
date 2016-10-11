package de.conradowatz.jkgvertretung.events;


public class AnalyticsScreenHitEvent {

    String screenName;

    public AnalyticsScreenHitEvent(String screenName) {

        this.screenName = screenName;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }
}
