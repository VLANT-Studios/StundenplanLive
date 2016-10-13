package de.conradowatz.jkgvertretung.tools;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.EventActivity;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.events.FaecherUpdateEvent;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.Ferien;
import de.conradowatz.jkgvertretung.variables.StuPlaKlasse;
import de.conradowatz.jkgvertretung.variables.Stunde;
import de.conradowatz.jkgvertretung.variables.Tag;

public class LocalData {

    public static final String SAVE_FILE_NAME = "localData.json";
    private static LocalData sInstance = null;
    private static int latestSaveFileVersion = 1;
    private int saveFileVersion;
    private List<Event> noFachEvents;
    private Date compareDate;
    private boolean isCompareAWoche;
    private List<Fach> fächer;
    private List<Ferien> ferien;
    private List<Integer> uniqueEventNumbers;

    private LocalData() {

        fächer = new ArrayList<>();
        noFachEvents = new ArrayList<>();
        ferien = new ArrayList<>();
        uniqueEventNumbers = new ArrayList<>();
        saveFileVersion = latestSaveFileVersion;

    }

    public static LocalData getInstance() {

        return sInstance;
    }

    public static void setInstance(LocalData localData) {

        sInstance = localData;
    }

    public static void saveToFile(Context context) {

        Gson gson = Utilities.getDefaultGson();
        String json = gson.toJson(LocalData.getInstance(), LocalData.class);


        try {

            FileOutputStream outputStream = context.openFileOutput(SAVE_FILE_NAME, Context.MODE_PRIVATE);
            outputStream.write(json.getBytes(Charset.forName("UTF-8")));
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static void createFromFile(Context context, CreateDataFromFileListener listener) {

        //Read Data
        try {
            FileInputStream inputStream = context.openFileInput(SAVE_FILE_NAME);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder(inputStream.available());
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }

            Gson gson = Utilities.getDefaultGson();
            LocalData localData = gson.fromJson(total.toString(), LocalData.class);
            if (localData == null || localData.saveFileVersion != latestSaveFileVersion)
                throw new Exception("Saved File is not compatible.");
            LocalData.setInstance(localData);

            //Lösche vergangene Events
            Calendar heute = Calendar.getInstance();
            List<Event> eventList = LocalData.getInstance().getNoFachEvents();
            for (int i = 0; i < eventList.size(); i++) {
                Event e = eventList.get(i);
                if (e.isDeleteWhenElapsed() && Utilities.compareDays(heute.getTime(), e.getDatum()) > 0) {
                    eventList.remove(i);
                    i--;
                }
            }
            for (Fach f : LocalData.getInstance().getFächer()) {
                eventList = f.getEvents();
                for (int i = 0; i < eventList.size(); i++) {
                    Event e = eventList.get(i);
                    if (e.isDeleteWhenElapsed() && Utilities.compareDays(heute.getTime(), e.getDatum()) > 0) {
                        eventList.remove(i);
                        i--;
                    }
                }
            }

            listener.onDataCreated();

        } catch (Exception e) {

            listener.onError(e);
        }

    }

    public static void createNewLocalData() {

        LocalData.setInstance(new LocalData());
    }

    public static String[] getAllRooms() {

        return new String[]{"%FK08", "%FK10",
                "%F004", "%F007", "008", "009", "010", "012",   //011 und 116 VKAs
                "108", "109", "110", "%F111", "%F113", "%F115",
                "%F201", "203", "204", "205", "208", "209", "210", "211", "212", "%F213", "%F215",
                "%F301", "%F302", "304", "305", "306", "308", "309", "310", "311", "312", "%F313", "%F315"};
    }

    public static boolean isOberstufe(Context context) {

        //Wenn die eigene Klasse eine der letzten beiden Einträge in der Liste ist muss es 11/12 sein
        return PreferenceHelper.readIntFromPreferences(context, "meineKlasseInt", 0) > VertretungsData.getInstance().getKlassenList().size() - 3;
    }

    public static boolean[][] getBelegteStunden(Fach fach, boolean isaWoche) {

        boolean[][] belegt = new boolean[5][9];

        for (int tag = 0; tag < 5; tag++) {
            for (int stunde = 0; stunde < 9; stunde++) {
                for (Fach f : getInstance().getFächer()) {
                    if (f == fach) continue;
                    if (f.getStunden(isaWoche)[tag][stunde]) {
                        belegt[tag][stunde] = true;
                        break;
                    }
                }
            }
        }
        return belegt;
    }

    public static List<Stunde> getOfflineStundenList(int dayOfWeek, boolean isAWoche) {

        List<Stunde> stundenList = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            for (Fach f : getInstance().getFächer()) {
                if (f.getStunden(isAWoche)[dayOfWeek - 1][i]) {
                    Stunde stunde = new Stunde(String.valueOf(i + 1), f.getName(), "", false, "", false, "");
                    stundenList.add(stunde);
                }
            }
        }

        return stundenList;
    }

