package de.conradowatz.jkgvertretung.fragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.List;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Kurs;

public class FachSettingsFragment extends Fragment {

    private AppCompatSpinner kursSpinner;
    private AppCompatSpinner notenModusSpinner;
    private CheckBox checkBox;
    private View contentView;
    private Fach fach;

    public FachSettingsFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_fach_settings, container, false);

        fach = ((FachActivity) getActivity()).getFach();

        checkBox = (CheckBox) contentView.findViewById(R.id.checkbox);
        kursSpinner = (AppCompatSpinner) contentView.findViewById(R.id.kursSpinner);
        notenModusSpinner = (AppCompatSpinner) contentView.findViewById(R.id.notenModusSpinner);

        setUp();


        return contentView;
    }

    private void setUp() {

        if (LocalData.isOberstufe()) {

            checkBox.setVisibility(View.VISIBLE);
            checkBox.setChecked(fach.isLeistungskurs());
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    fach.setLeistungskurs(isChecked);
                    fach.save();
                }
            });

        } else {
            checkBox.setVisibility(View.GONE);
        }

        Klasse selectedKlasse = Klasse.getSelectedKlasse();
        if (selectedKlasse!=null) {
            final List<Kurs> kurse = selectedKlasse.getSortedKurse();
            List<String> kursnamenListe = new ArrayList<>();
            kursnamenListe.add("Keine Verknüpfung");
            int selected = 0;
            for (int i=0; i<kurse.size(); i++) {
                Kurs k = kurse.get(i);
                String text = k.getFachName();
                if (k.getBezeichnung()!=null) {
                    text = text + ": " + k.getBezeichnung();
                }
                if (k.getLehrer()!=null) {
                    text = text + " (" + k.getLehrer().getName() + ")";
                }
                kursnamenListe.add(text);
                if (fach.getKurs()!=null && fach.getKurs().getNr()==k.getNr()) selected = i+1;
            }
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, kursnamenListe);
            kursSpinner.setAdapter(spinnerArrayAdapter);

            kursSpinner.setSelection(selected);
            kursSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position==0) {
                        fach.setKurs(null);
                        fach.setAssKursNr(0);
                    } else {
                        fach.setKurs(kurse.get(position-1));
                        fach.setAssKursNr(kurse.get(position-1).getNr());
                    }
                    fach.save();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, new String[]{"50:50", "70:30", "60:40", "2xLK"});
        notenModusSpinner.setAdapter(spinnerArrayAdapter);
        notenModusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        fach.setNotenModus(Fach.MODE_5050);
                        break;
                    case 1:
                        fach.setNotenModus(Fach.MODE_7030);
                        break;
                    case 2:
                        fach.setNotenModus(Fach.MODE_6040);
                        break;
                    case 3:
                        fach.setNotenModus(Fach.MODE_2LK);
                        break;
                }
                fach.save();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        switch (fach.getNotenModus()) {
            case Fach.MODE_5050:
                notenModusSpinner.setSelection(0);
                break;
            case Fach.MODE_7030:
                notenModusSpinner.setSelection(1);
                break;
            case Fach.MODE_6040:
                notenModusSpinner.setSelection(2);
                break;
            case Fach.MODE_2LK:
                notenModusSpinner.setSelection(3);
                break;
        }
    }
}
