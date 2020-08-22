package de.conradowatz.jkgvertretung.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.events.KlassenlistUpdatedEvent;
import de.conradowatz.jkgvertretung.events.KursChangedEvent;
import de.conradowatz.jkgvertretung.tools.ColorAPI;
import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Kurs;

public class KurswahlActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private AppCompatSpinner klassenSpinner;
    private LinearLayout kurseLayout;
    private Button buttonAlle;
    private Button buttonKeine;
    private SwitchCompat allSwitch;

    private List<Kurs> selectedKurse;

    private boolean isLoaded;
    private long selectedKlasseId;
    private boolean isSaveDialog;

    private EventBus eventBus = EventBus.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_kurswahl);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(new ColorAPI(this).getActionBarColor()));
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);

        eventBus.register(this);

        klassenSpinner = (AppCompatSpinner) findViewById(R.id.klassenSpinner);
        kurseLayout = (LinearLayout) findViewById(R.id.kurseLayout);
        buttonAlle = (Button) findViewById(R.id.buttonAlle);
        buttonKeine = (Button) findViewById(R.id.buttonKeine);
        allSwitch = (SwitchCompat) findViewById(R.id.allSwitch);

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
            selectedKlasseId = savedInstanceState.getLong("selectedKlasseId");
            selectedKurse = savedInstanceState.getParcelableArrayList("selectedKurse");
            isSaveDialog = savedInstanceState.getBoolean("isSaveDialog");

        } else {

            isLoaded = false;
            selectedKlasseId = -1;

        }

        showKlassen();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_save, menu);
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

        Klasse klasse = Klasse.getKlasse(selectedKlasseId);
        Klasse.setSelectedKlasse(klasse.getName());

        ArrayList<Integer> saveArray = new ArrayList<>();
        for (int i = 0; i < kurseLayout.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);
            if (!checkBox.isChecked()) {
                saveArray.add(selectedKurse.get(i).getNr());

            }
        }

        Kurs.setSelectedKurse(saveArray);

        eventBus.post(new KursChangedEvent());

    }

    /**
     * Läd alle Klassen in den Spinner
     */
    private void showKlassen() {

        new AsyncTask<Activity, Integer, List<String>>() {
            Activity activity;
            List<Klasse> klassen;
            @Override
            protected ArrayList<String> doInBackground(Activity... params) {

                this.activity = params[0];
                ArrayList<String> klassenNamenListe = new ArrayList<>();
                klassen = Klasse.getAllKlassenSorted();
                for (Klasse klasse : klassen) {
                    klassenNamenListe.add(klasse.getName());
                }

                return klassenNamenListe;
            }

            @Override
            protected void onPostExecute(List<String> klassenNamenListe) {

                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, klassenNamenListe);
                spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                klassenSpinner.setAdapter(spinnerArrayAdapter);

                klassenSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        showKurse(klassen.get(position));
                        selectedKlasseId = klassen.get(position).getId();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                allSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Klasse klasse = Klasse.getKlasse(selectedKlasseId);
                        if (klasse!=null) showKurse(klasse);
                    }
                });

                loadKlasse();

            }
        }.execute(this);
    }

    /**
     * Zeigt die gespeicherte Klasse aus den SharedPreferences an
     */
    private void loadKlasse() {

        new AsyncTask<Activity, Integer, List<Klasse>>() {
            Klasse selectedKlasse;
            @Override
            protected List<Klasse> doInBackground(Activity... params) {
                selectedKlasse = Klasse.getSelectedKlasse();
                if (selectedKlasse!=null) return Klasse.getAllKlassenSorted();
                else return null;
            }

            @Override
            protected void onPostExecute(List<Klasse> klassen) {
                if (klassen!=null) {
                    int selectedIndex = 0;
                    for (int i=0; i<klassen.size(); i++) {
                        if (klassen.get(i).getId()==selectedKlasse.getId()) {
                            selectedIndex = i;
                            break;
                        }
                    }
                    klassenSpinner.setSelection(selectedIndex);
                }
            }
        }.execute();

    }

    /**
     * Selektiert die Kurse, die vom Nutzer gespeichert wurden
     * in den SharedPreferences sind alle nicht ausgewählten Kurse gespeichert
     */
    private void loadKurse() {

        List<Integer> notSelectedKurseNr = Kurs.getNotSelectedKursNrn();
        if (notSelectedKurseNr != null) {

            for (int i = 0; i < kurseLayout.getChildCount(); i++) {
                CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);

                if (notSelectedKurseNr.contains(selectedKurse.get(i).getNr())) {
                    checkBox.setChecked(false);
                } else {
                    checkBox.setChecked(true);
                }
            }
        }

    }

    /**
     * Zeigt alle Kurse einer Klasse als Checkboxen
     *
     * @param klasse Klasse
     */
    private void showKurse(final Klasse klasse) {

        final Activity activity = this;
        new AsyncTask<Boolean, Integer, List<Kurs>>() {
            @Override
            protected List<Kurs> doInBackground(Boolean... params) {
                Boolean showAll = params[0];
                return showAll?klasse.getSortedKurse():klasse.getAuswaehlbareKurseSorted();
            }

            @Override
            protected void onPostExecute(List<Kurs> alleKurse) {

                selectedKurse = alleKurse;

                ArrayList<String> kurseStringList = new ArrayList<>();
                for (int i = 0; i < alleKurse.size(); i++) {
                    Kurs kurs = alleKurse.get(i);
                    String text = kurs.getFachName();
                    if (kurs.getBezeichnung()!=null) {
                        text = text + ": " + kurs.getBezeichnung();
                    }
                    if (kurs.getLehrer()!=null) {
                        text = text + " (" + kurs.getLehrer().getName() + ")";
                    }
                    kurseStringList.add(text);
                }

                kurseLayout.removeAllViews();
                for (int j = 0; j < kurseStringList.size(); j++) {
                    CheckBox checkBox = new CheckBox(activity);
                    checkBox.setText(kurseStringList.get(j));
                    kurseLayout.addView(checkBox);
                }

                loadKurse();

            }
        }.execute(allSwitch.isChecked());

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
        outState.putParcelableArrayList("selectedKurse", (ArrayList<? extends Parcelable>) selectedKurse);
        outState.putBoolean("isLoaded", isLoaded);
        outState.putLong("selectedKlasseId", selectedKlasseId);
        outState.putBoolean("isSaveDialog", isSaveDialog);
        super.onSaveInstanceState(outState);
    }

    /**
     * Läd die KlassenList neu
     * Wird gecallt wenn die Liste aktualisiert wurde
     */
    @Subscribe
    public void onEvent(KlassenlistUpdatedEvent event) {

        if (klassenSpinner != null)
            showKlassen();

    }


    @Override
    public void onPause() {
        super.onPause();

        eventBus.unregister(this);
    }

    @Override
    public void onBackPressed() {

        showSaveDialog();
    }


}
