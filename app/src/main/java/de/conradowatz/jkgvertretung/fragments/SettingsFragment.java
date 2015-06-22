package de.conradowatz.jkgvertretung.fragments;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;

import java.util.Arrays;

import de.conradowatz.jkgvertretung.R;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private ListPreference startScreen;
    private ListPreference maxDaysToFetchStart;
    private ListPreference maxDaysToFetchRefresh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        startScreen = (ListPreference) findPreference("startScreen");
        maxDaysToFetchRefresh = (ListPreference) findPreference("maxDaysToFetchRefresh");
        maxDaysToFetchStart = (ListPreference) findPreference("maxDaysToFetchStart");

        startScreen.setSummary(startScreen.getEntry());
        maxDaysToFetchRefresh.setSummary(maxDaysToFetchRefresh.getEntry());
        maxDaysToFetchStart.setSummary(maxDaysToFetchStart.getEntry());

        startScreen.setOnPreferenceChangeListener(this);
        maxDaysToFetchRefresh.setOnPreferenceChangeListener(this);
        maxDaysToFetchStart.setOnPreferenceChangeListener(this);
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        ListPreference listPreference = (ListPreference) preference;
        int position = Arrays.asList(listPreference.getEntryValues()).indexOf(newValue);
        listPreference.setSummary(listPreference.getEntries()[position]);
        return true;
    }
}
