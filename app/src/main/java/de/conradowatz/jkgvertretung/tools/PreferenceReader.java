package de.conradowatz.jkgvertretung.tools;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;

public class PreferenceReader {

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

    public static void saveStringListToPreferences(Context context, String preferenceName, ArrayList<String> preferenceValue) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(preferenceName + "_count", preferenceValue.size());
        for (int i=0; i<preferenceValue.size(); i++) {
            editor.putString(preferenceName+"_"+String.valueOf(i), preferenceValue.get(i));
        }

        editor.apply();
    }

    public static ArrayList<String> readStringListFromPreferences(Context context, String preferenceName) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        ArrayList<String> stringList = new ArrayList<>();
        int count = sharedPreferences.getInt(preferenceName+"_count", 0);
        for (int i=0; i<count; i++) {
            String string = sharedPreferences.getString(preferenceName+"_"+String.valueOf(i), "");
            stringList.add(string);
        }
        return stringList;
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
}
