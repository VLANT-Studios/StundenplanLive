package de.conradowatz.jkgvertretung.tools;


import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Kurs;
import de.conradowatz.jkgvertretung.variables.StuPlaKlasse;
import de.conradowatz.jkgvertretung.variables.Stunde;
import de.conradowatz.jkgvertretung.variables.Tag;
import de.conradowatz.jkgvertretung.variables.Vertretung;

public class VertretungsAPI {

    public static String SAVE_FILE_NAE = "savedSession.json";

    private AsyncHttpClient client = new AsyncHttpClient();

    private ArrayList<Klasse> klassenList;
    private ArrayList<Date> freieTageList;
    private ArrayList<Tag> tagList;

    /**
     * Prüft, ob die Benutzerdaten stimmen
     *
     * @param username             der Benutzername
     * @param password             das Passwort
     * @param loginResponseHandler ein Handler um die Antwort zu handhaben
     */
    public void checkLogin(String username, String password, final AsyncLoginResponseHandler loginResponseHandler) {

        client.setBasicAuth(username, password);
        client.get("http://kepler.c.sn.schule.de/stuplanindiware/VmobilS/mobdaten/Klassen.xml", new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                loginResponseHandler.onLoginFailed(throwable);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                loginResponseHandler.onLoggedIn();
            }
        });
    }

    /**
     * Wandelt die von HttpClient übergebenen Byte Werte in Progress um
     *
     * @param bytesWritten
     * @param totalSize
     * @return eine Prozent Zahl von 0-100; -1 bei Fehlern
     */
    private double bytesToProgress(long bytesWritten, long totalSize) {

        return (totalSize > 0) ? (bytesWritten * 1.0 / totalSize) * 100 : -1;
    }

    /**
     * Fragt nacheinander alle Informationen ab: klassenList, freieTageList, tagList
     * @param dayCount wie viele Tage maximal abgefragt werden
     * @param responseHandler ein Handler um die Antwort zu handhaben
     */
    public void getAllInfo(final int dayCount, final AsyncVertretungsResponseHandler responseHandler) {

        //KlassenList
        client.get("http://kepler.c.sn.schule.de/stuplanindiware/VmobilS/mobdaten/Klassen.xml", new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                if (statusCode == 401) {
                    responseHandler.onNoAccess();
                } else {
                    responseHandler.onNoConnection();
                }
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                responseHandler.onProgress(bytesToProgress(bytesWritten, totalSize) / 7);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {

                //Klassenliste
                try {
                    klassenList = makeKlassen(responseString);
                } catch (Exception e) {
                    responseHandler.onOtherError(e);
                    return;
                }
                responseHandler.onKlassenListFinished();

                //freie Tage
                try {
                    freieTageList = makeFreieTage(responseString);
                } catch (Exception e) {
                    responseHandler.onOtherError(e);
                    return;
                }

                //Tag Liste
                makeTagList(dayCount, 0, new GetDaysHandler() {
                    @Override
                    public void onFinished() {
                        responseHandler.onSuccess();
                    }

                    @Override
                    public void onProgress(double progress) {
                        responseHandler.onProgress(100.0 / 7.0 + (6.0 / 7.0) * progress);
                    }

                    @Override
                    public void onDayAdded() {

                        responseHandler.onDayAdded();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        responseHandler.onOtherError(throwable);
                    }
                });

            }
        });

    }

    public ArrayList<Klasse> getKlassenList() {
        return klassenList;
    }

    public ArrayList<Date> getFreieTageList() {
        return freieTageList;
    }

    public ArrayList<Tag> getTagList() {
        return tagList;
    }

    public VertretungsAPI() {

    }

    public VertretungsAPI(String username, String password) {
        client.setBasicAuth(username, password);
    }

    private ArrayList<Klasse> makeKlassen(String xml) throws XmlPullParserException, IOException {

        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
        XmlPullParser pullParser = xmlPullParserFactory.newPullParser();
        InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        pullParser.setInput(is, "UTF-8");

        ArrayList<Klasse> klassenList = new ArrayList<>();
        ArrayList<Kurs> currentKursList = new ArrayList<>();
        Klasse currentKlasse = null;

        int event = pullParser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {

            String name = pullParser.getName();
            switch (event) {
                case XmlPullParser.START_TAG:
                    if (name.equals("Kurz")) {
                        currentKlasse = new Klasse(pullParser.nextText());
                        currentKursList = new ArrayList<>();
                    }
                    if (name.equals("KKz")) {
                        String lehrer = pullParser.getAttributeValue(null, "KLe");
                        currentKursList.add(new Kurs(pullParser.nextText(), lehrer));
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (name.equals("Kurse")) {
                        currentKlasse.setKurse(currentKursList);
                        klassenList.add(currentKlasse);
                    }
                    break;
            }
            event = pullParser.next();

        }

        return klassenList;

    }

    private ArrayList<Date> makeFreieTage(String xml) throws XmlPullParserException, IOException {

        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
        XmlPullParser pullParser = xmlPullParserFactory.newPullParser();
        InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        pullParser.setInput(is, "UTF-8");

        ArrayList<Date> dateList = new ArrayList<>();

        int event = pullParser.getEventType();
        boolean abbruch = false;
        while (event != XmlPullParser.END_DOCUMENT && !abbruch) {

            String name = pullParser.getName();
            switch (event) {
                case XmlPullParser.START_TAG:
                    if (name.equals("ft")) {
                        String dateString = pullParser.nextText();
                        Date date = new SimpleDateFormat("yyMMdd").parse(dateString, new ParsePosition(0));
                        dateList.add(date);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (name.equals("freietage")) {
                        abbruch = true;
                    }
                    break;
            }
            event = pullParser.next();

        }

        try {
            Date date = new SimpleDateFormat("yyMMdd").parse("150615");
            dateList.add(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return dateList;

    }

    /**
     * berechnet den nächsten Schultag anhand von Wochenenden und freien Tagen
     * @param startDate Datum ab dem geschaut wird
     * @return Datum des nächsten Schultages
     */
    private Date nextSchoolDay(Date startDate) {

        Calendar nextCalendar = Calendar.getInstance();
        nextCalendar.setTime(startDate);
        Date nextDate;

        do {
            nextCalendar.add(Calendar.DAY_OF_YEAR, 1);
            nextDate = nextCalendar.getTime();

        } while (isntSchoolDay(nextDate));

        return nextDate;

    }

    private boolean isntSchoolDay(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        return (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY || freieTageList.contains(date));
    }

    /**
     * Fragt Stundenplan und Vertretungsplan ab und fügt diese der TagList hinzu
     * @param count wieviele Tage maximal abgefragt werden
     * @param skip wieviele Schultage sollen ab heute übersprungen werden
     * @param responseHandler ein Handler um die Antwort zu handhaben
     */
    public void makeTagList(final int count, int skip, final GetDaysHandler responseHandler) {

        Date startDate = Calendar.getInstance().getTime();

        if (isntSchoolDay(startDate)) skip++;

        for (int i = 0; i < skip; i++) {
            startDate = nextSchoolDay(startDate);
        }

        if (tagList == null)
            tagList = new ArrayList<>();

        downloadDays(count - 1, startDate, new DownloadDaysHandler() {
            @Override
            public void onFinished() {
                responseHandler.onFinished();
            }

            @Override
            public void onProgress(int currentCount, double progress) {
                responseHandler.onProgress((count - currentCount) / count * progress);
            }

            @Override
            public void onDayAdded() {
                responseHandler.onDayAdded();
            }

            @Override
            public void onError(Throwable throwable) {
                responseHandler.onError(throwable);
            }
        });


    }

    /**
     * Läd einen spezifischen Tag herunter und fügt diesen der tagList hinzu bzw. updatet ihn
     * @param count wieviele Tage insgasamm heruntergeladen werden sollen
     * @param date mit welchem Tag begonnen werden soll
     * @param responseHandler ein Handler um die Antwort zu handhaben
     */
    private void downloadDays(final int count, final Date date, final DownloadDaysHandler responseHandler) {

        String dateString = new SimpleDateFormat("yyyMMdd").format(date);
        String stundenPlanUrl = "http://kepler.c.sn.schule.de/stuplanindiware/VmobilS/mobdaten/PlanKl" + dateString + ".xml";
        final String vertretungsPlanUrl = "http://kepler.c.sn.schule.de/stuplanindiware/VplanonlineS/vdaten/VplanKl" + dateString + ".xml";
        //Log.d("JKGDEBUG", "DownloadURL 1: " + stundenPlanUrl);
        //Log.d("JKGDEBUG", "DownloadURL 2: " + vertretungsPlanUrl);

        client.get(stundenPlanUrl, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

                //kann nicht abgerufen werden -> beenden
                responseHandler.onFinished();
                if (statusCode != 401) {
                    //wenn Tag nicht vorhanden Fehlercode=401; wenn etwas anderes -> unbekannter Fehler
                    responseHandler.onError(throwable);
                }
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {

                double progress = bytesToProgress(bytesWritten, totalSize);
                responseHandler.onProgress(count, progress / 2);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {

                try {
                    final ArrayList<StuPlaKlasse> currentStundenList = makeStundenPlanList(responseString);
                    final String zeitStempel = responseString.substring(responseString.indexOf("<zeitstempel>") + 13, responseString.indexOf("</zeitstempel>"));

                    client.get(vertretungsPlanUrl, new TextHttpResponseHandler() {
                        @Override
                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

                            //Falls Vertretungsplan nicht da ist, trotzdem weiter machen
                            onNoVertretung();

                            if (statusCode != 401) {
                                responseHandler.onError(throwable);
                            }
                        }

                        @Override
                        public void onProgress(long bytesWritten, long totalSize) {
                            double progress = bytesToProgress(bytesWritten, totalSize);
                            responseHandler.onProgress(count, 50 + progress / 2);
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String responseString) {

                            try {

                                ArrayList<Vertretung> vertretungsList = makeVertretungsList(responseString);
                                vertretungsList = enhanceVertretungsList(vertretungsList, currentStundenList);

                                String datumString = responseString.substring(responseString.indexOf("<titel>") + 7, responseString.indexOf("</titel>"));
                                Tag tag = new Tag(date, datumString, zeitStempel, currentStundenList, vertretungsList);
                                addTag(tag);

                            } catch (Exception e) {
                                e.printStackTrace();
                                responseHandler.onFinished();
                                responseHandler.onError(e);
                            }

                        }

                        public void onNoVertretung() {

                            try {

                                ArrayList<Vertretung> vertretungsList = enhanceVertretungsList(new ArrayList<Vertretung>(), currentStundenList);

                                String datumString = new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMAN).format(date);
                                Tag tag = new Tag(date, datumString, zeitStempel, currentStundenList, vertretungsList);
                                addTag(tag);


                            } catch (Exception e) {
                                e.printStackTrace();
                                responseHandler.onFinished();
                                responseHandler.onError(e);
                            }

                        }

                        /**
                         * Fügt einen Tag zur Liste hinzu bzw. updated ihn falls schon vorhanden
                         *
                         * @param tag Der hinzu zu fügende Tag
                         */
                        private void addTag(Tag tag) {

                            //falls Tag schon vorhanden, updaten
                            boolean updated = false;
                            for (int i = 0; i < tagList.size(); i++) {
                                Calendar calendar1 = Calendar.getInstance();
                                calendar1.setTime(tagList.get(i).getDatum());
                                Calendar calendar2 = Calendar.getInstance();
                                calendar2.setTime(tag.getDatum());
                                if (calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)) {
                                    tagList.remove(i);
                                    tagList.add(i, tag);
                                    updated = true;
                                    break;
                                }
                            }
                            //falls nicht, hinten anhängen
                            if (!updated) tagList.add(tag);
                            responseHandler.onDayAdded();

                            //nächsten Tag abfragen, falls gewünscht, ansonsten beenden
                            if (count > 0) {
                                downloadDays(count - 1, nextSchoolDay(date), responseHandler);
                            } else {
                                responseHandler.onFinished();
                            }

                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    responseHandler.onFinished();
                    responseHandler.onError(e);
                }

            }
        });

    }

    private ArrayList<StuPlaKlasse> makeStundenPlanList(String xml) throws XmlPullParserException, IOException {

        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
        XmlPullParser pullParser = xmlPullParserFactory.newPullParser();
        InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        pullParser.setInput(is, "UTF-8");

        ArrayList<StuPlaKlasse> stundenPlanList = new ArrayList<>();
        StuPlaKlasse currentStuPlaKlasse = null;
        ArrayList<Stunde> currentStundeList = null;
        Stunde currentStunde = null;

        int event = pullParser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {

            String name = pullParser.getName();
            switch (event) {
                case XmlPullParser.START_TAG:
                    if (name.equals("Kurz")) {
                        currentStuPlaKlasse = new StuPlaKlasse();
                        currentStuPlaKlasse.setName(pullParser.nextText());
                        currentStundeList = new ArrayList<>();
                    }
                    if (name.equals("St")) {
                        currentStunde = new Stunde();
                        currentStunde.setStunde(pullParser.nextText());
                    }
                    if (name.equals("Fa")) {
                        currentStunde.setFachg(pullParser.getAttributeCount() > 0);
                        String fach = pullParser.nextText();
                        if (fach.startsWith("&nbsp")) {
                            currentStunde.setFach("");
                        } else {
                            currentStunde.setFach(fach);
                        }
                    }
                    if (name.equals("Ku2")) {
                        currentStunde.setKurs(pullParser.nextText());
                    }
                    if (name.equals("Ra")) {
                        String raum = pullParser.nextText();
                        if (raum.startsWith("&nbsp")) {
                            currentStunde.setRaum("");
                        } else {
                            currentStunde.setRaum(raum);
                        }
                    }
                    if (name.equals("If")) {
                        String info = pullParser.nextText();
                        if (info == null || info.startsWith("&nbsp")) {
                            currentStunde.setInfo("");
                        } else {
                            currentStunde.setInfo(info);
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (name.equals("Std")) {
                        currentStundeList.add(currentStunde);
                    }
                    if (name.equals("Kl")) {
                        currentStuPlaKlasse.setStundenList(currentStundeList);
                        stundenPlanList.add(currentStuPlaKlasse);
                    }
                    break;
            }
            event = pullParser.next();

        }

        return stundenPlanList;

    }

    private ArrayList<Vertretung> makeVertretungsList(String xml) throws XmlPullParserException, IOException {

        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
        XmlPullParser pullParser = xmlPullParserFactory.newPullParser();
        InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        pullParser.setInput(is, "UTF-8");

        ArrayList<Vertretung> vertretungsList = new ArrayList<>();
        Vertretung vertretung = null;

        int event = pullParser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {

            String name = pullParser.getName();
            switch (event) {
                case XmlPullParser.START_TAG:
                    if (name.equals("klasse")) {
                        vertretung = new Vertretung();
                        vertretung.setKlasse(pullParser.nextText());
                    }
                    if (name.equals("stunde")) {
                        vertretung.setStunde(pullParser.nextText());
                    }
                    if (name.equals("fach")) {
                        vertretung.setFach(pullParser.nextText());
                    }
                    if (name.equals("raum")) {
                        vertretung.setRaum(pullParser.nextText());
                    }
                    if (name.equals("info")) {
                        vertretung.setInfo(pullParser.nextText());
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (name.equals("aktion")) {
                        vertretungsList.add(vertretung);
                    }
                    break;
            }
            event = pullParser.next();

        }

        return vertretungsList;

    }

    /**
     * Fügt der vertrtungsList Informationen hinzu, die nicht im Vertretungspla, aber im Stundenplan stehen
     *
     * @param vertretungsList  die unvollständige vertretungsList
     * @param stuPlaKlasseList die stundenplanList mit der verglichen wird
     * @return die vollständige vertretungsList
     */
    private ArrayList<Vertretung> enhanceVertretungsList(ArrayList<Vertretung> vertretungsList, ArrayList<StuPlaKlasse> stuPlaKlasseList) {

        for (StuPlaKlasse stuPlaKlasse : stuPlaKlasseList) {

            for (Stunde stunde : stuPlaKlasse.getStundenList()) {

                if (!(stunde.isFachg() || stunde.isRaumg())) continue;

                boolean isInVertretung = false;

                for (Vertretung vertretung : vertretungsList) {

                    if (stunde.getStunde().equals(vertretung.getStunde()) && vertretung.getKlasse().contains(stuPlaKlasse.getName())) {
                        isInVertretung = true;
                        break;
                    }

                }

                if (!isInVertretung) {

                    String stundenName = stuPlaKlasse.getName();
                    if (stunde.getKurs() != null && !stunde.getKurs().isEmpty()) {
                        stundenName = stundenName + " / " + stunde.getKurs();
                    }
                    vertretungsList.add(new Vertretung(stundenName, stunde.getStunde(), stunde.getFach(), stunde.getRaum(), stunde.getInfo()));
                }

            }

        }

        return vertretungsList;
    }

    /**
     * Speichert den derzeitigen Zustand der VertretungsAPI in den AppSpeicher
     * @param context Context zum Zugriff auf den App-Speicher
     */
    public void saveToFile(final Context context) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    JSONObject alleDaten = new JSONObject();

                    //Klassenliste
                    JSONArray klassenListArray = new JSONArray();
                    for (Klasse klasse : klassenList) {
                        JSONObject klasseObject = new JSONObject();
                        klasseObject.put("name", klasse.getName());
                        //Kurse
                        JSONArray kurseArray = new JSONArray();
                        for (Kurs kurs : klasse.getKurse()) {
                            JSONObject kursObject = new JSONObject();
                            kursObject.put("name", kurs.getName());
                            kursObject.put("lehrer", kurs.getLehrer());
                            kurseArray.put(kursObject);
                        }
                        klasseObject.put("kurse", kurseArray);
                        klassenListArray.put(klasseObject);
                    }
                    alleDaten.put("klassenList", klassenListArray);

                    //Freie Tage
                    JSONArray freieTageListArray = new JSONArray();
                    for (Date date : freieTageList) {
                        String stringDate = new SimpleDateFormat("ddMMyyyy").format(date);
                        freieTageListArray.put(stringDate);
                    }
                    alleDaten.put("freieTageList", freieTageListArray);

                    //Tage
                    JSONArray tagListArray = new JSONArray();
                    for (Tag tag : tagList) {
                        JSONObject tagObject = new JSONObject();
                        String dateString = new SimpleDateFormat("ddMMyyyy").format(tag.getDatum());
                        tagObject.put("datum", dateString);
                        tagObject.put("datumString", tag.getDatumString());
                        tagObject.put("zeitStempel", tag.getZeitStempel());

                        //StuPlaKlassen Liste
                        JSONArray stuPlaKlasseListArray = new JSONArray();
                        for (StuPlaKlasse stuPlaKlasse : tag.getStuplaKlasseList()) {
                            JSONObject stuPlaKlasseObject = new JSONObject();
                            stuPlaKlasseObject.put("name", stuPlaKlasse.getName());
                            JSONArray stundenListArray = new JSONArray();
                            for (Stunde stunde : stuPlaKlasse.getStundenList()) {
                                JSONObject stundeObject = new JSONObject();
                                stundeObject.put("stunde", stunde.getStunde());
                                stundeObject.put("fach", stunde.getFach());
                                stundeObject.put("fachg", stunde.isFachg());
                                stundeObject.put("raum", stunde.getRaum());
                                stundeObject.put("raumg", stunde.isRaumg());
                                stundeObject.put("kurs", stunde.getKurs());
                                stundeObject.put("info", stunde.getInfo());
                                stundenListArray.put(stundeObject);
                            }
                            stuPlaKlasseObject.put("stundenList", stundenListArray);
                            stuPlaKlasseListArray.put(stuPlaKlasseObject);
                        }
                        tagObject.put("stuPlaKlasseList", stuPlaKlasseListArray);

                        //Vertretungs Liste
                        JSONArray vertretungsListArray = new JSONArray();
                        for (Vertretung vertretung : tag.getVertretungsList()) {
                            JSONObject vertretungObject = new JSONObject();
                            vertretungObject.put("klasse", vertretung.getKlasse());
                            vertretungObject.put("stunde", vertretung.getStunde());
                            vertretungObject.put("fach", vertretung.getFach());
                            vertretungObject.put("raum", vertretung.getRaum());
                            vertretungObject.put("info", vertretung.getInfo());
                            vertretungsListArray.put(vertretungObject);
                        }
                        tagObject.put("vertretungsList", vertretungsListArray);

                        tagListArray.put(tagObject);
                    }
                    alleDaten.put("tagList", tagListArray);

                    //JSON speichern
                    String dataToSave = alleDaten.toString();

                    FileOutputStream outputStream = context.openFileOutput(SAVE_FILE_NAE, Context.MODE_PRIVATE);
                    outputStream.write(dataToSave.getBytes(Charset.forName("UTF-8")));
                    outputStream.close();

                } catch (Exception e) {

                    Log.e("JKGDEBUG", "Error saving session");
                    e.printStackTrace();

                }

            }
        }).start();

    }

    public void setKlassenList(ArrayList<Klasse> klassenList) {
        this.klassenList = klassenList;
    }

    public void setFreieTageList(ArrayList<Date> freieTageList) {
        this.freieTageList = freieTageList;
    }

    public void setTagList(ArrayList<Tag> tagList) {
        this.tagList = tagList;
    }

    /**
     * Erstellt eine VertretungsAPI, wenn diese im AppSpeicher vorhanden ist; läuft in eigenem Thread
     * @param context die Activity für den handler und als Context für den Zugriff auf den AppSpeicher
     * @param username Bunutzername
     * @param password Passwort
     * @param handler ein Handler, da die Aktion Zeit in anspruch nimmt
     */
    public static void createFromFile(final Context context, final String username, final String password, final CreateFromFileHandler handler) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Calendar heute = Calendar.getInstance();

                try {

                    final VertretungsAPI vertretungsAPI = new VertretungsAPI(username, password);

                    //Read Data
                    FileInputStream inputStream = context.openFileInput(SAVE_FILE_NAE);
                    BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder total = new StringBuilder(inputStream.available());
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line);
                    }
                    JSONObject alleDaten = new JSONObject(total.toString());

                    //Klassenliste
                    ArrayList<Klasse> klassenList1 = new ArrayList<>();
                    JSONArray klassenListArray = alleDaten.getJSONArray("klassenList");
                    for (int i = 0; i < klassenListArray.length(); i++) {
                        JSONObject klasseObject = klassenListArray.getJSONObject(i);
                        String name = klasseObject.getString("name");
                        //Kurse
                        ArrayList<Kurs> kurse = new ArrayList<>();
                        JSONArray kurseArray = klasseObject.getJSONArray("kurse");
                        for (int j = 0; j < kurseArray.length(); j++) {
                            JSONObject kursObject = kurseArray.getJSONObject(j);
                            Kurs kurs = new Kurs(
                                    kursObject.getString("name"),
                                    kursObject.getString("lehrer")
                            );
                            kurse.add(kurs);
                        }
                        klassenList1.add(new Klasse(
                                name,
                                kurse
                        ));

                    }
                    vertretungsAPI.setKlassenList(klassenList1);


                    //Freie Tage
                    ArrayList<Date> freieTageList1 = new ArrayList<>();
                    JSONArray freieTageListArray = alleDaten.getJSONArray("freieTageList");
                    for (int i = 0; i < freieTageListArray.length(); i++) {
                        String stringDate = freieTageListArray.getString(i);
                        freieTageList1.add(new SimpleDateFormat("ddMMyyyy").parse(stringDate));
                    }
                    vertretungsAPI.setFreieTageList(freieTageList1);

                    //Tage
                    ArrayList<Tag> tagList1 = new ArrayList<>();
                    JSONArray tagListArray = alleDaten.getJSONArray("tagList");
                    for (int i = 0; i < tagListArray.length(); i++) {
                        Tag tag = new Tag();
                        JSONObject tagObject = tagListArray.getJSONObject(i);
                        String dateString = tagObject.getString("datum");
                        Date datum = new SimpleDateFormat("ddMMyyyy").parse(dateString);

                        //Wenn Tag ist älter als heute -> nicht zeigen
                        Calendar tagCalendar = Calendar.getInstance();
                        tagCalendar.setTime(datum);
                        if (tagCalendar.get(Calendar.DAY_OF_YEAR) < heute.get(Calendar.DAY_OF_YEAR))
                            continue;

                        tag.setDatum(datum);
                        tag.setDatumString(tagObject.getString("datumString"));
                        tag.setZeitStempel(tagObject.getString("zeitStempel"));

                        //StuPlaKlassen Liste
                        ArrayList<StuPlaKlasse> stuPlaKlasseList = new ArrayList<>();
                        JSONArray stuPlaKlasseListArray = tagObject.getJSONArray("stuPlaKlasseList");
                        for (int j = 0; j < stuPlaKlasseListArray.length(); j++) {
                            StuPlaKlasse stuPlaKlasse = new StuPlaKlasse();
                            JSONObject stuPlaKlasseObject = stuPlaKlasseListArray.getJSONObject(j);
                            stuPlaKlasse.setName(stuPlaKlasseObject.getString("name"));
                            ArrayList<Stunde> stundenList = new ArrayList<>();
                            JSONArray stundenListArray = stuPlaKlasseObject.getJSONArray("stundenList");
                            for (int k = 0; k < stundenListArray.length(); k++) {
                                JSONObject stundeObject = stundenListArray.getJSONObject(k);
                                stundenList.add(new Stunde(
                                        stundeObject.getString("stunde"),
                                        stundeObject.getString("fach"),
                                        stundeObject.has("kurs") ? stundeObject.getString("kurs") : null,
                                        stundeObject.getBoolean("fachg"),
                                        stundeObject.getString("raum"),
                                        stundeObject.getBoolean("raumg"),
                                        stundeObject.getString("info")
                                ));
                            }
                            stuPlaKlasse.setStundenList(stundenList);
                            stuPlaKlasseList.add(stuPlaKlasse);
                        }
                        tag.setStuplaKlasseList(stuPlaKlasseList);

                        //Vertretungs Liste
                        ArrayList<Vertretung> vertretungsList = new ArrayList<>();
                        JSONArray vertretungsListArray = tagObject.getJSONArray("vertretungsList");
                        for (int j = 0; j < vertretungsListArray.length(); j++) {
                            JSONObject vertretungObject = vertretungsListArray.getJSONObject(j);
                            vertretungsList.add(new Vertretung(
                                    vertretungObject.getString("klasse"),
                                    vertretungObject.getString("stunde"),
                                    vertretungObject.getString("fach"),
                                    vertretungObject.getString("raum"),
                                    vertretungObject.getString("info")
                            ));
                        }
                        tag.setVertretungsList(vertretungsList);

                        tagList1.add(tag);
                    }
                    vertretungsAPI.setTagList(tagList1);

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handler.onCreated(vertretungsAPI);
                        }
                    });

                } catch (final Exception e) {

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handler.onError(e);
                        }
                    });
                }

            }
        }).start();

    }

    public static abstract class AsyncLoginResponseHandler {

        public abstract void onLoggedIn();

        public abstract void onLoginFailed(Throwable throwable);
    }

    public static abstract class AsyncVertretungsResponseHandler {

        public abstract void onSuccess();

        public abstract void onNoConnection();

        public abstract void onNoAccess();

        public abstract void onOtherError(Throwable e);

        public void onProgress(double progress) {

        }

        public void onDayAdded() {

        }

        public void onKlassenListFinished() {

        }
    }

    public static abstract class GetDaysHandler {

        public abstract void onFinished();

        public abstract void onError(Throwable throwable);

        public void onProgress(double progress) {

        }

        public void onDayAdded() {

        }
    }

    private static abstract class DownloadDaysHandler {

        public abstract void onFinished();

        public abstract void onError(Throwable throwable);

        public void onProgress(int count, double progress) {

        }

        public void onDayAdded() {

        }
    }

    public static abstract class CreateFromFileHandler {

        public abstract void onCreated(VertretungsAPI vertretungsAPI);

        public abstract void onError(Throwable throwable);

        public void onProgress(double progress) {

        }

    }

}
