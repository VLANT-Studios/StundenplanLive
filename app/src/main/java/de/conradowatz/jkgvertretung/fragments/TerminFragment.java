package de.conradowatz.jkgvertretung.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.EventActivity;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.activities.FerienActivity;
import de.conradowatz.jkgvertretung.adapters.TerminRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.AnalyticsScreenHitEvent;
import de.conradowatz.jkgvertretung.events.EventsChangedEvent;
import de.conradowatz.jkgvertretung.events.FerienChangedEvent;
import de.conradowatz.jkgvertretung.events.TermineChangedEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Fach;

public class TerminFragment extends Fragment implements TerminRecyclerAdapter.Callback {

    public static final int MODE_FACH = 0;
    public static final int MODE_ALLGEMEIN = 1;
    public static final int MODE_FERIEN = 2;
    private View contentView;
    private RecyclerView terminRecycler;
    private FloatingActionButton fab;
    private Fach fach = null;
    private boolean isDeleteEventDialog;
    private boolean isDeleteFerienDialog;
    private boolean isNewEventDialog;
    private int deleteEventIndex;
    private int deleteFerienIndex;
    private int deleteFachIndex;
    private int deleteRecyclerIndex;
    private EventBus eventBus = EventBus.getDefault();
    private int mode;

    public TerminFragment() {
    }

    public static TerminFragment newInstance(int mode) {

        Bundle arguments = new Bundle();
        arguments.putInt("mode", mode);
        TerminFragment terminFragment = new TerminFragment();
        terminFragment.setArguments(arguments);
        return terminFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_fach_events, container, false);
        terminRecycler = (RecyclerView) contentView.findViewById(R.id.eventRecycler);
        fab = (FloatingActionButton) contentView.findViewById(R.id.fab);

        if (savedInstanceState == null) {
            mode = getArguments().getInt("mode");
            if (mode == MODE_ALLGEMEIN) eventBus.post(new AnalyticsScreenHitEvent("Termine"));
        } else {
            mode = savedInstanceState.getInt("mode");
            if (savedInstanceState.getBoolean("isDeleteEventDialog"))
                showDeleteEventDialog(savedInstanceState.getInt("deleteFachIndex"), savedInstanceState.getInt("deleteEventIndex"), savedInstanceState.getInt("deleteRecyclerIndex"));
            if (savedInstanceState.getBoolean("isDeleteFerienDialog"))
                showDeleteFerienDialog(savedInstanceState.getInt("deleteFerienIndex"), savedInstanceState.getInt("deleteRecyclerIndex"));
            if (savedInstanceState.getBoolean("isNewEventDialog")) newEvent();
        }

