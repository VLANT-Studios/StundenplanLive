package de.conradowatz.jkgvertretung.tools;

import android.content.Context;
import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

public class ColorAPI {
    private Context context;
    private Map<String, Integer> defaultColors = new HashMap<>();

    public void setUp() {
        defaultColors.put("actionBar", Color.rgb(0x4a, 0x8a, 0xba));
        defaultColors.put("accent", Color.rgb(0x4a, 0x8a, 0xba));
    }

    public ColorAPI(Context context) {
        this.context = context;
        setUp();
    }

    public int getActionBarColor() {
        return PreferenceHelper.readIntFromPreferences(context, "actionBar_color", defaultColors.get("actionBar"));
    }

    public int getAccentColor() {
        return PreferenceHelper.readIntFromPreferences(context, "accent_color", defaultColors.get("accent"));
    }

    public void setAccentColor(int color) {
        PreferenceHelper.saveIntToPreferences(context, "accent_color", color);
    }

    public void setActionBarColor(int color) {
        PreferenceHelper.saveIntToPreferences(context, "actionBar_color", color);
    }

    public int getTabAccentColor() {
        int acc = getActionBarColor();
        int r = Math.round(Color.red(acc) * 0.8f);
        int g = Math.round(Color.green(acc) * 0.8f);
        int b = Math.round(Color.blue(acc) * 0.8f);
        return Color.rgb(r, g, b);
    }
}
