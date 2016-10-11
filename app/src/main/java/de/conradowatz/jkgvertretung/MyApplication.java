package de.conradowatz.jkgvertretung;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.conradowatz.jkgvertretung.events.AnalyticsEventEvent;
import de.conradowatz.jkgvertretung.events.AnalyticsScreenHitEvent;

public class MyApplication extends Application {

    public static GoogleAnalytics analytics;
    public static Tracker tracker;
    private static MyApplication sInstance;

    private EventBus eventBus = EventBus.getDefault();

    public static MyApplication getsInstance() {
        return sInstance;
    }

    public static Context getAppContext() {
        return sInstance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        eventBus.register(this);

        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(600);

        tracker = analytics.newTracker(R.xml.app_tracker);
        tracker.enableExceptionReporting(true);
        tracker.enableAdvertisingIdCollection(true);
        tracker.enableAutoActivityTracking(true);
    }

    private void fireScreenHit(String screenName) {

        tracker.setScreenName(screenName);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

    }

    private void fireEvent(String category, String action) {

        tracker.send(new HitBuilders.EventBuilder(category, action).build());

    }

    public void fireException(Throwable t) {

        tracker.send(new HitBuilders.ExceptionBuilder().setDescription(
                new StandardExceptionParser(getApplicationContext(), null)
                        .getDescription(Thread.currentThread().getName(), t))
                .setFatal(false)
                .build());
    }

    @Subscribe
    public void onEvent(AnalyticsScreenHitEvent event) {
        fireScreenHit(event.getScreenName());
    }

    @Subscribe
    public void onEvent(AnalyticsEventEvent event) {
        fireEvent(event.getCategory(), event.getAction());
    }

    @Override
    public void onTerminate() {
        eventBus.unregister(this);
        super.onTerminate();
    }
}