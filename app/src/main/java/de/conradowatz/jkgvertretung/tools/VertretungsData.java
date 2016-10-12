package de.conradowatz.jkgvertretung.tools;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Tag;

public class VertretungsData {

    public static String SAVE_FILE_NAME = "vertretungsData.json";
    private static VertretungsData sInstance = null;
    private static int latestSaveFileVersion = 1;
    private int saveFileVersion;
    private ArrayList<Klasse> klassenList;
    private ArrayList<Date> freieTageList;
    private ArrayList<Tag> tagList;

    private VertretungsData() {

        saveFileVersion = latestSaveFileVersion;
    }

    public static VertretungsData getInstance() {

        if (sInstance == null) sInstance = new VertretungsData();
        return sInstance;
    }

    public static void setInstance(VertretungsData vertretungsData) {

        sInstance = vertretungsData;
    }

    /**
     * Speichert den derzeitigen Zustand der VertretungsData in den AppSpeicher
     *
     * @param context Context zum Zugriff auf den App-Speicher
     */
    public static void saveDataToFile(final Context context) {

        VertretungsData vertretungsData = VertretungsData.getInstance();

        try {

            Gson gson = new Gson();
            String dataToSave = gson.toJson(vertretungsData, VertretungsData.class);

            FileOutputStream outputStream = context.openFileOutput(SAVE_FILE_NAME, Context.MODE_PRIVATE);
            outputStream.write(dataToSave.getBytes(Charset.forName("UTF-8")));
            outputStream.close();

        } catch (Exception e) {

            Log.e("JKGDEBUG", "Fehler beim Speichern der VertretungsData");
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

        try {

            //Read Data
            FileInputStream inputStream = context.openFileInput(SAVE_FILE_NAME);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder(inputStream.available());
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }

            Gson gson = new Gson();
            VertretungsData vertretungsData = gson.fromJson(total.toString(), VertretungsData.class);
            if (vertretungsData == null || vertretungsData.saveFileVersion != latestSaveFileVersion)
                throw new Exception("Saved File is not compatible.");
            VertretungsData.setInstance(vertretungsData);

            //Löscht veraltete Tage
            for (int i = 0; i < VertretungsData.getInstance().getTagList().size(); i++) {
                Calendar tagCalendar = Calendar.getInstance();
                tagCalendar.setTime(VertretungsData.getInstance().getTagList().get(i).getDatum());
                if (Utilities.compareDays(tagCalendar, heute) < 0) {
                    VertretungsData.getInstance().getTagList().remove(i);
                    i--;
                }
            }
            createDataFromFileListener.onCreated();

        } catch (Exception e) {

            e.printStackTrace();
            createDataFromFileListener.onError(e);
        }

    }

    public ArrayList<Klasse> getKlassenList() {
        return klassenList;
    }

    public void setKlassenList(ArrayList<Klasse> klassenList) {
        this.klassenList = klassenList;
    }

    public ArrayList<Date> getFreieTageList() {
        return freieTageList;
    }

    public void setFreieTageList(ArrayList<Date> freieTageList) {
        this.freieTageList = freieTageList;
    }

    public ArrayList<Tag> getTagList() {
        return tagList;
    }

    public void setTagList(ArrayList<Tag> tagList) {
        this.tagList = tagList;
    }

    public boolean isReady() {

        return (klassenList != null && freieTageList != null && tagList != null);
    }

    public interface CreateDataFromFileListener {

        void onCreated();

        void onError(Throwable throwable);

    }
}
