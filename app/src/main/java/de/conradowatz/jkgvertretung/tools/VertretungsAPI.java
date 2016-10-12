package de.conradowatz.jkgvertretung.tools;


import android.content.Context;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Kurs;
import de.conradowatz.jkgvertretung.variables.OnlineEvents;
import de.conradowatz.jkgvertretung.variables.StuPlaKlasse;
import de.conradowatz.jkgvertretung.variables.Stunde;
import de.conradowatz.jkgvertretung.variables.Tag;
import de.conradowatz.jkgvertretung.variables.Vertretung;

public class VertretungsAPI {

    private Date endDate = null;
    private String username;
    private String password;
    private Context context;
    private int pendingDownloads = 0;

    private boolean onlineEventsFinished;
    private boolean vertretungsDataFinished;

    public VertretungsAPI(String username, String password, Context context) {

        this.username = username;
        this.password = password;
        this.context = context;
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

        String url = "http://kepler-chemnitz.de/stuplanindiware/VmobilS/mobdaten/Klassen.xml";
        AuthedStringRequest request = new AuthedStringRequest(url, listener, errorListener);
        request.setBasicAuth(username, password);
        VolleySingelton.getsInstance().getRequestQueue().add(request);
    }

    /**
     * Berechnet den nächsten Schultag anhand von Wochenenden und der freieTageList
     *
     * @param startDate Datum ab dem geschaut wird (kann nicht zurückgegeben werden)
     * @return Datum des nächsten Schultages nach dem startDate
     */
    public static Date nextSchoolDay(Date startDate) {

        Calendar nextCalendar = Calendar.getInstance();
        nextCalendar.setTime(startDate);
        Date nextDate;

        do {
            nextCalendar.add(Calendar.DATE, 1);
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
    public static boolean isntSchoolDay(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        return (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY || VertretungsData.getInstance().getFreieTageList().contains(date) || LocalData.getInstance().isFerien(date));
    }

    /**
     * Fragt nacheinander alle Informationen ab: klassenList, freieTageList, tagList
     *
     * @param dayCount                wie viele Tage maximal abgefragt werden, -1 wenn alle verfügbaren
     * @param downloadAllDataResponseListener ein Handler um die Antwort zu handhaben
     */
    public void downloadAllData(final int dayCount, final boolean downloadEvents, final DownloadAllDataResponseListener downloadAllDataResponseListener) {

        final boolean downloadAllDays = dayCount == -1;

        String url = "http://kepler-chemnitz.de/stuplanindiware/VmobilS/mobdaten/Klassen.xml";
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

                if (!downloadAllDays)
                    downloadAllDataResponseListener.onProgress(100 / (dayCount * 2 + 1));

                //Tag Liste
                downloadDays(downloadAllDays ? 14 : dayCount, new DownloadDaysListener() {
                    @Override
                    public void onFinished(Date endDate) {

                        if (downloadAllDays) {
                            ArrayList<Tag> tagList = VertretungsData.getInstance().getTagList();
                            if (tagList.size() > 0 && tagList.get(tagList.size() - 1).getDatum() == endDate) {
                                downloadDays(14, this);
                                return;
                            }
                        }
                        vertretungsDataFinished = true;
                        if (!downloadEvents || onlineEventsFinished)
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

                        if (dayCount == 0) return;
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

        StringRequest onlineEventsRequest = new StringRequest("http://conradowatz.de/apps/jkgstundenplan/onlineevents/onlineevents.json", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Gson gson = new Gson();
                OnlineEvents onlineEvents = gson.fromJson(response, OnlineEvents.class);
                if (onlineEvents == null || onlineEvents.getFileFormatVersion() != OnlineEvents.compatibleFileFormatVersion) {
                    if (vertretungsDataFinished) downloadAllDataResponseListener.onSuccess();
                    return;
                }

                List<Integer> localUniqueNumbers = LocalData.getInstance().getUniqueEventNumbers();
                //Alte uniqueNumbers löschen
                for (int i = 0; i < localUniqueNumbers.size(); i++) {
                    Integer uniqueNumber = localUniqueNumbers.get(i);
                    if (!onlineEvents.getUniqueEventNumbers().contains(uniqueNumber)) {
                        localUniqueNumbers.remove(i);
                        i--;
                    }
                }

                //neue Events hinzufügen
                Calendar heute = Calendar.getInstance();
                for (int i = 0; i < onlineEvents.getUniqueEventNumbers().size(); i++) {
                    Integer uniqueNumber = onlineEvents.getUniqueEventNumbers().get(i);
                    if (!localUniqueNumbers.contains(uniqueNumber)) {
                        localUniqueNumbers.add(uniqueNumber);

                        Event event = onlineEvents.getEvents().get(i);
                        if (Utilities.compareDays(heute.getTime(), event.getDatum()) < 0) {
                            LocalData.getInstance().getNoFachEvents().add(event);
                            LocalData.getInstance().sortNoFachEvents();
                            LocalData.saveToFile(context);
                            LocalData.addEventReminder(context, event, -1, LocalData.getInstance().getNoFachEvents().indexOf(event));
                        }
                    }
                }

                onlineEventsFinished = true;
                if (vertretungsDataFinished) downloadAllDataResponseListener.onSuccess();
                downloadAllDataResponseListener.onEventsAdded();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                onlineEventsFinished = true;
                if (vertretungsDataFinished) downloadAllDataResponseListener.onSuccess();

                error.printStackTrace();
            }
        });

        VolleySingelton.getsInstance().getRequestQueue().add(request);
        if (downloadEvents)
            VolleySingelton.getsInstance().getRequestQueue().add(onlineEventsRequest);
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
     * Läd eine bestimmte Anzahl Tage heruter und fügt sie der tagList hinzu
     *
     * @param count                wieviele Schultage insgasamm heruntergeladen werden sollen
     * @param downloadDaysListener ein Listener für Sucess / Progress / Error
     */
    private void downloadDays(final int count, final DownloadDaysListener downloadDaysListener) {

        VertretungsData vertretungsData = VertretungsData.getInstance();

        if (vertretungsData.getTagList() == null)
            vertretungsData.setTagList(new ArrayList<Tag>());

        if (count == 0) {
            downloadDaysListener.onFinished(null);
            return;
        }

        Date startDate = Calendar.getInstance().getTime();

        if (isntSchoolDay(startDate)) startDate = nextSchoolDay(startDate);

        final Tag[] tagArray = new Tag[count];
        Date date = startDate;
        pendingDownloads = count * 2;

        for (int i = 0; i < count; i++) {

            if (i == count - 1) endDate = date;

            String dateString = new SimpleDateFormat("yyyMMdd", Locale.GERMAN).format(date);
            String stundenPlanUrl = "http://kepler-chemnitz.de/stuplanindiware/VmobilS/mobdaten/PlanKl" + dateString + ".xml";
            String vertretungsPlanUrl = "http://kepler-chemnitz.de/stuplanindiware/VplanonlineS/vdaten/VplanKl" + dateString + ".xml";

            //Ruft Stundenplan für den Tag ab

            final int finalI = i;
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
                    }

                    pendingDownloads--;
                    float progress = (count - pendingDownloads) * 100 / count;
                    downloadDaysListener.onProgress(Math.round(progress));
                    if (pendingDownloads == 0) downloadDaysListener.onFinished(endDate);

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    if (error.networkResponse != null && error.networkResponse.statusCode != 404)
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

                        }
                    }

                    pendingDownloads--;
                    float progress = (count - pendingDownloads) * 100 / count;
                    downloadDaysListener.onProgress(Math.round(progress));
                    if (pendingDownloads == 0) downloadDaysListener.onFinished(endDate);
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
                    } else {
                        tagArray[finalI].setStuplaKlasseList(new ArrayList<StuPlaKlasse>());
                        String zeitstempelString = response.substring(response.indexOf("<datum>") + 7, response.indexOf("</datum>"));
                        tagArray[finalI].setZeitStempel(zeitstempelString);
                    }

                    pendingDownloads--;
                    float progress = (count - pendingDownloads) * 100 / count;
                    downloadDaysListener.onProgress(Math.round(progress));
                    if (pendingDownloads == 0) downloadDaysListener.onFinished(endDate);

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    if (error.networkResponse != null && error.networkResponse.statusCode != 404)
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
                        }
                    }

