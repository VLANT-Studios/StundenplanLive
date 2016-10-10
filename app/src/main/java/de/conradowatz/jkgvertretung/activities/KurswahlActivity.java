package de.conradowatz.jkgvertretung.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.events.DataReadyEvent;
import de.conradowatz.jkgvertretung.events.KlassenlistUpdatedEvent;
import de.conradowatz.jkgvertretung.events.KursChangedEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Kurs;

public class KurswahlActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Spinner klassenSpinner;
    private LinearLayout kurseLayout;
    private Button buttonAlle;
    private Button buttonKeine;

    private ArrayList<Kurs> selectedKurse;

    private boolean isLoaded;
    private int selected;
    private boolean isSaveDialog;

    private EventBus eventBus = EventBus.getDefault();
    private boolean waitingForData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!VertretungsData.getInstance().isReady() || !LocalData.isReady()) {
            Intent spashIntent = new Intent(this, SplashActivity.class);
            spashIntent.putExtra("intent", getIntent());
            startActivity(spashIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_kurswahl);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);

        eventBus.register(this);

        klassenSpinner = (Spinner) findViewById(R.id.klassenSpinner);
        kurseLayout = (LinearLayout) findViewById(R.id.kurseLayout);
        buttonAlle = (Button) findViewById(R.id.buttonAlle);
        buttonKeine = (Button) findViewById(R.id.buttonKeine);

        buttonAlle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectAlleKurse();
            }
        });
        buttonKeine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectKeineKurse();
            }
        });

        if (savedInstanceState != null) {

            String[] checkBoxNames = savedInstanceState.getStringArray("checkBoxNames");
            boolean[] checkBoxChecks = savedInstanceState.getBooleanArray("checkBoxChecks");
            kurseLayout.removeAllViews();
            for (int j = 0; j < checkBoxChecks.length; j++) {
                CheckBox checkBox = new CheckBox(this);
                checkBox.setTextSize(20);
                checkBox.setText(checkBoxNames[j]);
                checkBox.setChecked(checkBoxChecks[j]);
                kurseLayout.addView(checkBox);
            }
            isLoaded = savedInstanceState.getBoolean("isLoaded");
            selected = savedInstanceState.getInt("selected");
            selectedKurse = savedInstanceState.getParcelableArrayList("selectedKurse");
            isSaveDialog = savedInstanceState.getBoolean("isSaveDialog");

        } else {

            isLoaded = false;
            selected = -1;

        }

        showKlassen();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_kurswahl, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_save) {

            saveKurse();
            finish();
            return true;

        } else if (id == android.R.id.home) {

            showSaveDialog();
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    private void showSaveDialog() {

        isSaveDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Änderungen verwerfen");
        builder.setMessage("Bist du sicher, dass du die Änderungen nicht speichern möchtest?");
        builder.setPositiveButton("Verwerfen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isSaveDialog = false;
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isSaveDialog = false;
            }
        });
        builder.show();

    }

    /**
     * Speichert die ausgewählten Kurse in den SharedPreferences
     */
    private void saveKurse() {

        int meineKlasseInt = klassenSpinner.getSelectedItemPosition();
        PreferenceHelper.saveIntToPreferences(getApplicationContext(), "meineKlasseInt", meineKlasseInt);

        ArrayList<String> saveArray = new ArrayList<>();
        for (int i = 0; i < kurseLayout.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);
            if (!checkBox.isChecked()) {
                saveArray.add(selectedKurse.get(i).getName());

            }
        }

        PreferenceHelper.saveStringListToPreferences(getApplicationContext(), "meineNichtKurse", saveArray);
        PreferenceHelper.saveStringListToPreferences(getApplicationContext(), "nichtKurse" + meineKlasseInt, saveArray);

        eventBus.post(new KursChangedEvent());

    }

    /**
     * Läd alle Klassen in den Spinner
     */
    private void showKlassen() {

        ArrayList<String> klassennamenListe = new ArrayList<>();
        for (Klasse klasse : VertretungsData.getInstance().getKlassenList()) {
            klassennamenListe.add(klasse.getName());
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, klassennamenListe);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        klassenSpinner.setAdapter(spinnerArrayAdapter);

        klassenSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != selected) {
                    showKurse(position);
                    selected = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        loadKlasse();
    }

    /**
     * Zeigt die gespeicherte Klasse aus den SharedPreferences an
     */
    private void loadKlasse() {

        int meineKlasseInt = PreferenceHelper.readIntFromPreferences(getApplicationContext(), "meineKlasseInt", -1);
        if (meineKlasseInt >= 0) {
            klassenSpinner.setSelection(meineKlasseInt);
        }

    }

    /**
     * Selektiert die Kurse, die vom Nutzer gespeichert wurden
     * in den SharedPreferences sind alle nicht ausgewählten Kurse gespeichert
     */
    private void loadKurse(int position) {

        //if (isLoaded) return;

        ArrayList<String> meineNichtKurse = PreferenceHelper.readStringListFromPreferences(getApplicationContext(), "nichtKurse" + position);
        if (meineNichtKurse != null) {

            for (int i = 0; i < kurseLayout.getChildCount(); i++) {
                CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);

                if (meineNichtKurse.contains(selectedKurse.get(i).getName())) {
                    checkBox.setChecked(false);
                } else {
                    checkBox.setChecked(true);
                }
            }
        }

        //isLoaded = true;

    }

    /**
     * Zeigt alle Kurse einer Klasse als Checkboxen
     *
     * @param position der Index der Klasse
     */
    private void showKurse(int position) {

        ArrayList<Kurs> alleKurse = VertretungsData.getInstance().getKlassenList().get(position).getKurse();

        int size = alleKurse.size();

        //Manche Kurse haben den selben Namen, diese können nicht auseinander gehalten werden, also werden sie zusammen gefasst
        for (int i = 0; i < size - 1; i++) {
            for (int j = i + 1; j < size; j++) {
                if (!alleKurse.get(j).getName().equals(alleKurse.get(i).getName())) {
                    continue;
                }
                alleKurse.get(i).setLehrer(alleKurse.get(i).getLehrer() + ", " + alleKurse.get(j).getLehrer());
                alleKurse.remove(j);
                j--;
                size--;
            }
        }

        selectedKurse = alleKurse;

        ArrayList<String> kurseStringList = new ArrayList<>();
        for (int i = 0; i < alleKurse.size(); i++) {
            Kurs kurs = alleKurse.get(i);
            String text = kurs.getName();
            if (!kurs.getLehrer().isEmpty()) {
                text = text + " (" + kurs.getLehrer() + ")";
            }
            kurseStringList.add(text);
        }

        kurseLayout.removeAllViews();
        for (int j = 0; j < kurseStringList.size(); j++) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(kurseStringList.get(j));
            kurseLayout.addView(checkBox);
        }

        loadKurse(position);

    }

    /**
     * Selektiert alle Kurs-Checkboxen
     */
    private void selectAlleKurse() {

        for (int i = 0; i < kurseLayout.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);
            checkBox.setChecked(true);
        }

    }

    /**
     * Deselektiert alle Kurs-Checkboxen
     */
    private void selectKeineKurse() {

        for (int i = 0; i < kurseLayout.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);
            checkBox.setChecked(false);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        String[] checkBoxNames = new String[kurseLayout.getChildCount()];
        boolean[] checkBoxChecks = new boolean[kurseLayout.getChildCount()];
        for (int i = 0; i < kurseLayout.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);
            checkBoxNames[i] = checkBox.getText().toString();
            checkBoxChecks[i] = checkBox.isChecked();
        }
        outState.putStringArray("checkBoxNames", checkBoxNames);
        outState.putBooleanArray("checkBoxChecks", checkBoxChecks);
        outState.putParcelableArrayList("selectedKurse", selectedKurse);
        outState.putBoolean("isLoaded", isLoaded);
        outState.putInt("selected", selected);
        outState.putBoolean("isSaveDialog", isSaveDialog);
        super.onSaveInstanceState(outState);
    }

    /**
     * Läd die KlassenList neu
     * Wird gecallt wenn die Liste aktualisiert wurde
     */
    @Subscribe
    public void onEvent(KlassenlistUpdatedEvent event) {

        if (klassenSpinner != null && VertretungsData.getInstance().isReady())
            showKlassen();

    }

    @Subscribe
    public void onEvent(DataReadyEvent event) {

        if (waitingForData) {
            waitingForData = false;

            showKlassen();
        }
    }


    @Override
    public void onPause() {
        eventBus.unregister(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {

        showSaveDialog();
    }


}
