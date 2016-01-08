package de.conradowatz.jkgvertretung.fragments;


import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.ArrayList;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.DataReadyEvent;
import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.KlassenlistUpdatedEvent;
import de.conradowatz.jkgvertretung.variables.Kurs;
import de.greenrobot.event.EventBus;


public class KurswahlFragment extends Fragment {

    private View contentView;

    private Spinner klassenSpinner;
    private LinearLayout kurseLayout;
    private Button buttonAlle;
    private Button buttonKeine;
    private FloatingActionButton fab;

    private ArrayList<Kurs> selectedKurse;

    private boolean editMode;
    private boolean isLoaded;
    private int selected;

    private EventBus eventBus = EventBus.getDefault();
    private boolean waitingForData = false;

    public KurswahlFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Analytics
        MyApplication analytics = (MyApplication) getActivity().getApplication();
        analytics.fireScreenHit("Kurswahl");

        eventBus.register(this);

        contentView = inflater.inflate(R.layout.fragment_kurswahl, container, false);

        klassenSpinner = (Spinner) contentView.findViewById(R.id.klassenSpinner);
        kurseLayout = (LinearLayout) contentView.findViewById(R.id.kurseLayout);
        buttonAlle = (Button) contentView.findViewById(R.id.buttonAlle);
        buttonKeine = (Button) contentView.findViewById(R.id.buttonKeine);
        fab = (FloatingActionButton) contentView.findViewById(R.id.fab);

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

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!editMode) {
                    setEditable(true);
                } else {
                    saveKurse();
                    setEditable(false);
                }
            }
        });

        if (savedInstanceState != null) {

            String[] checkBoxNames = savedInstanceState.getStringArray("checkBoxNames");
            boolean[] checkBoxChecks = savedInstanceState.getBooleanArray("checkBoxChecks");
            kurseLayout.removeAllViews();
            for (int j = 0; j < checkBoxChecks.length; j++) {
                CheckBox checkBox = new CheckBox(getActivity());
                checkBox.setText(checkBoxNames[j]);
                checkBox.setChecked(checkBoxChecks[j]);
                kurseLayout.addView(checkBox);
            }
            editMode = savedInstanceState.getBoolean("editMode");
            if (editMode) {
                fab.setImageResource(R.drawable.ic_done);
            } else {
                fab.setImageResource(R.drawable.ic_mode_edit);
            }
            isLoaded = savedInstanceState.getBoolean("isLoaded");
            selected = savedInstanceState.getInt("selected");
            setEditable(editMode);
            selectedKurse = savedInstanceState.getParcelableArrayList("selectedKurse");

        } else {

            editMode = false;
            isLoaded = false;
            selected = -1;
            setEditable(false);

        }

        return contentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (!VertretungsData.getInstance().isReady()) {
            waitingForData = true;
            return;
        }

        showKlassen();
    }

    /**
     * Speichert die ausgewählten Kurse in den SharedPreferences
     */
    private void saveKurse() {

        PreferenceReader.saveIntToPreferences(getActivity(), "meineKlasseInt", klassenSpinner.getSelectedItemPosition());

        ArrayList<String> saveArray = new ArrayList<>();
        for (int i = 0; i < kurseLayout.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);
            if (!checkBox.isChecked()) {
                saveArray.add(selectedKurse.get(i).getName());

            }
        }

        PreferenceReader.saveStringListToPreferences(getActivity(), "meineNichtKurse", saveArray);

    }

    /**
     * Wechselt den EditMode und ändert den FAB
     *
     * @param editable EditMode
     */
    private void setEditable(boolean editable) {

        editMode = editable;
        if (editMode) {
            fab.setImageResource(R.drawable.ic_done);
        } else {
            fab.setImageResource(R.drawable.ic_mode_edit);
        }

        klassenSpinner.setEnabled(editable);
        buttonKeine.setEnabled(editable);
        buttonAlle.setEnabled(editable);
        for (int i = 0; i < kurseLayout.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);
            checkBox.setEnabled(editable);
        }

    }

    /**
     * Läd alle Klassen in den Spinner
     */
    private void showKlassen() {

        ArrayList<String> klassennamenListe = new ArrayList<>();
        for (Klasse klasse : VertretungsData.getInstance().getKlassenList()) {
            klassennamenListe.add(klasse.getName());
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, klassennamenListe);
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

        int meineKlasseInt = PreferenceReader.readIntFromPreferences(getActivity(), "meineKlasseInt", -1);
        if (meineKlasseInt >= 0) {
            klassenSpinner.setSelection(meineKlasseInt);
        }

    }

    /**
     * Selektiert die Kurse, die vom Nutzer gespeichert wurden
     * in den SharedPreferences sind alle nicht ausgewählten Kurse gespeichert
     */
    private void loadKurse() {

        if (isLoaded) return;

        ArrayList<String> meineNichtKurse = PreferenceReader.readStringListFromPreferences(getActivity(), "meineNichtKurse");
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

        isLoaded = true;

    }

    /**
     * Zeigt alle Kurse einer Klasse als Checkboxen
     * @param position der Index der Klasse
     */
    private void showKurse(int position) {

        ArrayList<Kurs> alleKurse = VertretungsData.getInstance().getKlassenList().get(position).getKurse();

        int size = alleKurse.size();

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
            CheckBox checkBox = new CheckBox(getActivity());
            checkBox.setText(kurseStringList.get(j));
            checkBox.setEnabled(editMode);
            kurseLayout.addView(checkBox);
        }

        loadKurse();

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
        outState.putBoolean("editMode", editMode);
        outState.putBoolean("isLoaded", isLoaded);
        outState.putInt("selected", selected);
        super.onSaveInstanceState(outState);
    }

    /**
     * Läd die KlassenList neu
     * Wird gecallt wenn die Liste aktualisiert wurde
     */
    public void onEvent(KlassenlistUpdatedEvent event) {

        if (contentView != null && VertretungsData.getInstance().isReady())
            showKlassen();

    }

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
}
