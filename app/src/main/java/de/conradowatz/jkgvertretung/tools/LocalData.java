package de.conradowatz.jkgvertretung.tools;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import androidx.core.app.TaskStackBuilder;
import androidx.core.app.NotificationCompat;

import android.widget.Toast;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;
import com.raizlabs.android.dbflow.structure.database.transaction.ITransaction;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.EventActivity;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.events.EventsChangedEvent;
import de.conradowatz.jkgvertretung.events.FaecherUpdateEvent;
import de.conradowatz.jkgvertretung.events.FerienChangedEvent;
import de.conradowatz.jkgvertretung.variables.AppDatabase;
import de.conradowatz.jkgvertretung.variables.Erinnerung;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Event_Table;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.Ferien;
import de.conradowatz.jkgvertretung.variables.Ferien_Table;
import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Kurs;
import de.conradowatz.jkgvertretung.variables.Schule;
import de.conradowatz.jkgvertretung.variables.Stunde;
import de.conradowatz.jkgvertretung.variables.UnterrichtsZeit;
import de.conradowatz.jkgvertretung.variables.Zensur;

public class LocalData {

    public static boolean isOberstufe() {

        String klassenName = PreferenceHelper.readStringFromPreferences(MyApplication.getAppContext(), "selectedKlasse", "");
        return klassenName.contains("11") || klassenName.contains("12");
    }

