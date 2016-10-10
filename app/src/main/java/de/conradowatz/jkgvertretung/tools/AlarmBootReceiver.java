package de.conradowatz.jkgvertretung.tools;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmBootReceiver extends BroadcastReceiver {

    public static String NOTIFICATION_ID = "notification-id";
    public static String NOTIFICATION_DATE = "notification-date";
    public static String NOTIFICATION = "notification";
    public static String INTENT_NOTIFICATION = "intent.action.EVENT_NOTIFICATION";

    @Override
    public void onReceive(final Context context, Intent intent) {

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            LocalData.createFromFile(context, new LocalData.CreateDataFromFileListener() {
                @Override
                public void onDataCreated() {
                    LocalData.recreateNotificationAlarms(context);
                }

                @Override
                public void onError(Throwable throwable) {

                }
            });
        } else if (intent.getAction().equals(INTENT_NOTIFICATION)) {

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            Notification notification = intent.getParcelableExtra(NOTIFICATION);
            int id = intent.getIntExtra(NOTIFICATION_ID, 0);
            notificationManager.notify(id, notification);
        }
    }
}
