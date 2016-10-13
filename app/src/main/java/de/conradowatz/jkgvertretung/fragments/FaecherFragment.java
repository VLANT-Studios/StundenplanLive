package de.conradowatz.jkgvertretung.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.activities.KurswahlActivity;
import de.conradowatz.jkgvertretung.adapters.FaecherRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.AnalyticsEventEvent;
import de.conradowatz.jkgvertretung.events.FaecherUpdateEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.Fach;

public class FaecherFragment extends Fragment implements FaecherRecyclerAdapter.Callback {

    private static final int MODE_NOFAECHER = 1;
    private static final int MODE_NORMAL = 2;
    private View contentView;
    private RecyclerView recyclerView;
    private LinearLayout nofaecherLayout;
    private Button smartImportButton;
    private Button kurswahlButton;
    private boolean isDeleteDialog;
    private int deleteDialogIndex;
    private int mode;
    private EventBus eventBus = EventBus.getDefault();

    public FaecherFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_faecher, container, false);
        recyclerView = (RecyclerView) contentView.findViewById(R.id.recyclerView);
        nofaecherLayout = (LinearLayout) contentView.findViewById(R.id.nofaecherLayout);
        smartImportButton = (Button) contentView.findViewById(R.id.smartImportButton);
        kurswahlButton = (Button) contentView.findViewById(R.id.kurswahlButton);

        setHasOptionsMenu(true);

        eventBus.register(this);

        setUp();

        if (savedInstanceState != null) {

            if (savedInstanceState.getBoolean("isDeleteDialog"))
                showDeleteDialog(savedInstanceState.getInt("deleteDialogIndex"));
            mode = savedInstanceState.getInt("mode");
        }

        return contentView;
    }

    private void setUp() {

        if (LocalData.getInstance().getFächer().size() == 0) {

            mode = MODE_NOFAECHER;

            recyclerView.setVisibility(View.GONE);
            nofaecherLayout.setVisibility(View.VISIBLE);
            smartImportButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showSmartImportDialog();
                }
            });
            kurswahlButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startKurswahlActivity();
                }
            });
        } else {

            mode = MODE_NORMAL;

            recyclerView.setVisibility(View.VISIBLE);
            nofaecherLayout.setVisibility(View.GONE);
            setUpRecycler();
        }
    }

    private void setUpRecycler() {

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        FaecherRecyclerAdapter adapter = new FaecherRecyclerAdapter(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

    }

    @Override
    public void onFachClicked(int fachIndex) {

        startFachActivity(fachIndex);

    }

    @Override
    public void onFachLongClicked(int fachIndex) {

        showDeleteDialog(fachIndex);

    }

    private void showDeleteDialog(final int fachIndex) {

        isDeleteDialog = true;
        deleteDialogIndex = fachIndex;

        Fach fach = LocalData.getInstance().getFächer().get(fachIndex);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("'" + fach.getName() + "' löschen");
        builder.setMessage("Bist du sicher dass du dieses Fach löschen möchtest?");
        builder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isDeleteDialog = false;

                LocalData.getInstance().getFächer().remove(fachIndex);
                eventBus.post(new FaecherUpdateEvent(FaecherUpdateEvent.TYPE_REMOVED, fachIndex));
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

        if (recyclerView != null) {
            if (event.getType() == FaecherUpdateEvent.TYPE_CHANGED)
                recyclerView.getAdapter().notifyDataSetChanged();
            else if (event.getType() == FaecherUpdateEvent.TYPE_REMOVED)
                recyclerView.getAdapter().notifyItemRemoved(event.getRecyclerIndex());

            int faecherCount = LocalData.getInstance().getFächer().size();
            if ((mode == MODE_NORMAL && faecherCount == 0) || (mode == MODE_NOFAECHER && faecherCount > 0))
                setUp();
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

            showSmartImportDialog();
        } else if (id == R.id.action_kurswahl) {

            startKurswahlActivity();
        }

        return super.onOptionsItemSelected(item);
    }

    private void startKurswahlActivity() {

        Intent openKurswahlIntent = new Intent(getActivity(), KurswahlActivity.class);
        startActivity(openKurswahlIntent);

    }

    private void showSmartImportDialog() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Smart Import");
        dialogBuilder.setMessage(R.string.smart_import_desc);
        dialogBuilder.setPositiveButton("Importieren", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                eventBus.post(new AnalyticsEventEvent("Manager", "Smart Import"));
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
        outState.putInt("mode", mode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        eventBus.unregister(this);
        super.onDestroyView();
    }
}
