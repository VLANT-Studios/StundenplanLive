package de.conradowatz.jkgvertretung.tools;


import android.content.Context;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

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

    public static String SAVE_FILE_NAME = "savedSession.json";

    private String username;
    private String password;

    private int pendingDownloads = 0;

    public VertretungsAPI(String username, String password) {

        this.username = username;
        this.password = password;
    }

    /**
     * Prüft, ob die Benutzerdaten stimmen
     *
     * @param username             der Benutzername
     * @param password             das Passwort
     * @param listener             ein Listener um die Antwort zu handhaben
     * @param errorListener        ein Listener um die Antwort zu handhaben
     */
    public static void checkLogin(String username, String password, Response.Listener<String> listener, Response.ErrorListener errorListener) {

        String url = "http://kepler.c.sn.schule.de/stuplanindiware/VmobilS/mobdaten/Klassen.xml";
        AuthedStringRequest request = new AuthedStringRequest(url, listener, errorListener);
        request.setBasicAuth(username, password);
        VolleySingelton.getsInstance().getRequestQueue().add(request);
    }

    /**
     * Speichert den derzeitigen Zustand der VertretungsData in den AppSpeicher
     *
     * @param context Context zum Zugriff auf den App-Speicher
     */
    public static void saveDataToFile(final Context context) {

        VertretungsData vertretungsData = VertretungsData.getInstance();

        try {

            JSONObject alleDaten = new JSONObject();

            //Klassenliste
            JSONArray klassenListArray = new JSONArray();
            for (Klasse klasse : vertretungsData.getKlassenList()) {
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
            for (Date date : vertretungsData.getFreieTageList()) {
                String stringDate = new SimpleDateFormat("ddMMyyyy", Locale.GERMAN).format(date);
                freieTageListArray.put(stringDate);
            }
            alleDaten.put("freieTageList", freieTageListArray);

            //Tage
            JSONArray tagListArray = new JSONArray();
            for (Tag tag : vertretungsData.getTagList()) {
                JSONObject tagObject = new JSONObject();
                String dateString = new SimpleDateFormat("ddMMyyyy", Locale.GERMAN).format(tag.getDatum());
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

            FileOutputStream outputStream = context.openFileOutput(SAVE_FILE_NAME, Context.MODE_PRIVATE);
            outputStream.write(dataToSave.getBytes(Charset.forName("UTF-8")));
            outputStream.close();

        } catch (Exception e) {

            Log.e("JKGDEBUG", "Error saving session");
            e.printStackTrace();

        }

    }

    /**
     * Ruft die VertretungsData aus dem App Speicher ab, falls diese vorhanden ist
     *
     * @param context                    die Activity für den handler und als Context für den Zugriff auf den AppSpeicher
     * @param createDataFromFileListener ein Listener für Created / Error
     */
    public static void createDataFromFile(final Context context, final CreateDataFromFileListener createDataFromFileListener) {


        Calendar heute = Calendar.getInstance();
        VertretungsData vertretungsData = VertretungsData.getInstance();

        try {

            //Read Data
            FileInputStream inputStream = context.openFileInput(SAVE_FILE_NAME);
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
            vertretungsData.setKlassenList(klassenList1);


            //Freie Tage
            ArrayList<Date> freieTageList1 = new ArrayList<>();
            JSONArray freieTageListArray = alleDaten.getJSONArray("freieTageList");
            for (int i = 0; i < freieTageListArray.length(); i++) {
                String stringDate = freieTageListArray.getString(i);
                freieTageList1.add(new SimpleDateFormat("ddMMyyyy", Locale.GERMAN).parse(stringDate));
            }
            vertretungsData.setFreieTageList(freieTageList1);

            //Tage
            ArrayList<Tag> tagList1 = new ArrayList<>();
            JSONArray tagListArray = alleDaten.getJSONArray("tagList");
            for (int i = 0; i < tagListArray.length(); i++) {
                Tag tag = new Tag();
                JSONObject tagObject = tagListArray.getJSONObject(i);
                String dateString = tagObject.getString("datum");
                Date datum = new SimpleDateFormat("ddMMyyyy", Locale.GERMAN).parse(dateString);

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
            vertretungsData.setTagList(tagList1);

            if (vertretungsData.getTagList().size() == 0)
                createDataFromFileListener.onError(new Throwable("Tagliste nicht mehr aktuell!"));
            else createDataFromFileListener.onCreated();

        } catch (Exception e) {

            createDataFromFileListener.onError(e);
        }

    }

    /**
     * Fragt nacheinander alle Informationen ab: klassenList, freieTageList, tagList
     *
     * @param dayCount                wie viele Tage maximal abgefragt werden
     * @param downloadAllDataResponseListener ein Handler um die Antwort zu handhaben
     */
    public void downloadAllData(final int dayCount, final DownloadAllDataResponseListener downloadAllDataResponseListener) {

        String url = "http://kepler.c.sn.schule.de/stuplanindiware/VmobilS/mobdaten/Klassen.xml";
        AuthedStringRequest request = new AuthedStringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                VertretungsData vertretungsData = VertretungsData.getInstance();

                //Klassenliste
                try {
                    vertretungsData.setKlassenList(makeKlassen(response));
                } catch (Exception e) {
                    downloadAllDataResponseListener.onOtherError(e);
                    return;
                }
                downloadAllDataResponseListener.onKlassenListFinished();

                //freie Tage
                try {
                    vertretungsData.setFreieTageList(makeFreieTage(response));
                } catch (Exception e) {
                    downloadAllDataResponseListener.onOtherError(e);
                    return;
                }

                downloadAllDataResponseListener.onProgress(100 / (dayCount * 2 + 1));

                //Tag Liste
                downloadDays(dayCount, 0, new DownloadDaysListener() {
                    @Override
                    public void onFinished() {

                        downloadAllDataResponseListener.onSuccess();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        downloadAllDataResponseListener.onOtherError(throwable);
                    }

                    @Override
                    public void onDayAdded(int position) {
                        downloadAllDataResponseListener.onDayAdded(position);
                    }

                    @Override
                    public void onProgress(int progress) {

                        float endProgress = 100 / (dayCount * 2 + 1) + progress / (100 / (dayCount * 2 + 1));
                        downloadAllDataResponseListener.onProgress(Math.round(endProgress));
                    }
                });

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    downloadAllDataResponseListener.onNoAccess();
                } else {
                    downloadAllDataResponseListener.onNoConnection();
                }
            }
        });
        request.setBasicAuth(username, password);

        VolleySingelton.getsInstance().getRequestQueue().add(request);
    }

    /**
     * Erzeugt aus dem XML String die klassenlist
     *
     * @param xml der XML String
     * @return die KlassenList
     * @throws XmlPullParserException es gab einen Fehler bei der Umwandlung
     * @throws IOException            der XML String ist Fehlerhaft
     */
    private ArrayList<Klasse> makeKlassen(String xml) throws XmlPullParserException, IOException {

        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
        XmlPullParser pullParser = xmlPullParserFactory.newPullParser();
        InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        pullParser.setInput(is, "UTF-8");

        ArrayList<Klasse> klassenList = new ArrayList<>();
        ArrayList<Kurs> currentKursList = new ArrayList<>();
        Klasse currentKlasse = new Klasse("");

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

    /**
     * Erzeugt aus dem XML String eine freieTageList
     *
     * @param xml der XML String
     * @return die freieTageList
     * @throws XmlPullParserException es gab einen Fehler bei der Umwandlung
     * @throws IOException            der XML String ist Fehlerhaft
     */
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
                        Date date = new SimpleDateFormat("yyMMdd", Locale.GERMAN).parse(dateString, new ParsePosition(0));
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
            Date date = new SimpleDateFormat("yyMMdd", Locale.GERMAN).parse("150615");
            dateList.add(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return dateList;

    }

    /**
     * Berechnet den nächsten Schultag anhand von Wochenenden und der freieTageList
     *
     * @param startDate Datum ab dem geschaut wird (kann nicht zurückgegeben werden)
     * @return Datum des nächsten Schultages nach dem startDate
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

    /**
     * Gibt zurück ob das gegebene Datum kein Schultag ist
     *
     * @param date Datum das geprüft werden soll
     * @return ist der Datum kein Schultag
     */
    private boolean isntSchoolDay(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        return (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY || VertretungsData.getInstance().getFreieTageList().contains(date));
    }

    /**
     * Läd eine bestimmte Anzahl Tage heruter und fügt sie der tagList hinzu
     *
     * @param count                wieviele Schultage insgasamm heruntergeladen werden sollen
     * @param skip                 wie viele Schultage sollen übersprungen werden
     * @param downloadDaysListener ein Listener für Sucess / Progress / Error
     */
    public void downloadDays(final int count, int skip, final DownloadDaysListener downloadDaysListener) {

        Date startDate = Calendar.getInstance().getTime();

        if (isntSchoolDay(startDate)) startDate = nextSchoolDay(startDate);

        for (int i = 0; i < skip; i++) {
            startDate = nextSchoolDay(startDate);
        }

        VertretungsData vertretungsData = VertretungsData.getInstance();

        if (vertretungsData.getTagList() == null)
            vertretungsData.setTagList(new ArrayList<Tag>());

        final Tag[] tagArray = new Tag[count];
        Date date = startDate;
        pendingDownloads = count * 2;

        for (int i = 0; i < count; i++) {

            String dateString = new SimpleDateFormat("yyyMMdd", Locale.GERMAN).format(date);
            String stundenPlanUrl = "http://kepler.c.sn.schule.de/stuplanindiware/VmobilS/mobdaten/PlanKl" + dateString + ".xml";
            String vertretungsPlanUrl = "http://kepler.c.sn.schule.de/stuplanindiware/VplanonlineS/vdaten/VplanKl" + dateString + ".xml";

            //Ruft Stundenplan für den Tag ab

            final int finalI = i;
            final int finalSkip = skip;
            final Date finalDate = date;
            AuthedStringRequest stundenplanRequest = new AuthedStringRequest(stundenPlanUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    ArrayList<StuPlaKlasse> stundenList = null;
                    try {
                        stundenList = makeStundenPlanList(response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String zeitStempel = response.substring(response.indexOf("<zeitstempel>") + 13, response.indexOf("</zeitstempel>"));

                    boolean finished = true;
                    if (tagArray[finalI] == null) {
                        tagArray[finalI] = new Tag();
                        finished = false;
                    }

                    tagArray[finalI].setStuplaKlasseList(stundenList);
                    tagArray[finalI].setZeitStempel(zeitStempel);

                    if (finished) {
                        if (tagArray[finalI].getVertretungsList() == null) {
                            tagArray[finalI].setVertretungsList(createFallbackVerterungsList(stundenList));
                            String datumString = new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMAN).format(finalDate);
                            tagArray[finalI].setDatumString(datumString);
                        }
                        tagArray[finalI].setDatum(finalDate);
                        addTag(tagArray[finalI], downloadDaysListener);
                        downloadDaysListener.onDayAdded(finalI + finalSkip);
                    }

                    pendingDownloads--;
                    float progress = (count - pendingDownloads) * 100 / count;
                    downloadDaysListener.onProgress(Math.round(progress));
                    if (pendingDownloads == 0) downloadDaysListener.onFinished();

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    if (error.networkResponse != null && error.networkResponse.statusCode != 401)
                        downloadDaysListener.onError(error.getCause());

                    boolean finished = true;
                    if (tagArray[finalI] == null) {
                        tagArray[finalI] = new Tag();
                        finished = false;
                    }

                    if (finished) {
                        if (tagArray[finalI].getVertretungsList() != null) {
                            tagArray[finalI].setDatum(finalDate);
                            addTag(tagArray[finalI], downloadDaysListener);
                            downloadDaysListener.onDayAdded(finalI + finalSkip);

                        }
                    }

                    pendingDownloads--;
                    float progress = (count - pendingDownloads) * 100 / count;
                    downloadDaysListener.onProgress(Math.round(progress));
                    if (pendingDownloads == 0) downloadDaysListener.onFinished();
                }
            });
            stundenplanRequest.setBasicAuth(username, password);

            //Ruft Vertretungsplan für den Tag ab
            AuthedStringRequest vertretungsplanRequest = new AuthedStringRequest(vertretungsPlanUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    ArrayList<Vertretung> vertretungsList = null;
                    try {
                        vertretungsList = makeVertretungsList(response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String datumString = response.substring(response.indexOf("<titel>") + 7, response.indexOf("</titel>"));

                    boolean finished = true;
                    if (tagArray[finalI] == null) {
                        tagArray[finalI] = new Tag();
                        finished = false;
                    }

                    tagArray[finalI].setVertretungsList(vertretungsList);
                    tagArray[finalI].setDatumString(datumString);

                    if (finished) {
                        if (tagArray[finalI].getStuplaKlasseList() == null) {
                            tagArray[finalI].setStuplaKlasseList(new ArrayList<StuPlaKlasse>());
                            String zeitstempelString = response.substring(response.indexOf("<datum>") + 7, response.indexOf("</datum>"));
                            tagArray[finalI].setZeitStempel(zeitstempelString);
                        }
                        tagArray[finalI].setDatum(finalDate);
                        addTag(tagArray[finalI], downloadDaysListener);
                        downloadDaysListener.onDayAdded(finalI + finalSkip);
                    } else {
                        tagArray[finalI].setStuplaKlasseList(new ArrayList<StuPlaKlasse>());
                        String zeitstempelString = response.substring(response.indexOf("<datum>") + 7, response.indexOf("</datum>"));
                        tagArray[finalI].setZeitStempel(zeitstempelString);
                    }

                    pendingDownloads--;
                    float progress = (count - pendingDownloads) * 100 / count;
                    downloadDaysListener.onProgress(Math.round(progress));
                    if (pendingDownloads == 0) downloadDaysListener.onFinished();

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    if (error.networkResponse != null && error.networkResponse.statusCode != 401)
                        downloadDaysListener.onError(new Throwable(error.getMessage()));

                    boolean finished = true;
                    if (tagArray[finalI] == null) {
                        tagArray[finalI] = new Tag();
                        finished = false;
                    }

                    if (finished) {
                        if (tagArray[finalI].getStuplaKlasseList() != null) {

                            tagArray[finalI].setVertretungsList(createFallbackVerterungsList(tagArray[finalI].getStuplaKlasseList()));
                            String datumString = new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMAN).format(finalDate);
                            tagArray[finalI].setDatumString(datumString);
                            tagArray[finalI].setDatum(finalDate);
                            addTag(tagArray[finalI], downloadDaysListener);
                            downloadDaysListener.onDayAdded(finalI + finalSkip);
                        }
                    }

                    pendingDownloads--;
                    float progress = (count - pendingDownloads) * 100 / count;
                    downloadDaysListener.onProgress(Math.round(progress));
                    if (pendingDownloads == 0) downloadDaysListener.onFinished();
                }
            });
            vertretungsplanRequest.setBasicAuth(username, password);

            VolleySingelton.getsInstance().getRequestQueue().add(stundenplanRequest);
            VolleySingelton.getsInstance().getRequestQueue().add(vertretungsplanRequest);

            date = nextSchoolDay(date);

        }

    }

    /**
     * Fügt einen Tag zur Liste hinzu bzw. updated ihn falls schon vorhanden
     *
     * @param neuerTag Der hinzu zu fügende Tag
     */
    private void addTag(Tag neuerTag, DownloadDaysListener callback) {

        Calendar neuerTagC = Calendar.getInstance();
        neuerTagC.setTime(neuerTag.getDatum());
        VertretungsData vertretungsData = VertretungsData.getInstance();

        for (int i = 0; i < vertretungsData.getTagList().size(); i++) {

            Tag tag = vertretungsData.getTagList().get(i);
            Calendar tagC = Calendar.getInstance();
            tagC.setTime(tag.getDatum());
            //falls Tag schon vorhanden, updaten
            if (tagC.get(Calendar.DAY_OF_YEAR) == neuerTagC.get(Calendar.DAY_OF_YEAR)) {

                vertretungsData.getTagList().remove(i);
                vertretungsData.getTagList().add(i, neuerTag);
                callback.onDayAdded(i);
                return;
            }
            //falls nicht, einordnen
            if (neuerTagC.get(Calendar.DAY_OF_YEAR) < tagC.get(Calendar.DAY_OF_YEAR)) {

                vertretungsData.getTagList().add(i, neuerTag);
                callback.onDayAdded(i);
                int toUpdate = vertretungsData.getTagList().size() - 1 - i;
                for (int j = 0; j < toUpdate; j++) {
                    callback.onDayAdded(vertretungsData.getTagList().size() - 1 - j);
                }
                return;
            }
        }

        //falls neu, hinzufügen
        vertretungsData.getTagList().add(neuerTag);
        callback.onDayAdded(vertretungsData.getTagList().size() - 1);

    }

    /**
     * Erzeugt aus dem XML String eine stundenPlanList
     *
     * @param xml der XML String
     * @return die stundenPlanList
     * @throws XmlPullParserException es gab einen Fehler bei der Umwandlung
     * @throws IOException            der XML String ist Fehlerhaft
     */
    private ArrayList<StuPlaKlasse> makeStundenPlanList(String xml) throws XmlPullParserException, IOException {

        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
        XmlPullParser pullParser = xmlPullParserFactory.newPullParser();
        InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        pullParser.setInput(is, "UTF-8");

        ArrayList<StuPlaKlasse> stundenPlanList = new ArrayList<>();
        StuPlaKlasse currentStuPlaKlasse = new StuPlaKlasse();
        ArrayList<Stunde> currentStundeList = new ArrayList<>();
        Stunde currentStunde = new Stunde();

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

    /**
     * Erzeugt aus dem XML String eine vertretungsPlanList
     *
     * @param xml der XML String
     * @return die vertretungsplanList
     * @throws XmlPullParserException es gab einen Fehler bei der Umwandlung
     * @throws IOException            der XML String ist Fehlerhaft
     */
    private ArrayList<Vertretung> makeVertretungsList(String xml) throws XmlPullParserException, IOException {

        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
        XmlPullParser pullParser = xmlPullParserFactory.newPullParser();
        InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        pullParser.setInput(is, "UTF-8");

        ArrayList<Vertretung> vertretungsList = new ArrayList<>();
        Vertretung vertretung = new Vertretung();

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
     * Erstellt selbst einen Vertretungsplan aus dem Stundenplan falls dieser nicht abgerufen werden kann
     *
     * @param stuPlaKlasseList die stundenplanList mit der verglichen wird
     * @return der Vertretungsplan
     */
    private ArrayList<Vertretung> createFallbackVerterungsList(ArrayList<StuPlaKlasse> stuPlaKlasseList) {

        ArrayList<Vertretung> vertretungsList = new ArrayList<>();

        for (StuPlaKlasse stuPlaKlasse : stuPlaKlasseList) {

            for (Stunde stunde : stuPlaKlasse.getStundenList()) {

                if (!(stunde.isFachg() || stunde.isRaumg())) continue;

                String stundenName = stuPlaKlasse.getName();
                if (stunde.getKurs() != null && !stunde.getKurs().trim().isEmpty()) {
                    stundenName = stundenName + " / " + stunde.getKurs();
                }

                vertretungsList.add(new Vertretung(stundenName, stunde.getStunde(), stunde.getFach(), stunde.getRaum(), stunde.getInfo()));
            }

        }

        return vertretungsList;
    }

    public static abstract class AsyncLoginResponseListener {

        public abstract void onLoggedIn();

        public abstract void onLoginFailed(Throwable throwable);
    }

    public static abstract class DownloadAllDataResponseListener {

        public abstract void onSuccess();

        public abstract void onNoConnection();

        public abstract void onNoAccess();

        public abstract void onOtherError(Throwable throwable);

        public void onProgress(int progress) {

        }

        public void onKlassenListFinished() {

        }

        public void onDayAdded(int position) {

        }
    }

    public static abstract class DownloadDaysListener {

        public abstract void onFinished();

        public abstract void onError(Throwable throwable);

        public void onProgress(int progress) {

        }

        public abstract void onDayAdded(int position);
    }

    public static abstract class CreateDataFromFileListener {

        public abstract void onCreated();

        public abstract void onError(Throwable throwable);

    }

}
