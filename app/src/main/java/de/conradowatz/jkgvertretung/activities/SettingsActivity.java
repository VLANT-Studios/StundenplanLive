package de.conradowatz.jkgvertretung.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import de.conradowatz.jkgvertretung.fragments.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

}
