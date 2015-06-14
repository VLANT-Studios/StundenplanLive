package de.conradowatz.jkgvertretung.tools;


import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Kurs;
import de.conradowatz.jkgvertretung.variables.StuPlaKlasse;
import de.conradowatz.jkgvertretung.variables.Stunde;
import de.conradowatz.jkgvertretung.variables.Tag;
import de.conradowatz.jkgvertretung.variables.Vertretung;

public class VertretungsAPI {

    private AsyncHttpClient client = new AsyncHttpClient();
    private String username;
    private String password;

    private ArrayList<Klasse> klassenList;
    private ArrayList<Date> freieTageList;
    private ArrayList<Tag> tagList;

    public void checkLogin (String username, String password, final AsyncLoginResponseHandler loginResponseHandler) {

        this.username = username;
        this.password = password;

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

    private double bytesToProgress(long bytesWritten, long totalSize) {

        return (totalSize > 0) ? (bytesWritten * 1.0 / totalSize) * 100 : -1;
    }

    public void getAllInfo (final AsyncVertretungsResponseHandler responseHandler) {

        //KlassenList
        client.get("http://kepler.c.sn.schule.de/stuplanindiware/VmobilS/mobdaten/Klassen.xml", new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                if (statusCode==401) {
                    responseHandler.onNoAccess();
                } else {
                    responseHandler.onNoConnection();
                }
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                responseHandler.onProgress(bytesToProgress(bytesWritten, totalSize)/7);
                super.onProgress(bytesWritten, totalSize);
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

                //freie Tage
                try {
                    freieTageList = makeFreieTage(responseString);
                } catch (Exception e) {
                    responseHandler.onOtherError(e);
                    return;
                }


                //Tag Liste
                makeTagList(3, 0, new SimpleFinishedHandler() {
                    @Override
                    public void onFinished() {
                        responseHandler.onSuccess();
                    }

                    @Override
                    public void onProgress(double progress) {
                        responseHandler.onProgress(100.0/7.0+(6.0/7.0)*progress);
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
        this.username = username;
        this.password = password;
        client.setBasicAuth(username, password);
    }

    private ArrayList<Klasse> makeKlassen (String xml) throws XmlPullParserException, IOException {

        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
        XmlPullParser pullParser = xmlPullParserFactory.newPullParser();
        InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        pullParser.setInput(is, "UTF-8");

        ArrayList<Klasse> klassenList = new ArrayList<>();
        ArrayList<Kurs> currentKursList = new ArrayList<>();
        Klasse currentKlasse = null;

        int event = pullParser.getEventType();
        while (event!=XmlPullParser.END_DOCUMENT) {

            String name = pullParser.getName();
            switch (event) {
                case XmlPullParser.START_TAG:
                    if (name.equals("Kurz")) {
                        currentKlasse = new Klasse( pullParser.nextText() );
                        currentKursList = new ArrayList<>();
                    }
                    if (name.equals("KKz")) {
                        String lehrer = pullParser.getAttributeValue(null, "KLe");
                        currentKursList.add( new Kurs(pullParser.nextText(), lehrer) );
                        //Log.d("LOGY", pullParser.nextText());
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
        while (event!=XmlPullParser.END_DOCUMENT && !abbruch) {

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

    private Date nextSchoolDay(Date startDate) {

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);
        Calendar nextCalendar = startCalendar;
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

        return (dayOfWeek==Calendar.SATURDAY || dayOfWeek==Calendar.SUNDAY || freieTageList.contains(date));
    }

    private void makeTagList (final int count, int skip, final SimpleFinishedHandler responseHandler) {

        Date startDate = Calendar.getInstance().getTime();

        if (isntSchoolDay(startDate)) {
            skip++;
        }

        for (int i=0; i<skip; i++) {
            startDate = nextSchoolDay(startDate);
        }

        tagList = new ArrayList<>();

        downloadDays(count-1, startDate, new DownloadDaysHandler() {
            @Override
            public void onFinished() {
                responseHandler.onFinished();
            }

            @Override
            public void onProgress(int currentCount, double progress) {
                double doubleCount = count;
                responseHandler.onProgress((doubleCount-currentCount)/doubleCount * progress);
            }

            @Override
            public void onError(Throwable throwable) {
                responseHandler.onError(throwable);
            }
        });


    }

    private void downloadDays(final int count, final Date date, final DownloadDaysHandler responseHandler) {

        String dateString = new SimpleDateFormat("yyyMMdd").format(date);
        String stundenPlanUrl = "http://kepler.c.sn.schule.de/stuplanindiware/VmobilS/mobdaten/PlanKl"+dateString+".xml";
        final String vertretungsPlanUrl = "http://kepler.c.sn.schule.de/stuplanindiware/VplanonlineS/vdaten/VplanKl" + dateString + ".xml";
        Log.d("SWAG", stundenPlanUrl);
        Log.d("SWAG", vertretungsPlanUrl);

        client.get(stundenPlanUrl, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                responseHandler.onFinished();
                if (statusCode!=401) {
                    responseHandler.onError(throwable);
                }
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                double progress = bytesToProgress(bytesWritten, totalSize);
                responseHandler.onProgress(count, progress / 2);
                super.onProgress(bytesWritten, totalSize);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {

                try {
                    final ArrayList<StuPlaKlasse> currentStundenList = makeStundenPlanList(responseString);

                    client.get(vertretungsPlanUrl, new TextHttpResponseHandler() {
                        @Override
                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                            responseHandler.onFinished();
                            if (statusCode!=401) {
                                responseHandler.onError(throwable);
                            }
                        }

                        @Override
                        public void onProgress(long bytesWritten, long totalSize) {
                            double progress = bytesToProgress(bytesWritten, totalSize);
                            responseHandler.onProgress(count, 50 + progress / 2);
                            super.onProgress(bytesWritten, totalSize);
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String responseString) {

                            try {

                                ArrayList<Vertretung> vertretungsList = null;
                                vertretungsList = makeVertretungsList(responseString);

                                String datumString = responseString.substring(responseString.indexOf("<titel>")+7, responseString.indexOf("</titel>"));


                                Tag tag = new Tag(date, datumString, currentStundenList, vertretungsList);
                                tagList.add(tag);

                                if (count>0) {
                                    downloadDays(count - 1, nextSchoolDay(date), responseHandler);
                                } else {
                                    responseHandler.onFinished();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                responseHandler.onFinished();
                                responseHandler.onError(e);
                                return;
                            }

                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    responseHandler.onFinished();
                    responseHandler.onError(e);
                    return;
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
        while (event!=XmlPullParser.END_DOCUMENT) {

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
                    if(name.equals("Ra")) {
                        String raum = pullParser.nextText();
                        if (raum.startsWith("&nbsp")) {
                            currentStunde.setRaum("");
                        } else {
                            currentStunde.setRaum(raum);
                        }
                    }
                    if (name.equals("If")) {
                        String info = pullParser.nextText();
                        if (info==null || info.startsWith("&nbsp")) {
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
        while (event!=XmlPullParser.END_DOCUMENT) {

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
    }

    private static abstract class SimpleFinishedHandler {

        public abstract void onFinished();
        public abstract void onError(Throwable throwable);
        public void onProgress(double progress) {

        }
    }

    private static abstract class DownloadDaysHandler {

        public abstract void onFinished();
        public abstract void onError(Throwable throwable);
        public void onProgress(int count, double progress) {

        }
    }

}