        if (mode == MODE_FACH) fach = ((FachActivity) getActivity()).getFach();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mode == MODE_FACH)
                    startEventActivity(mode == MODE_FACH ? LocalData.getInstance().getFächer().indexOf(fach) : -1, -1);
                else
                    newEvent();
            }
        });

        eventBus.register(this);

        setUp();

        return contentView;
    }

    private void setUp() {

        if (mode == MODE_FERIEN) fab.setVisibility(View.GONE);
        else fab.setVisibility(View.VISIBLE);
        setUpRecycler();
    }

    private void newEvent() {

        isNewEventDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Termin erstellen");
        String[] faecher = new String[LocalData.getInstance().getFächer().size() + 2];
        faecher[0] = "Ferien hinzufügen";
        faecher[1] = "Event ohne Fach";
        for (int i = 0; i < LocalData.getInstance().getFächer().size(); i++) {
            Fach f = LocalData.getInstance().getFächer().get(i);
            faecher[i + 2] = String.format(Locale.GERMANY, "%s Event", f.getName());
        }
        builder.setItems(faecher, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int pos) {

                isNewEventDialog = false;

                if (pos == 0) showFerienActivity(-1);
                else startEventActivity(pos - 2, -1);
            }
        });
        builder.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isNewEventDialog = false;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isNewEventDialog = false;
            }
        });
        dialog.show();
    }

    /**
     * @param eventIndex index des zu bearbeitenden Events; -1 wenn neues
     */
    private void startEventActivity(int fachIndex, int eventIndex) {

        Intent startEventActivityIntent = new Intent(getActivity(), EventActivity.class);
        startEventActivityIntent.putExtra("fachInt", fachIndex);
        startEventActivityIntent.putExtra("eventInt", eventIndex);
        getActivity().startActivity(startEventActivityIntent);
    }

    private void showFerienActivity(int ferienInt) {

        Intent newFerienActivityIntent = new Intent(getActivity().getApplicationContext(), FerienActivity.class);
        newFerienActivityIntent.putExtra("ferienInt", ferienInt);
        startActivity(newFerienActivityIntent);
    }

    private void setUpRecycler() {

        RecyclerView.LayoutManager lManager = new LinearLayoutManager(getActivity());
        TerminRecyclerAdapter adapter;
        switch (mode) {
            case MODE_ALLGEMEIN:
                adapter = new TerminRecyclerAdapter(TerminRecyclerAdapter.MODE_TERMINE, this);
                break;
            case MODE_FACH:
                adapter = new TerminRecyclerAdapter(TerminRecyclerAdapter.MODE_EVENTS, fach, this);
                break;
            default:
                adapter = new TerminRecyclerAdapter(TerminRecyclerAdapter.MODE_FERIEN, this);
        }
        terminRecycler.setLayoutManager(lManager);
        terminRecycler.setAdapter(adapter);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt("mode", mode);
        outState.putBoolean("isDeleteEventDialog", isDeleteEventDialog);
        outState.putBoolean("isDeleteFerienDialog", isDeleteFerienDialog);
        outState.putBoolean("isNewEventDialog", isNewEventDialog);
        outState.putInt("deleteEventIndex", deleteEventIndex);
        outState.putInt("deleteFerienIndex", deleteFerienIndex);
        outState.putInt("deleteFachIndex", deleteFachIndex);
        outState.putInt("deleteRecyclerIndex", deleteRecyclerIndex);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onEditEvent(int fachIndex, int eventIndex) {

        startEventActivity(fachIndex, eventIndex);
    }

    @Override
    public void onDeleteEvent(int fachIndex, int eventIndex, int recyclerIndex) {

        showDeleteEventDialog(fachIndex, eventIndex, recyclerIndex);
    }

    @Override
    public void onEditFerien(int ferienIndex) {

        showFerienActivity(ferienIndex);
    }

    @Override
    public void onDeleteFerien(int ferienIndex, int recyclerIndex) {

        showDeleteFerienDialog(ferienIndex, recyclerIndex);
    }

    @Subscribe
    public void onEvent(TermineChangedEvent event) {

        ((TerminRecyclerAdapter) terminRecycler.getAdapter()).notifyTermineChanged();
    }

    private void showDeleteEventDialog(final int fachIndex, final int eventIndex, final int recyclerIndex) {

        isDeleteEventDialog = true;
        deleteEventIndex = eventIndex;
        deleteFachIndex = fachIndex;
        deleteRecyclerIndex = recyclerIndex;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Löschen bestätigen");
        dialogBuilder.setMessage("Bist du sicher, dass du dieses Event löschen willst?");
        dialogBuilder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isDeleteEventDialog = false;

                if (fachIndex > -1) {
                    Event event = LocalData.getInstance().getFächer().get(fachIndex).getEvents().get(eventIndex);
                    LocalData.removeEventReminder(getActivity().getApplicationContext(), event, fachIndex, eventIndex);
                    LocalData.getInstance().getFächer().get(fachIndex).getEvents().remove(eventIndex);
                } else {
                    Event event = LocalData.getInstance().getNoFachEvents().get(eventIndex);
                    LocalData.removeEventReminder(getActivity().getApplicationContext(), event, fachIndex, eventIndex);
                    LocalData.getInstance().getNoFachEvents().remove(eventIndex);
                }

                eventBus.post(new EventsChangedEvent());

                LocalData.saveToFile(getActivity().getApplicationContext());
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isDeleteEventDialog = false;
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isDeleteEventDialog = false;
            }
        });
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.warn_text));
            }
        });
        dialog.show();
    }

    private void showDeleteFerienDialog(final int ferienIndex, final int recyclerIndex) {

        isDeleteFerienDialog = true;
        deleteFerienIndex = ferienIndex;
        deleteRecyclerIndex = recyclerIndex;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Löschen bestätigen");
        dialogBuilder.setMessage("Bist du sicher, dass du '" + LocalData.getInstance().getFerien().get(ferienIndex).getName() + "' löschen willst?");
        dialogBuilder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isDeleteFerienDialog = false;

                LocalData.getInstance().getFerien().remove(ferienIndex);
                LocalData.saveToFile(getActivity().getApplicationContext());

                eventBus.post(new FerienChangedEvent());
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isDeleteFerienDialog = false;
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isDeleteFerienDialog = false;
            }
        });
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.warn_text));
            }
        });
        dialog.show();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        eventBus.unregister(this);
    }
}