                    pendingDownloads--;
                    float progress = (count - pendingDownloads) * 100 / count;
                    downloadDaysListener.onProgress(Math.round(progress));
                    if (pendingDownloads == 0) downloadDaysListener.onFinished(endDate);
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

        VertretungsData vertretungsData = VertretungsData.getInstance();

        for (int i = 0; i < vertretungsData.getTagList().size(); i++) {

            Tag tag = vertretungsData.getTagList().get(i);
            //falls Tag schon vorhanden, updaten
            if (Utilities.compareDays(neuerTag.getDatum(), tag.getDatum()) == 0) {

                vertretungsData.getTagList().remove(i);
                vertretungsData.getTagList().add(i, neuerTag);
                callback.onDayAdded(i);
                return;
            }
            //falls nicht, einordnen
            if (Utilities.compareDays(neuerTag.getDatum(), tag.getDatum()) < 0) {

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
                        currentStunde.setRaumg(pullParser.getAttributeCount() > 0);
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

    public interface DownloadAllDataResponseListener {

        void onSuccess();

        void onNoConnection();

        void onNoAccess();

        void onProgress(int progress);

        void onOtherError(Throwable throwable);

        void onKlassenListFinished();

        void onDayAdded(int position);

        void onEventsAdded();
    }

    private interface DownloadDaysListener {

        void onFinished(Date endDate);

        void onError(Throwable throwable);

        void onProgress(int progress);

        void onDayAdded(int position);
    }

}