    private static void activateAlarmManager(Context context) {

        ComponentName receiver = new ComponentName(context, AlarmBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    private static void deactivateAlarmManager(Context context) {

        ComponentName receiver = new ComponentName(context, AlarmBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static void deleteNotificationAlarms(Context context) {

        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();

        for (int i = 0; i < LocalData.getInstance().getNoFachEvents().size(); i++) {
            Event e = LocalData.getInstance().getNoFachEvents().get(i);
            for (Date d : e.getReminders()) {
                if (d.after(now))
                    cancelNotification(context, buildReminderNotification(context, e, -1, i), d);
            }
        }

        for (int i = 0; i < LocalData.getInstance().getFächer().size(); i++) {
            Fach f = LocalData.getInstance().getFächer().get(i);
            for (int j = 0; j < f.getEvents().size(); j++) {
                Event e = f.getEvents().get(j);
                for (Date d : e.getReminders()) {
                    if (d.after(now))
                        cancelNotification(context, buildReminderNotification(context, e, i, j), d);
                }
            }
        }
    }

    public static void recreateNotificationAlarms(Context context) {

        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();

        for (int i = 0; i < LocalData.getInstance().getNoFachEvents().size(); i++) {
            Event e = LocalData.getInstance().getNoFachEvents().get(i);
            for (Date d : e.getReminders()) {
                if (d.after(now))
                    scheduleNotification(context, buildReminderNotification(context, e, -1, i), d);
            }
        }

        for (int i = 0; i < LocalData.getInstance().getFächer().size(); i++) {
            Fach f = LocalData.getInstance().getFächer().get(i);
            for (int j = 0; j < f.getEvents().size(); j++) {
                Event e = f.getEvents().get(j);
                for (Date d : e.getReminders()) {
                    if (d.after(now))
                        scheduleNotification(context, buildReminderNotification(context, e, i, j), d);
                }
            }
        }
    }

    private static int getNotificationDefaults(Context context) {

        String notificationType = PreferenceHelper.readStringFromPreferences(context, "notificationType", "1");
        switch (notificationType) {
            case "1":
                return NotificationCompat.DEFAULT_ALL;
            case "2":
                return (NotificationCompat.DEFAULT_SOUND | +NotificationCompat.DEFAULT_LIGHTS);
            default:
                return NotificationCompat.DEFAULT_LIGHTS;
        }
    }

    private static Notification buildReminderNotification(Context context, Event event, int fachInt, int eventInt) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        String title = event.getTitle();
        if (fachInt > -1) {
            Fach fach = LocalData.getInstance().getFächer().get(fachInt);
            title = fach.getName() + ": " + title;
        }
        builder.setContentTitle(title);
        if (!event.getDescription().isEmpty()) builder.setContentText(event.getDescription());
        builder.setSmallIcon(R.drawable.ic_assignment_late_white_18dp);
        builder.setAutoCancel(true);
        builder.setDefaults(getNotificationDefaults(context));

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(title);
        if (event.getDescription().contains("\n")) {
            String[] descArray = event.getDescription().split("\n");
            for (int i = 0; i < descArray.length && i < 5; i++) {
                inboxStyle.addLine(descArray[i]);
            }
        } else if (!event.getDescription().isEmpty()) inboxStyle.addLine(event.getDescription());

        builder.setStyle(inboxStyle);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        Intent openEventIntent = new Intent(context, EventActivity.class);
        openEventIntent.putExtra("eventInt", eventInt);
        openEventIntent.putExtra("fachInt", fachInt);

        if (fachInt > -1) {
            Intent openFachEvent = new Intent(context, FachActivity.class);
            openEventIntent.putExtra("fachIndex", fachInt);
            stackBuilder.addNextIntent(openFachEvent);
        }
        stackBuilder.addNextIntent(openEventIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        return builder.build();

    }

    public static void removeEventReminder(Context context, Event event, int fachInt, int eventInt) {

        for (Date d : event.getReminders())
            cancelNotification(context, buildReminderNotification(context, event, fachInt, eventInt), d);

    }

    public static void addEventReminder(Context context, Event event, int fachInt, int eventInt) {

        if (LocalData.getInstance().hasReminders()) activateAlarmManager(context);
        else deactivateAlarmManager(context);

        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();

        for (Date d : event.getReminders())
            if (d.after(now))
                scheduleNotification(context, buildReminderNotification(context, event, fachInt, eventInt), d);
    }

    private static void scheduleNotification(Context context, Notification notification, Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        Intent notificationIntent = new Intent(context, AlarmBootReceiver.class);
        notificationIntent.putExtra(AlarmBootReceiver.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(AlarmBootReceiver.NOTIFICATION_DATE, calendar.getTimeInMillis());
        notificationIntent.putExtra(AlarmBootReceiver.NOTIFICATION, notification);
        notificationIntent.setAction(AlarmBootReceiver.INTENT_NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    private static void cancelNotification(Context context, Notification notification, Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        Intent notificationIntent = new Intent(context, AlarmBootReceiver.class);
        notificationIntent.putExtra(AlarmBootReceiver.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(AlarmBootReceiver.NOTIFICATION_DATE, calendar.getTimeInMillis());
        notificationIntent.putExtra(AlarmBootReceiver.NOTIFICATION, notification);
        notificationIntent.setAction(AlarmBootReceiver.INTENT_NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    public static boolean isReady() {
        return sInstance != null && sInstance.compareDate != null;
    }

    public List<Event> getNoFachEvents() {
        return noFachEvents;
    }

    public void setNoFachEvents(List<Event> noFachEvents) {
        this.noFachEvents = noFachEvents;
    }

    public void updateCompareDate() {

        int size = VertretungsData.getInstance().getTagList().size();
        if (size > 0) {
            compareDate = VertretungsData.getInstance().getTagList().get(size - 1).getDatum();
            isCompareAWoche = VertretungsData.getInstance().getTagList().get(size - 1).getDatumString().contains("A");
        }
    }

    public void setCompareDate(Date compareDate, boolean isCompareAWoche) {

        this.compareDate = compareDate;
        this.isCompareAWoche = isCompareAWoche;
    }

    public List<Fach> getFächer() {
        return fächer;
    }

    public void setFächer(List<Fach> fächer) {
        this.fächer = fächer;
    }

    public void sortFächer() {

        Collections.sort(fächer, new Comparator<Fach>() {
            @Override
            public int compare(Fach f1, Fach f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });
    }

    public List<Integer> getUniqueEventNumbers() {
        return uniqueEventNumbers;
    }

    public void setUniqueEventNumbers(List<Integer> uniqueEventNumbers) {
        this.uniqueEventNumbers = uniqueEventNumbers;
    }

    public void sortNoFachEvents() {

        Collections.sort(noFachEvents, new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return e1.getDatum().compareTo(e2.getDatum());
            }
        });
    }

    public void sortFerien() {

        Collections.sort(ferien, new Comparator<Ferien>() {
            @Override
            public int compare(Ferien f1, Ferien f2) {
                return f1.getStartDate().compareTo(f2.getStartDate());
            }
        });

    }

    public List<Ferien> getFerien() {
        return ferien;
    }

    public void setFerien(List<Ferien> ferien) {
        this.ferien = ferien;
    }

    public boolean isFerien(Date date) {

        for (Ferien f : ferien) {
            if (Utilities.compareDays(date, f.getStartDate()) >= 0 && Utilities.compareDays(date, f.getEndDate()) <= 0)
                return true;
        }
        return false;
    }

    public boolean isAWoche(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Calendar compareCalendar = Calendar.getInstance();
        compareCalendar.setTime(compareDate);

        int diff = calendar.get(Calendar.WEEK_OF_YEAR) - compareCalendar.get(Calendar.WEEK_OF_YEAR);
        return ((diff % 2 == 1) != isCompareAWoche);

    }

    private boolean hasReminders() {

        for (Event e : noFachEvents) {
            if (e.getReminders().size() > 0) return true;
        }
        for (Fach f : fächer) {
            for (Event e : f.getEvents()) {
                if (e.getReminders().size() > 0) return true;
            }
        }
        return false;
    }

    /**
     * Erstellt offline Daten Fächer automatisch anhand der vorhandenen online Vertretungs Daten
     */
    public void smartImport(Activity context) {

        int meineKlasseInt = PreferenceHelper.readIntFromPreferences(context.getApplicationContext(), "meineKlasseInt", -1);
        if (meineKlasseInt == -1) {
            Toast.makeText(context, "Kein(e) Klasse/Kurs ausgewählt!", Toast.LENGTH_LONG).show();
            return;
        }
        ArrayList<String> nichtKurse = PreferenceHelper.readStringListFromPreferences(context.getApplicationContext(), "meineNichtKurse");
        if (nichtKurse == null) nichtKurse = new ArrayList<>();

        for (Tag tag : VertretungsData.getInstance().getTagList()) {
            boolean isAWoche = isAWoche(tag.getDatum());
            int wochenTagIndex = Utilities.getDayOfWeek(tag.getDatum()) - 1; //0-4
            StuPlaKlasse stuPlaKlasse = tag.getStuplaKlasseList().get(meineKlasseInt);
            for (Stunde stunde : stuPlaKlasse.getStundenList()) {

                if (nichtKurse.contains(stunde.getKurs())) continue;

                String stundenName = stunde.getKurs() != null ? stunde.getKurs() : stunde.getFach();

                try {
                    int stundenIndex = Integer.valueOf(stunde.getStunde()) - 1; //0-9
                    Fach fach = null;
                    for (Fach f : LocalData.getInstance().getFächer()) {
                        if (f.getName().equals(stundenName)) {
                            fach = f;
                            break;
                        }
                    }
                    if (fach == null) {
                        fach = new Fach(stundenName);
                        LocalData.getInstance().getFächer().add(fach);
                    }
                    fach.getStunden(isAWoche)[wochenTagIndex][stundenIndex] = true;
                } catch (NumberFormatException e) {
                    Log.e("JKGDEBUG", "SmartImport: Stunde hat iregulären Index");
                    e.printStackTrace();
                }
            }
        }

        LocalData.getInstance().sortFächer();
        EventBus.getDefault().post(new FaecherUpdateEvent());
        LocalData.saveToFile(context.getApplicationContext());

    }

    public interface CreateDataFromFileListener {

        void onDataCreated();

        void onError(Throwable throwable);
    }
}
