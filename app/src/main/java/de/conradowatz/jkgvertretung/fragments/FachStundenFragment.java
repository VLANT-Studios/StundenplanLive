package de.conradowatz.jkgvertretung.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.List;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.adapters.FachStundenRecyclerAdapter;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.UnterrichtsZeit;

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

        ArrayAdapter<String> spinnerArrayAdapter = null;
        if (LocalData.hasABWoche())
            spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, new String[]{"Jede Woche", "A-Woche", "B-Woche"});
        else
            spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, new String[]{"Wöchentlich"});
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

    private int getState() {

        return LocalData.hasABWoche()?wochenSpinner.getSelectedItemPosition():1;
    }


    private void setUpRecycler() {

        int state = getState();
        final FachStundenRecyclerAdapter adapter = new FachStundenRecyclerAdapter(state, fach, this);

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 16); //3 pro Stunde, 1 pro Label = 21
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {

            @Override
            public int getSpanSize(int position) {
                if (position == 0) return 1;
                switch (adapter.getItemViewType(position)) {
                    case FachStundenRecyclerAdapter.TYPE_LEFT:
                        return 1;
                    default:
                        return 3;
                }
            }
        });

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

        int state = getState();
        if (state == STATE_AWOCHE || state == STATE_IMMER) new UnterrichtsZeit(tag, stunde, true, fach).save();
        if (state == STATE_BWOCHE || state == STATE_IMMER) new UnterrichtsZeit(tag, stunde, false, fach).save();
        ((FachStundenRecyclerAdapter)stundenRecycler.getAdapter()).updateData(pos);
    }

    @Override
    public void onRemoveStunde(int tag, int stunde, int pos) {

        int state = getState();
        if (state == STATE_AWOCHE || state == STATE_IMMER) {
            UnterrichtsZeit u = fach.getUnterrichtsZeit(tag, stunde, true);
            if (u!=null) u.delete();
        }
        if (state == STATE_BWOCHE || state == STATE_IMMER) {
            UnterrichtsZeit u = fach.getUnterrichtsZeit(tag, stunde, false);
            if (u!=null) u.delete();
        }
        ((FachStundenRecyclerAdapter)stundenRecycler.getAdapter()).updateData(pos);

    }

    @Override
    public void onReplaceStunde(final int tag, final int stunde, final int pos) {

        isReplaceDialog = true;
        replaceTag = tag;
        replaceStunde = stunde;
        replacePos = pos;

        final int state = getState();
        List<UnterrichtsZeit> replaceUnterricht = UnterrichtsZeit.getBelegtenUnterricht(fach.getId(), tag, stunde, state);

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Stunde ersetzen");
        String fachString = replaceUnterricht.get(0).getFach().getName();
        if (replaceUnterricht.size() > 1)
            fachString = fachString + " und " + replaceUnterricht.get(1).getFach().getName();
        dialogBuilder.setMessage("In dieser Stunde wurde bereits " + fachString + " eingetragen.\nSicher, dass dieses ersetzt werden soll?");

        dialogBuilder.setPositiveButton("Ersetzen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isReplaceDialog = false;

                UnterrichtsZeit.deleteBelegtenUnterricht(fach.getId(), tag, stunde, state);
                if (state == STATE_AWOCHE || state == STATE_IMMER) new UnterrichtsZeit(tag, stunde, true, fach).save();
                if (state == STATE_BWOCHE || state == STATE_IMMER) new UnterrichtsZeit(tag, stunde, false, fach).save();
                ((FachStundenRecyclerAdapter)stundenRecycler.getAdapter()).updateData(pos);

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
