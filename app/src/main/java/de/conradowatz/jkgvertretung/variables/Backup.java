package de.conradowatz.jkgvertretung.variables;

import android.content.Context;

import java.util.Map;

import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.tools.VertretungsData;

public class Backup {

    public static final String SAVE_FILE_NAME = "backup%s.json";
    public static final String DATE_FORMAT = "dd-MM-yyyy_HH-mm-ss";
    public static int latestSaveFileVersion = 1;
    private int saveFileVersion;

    private LocalData localData;
    private VertretungsData vertretungsData;
    private Map<String, ?> sharedPreferences;

    public Backup(LocalData localData, VertretungsData vertretungsData, Map<String, ?> sharedPreferences) {
        this.localData = localData;
        this.vertretungsData = vertretungsData;
        this.sharedPreferences = sharedPreferences;
    }

    public Backup(Context context) {
        localData = LocalData.getInstance();
        vertretungsData = VertretungsData.getInstance();
        sharedPreferences = PreferenceHelper.getSharedPrefrencesForBackup(context);
        saveFileVersion = latestSaveFileVersion;
    }

    public LocalData getLocalData() {
        return localData;
    }

    public void setLocalData(LocalData localData) {
        this.localData = localData;
    }

    public VertretungsData getVertretungsData() {
        return vertretungsData;
    }

    public void setVertretungsData(VertretungsData vertretungsData) {
        this.vertretungsData = vertretungsData;
    }

    public int getSaveFileVersion() {
        return saveFileVersion;
    }

    public void setSaveFileVersion(int saveFileVersion) {

        this.saveFileVersion = saveFileVersion;
    }

    public Map<String, ?> getSharedPreferences() {
        return sharedPreferences;
    }

    public void setSharedPreferences(Map<String, ?> sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }
}
