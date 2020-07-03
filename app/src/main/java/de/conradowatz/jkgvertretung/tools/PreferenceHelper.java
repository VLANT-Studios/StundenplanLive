package de.conradowatz.jkgvertretung.tools;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PreferenceHelper {

    public static void deleteAllPreferences(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public static void saveStringToPreferences(Context context, String preferenceName, String preferenceValue) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(preferenceName, preferenceValue);
        editor.apply();
    }

    public static String readStringFromPreferences(Context context, String preferenceName, String defaultValue) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(preferenceName, defaultValue);
    }

    public static boolean readBooleanFromPreferences(Context context, String preferenceName, boolean defaultValue) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(preferenceName, defaultValue);
    }

    public static void saveBooleanToPrefernces(Context context, String preferenceName, Boolean preferenceValue) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(preferenceName, preferenceValue);
        editor.apply();
    }

    public static void saveStringListToPreferences(Context context, String preferenceName, List<String> preferenceValue) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(preferenceName + "_count", preferenceValue.size());
        for (int i = 0; i < preferenceValue.size(); i++) {
            editor.putString(preferenceName + "_" + String.valueOf(i), preferenceValue.get(i));
        }

        editor.apply();
    }

    public static List<String> readStringListFromPreferences(Context context, String preferenceName) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        ArrayList<String> stringList = new ArrayList<>();
        int count = sharedPreferences.getInt(preferenceName + "_count", -1);
        if (count < 0) return null;
        for (int i = 0; i < count; i++) {
            String string = sharedPreferences.getString(preferenceName + "_" + String.valueOf(i), "");
            stringList.add(string);
        }
        return stringList;
    }

    public static void saveIntListToPreferences(Context context, String preferenceName, List<Integer> preferenceValue) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(preferenceName + "_count", preferenceValue.size());
        for (int i = 0; i < preferenceValue.size(); i++) {
            editor.putInt(preferenceName + "_" + String.valueOf(i), preferenceValue.get(i));
        }

        editor.apply();
    }

    public static List<Integer> readIntListFromPreferences(Context context, String preferenceName) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        ArrayList<Integer> intList = new ArrayList<>();
        int count = sharedPreferences.getInt(preferenceName + "_count", -1);
        if (count < 0) return null;
        for (int i = 0; i < count; i++) {
            int number = sharedPreferences.getInt(preferenceName + "_" + String.valueOf(i), 0);
            intList.add(number);
        }
        return intList;
    }

    public static void saveIntToPreferences(Context context, String preferenceName, int preferenceValue) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(preferenceName, preferenceValue);
        editor.apply();
    }

    public static int readIntFromPreferences(Context context, String preferenceName, int defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(preferenceName, defaultValue);
    }

    public static void saveLongToPreferences(Context context, String preferenceName, long preferenceValue) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(preferenceName, preferenceValue);
        editor.apply();
    }

    public static long readLongFromPreferences(Context context, String preferenceName, long defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(preferenceName, defaultValue);
    }

    public static Map<String, ?> getSharedPrefrencesForBackup(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getAll();
    }

    public static void setSharedPrefrencesFromBackup(Context context, Map<String, ?> map) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();

            if (v instanceof Boolean)
                editor.putBoolean(key, (Boolean) v);
            else if (v instanceof Double)
                editor.putInt(key, ((Double) v).intValue());
            else if (v instanceof String)
                editor.putString(key, (String) v);
        }
        editor.apply();

    }
}