    public static String makeDateHeadingString(Date date) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMAN);
        if (hasABWoche()) {
            String wochenString = (LocalData.isAWoche(date)) ? "(A-Woche)" : "(B-Woche)";
            return String.format(Locale.GERMANY, "%s %s", dateFormat.format(date), wochenString);
        } else return dateFormat.format(date);
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

        List<Erinnerung> erinnerungen = SQLite.select().from(Erinnerung.class).queryList();

        for (Erinnerung e : erinnerungen) {
            if (e.getDate().after(now))
                cancelNotification(context, buildReminderNotification(context, e.getEvent()), e.getDate());
        }
    }

    public static void recreateNotificationAlarms(Context context) {

        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();

        List<Erinnerung> erinnerungen = SQLite.select().from(Erinnerung.class).queryList();

        for (Erinnerung e : erinnerungen) {
            if (e.getDate().after(now))
                scheduleNotification(context, buildReminderNotification(context, e.getEvent()), e.getDate());
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

    private static Notification buildReminderNotification(Context context, Event event) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        String title = event.getName();
        if (event.getFach() != null) {
            title = event.getFach().getName() + ": " + title;
        }
        builder.setContentTitle(title);
        if (!event.getDescription().isEmpty()) builder.setContentText(event.getDescription());
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
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
        openEventIntent.putExtra("eventId", event.getId());

        if (event.getFach() != null) {
            Intent openFachEvent = new Intent(context, FachActivity.class);
            openEventIntent.putExtra("fachId", event.getFach().getId());
            openFachEvent.putExtra("fachId", event.getFach().getId());
            stackBuilder.addNextIntent(openFachEvent);
        }
        stackBuilder.addNextIntent(openEventIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        return builder.build();

    }

    public static void removeEventReminder(Context context, Event event) {

        for (Erinnerung e : event.getErinnerungen())
            cancelNotification(context, buildReminderNotification(context, e.getEvent()), e.getDate());

    }

    public static void addEventReminder(Context context, Event event) {

        if (LocalData.hasReminders()) activateAlarmManager(context);
        else deactivateAlarmManager(context);

        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();

        for (Erinnerung e : event.getErinnerungen())
            if (e.getDate().after(now))
                scheduleNotification(context, buildReminderNotification(context, event), e.getDate());
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

    public static boolean isLoggedIn() {

        return PreferenceHelper.readBooleanFromPreferences(MyApplication.getAppContext(), "loggedIn", false);
    }

    public static void setLoggedIn(boolean loggedIn) {

        PreferenceHelper.saveBooleanToPrefernces(MyApplication.getAppContext(), "loggedIn", loggedIn);
    }

    public static Schule[] getSchulen() {

        return new Schule[]{
                new Schule("Johannes-Kepler-Gymnasium Chemnitz", true, "https://plan.kepler-chemnitz.de/stuplanindiware/VmobilS/"),
                new Schule("Immanuel-Kant-Gymnasium Leipzig", false, "http://www.kantgym-leipzig.de/App/"),
                new Schule("KGS Altentreptow", false, "http://mobil.kgs-altentreptow.de/"),
                new Schule("neue friedländer gesamtschule", false, "http://www.nfg24.de/planung/mobil/"),
                new Schule("Gymnasium Martineum Halberstadt", false, "http://www.martineum-halberstadt.de/mobil/"),
                new Schule("Evangelisches Schulzentrum Leipzig", false, "https://schulzentrum.de/files/plaene/VPmobil/"),
                new Schule("Wehrdigtschule Glauchau", false, "https://www.wehrdigtschule.de/mobil/"),
                new Schule("Martin-Rinckart-Gymnasium Eilenburg", false, "https://www.mrge.de/mobile/kl/"),
                new Schule("Gymnasium Luisenstift Radebeul", false, "http://luisenstift.de/vplan/mobil/"),
                new Schule("Freies Gymnasium Naunhof", false, "http://freies-gymnasium-naunhof.de/plan/klassen/"),
                new Schule("Christian Weise Gymnasium Zittau", false, "http://gymnasium-zittau.de/indiware/mobil/"),
                new Schule("Oberschule Bischofswerda", false, "http://os.bischofswerda.de/vp/mobil/"),
                new Schule("Staatliche Regelschule \"Anne Frank\" Themar", false, "http://www.regelschule-annefrank-themar.de/Mobil/"),
                new Schule("CJD Christophorusschule Droyßig", false, "http://www.cjd-droyssig.de/fileadmin/assets/droyssig/2014/Termine/VPmobilKlassen/"),
                new Schule("Luther-Melanchthon-Gymnasium Wittenberg", false, "http://www.hundertwasserschule.de/fileadmin/Daten/Daten_Lehrer/Daten_Vertretungsplan/IndiwareMobil/"),
                new Schule("Gerhart-Hauptmann-Gymnasium Wernigerode", false, "http://www.ghgw.de/mobil/"),
                new Schule("Kurfürst-Joachim-Friedrich-Gymnasium", true, "https://kjf-gym.de/vplansmobile/"),
                new Schule("Schulzentrum Am Sund Stralsund", false, "http://plan.schulzentrum-am-sund.de/"),
                new Schule("Regionale Schule Waren/West", false, "http://www.rww24.de/Vertretungsplan/mobil-Schueler/"),
                new Schule("SportOberschule Leipzig", false, "http://www.sportoberschule-leipzig.de/1_Vertretungsplan/"),
                new Schule("Hans-Dietrich-Genscher Gymnasium Halle", true, "http://planung.hdg-gymnasium.de/mobil/"),
                new Schule("Gymnasiales Schulzentrum \"Felix Stillfried\" Stralendorf", false, "http://www.schulzentrum-stralendorf.de/mobil/"),
                new Schule("Thüringer Gemeinschaftsschule Bürgel", false, "http://www.schule-buergel.de/tl_files/schwarzesbrett/Mobil/"),
                new Schule("Friedrich-Ludwig-Jahn-Gymnasium Salzwedel", false, "http://mobil.jahngymnasium-salzwedel.de/"),
                new Schule("Albert-Schweitzer-Gymnasium Wolfsburg", false, "http://asg-wob.de/sites/default/files/mobil/"),
                new Schule("Christliche Schule Dresden-Zschachwitz", false, "http://archiv.cs-dresden.de/gym/ablage/mobil/"),
                new Schule("Geschwister-Scholl-Gymnasium Taucha", true, "http://www.gymnasium-taucha.de/vplan/mobil/"),
                new Schule("Richard-Wossidlo-Gymnasium Waren", false, "http://www.richard-wossidlo-gymnasium-waren.de/stundenplan/vplan/"),
                new Schule("Léon-Foucault-Gymnasium Hoyerswerda", true, "http://www.foucault-gymnasium.de/svplan/"),
                new Schule("Goethe-Gymnasium Bischofswerda", false, "http://www.goethegym-biw.de/mobile/klassen/")
        };
    }

    /*public static void testSchools() {

        for (final Schule s : getSchulen()) {

            if (s.baseUrl.isEmpty() || s.hasAuth) continue;

            OkHttpClient client = new OkHttpClient.Builder().build();
            client.newCall(new Request.Builder().url(s.baseUrl+"mobdaten/Klassen.xml").build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d("JKGDEBUG", "Schule: " + s.name);
                    Log.d("JKGDEBUG", "failed");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String html = response.body().string();

                        try {
                            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder builder = factory.newDocumentBuilder();
                            InputSource is = new InputSource();
                            is.setCharacterStream(new StringReader(html));
                            Document dom = builder.parse(is);

                            NodeList list = dom.getElementsByTagName("UeNr");
                            Log.d("JKGDEBUG", "Schule: " + s.name);
                            Log.d("JKGDEBUG", "found: "+list.getLength());
                        } catch (SAXException | ParserConfigurationException e) {
                            Log.d("JKGDEBUG", "Schule: " + s.name);
                            Log.d("JKGDEBUG", "failed");
                            e.printStackTrace();
                        }


                    } else {
                        Log.d("JKGDEBUG", "Schule: " + s.name);
                        Log.d("JKGDEBUG", "failed");
                    }
                }
            });
        }
    }*/

    public static Schule setSchule(int index) {

        Schule schule = getSchulen()[index];
        PreferenceHelper.saveBooleanToPrefernces(MyApplication.getAppContext(), "hasAuth", schule.hasAuth);
        PreferenceHelper.saveStringToPreferences(MyApplication.getAppContext(), "baseUrl", schule.baseUrl);
        return schule;

    }

    public static void setStundenplan24(String schulnummer) {

        PreferenceHelper.saveStringToPreferences(MyApplication.getAppContext(), "baseUrl", "https://www.stundenplan24.de/" + schulnummer + "/mobil/");

    }

    public static void setSchulUrl(String url) {

        int slashIndex = url.lastIndexOf("/");
        //remove subpage
        if (slashIndex>-1&&slashIndex!=url.indexOf("/")+1&&slashIndex<url.lastIndexOf("."))
            url = url.substring(0, slashIndex);
        //add trailing slash
        if (slashIndex+1!=url.length())
            url += "/";
        //add http
        if (!url.startsWith("http://")&&!url.startsWith("https://"))
            url = "http://"+url;
        PreferenceHelper.saveStringToPreferences(MyApplication.getAppContext(), "baseUrl", url);

    }

    public static void setHasABWoche(boolean hasABWoche) {

        PreferenceHelper.saveBooleanToPrefernces(MyApplication.getAppContext(), "hasABWoche", hasABWoche);
    }

    public static boolean hasABWoche() {

        return PreferenceHelper.readBooleanFromPreferences(MyApplication.getAppContext(), "hasABWoche", false);
    }

    public static String getBaseUrl() {

        return PreferenceHelper.readStringFromPreferences(MyApplication.getAppContext(), "baseUrl", null);
    }

    public static boolean isAWoche(Date date) {

        if (!hasABWoche()) return true;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Calendar compareCalendar = Calendar.getInstance();
        compareCalendar.setTime(getCompareDate());

        int diff = calendar.get(Calendar.WEEK_OF_YEAR) - compareCalendar.get(Calendar.WEEK_OF_YEAR);
        boolean isCompareAWoche = true;
        return ((diff % 2 == 1) != isCompareAWoche);

    }

    public static void setCompareDate(Date date) {

        PreferenceHelper.saveLongToPreferences(MyApplication.getAppContext(), "compareDate", date.getTime());
    }

    private static Date getCompareDate() {

        long time = PreferenceHelper.readLongFromPreferences(MyApplication.getAppContext(), "compareDate", 0);
        if (time == 0) return null;
        else return new Date(time);
    }

    private static boolean hasReminders() {

        return SQLite.select().from(Erinnerung.class).querySingle() != null;
    }

    /**
     * Erstellt offline Daten Fächer automatisch anhand der vorhandenen online Vertretungs Daten
     */
    public static void smartImport(Activity context) {

        if (Klasse.getSelectedKlasse() == null) {
            Toast.makeText(context, "Kein(e) Klasse/Kurs ausgewählt!", Toast.LENGTH_LONG).show();
            return;
        }

        FlowManager.getDatabase(AppDatabase.class).beginTransactionAsync(new ITransaction() {
            @Override
            public void execute(DatabaseWrapper databaseWrapper) {

                boolean isOberstufe = isOberstufe();

                List<Kurs> meineKurse = Kurs.getAllSelectedKurse();
                for (Kurs k : meineKurse) {
                    Fach fach = k.getFach();
                    if (fach == null) {
                        fach = new Fach(k.getFachName());
                        fach.setKurs(k);
                        fach.setAssKursNr(k.getNr());
                    }

                    if (isOberstufe && k.getBezeichnung() != null) {
                        boolean isLeistungskurs = true;
                        for (char c : k.getBezeichnung().toCharArray())
                            if (Character.isLetter(c) && Character.isLowerCase(c)) {
                                isLeistungskurs = false;
                                break;
                            }
                        fach.setLeistungskurs(isLeistungskurs);
                    }

                    fach.save();

                    List<Stunde> stunden = k.getStunden();
                    for (Stunde s : stunden) {
                        int stunde = s.getStunde();
                        int wochenTag = Utilities.getDayOfWeek(s.getOnlineTag().getDate());
                        boolean aWoche = isAWoche(s.getOnlineTag().getDate());
                        if (!fach.hasUnterrichtsZeit(stunde, wochenTag, aWoche)) {
                            UnterrichtsZeit u = new UnterrichtsZeit(wochenTag, stunde, aWoche, fach);
                            u.save();
                        }
                    }
                }


                EventBus.getDefault().post(new FaecherUpdateEvent());
            }
        }).execute();

    }

    public static void deleteElapsedData() {

        FlowManager.getDatabase(AppDatabase.class).beginTransactionAsync(new ITransaction() {
            @Override
            public void execute(DatabaseWrapper databaseWrapper) {
                Date heute = Utilities.getToday().getTime();
                List<Event> events = SQLite.select().from(Event.class).where(Event_Table.deleteWhenElapsed.eq(true)).and(Event_Table.date.lessThan(heute)).queryList(databaseWrapper);
                for (Event e : events) removeEventReminder(MyApplication.getAppContext(), e);
                SQLite.delete().from(Event.class).where(Event_Table.deleteWhenElapsed.eq(true)).and(Event_Table.date.lessThan(heute)).execute(databaseWrapper);
                SQLite.delete().from(Ferien.class).where(Ferien_Table.endDate.lessThan(heute)).execute(databaseWrapper);

                EventBus.getDefault().post(new EventsChangedEvent());
                EventBus.getDefault().post(new FerienChangedEvent());
            }
        }).execute();
    }

    public static boolean isOldLocalData() {

        String SAVE_FILE_NAME = "localData.json";
        File localDataFile = new File(MyApplication.getAppContext().getFilesDir(), SAVE_FILE_NAME);
        return localDataFile.exists();

    }

    public static void deleteOldLocalData() {

        PreferenceHelper.deleteAllPreferences(MyApplication.getAppContext());
        String SAVE_FILE_NAME = "localData.json";
        File localDataFile = new File(MyApplication.getAppContext().getFilesDir(), SAVE_FILE_NAME);
        localDataFile.delete();
    }

    public static void importOldLocalData(final Context context) {

        final String SAVE_FILE_NAME = "localData.json";
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        LocalData localData;
        //Read Data
        try {
            FileInputStream inputStream = MyApplication.getAppContext().openFileInput(SAVE_FILE_NAME);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder(inputStream.available());
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            r.close();
            inputStream.close();

            final String json = total.toString();
            FlowManager.getDatabase(AppDatabase.class).beginTransactionAsync(new ITransaction() {
                @Override
                public void execute(DatabaseWrapper databaseWrapper) {

                    try {
                        Toast.makeText(context, "Start import...", Toast.LENGTH_SHORT).show();

                        JSONObject all = new JSONObject(json);
                        if (all.getInt("saveFileVersion") != 2)
                            throw new Exception("saveFileVersion wrong");

                        //ferien
                        JSONArray ferienArray = all.getJSONArray("ferien");
                        for (int i = 0; i < ferienArray.length(); i++) {
                            JSONObject ferienObject = ferienArray.getJSONObject(i);
                            Ferien ferien = new Ferien(
                                    dateFormat.parse(ferienObject.getString("startDate")),
                                    dateFormat.parse(ferienObject.getString("endDate")),
                                    ferienObject.getString("name")
                            );
                            ferien.save(databaseWrapper);
                        }

                        //fächer
                        JSONArray faecherArray = all.getJSONArray("fächer");
                        for (int i = 0; i < faecherArray.length(); i++) {
                            JSONObject faecherObject = faecherArray.getJSONObject(i);
                            String fachName = faecherObject.getString("name");
                            Fach fach = new Fach(fachName);
                            boolean isLeistungskurs = true;
                            for (char c : fachName.toCharArray())
                                if (Character.isLetter(c) && Character.isLowerCase(c)) {
                                    isLeistungskurs = false;
                                    break;
                                }
                            fach.setLeistungskurs(isLeistungskurs);
                            fach.save(databaseWrapper);

                            JSONArray aStundenArray = faecherObject.getJSONArray("aStunden");
                            for (int j = 0; j < aStundenArray.length(); j++) { //j=wochenTag-1
                                JSONArray tag = aStundenArray.getJSONArray(j);
                                for (int k = 0; k < tag.length(); k++)
                                    if (tag.getBoolean(k))
                                        new UnterrichtsZeit(j + 1, k + 1, true, fach).save(databaseWrapper);
                            }
                            JSONArray bStundenArray = faecherObject.getJSONArray("bStunden");
                            for (int j = 0; j < bStundenArray.length(); j++) { //j=wochenTag-1
                                JSONArray tag = bStundenArray.getJSONArray(j);
                                for (int k = 0; k < tag.length(); k++)
                                    if (tag.getBoolean(k))
                                        new UnterrichtsZeit(j + 1, k + 1, false, fach).save(databaseWrapper);
                            }

                            JSONArray eventsArray = faecherObject.getJSONArray("events");
                            for (int j = 0; j < eventsArray.length(); j++) {
                                JSONObject eventObject = eventsArray.getJSONObject(j);
                                Event event = new Event();
                                event.setDate(dateFormat.parse(eventObject.getString("datum")));
                                event.setDeleteWhenElapsed(eventObject.getBoolean("deleteWhenElapsed"));
                                event.setDescription(eventObject.getString("description"));
                                event.setName(eventObject.getString("title"));
                                event.setFach(fach);
                                event.save(databaseWrapper);
                                JSONArray reminderArray = eventObject.getJSONArray("reminders");
                                for (int k = 0; k < reminderArray.length(); k++)
                                    new Erinnerung(dateFormat.parse(reminderArray.getString(k)), event).save(databaseWrapper);
                            }

                            JSONArray klausurArray = faecherObject.getJSONArray("klausurenNoten");
                            for (int j = 0; j < klausurArray.length(); j++)
                                new Zensur(klausurArray.getInt(j), true, fach).save(databaseWrapper);
                            JSONArray sonstigeArray = faecherObject.getJSONArray("sonstigeNoten");
                            for (int j = 0; j < sonstigeArray.length(); j++)
                                new Zensur(sonstigeArray.getInt(j), false, fach).save(databaseWrapper);

                        }

                        //sonsige Events
                        JSONArray eventsArray = all.getJSONArray("noFachEvents");
                        for (int j = 0; j < eventsArray.length(); j++) {
                            JSONObject eventObject = eventsArray.getJSONObject(j);
                            Event event = new Event();
                            event.setDate(dateFormat.parse(eventObject.getString("datum")));
                            event.setDeleteWhenElapsed(eventObject.getBoolean("deleteWhenElapsed"));
                            event.setDescription(eventObject.getString("description"));
                            event.setName(eventObject.getString("title"));
                            event.save(databaseWrapper);
                            JSONArray reminderArray = eventObject.getJSONArray("reminders");
                            for (int k = 0; k < reminderArray.length(); k++)
                                new Erinnerung(dateFormat.parse(reminderArray.getString(k)), event).save(databaseWrapper);
                        }

                        //delete File
                        deleteOldLocalData();

                        Toast.makeText(context, "Import erfolgreich!", Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        Toast.makeText(context, "Fehler beim Import!", Toast.LENGTH_SHORT).show();
                    }

                }
            }).execute();

        } catch (Exception e) {

            Toast.makeText(context, "Fehler beim Import!", Toast.LENGTH_SHORT).show();

        }

    }

    public static boolean isFreeVersion(Context context) {

        return !context.getPackageName().contains("pro");
    }
}
