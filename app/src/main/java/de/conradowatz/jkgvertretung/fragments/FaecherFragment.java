package de.conradowatz.jkgvertretung.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.activities.KurswahlActivity;
import de.conradowatz.jkgvertretung.events.FaecherUpdateEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.Fach;

public class FaecherFragment extends Fragment {

    private View contentView;
    private ListView listView;

    private boolean isDeleteDialog;
    private int deleteDialogIndex;

    private EventBus eventBus = EventBus.getDefault();

    public FaecherFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_faecher, container, false);
        listView = (ListView) contentView.findViewById(R.id.listView);

        setHasOptionsMenu(true);

        eventBus.register(this);

        setUpListView();

        if (savedInstanceState != null) {

            if (savedInstanceState.getBoolean("isDeleteDialog"))
                showDeleteDialog(savedInstanceState.getInt("deleteDialogIndex"));
        }

        return contentView;
    }

    private void setUpListView() {

        List<String> fachNamen = new ArrayList<>();
        for (Fach f : LocalData.getInstance().getFächer()) {
            fachNamen.add(f.getName());
        }
        ArrayAdapter adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, fachNamen);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                startFachActivity(i);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                showDeleteDialog(i);
                return true;
            }
        });

    }

    private void showDeleteDialog(final int fachIndex) {

        isDeleteDialog = true;
        deleteDialogIndex = fachIndex;

        Fach fach = LocalData.getInstance().getFächer().get(fachIndex);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("\"" + fach.getName() + "\" löschen");
        builder.setMessage("Bist du sicher dass du dieses Fach löschen möchtest?");
        builder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                LocalData.getInstance().getFächer().remove(fachIndex);
                eventBus.post(new FaecherUpdateEvent());
                LocalData.saveToFile(getActivity().getApplicationContext());
            }
        });
        builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isDeleteDialog = false;
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isDeleteDialog = false;
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.warn_text));
            }
        });
        dialog.show();

    }

    private void startFachActivity(int fachIndex) {

        Intent openFachIntent = new Intent(getContext(), FachActivity.class);
        openFachIntent.putExtra("fachIndex", fachIndex);
        startActivity(openFachIntent);

    }

    @Subscribe
    public void onEvent(FaecherUpdateEvent event) {

        if (listView != null) {
            setUpListView();
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_faecher, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_recognizefaecher) {

            showRecognizeDialog();
        } else if (id == R.id.action_kurswahl) {

            startKurswahlActivity();
        }

        return super.onOptionsItemSelected(item);
    }

    private void startKurswahlActivity() {

        Intent openKurswahlIntent = new Intent(getActivity(), KurswahlActivity.class);
        startActivity(openKurswahlIntent);

    }

    private void showRecognizeDialog() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Smart Import");
        dialogBuilder.setMessage(R.string.smart_import_desc);
        dialogBuilder.setPositiveButton("Importieren", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                LocalData.getInstance().smartImport(getActivity());
                eventBus.post(new FaecherUpdateEvent());
                LocalData.saveToFile(getActivity().getApplicationContext());
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", null);
        dialogBuilder.show();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putBoolean("isDeleteDialog", isDeleteDialog);
        outState.putInt("deleteDialogIndex", deleteDialogIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        eventBus.unregister(this);
        super.onDestroyView();
    }
}
