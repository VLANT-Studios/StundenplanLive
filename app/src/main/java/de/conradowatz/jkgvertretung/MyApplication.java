package de.conradowatz.jkgvertretung;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class MyApplication extends Application {

    public static GoogleAnalytics analytics;
    public static Tracker tracker;

    @Override
    public void onCreate() {
        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(30);

        tracker = analytics.newTracker(R.xml.app_tracker);
        tracker.enableExceptionReporting(true);
        tracker.enableAdvertisingIdCollection(true);
        tracker.enableAutoActivityTracking(true);
    }

    public void fireScreenHit(String screenName) {

        tracker.setScreenName(screenName);
        tracker.send(new HitBuilders.AppViewBuilder().build());

    }

    public void fireEvent(String category, String action) {

        tracker.send(new HitBuilders.EventBuilder(category, action).build());

    }
}