package de.conradowatz.jkgvertretung.tools;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import de.conradowatz.jkgvertretung.variables.Backup;

public class DataVersionCompat {

    public static LocalData createLocalData(String json) {

        int saveFileVersion = getFileVersionFromJSON(json);

        if (saveFileVersion == 1) {

            try {
                Gson gson = new Gson(); //new Gson() instead of Utilities.getDefaultGson()
                return gson.fromJson(json, LocalData.class);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }

        return null;
    }

    public static Backup createBackupData(String json) {

        int saveFileVersion = getFileVersionFromJSON(json);

        if (saveFileVersion == 1) {

            try {
                Gson gson = new Gson(); //new Gson() instead of Utilities.getDefaultGson()
                return gson.fromJson(json, Backup.class);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }

        return null;
    }

    private static int getFileVersionFromJSON(String json) {

        try {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.getInt("saveFileVersion");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
