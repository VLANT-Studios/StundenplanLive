package de.conradowatz.jkgvertretung.fragments;


import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ObservableScrollView;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;

import java.util.ArrayList;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.MainActivity;
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Kurs;


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

    public KurswahlFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d("SWAG", "onCreateView");

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
                    editMode();
                } else {
                    saveKurse();
                }
            }
        });
        ObservableScrollView scrollView = (ObservableScrollView) contentView.findViewById(R.id.scrollView);
        fab.attachToScrollView(scrollView);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity.vertretungsAPI==null) return contentView;
        showKlassen(mainActivity.vertretungsAPI.getKlassenList());

        if (savedInstanceState!=null) {
            String[] checkBoxNames = savedInstanceState.getStringArray("checkBoxNames");
            boolean[] checkBoxChecks = savedInstanceState.getBooleanArray("checkBoxChecks");
            kurseLayout.removeAllViews();
            for (int j=0; j<checkBoxChecks.length; j++) {
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

    private void saveKurse() {

        editMode = false;
        fab.setImageResource(R.drawable.ic_mode_edit);

        Log.d("SWAG", "saving...");

        PreferenceReader.saveIntToPreferences(getActivity(), "meineKlasseInt", klassenSpinner.getSelectedItemPosition());

        ArrayList<String> saveArray = new ArrayList<>();
        for (int i = 0; i<kurseLayout.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);
            if (!checkBox.isChecked()) {
                saveArray.add(selectedKurse.get(i).getName());

            }
        }

        PreferenceReader.saveStringListToPreferences(getActivity(), "meineNichtKurse", saveArray);

        setEditable(false);

    }

    private void setEditable(boolean editable) {

        klassenSpinner.setEnabled(editable);
        buttonKeine.setEnabled(editable);
        buttonAlle.setEnabled(editable);
        for (int i = 0; i<kurseLayout.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);
            checkBox.setEnabled(editable);
        }

    }

    private void editMode() {

        Log.d("SWAG", "editMode");

        editMode = true;
        fab.setImageResource(R.drawable.ic_done);

        setEditable(true);

    }

    private void showKlassen(final ArrayList<Klasse> klassenList) {

        Log.d("SWAG", "showKlassen");

        ArrayList<String> klassennamenListe = new ArrayList<>();
        for (Klasse klasse : klassenList) {
            klassennamenListe.add(klasse.getName());
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, klassennamenListe);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        klassenSpinner.setAdapter(spinnerArrayAdapter);

        klassenSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position!=selected) {
                    showKurse(klassenList, position);
                    selected = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        loadKlasse();
    }

    private void loadKlasse() {

        Log.d("SWAG", "load saves...");

        int meineKlasseInt = PreferenceReader.readIntFromPreferences(getActivity(), "meineKlasseInt", -1);
        if (meineKlasseInt>=0) {
            klassenSpinner.setSelection(meineKlasseInt);
        }

    }

    private void loadKurse() {

        if (isLoaded) return;

        Log.d("SWAG", "loadKurse");

        ArrayList<String> meineNichtKurse = PreferenceReader.readStringListFromPreferences(getActivity(), "meineNichtKurse");
        if (meineNichtKurse!=null) {

            Log.d("SWAG", String.valueOf(kurseLayout.getChildCount()));
            for (int i = 0; i<kurseLayout.getChildCount(); i++) {
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

    private void showKurse(ArrayList<Klasse> klassenList, int position) {

        ArrayList<Kurs> alleKurse = klassenList.get(position).getKurse();
        Log.d("SWAG", "showKurse");

        int size = alleKurse.size();

        for (int i=0; i<size-1; i++) {
            for (int j=i+1; j<size; j++) {
                if (!alleKurse.get(j).getName().equals(alleKurse.get(i).getName())) {
                    continue;
                }
                alleKurse.get(i).setLehrer(alleKurse.get(i).getLehrer()+", "+alleKurse.get(j).getLehrer());
                alleKurse.remove(j);
                j--;
                size--;
            }
        }

        selectedKurse = alleKurse;

        ArrayList<String> kurseStringList = new ArrayList<>();
        for (int i=0; i<alleKurse.size(); i++) {
            Kurs kurs = alleKurse.get(i);
            String text = kurs.getName();
            if (!kurs.getLehrer().isEmpty()) {
                text = text+" ("+kurs.getLehrer()+")";
            }
            kurseStringList.add(text);
        }

        kurseLayout.removeAllViews();
        for (int j=0; j<kurseStringList.size(); j++) {
            CheckBox checkBox = new CheckBox(getActivity());
            checkBox.setText(kurseStringList.get(j));
            checkBox.setEnabled(editMode);
            kurseLayout.addView(checkBox);
        }

        loadKurse();

    }

    private void selectAlleKurse() {

        for (int i = 0; i<kurseLayout.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);
            checkBox.setChecked(true);
        }

    }

    private void selectKeineKurse() {

        for (int i = 0; i<kurseLayout.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) kurseLayout.getChildAt(i);
            checkBox.setChecked(false);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        String[] checkBoxNames = new String[kurseLayout.getChildCount()];
        boolean[] checkBoxChecks = new boolean[kurseLayout.getChildCount()];
        for (int i = 0; i<kurseLayout.getChildCount(); i++) {
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
}
