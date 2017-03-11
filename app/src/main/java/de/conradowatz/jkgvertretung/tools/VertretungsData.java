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

import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Tag;

public class VertretungsData {

    public static String SAVE_FILE_NAME = "vertretungsData.json";
    public static int latestSaveFileVersion = 1;
    private static VertretungsData sInstance = null;
    private int saveFileVersion;
    private ArrayList<Klasse> klassenList;
    private ArrayList<String> freieTageList;
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

            Gson gson = Utilities.getDefaultGson();
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

        String json = "";
        VertretungsData vertretungsData;
        try {

            //Read Data
            FileInputStream inputStream = context.openFileInput(SAVE_FILE_NAME);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder(inputStream.available());
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            json = total.toString();

            Gson gson = Utilities.getDefaultGson();
            vertretungsData = gson.fromJson(json, VertretungsData.class);
            //if (vertretungsData == null || vertretungsData.saveFileVersion != latestSaveFileVersion)

        } catch (Exception e) {

            vertretungsData = null;
            e.printStackTrace();
        }

        if (vertretungsData == null) {

            createDataFromFileListener.onError(new Throwable("Vertrungsdata Datei kann nicht gelesen werden."));
            return;
        }

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

    }

    public void setSaveFileVersion(int saveFileVersion) {

        this.saveFileVersion = saveFileVersion;
    }

    public ArrayList<Klasse> getKlassenList() {
        return klassenList;
    }

    public void setKlassenList(ArrayList<Klasse> klassenList) {
        this.klassenList = klassenList;
    }

    public ArrayList<String> getFreieTageList() {
        return freieTageList;
    }

    public void setFreieTageList(ArrayList<String> freieTageList) {
        this.freieTageList = freieTageList;
    }

    public ArrayList<Tag> getTagList() {
        return tagList;
    }

    public void setTagList(ArrayList<Tag> tagList) {
        this.tagList = tagList;
    }

    public boolean isReady() {

        return (klassenList != null && klassenList.size() > 0 && freieTageList != null && tagList != null);
    }

    public interface CreateDataFromFileListener {

        void onCreated();

        void onError(Throwable throwable);

    }
}
