package de.conradowatz.jkgvertretung.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.adapters.FachStundenRecyclerAdapter;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.Fach;

public class FachStundenFragment extends Fragment implements FachStundenRecyclerAdapter.Callback {

    public static final int STATE_IMMER = 0;
    public static final int STATE_AWOCHE = 1;
    public static final int STATE_BWOCHE = 2;
    private Fach fach;
    private boolean isReplaceDialog;
    private int replaceTag;
    private int replaceStunde;
    private int replacePos;
    private View contentView;
    private RecyclerView stundenRecycler;
    private Spinner wochenSpinner;

    public FachStundenFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_fach_stunden, container, false);
        stundenRecycler = (RecyclerView) contentView.findViewById(R.id.stundenRecycler);
        wochenSpinner = (Spinner) contentView.findViewById(R.id.wochenSpinner);

        fach = ((FachActivity) getActivity()).getFach();

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("isReplaceDialog"))
                onReplaceStunde(savedInstanceState.getInt("replaceTag"), savedInstanceState.getInt("replaceStunde"), savedInstanceState.getInt("replacePos"));
        }

        setUpSpinner();

        return contentView;
    }

    private void setUpSpinner() {

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, new String[]{"Jede Woche", "A-Woche", "B-Woche"});

        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        wochenSpinner.setAdapter(spinnerArrayAdapter);
        wochenSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setUpRecycler();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }


    private void setUpRecycler() {

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getActivity(), 6);

        int state = wochenSpinner.getSelectedItemPosition();

        FachStundenRecyclerAdapter adapter = new FachStundenRecyclerAdapter(getActivity().getApplicationContext(), fach, state, this);
        stundenRecycler.setLayoutManager(layoutManager);
        stundenRecycler.setAdapter(adapter);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putBoolean("isReplaceDialog", isReplaceDialog);
        outState.putInt("replaceStunde", replaceStunde);
        outState.putInt("replaceTag", replaceTag);
        outState.putInt("replacePos", replacePos);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onNewStunde(int tag, int stunde, int pos) {

        int state = wochenSpinner.getSelectedItemPosition();
        if (state == STATE_AWOCHE || state == STATE_IMMER) fach.getaStunden()[tag][stunde] = true;
        if (state == STATE_BWOCHE || state == STATE_IMMER) fach.getbStunden()[tag][stunde] = true;
        if (state == STATE_IMMER)
            ((FachStundenRecyclerAdapter) stundenRecycler.getAdapter()).calculateStateImmer();
        stundenRecycler.getAdapter().notifyItemChanged(pos);
    }

    @Override
    public void onRemoveStunde(int tag, int stunde, int pos) {

        int state = wochenSpinner.getSelectedItemPosition();
        if (state == STATE_AWOCHE || state == STATE_IMMER) fach.getaStunden()[tag][stunde] = false;
        if (state == STATE_BWOCHE || state == STATE_IMMER) fach.getbStunden()[tag][stunde] = false;
        if (state == STATE_IMMER)
            ((FachStundenRecyclerAdapter) stundenRecycler.getAdapter()).calculateStateImmer();
        stundenRecycler.getAdapter().notifyItemChanged(pos);

    }

    @Override
    public void onReplaceStunde(final int tag, final int stunde, final int pos) {

        isReplaceDialog = true;
        replaceTag = tag;
        replaceStunde = stunde;
        replacePos = pos;

        final int state = wochenSpinner.getSelectedItemPosition();
        final List<Fach> replaceFaecher = new ArrayList<>();
        if (state != STATE_IMMER) {
            for (Fach f : LocalData.getInstance().getFächer())
                if (f != fach && f.getStunden(state == STATE_AWOCHE)[tag][stunde]) {
                    replaceFaecher.add(f);
                    break;
                }
        } else {
            for (Fach f : LocalData.getInstance().getFächer())
                if (f != fach && (f.getaStunden()[tag][stunde] || f.getbStunden()[tag][stunde])) {
                    replaceFaecher.add(f);
                    if (replaceFaecher.size() == 2) break;
                }
        }


        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Stunde ersetzen");
        String fachString = replaceFaecher.get(0).getName();
        if (replaceFaecher.size() > 1)
            fachString = fachString + " und " + replaceFaecher.get(1).getName();
        dialogBuilder.setMessage("In dieser Stunde wurde bereits " + fachString + " eingetragen.\nSicher, dass dieses ersetzt werden soll?");

        dialogBuilder.setPositiveButton("Ersetzen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (state == STATE_AWOCHE || state == STATE_IMMER) {
                    replaceFaecher.get(0).getaStunden()[tag][stunde] = false;
                    fach.getaStunden()[tag][stunde] = true;
                }
                if (state == STATE_BWOCHE || state == STATE_IMMER) {
                    replaceFaecher.get(0).getbStunden()[tag][stunde] = false;
                    fach.getbStunden()[tag][stunde] = true;
                }
                if (replaceFaecher.size() > 1) {
                    replaceFaecher.get(1).getaStunden()[tag][stunde] = false;
                    replaceFaecher.get(1).getbStunden()[tag][stunde] = false;
                }
                if (state == STATE_IMMER)
                    ((FachStundenRecyclerAdapter) stundenRecycler.getAdapter()).calculateStateImmer();
                stundenRecycler.getAdapter().notifyItemChanged(pos);
                isReplaceDialog = false;
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isReplaceDialog = false;
            }
        });
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isReplaceDialog = false;
            }
        });
        dialogBuilder.show();

    }
}
