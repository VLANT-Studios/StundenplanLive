package de.conradowatz.jkgvertretung.tools;


import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;
import com.raizlabs.android.dbflow.structure.database.transaction.ITransaction;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.variables.AppDatabase;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.Fach_Table;
import de.conradowatz.jkgvertretung.variables.Ferien;
import de.conradowatz.jkgvertretung.variables.Ferien_Table;
import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Klasse_Kurs;
import de.conradowatz.jkgvertretung.variables.Klasse_Kurs_Table;
import de.conradowatz.jkgvertretung.variables.Klasse_Table;
import de.conradowatz.jkgvertretung.variables.Kurs;
import de.conradowatz.jkgvertretung.variables.Kurs_Table;
import de.conradowatz.jkgvertretung.variables.Lehrer;
import de.conradowatz.jkgvertretung.variables.Lehrer_Table;
import de.conradowatz.jkgvertretung.variables.OnlineTag;
import de.conradowatz.jkgvertretung.variables.OnlineTag_Table;
import de.conradowatz.jkgvertretung.variables.Stunde;
import de.conradowatz.jkgvertretung.variables.Stunde_Table;
import de.conradowatz.jkgvertretung.variables.Vertretung;
import de.conradowatz.jkgvertretung.variables.Vertretung_Table;
import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class VertretungsAPI {

    private Date endDate = null;
    private String username;
    private String password;
    private boolean hasAuth;
    private int pendingDownloads = 0;
    private OkHttpClient client;

    public VertretungsAPI(String username, String password) {

        this.hasAuth = true;
        this.username = username;
        this.password = password;
        client = new OkHttpClient();
    }

    public VertretungsAPI() {

        this.hasAuth = false;
        client = new OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build();
    }

    private Request getRequest(String url) {

        if (hasAuth)
            return new Request.Builder().url(url).header("Authorization", Credentials.basic(username, password)).build();
        else
            return new Request.Builder().url(url).build();

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

        int dayOfWeek = Utilities.getDayOfWeek(date);

        return (dayOfWeek >= 6 || Ferien.isFerien(date));
    }

    public static String getFreieTageDateFormat() {

        return "yyMMdd";
    }

    /**
     * Fragt nacheinander alle Informationen ab: klassenList, freieTageList, tagList
     *
     * @param dayCount                        wie viele Tage maximal abgefragt werden, -1 wenn alle verfügbaren
     * @param downloadAllDataResponseListener ein Handler um die Antwort zu handhaben
     */
    public void downloadAllData(final int dayCount, final DownloadAllDataResponseListener downloadAllDataResponseListener) {

        final boolean downloadAllDays = dayCount == -1;

        String url = LocalData.getBaseUrl() + "mobdaten/Klassen.xml";
        //Log.d("JKGDebug", url);

        Request request = getRequest(url);

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {

                String html = response.body().string();

                //Klassenliste
                try {
                    updateKlassen(html);
                } catch (Exception e) {
                    downloadAllDataResponseListener.onOtherError(e);
                    return;
                }
                Thread.yield();
                downloadAllDataResponseListener.onKlassenListFinished();

                //freie Tage
                try {
                    updateFreieTageToFerien(html);
                } catch (Exception e) {
                    downloadAllDataResponseListener.onOtherError(e);
                    return;
                }
                downloadAllDataResponseListener.onFreieTageAdded();
                Thread.yield();

                if (!downloadAllDays)
                    downloadAllDataResponseListener.onProgress(100 / (dayCount * 2 + 1));

                //alle alten Tage löschen
                Date heute = Utilities.getToday().getTime();
                SQLite.delete().from(OnlineTag.class).where(OnlineTag_Table.date.lessThan(heute)).execute();

                //OnlineTag Liste
                downloadDays(downloadAllDays ? 14 : dayCount, new DownloadDaysListener() {
                    @Override
                    public void onFinished(Date endDate) {

                        if (downloadAllDays) {
                            OnlineTag lastOnlineTag = OnlineTag.getOnlineTag(endDate);
                            if (lastOnlineTag != null) {
                                downloadDays(14, this);
                                return;
                            }
                        }

                        downloadAllDataResponseListener.onDaysUpdated();

                        downloadAllDataResponseListener.onSuccess();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        downloadAllDataResponseListener.onOtherError(throwable);
                    }

                    @Override
                    public void onDaysUpdated() {
                        Thread.yield();
                    }

                    @Override
                    public void onProgress(int progress) {

                        if (dayCount == 0) return;
                        float endProgress = 100 / (dayCount * 2 + 1) + progress / (100 / (dayCount * 2 + 1));
                        downloadAllDataResponseListener.onProgress(Math.round(endProgress));
                    }
                });

            } else {

                if (response.code() == 401) {
                    downloadAllDataResponseListener.onNoAccess();
                } else {
                    downloadAllDataResponseListener.onNoConnection();
                }

            }
        } catch (IOException e) {

            e.printStackTrace();
            downloadAllDataResponseListener.onNoConnection();
        }

    }

    /**
     * Löscht alle Klassen und Kurse und erstellt sie aus Klassen.xml
     *
     * @param xml der XML String
     */
    private void updateKlassen(String xml) throws IOException, ParserConfigurationException, SAXException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xml));
        Document dom = builder.parse(is);

        final NodeList klassen = dom.getElementsByTagName("Kl");

        FlowManager.getDatabase(AppDatabase.class).executeTransaction(new ITransaction() {
            @Override
            public void execute(DatabaseWrapper databaseWrapper) {

                //Klassen werden nicht gelöscht, da dann alle damit verbundenen Stunden gelöscht wären
                //Stattdessen werden diejenigen am Ende aussortiert die es nicht mehr gibt
                List<String> klassenNamen = new ArrayList<>();
                SQLite.delete().from(Kurs.class).execute(databaseWrapper);

                for (int i = 0; i < klassen.getLength(); i++) {
                    Element kl = (Element) klassen.item(i);

                    String klassenName = kl.getElementsByTagName("Kurz").item(0).getTextContent();
                    klassenNamen.add(klassenName);
                    Klasse klasse = SQLite.select().from(Klasse.class).where(Klasse_Table.name.eq(klassenName)).querySingle(databaseWrapper);
                    if (klasse==null) klasse = new Klasse();
                    klasse.setName(klassenName);
                    klasse.save(databaseWrapper);

                    List<String> auswaehllbareKurse = new ArrayList<>();
                    NodeList kkz = kl.getElementsByTagName("KKz");
                    for (int j = 0; j < kkz.getLength(); j++) {
                        Element auswaehlbar = (Element) kkz.item(j);
                        auswaehllbareKurse.add(auswaehlbar.getTextContent());
                    }

                    NodeList kurse = kl.getElementsByTagName("UeNr");
                    if (kurse.getLength() > 0) {
                        for (int j = 0; j < kurse.getLength(); j++) {
                            Element ue = (Element) kurse.item(j);

                            int kursNr = Integer.parseInt(ue.getTextContent());
                            Fach fach = SQLite.select().from(Fach.class).where(Fach_Table.assKursNr.eq(kursNr)).querySingle(databaseWrapper);
                            Kurs kurs = new Kurs();
                            kurs.setNr(kursNr);
                            kurs.setFachName(ue.getAttribute("UeFa"));
                            kurs.setBezeichnung(ue.getAttribute("UeGr"));
                            String lehrerName = ue.getAttribute("UeLe");
                            Lehrer lehrer = SQLite.select().from(Lehrer.class).where(Lehrer_Table.name.eq(lehrerName)).querySingle(databaseWrapper);
                            if (lehrer != null) kurs.setLehrer(lehrer);
                            else {
                                Lehrer newLehrer = new Lehrer(lehrerName);
                                newLehrer.save(databaseWrapper);
                                kurs.setLehrer(newLehrer);
                            }
                            kurs.setAuswaehlbar(auswaehllbareKurse.contains(kurs.getBezeichnung()));
                            kurs.save(databaseWrapper);

                            Klasse_Kurs kk = new Klasse_Kurs();
                            kk.setKurs(kurs);
                            kk.setKlasse(klasse);
                            kk.save(databaseWrapper);

                            if (fach != null) {
                                fach.setKurs(kurs);
                                fach.save(databaseWrapper);
                            }
                        }

                    } else { //Compat für alte Systeme ohne KursNr

                        for (int kursNr = 1; kursNr <= kkz.getLength(); kursNr++) {

                            Element aKurs = (Element) kkz.item(kursNr - 1);

                            Kurs kurs = new Kurs();
                            kurs.setFachName(aKurs.getTextContent());
                            kurs.setBezeichnung(aKurs.getTextContent());
                            String lehrerName = aKurs.getAttribute("KLe");
                            Lehrer lehrer = SQLite.select().from(Lehrer.class).where(Lehrer_Table.name.eq(lehrerName)).querySingle(databaseWrapper);
                            if (lehrer != null) kurs.setLehrer(lehrer);
                            else {
                                Lehrer newLehrer = new Lehrer(lehrerName);
                                newLehrer.save(databaseWrapper);
                                kurs.setLehrer(newLehrer);
                            }
                            kurs.setNr(kursNr);
                            Fach fach = SQLite.select().from(Fach.class).where(Fach_Table.assKursNr.eq(kursNr)).querySingle(databaseWrapper);
                            kurs.setAuswaehlbar(true);
                            kurs.save(databaseWrapper);

                            Klasse_Kurs kk = new Klasse_Kurs();
                            kk.setKurs(kurs);
                            kk.setKlasse(klasse);
                            kk.save(databaseWrapper);

                            if (fach != null) {
                                fach.setKurs(kurs);
                                fach.save(databaseWrapper);
                            }

                        }
                    }

                }
                SQLite.delete().from(Klasse.class).where(Klasse_Table.name.notIn(klassenNamen)).execute(databaseWrapper);

            }
        });

    }

    /**
     * fügt gegebenenfalls neue Ferien hinzu, aus den freien Tagen von Klassen.xml
     *
     * @param xml der XML String
     */
    private void updateFreieTageToFerien(String xml) throws ParserConfigurationException, IOException, SAXException, ParseException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xml));
        Document dom = builder.parse(is);

        NodeList fts = dom.getElementsByTagName("ft");

        final List<Date> freieTage = new ArrayList<>();
        for (int i = 0; i < fts.getLength(); i++) {
            Element ft = (Element) fts.item(i);
            freieTage.add(new SimpleDateFormat(getFreieTageDateFormat(), Locale.GERMAN).parse(ft.getTextContent()));
        }

        Collections.sort(freieTage);

        FlowManager.getDatabase(AppDatabase.class).executeTransaction(new ITransaction() {
            @Override
            public void execute(DatabaseWrapper databaseWrapper) {

                Calendar heute = Utilities.getToday();

                for (Date freierTag : freieTage) {

                    if (Utilities.compareDays(heute.getTime(), freierTag) > 0) continue;
                    if (Ferien.isFerien(freierTag)) continue;

                    //Berechnet den Tag vor dem freien Tag
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(freierTag);
                    if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
                        calendar.add(Calendar.DATE, -3);
                    else calendar.add(Calendar.DATE, -1);
                    Date prevDay = calendar.getTime();

                    //Wenn Tag davor Ferien -> freien Tag hinzufügen

                    Ferien prevFerien = SQLite.select().from(Ferien.class).where(Ferien_Table.endDate.eq(prevDay)).querySingle();

                    if (prevFerien != null) prevFerien.setEndDate(freierTag);
                    else prevFerien = new Ferien(freierTag, freierTag, "Schulfrei");
                    prevFerien.save(databaseWrapper);

                }

            }
        });
    }

    /**
     * Löscht alle Stunden/Vertretung /OnlineTage, und läd eine bestimmte Anzahl Tage neu herunter
     *
     * @param count                wieviele Schultage insgasamm heruntergeladen werden sollen
     * @param downloadDaysListener ein Listener für Sucess / Progress / Error
     */
    private void downloadDays(final int count, final DownloadDaysListener downloadDaysListener) {

        if (count == 0) {
            downloadDaysListener.onFinished(null);
            return;
        }

        List<OnlineTag> onlineTagList = SQLite.select().from(OnlineTag.class).queryList();

        Date startDate = Utilities.getToday().getTime();

        if (isntSchoolDay(startDate)) startDate = nextSchoolDay(startDate);
        Date date = startDate;
        //pendingDownloads = count * 2;
        pendingDownloads = count;

        for (int i = 0; i < count; i++) {

            if (i == count - 1) endDate = date;

            String dateString = new SimpleDateFormat("yyyMMdd", Locale.GERMAN).format(date);
            String stundenPlanUrl = LocalData.getBaseUrl() + "mobdaten/PlanKl" + dateString + ".xml";

            //delete old OnlineTag falls vorhanden
            OnlineTag oldOnlineTag = null;
            for (OnlineTag o : onlineTagList) if (o.getDate().equals(date)) {
                oldOnlineTag = o;
                break;
            }
            final OnlineTag onlineTag = oldOnlineTag == null ? new OnlineTag(date) : oldOnlineTag;

            //Ruft Stundenplan für den OnlineTag ab
            Request stundenplanRequest = getRequest(stundenPlanUrl);

            client.newCall(stundenplanRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                    downloadDaysListener.onError(e);

                    pendingDownloads--;
                    float progress = (count - pendingDownloads) * 100 / count;
                    downloadDaysListener.onProgress(Math.round(progress));
                    if (pendingDownloads == 0) downloadDaysListener.onFinished(endDate);

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    if (response.isSuccessful()) {

                        String html = response.body().string();

                        try {
                            addStundenToDay(html, onlineTag);
                            downloadDaysListener.onDaysUpdated();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        pendingDownloads--;
                        float progress = (count - pendingDownloads) * 100 / count;
                        downloadDaysListener.onProgress(Math.round(progress));
                        if (pendingDownloads == 0) downloadDaysListener.onFinished(endDate);

                    } else {

                        if (response.code() != 404)
                            downloadDaysListener.onError(new Throwable(response.message()));

                        pendingDownloads--;
                        float progress = (count - pendingDownloads) * 100 / count;
                        downloadDaysListener.onProgress(Math.round(progress));
                        if (pendingDownloads == 0) downloadDaysListener.onFinished(endDate);

                    }

                }
            });

            date = nextSchoolDay(date);

        }

    }

    /**
     * Fügt zu einem OnlineTag die Stunden und den Zeitstempel aus PlanKl.xml
     *
     * @param xml der XML String
     */
    private void addStundenToDay(String xml, final OnlineTag onlineTag) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xml));
        final Document dom = builder.parse(is);


        FlowManager.getDatabase(AppDatabase.class).executeTransaction(new ITransaction() {
            @Override
            public void execute(DatabaseWrapper databaseWrapper) {

                //delete old Stunden
                new Delete().from(Stunde.class).where(Stunde_Table.onlineTag_id.eq(onlineTag.getId())).execute(databaseWrapper);

                onlineTag.setZeitStempel(dom.getElementsByTagName("zeitstempel").item(0).getTextContent());
                NodeList infos = dom.getElementsByTagName("ZiZeile");
                String infoString = "";
                for (int i=0; i<infos.getLength(); i++) {
                    infoString += infos.item(i).getTextContent()+"\n";
                }
                onlineTag.setInfotext(infoString.trim());
                onlineTag.save(databaseWrapper);

                NodeList stds = dom.getElementsByTagName("Std");
                for (int i = 0; i < stds.getLength(); i++) {
                    Element std = (Element) stds.item(i);
                    Element kl = (Element) std.getParentNode().getParentNode();
                    String klassenName = kl.getElementsByTagName("Kurz").item(0).getTextContent();
                    Klasse klasse = new Select().from(Klasse.class).where(Klasse_Table.name.eq(klassenName)).querySingle();
                    int nr = std.getElementsByTagName("Nr").getLength() > 0 ? Integer.parseInt(std.getElementsByTagName("Nr").item(0).getTextContent()) : -1;
                    Kurs kurs = new Select().from(Kurs.class).where(Kurs_Table.nr.eq(nr)).querySingle();

                    Stunde stunde = new Stunde();
                    stunde.setStunde(Integer.parseInt(std.getElementsByTagName("St").item(0).getTextContent()));
                    stunde.setKurs(kurs);
                    stunde.setKlasse(klasse);
                    stunde.setOnlineTag(onlineTag);
                    stunde.setFachGeaendert(std.getElementsByTagName("Fa").item(0).hasAttributes());
                    stunde.setFach(std.getElementsByTagName("Fa").item(0).getTextContent().replaceAll("&nbsp;", ""));
                    stunde.setLehrerGeaendert(std.getElementsByTagName("Le").item(0).hasAttributes());
                    stunde.setLehrer(std.getElementsByTagName("Le").item(0).getTextContent().replaceAll("&nbsp;", ""));
                    stunde.setRaumGeaendert(std.getElementsByTagName("Ra").item(0).hasAttributes());
                    stunde.setRaum(std.getElementsByTagName("Ra").item(0).getTextContent().replaceAll("&nbsp;", ""));
                    stunde.setInfo(std.getElementsByTagName("If").item(0).getTextContent().replaceAll("&nbsp;", ""));

                    stunde.save(databaseWrapper);

                }
            }
        });

    }

    /**
     * Fügt zu einem OnlineTag die Vertretung, abwesendeLehrer, aenderungLehrer und aenderungKlassen aus VplanKl.xml
     *
     *
     *//*
    private void addVertretungtoDay(String xml, final OnlineTag onlineTag) throws ParserConfigurationException, IOException, SAXException {

        //delete old Vertretung
        new Delete().from(Vertretung.class).where(Vertretung_Table.onlineTag_id.eq(onlineTag.getId())).execute();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xml));
        final Document dom = builder.parse(is);

        FlowManager.getDatabase(AppDatabase.class).beginTransactionAsync(new ITransaction() {
            @Override
            public void execute(DatabaseWrapper databaseWrapper) {

                onlineTag.setAbwesendeLehrer(dom.getElementsByTagName("abwesendl").item(0).getTextContent());
                onlineTag.setAenderungLehrer(dom.getElementsByTagName("aenderungl").item(0).getTextContent());
                onlineTag.setAenderungKlassen(dom.getElementsByTagName("aenderungk").item(0).getTextContent());
                onlineTag.save(databaseWrapper);

                NodeList aktionen = dom.getElementsByTagName("aktion");
                for (int i = 0; i < aktionen.getLength(); i++) {
                    Element aktion = (Element) aktionen.item(i);
                    Vertretung vertretung = new Vertretung();
                    vertretung.setKlasse(aktion.getElementsByTagName("klasse").item(0).getTextContent());
                    vertretung.setStunde(aktion.getElementsByTagName("stunde").item(0).getTextContent());
                    vertretung.setFach(aktion.getElementsByTagName("fach").item(0).getTextContent());
                    vertretung.setLehrer(aktion.getElementsByTagName("lehrer").item(0).getTextContent());
                    vertretung.setLehrerGeaendert(aktion.getElementsByTagName("lehrer").item(0).getAttributes().getLength() > 0);
                    vertretung.setRaum(aktion.getElementsByTagName("raum").item(0).getTextContent());
                    vertretung.setRaumGeaendert(aktion.getElementsByTagName("raum").item(0).getAttributes().getLength() > 0);
                    vertretung.setInfo(aktion.getElementsByTagName("info").item(0).getTextContent());
                    vertretung.setOnlineTag(onlineTag);

                    vertretung.save(databaseWrapper);
                }

            }
        }).execute();
    }*/

    public interface DownloadAllDataResponseListener {

        void onSuccess();

        void onNoConnection();

        void onNoAccess();

        void onProgress(int progress);

        void onOtherError(Throwable throwable);

        void onKlassenListFinished();

        void onFreieTageAdded();

        void onDaysUpdated();
    }

    private interface DownloadDaysListener {

        void onFinished(Date endDate);

        void onError(Throwable throwable);

        void onProgress(int progress);

        void onDaysUpdated();
    }

}
