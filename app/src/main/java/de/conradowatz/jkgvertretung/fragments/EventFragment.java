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

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.EventActivity;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.adapters.EventRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.AnalyticsScreenHitEvent;
import de.conradowatz.jkgvertretung.events.EventsChangedEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Fach;

public class EventFragment extends Fragment implements EventRecyclerAdapter.Callback {

    public static final int MODE_FACH = 0;
    public static final int MODE_ALLGEMEIN = 1;
    private View contentView;
    private RecyclerView eventRecycler;
    private FloatingActionButton fab;
    private Fach fach = null;
    private boolean isDeleteDialog;
    private boolean isNewEventDialog;
    private int deleteEventIndex;
    private int deleteFachIndex;
    private int deleteRecyclerIndex;
    private EventBus eventBus = EventBus.getDefault();
    private int mode;

    public EventFragment() {
    }

    public static EventFragment newInstance(int mode) {

        Bundle arguments = new Bundle();
        arguments.putInt("mode", mode);
        EventFragment eventFragment = new EventFragment();
        eventFragment.setArguments(arguments);
        return eventFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_fach_events, container, false);
        eventRecycler = (RecyclerView) contentView.findViewById(R.id.eventRecycler);
        fab = (FloatingActionButton) contentView.findViewById(R.id.fab);

        if (savedInstanceState == null) {
            mode = getArguments().getInt("mode");
            if (mode == MODE_ALLGEMEIN) eventBus.post(new AnalyticsScreenHitEvent("Termine"));
        } else {
            mode = savedInstanceState.getInt("mode");
            if (savedInstanceState.getBoolean("isDeleteDialog"))
                showDeleteDialog(savedInstanceState.getInt("deleteFachIndex"), savedInstanceState.getInt("deleteEventIndex"), savedInstanceState.getInt("deleteRecyclerIndex"));
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

        setUpRecycler();

        return contentView;
    }

    private void newEvent() {

        isNewEventDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Event erstellen");
        //builder.setMessage("In welchem Fach?");
        String[] faecher = new String[LocalData.getInstance().getFächer().size() + 1];
        faecher[0] = "Ohne Fach";
        for (int i = 0; i < LocalData.getInstance().getFächer().size(); i++) {
            Fach f = LocalData.getInstance().getFächer().get(i);
            faecher[i + 1] = f.getName();
        }
        builder.setItems(faecher, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int pos) {
                isNewEventDialog = false;
                startEventActivity(pos - 1, -1);
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

    private void setUpRecycler() {

        RecyclerView.LayoutManager lManager = new LinearLayoutManager(getActivity());
        EventRecyclerAdapter adapter = new EventRecyclerAdapter(fach, this);
        eventRecycler.setLayoutManager(lManager);
        eventRecycler.setAdapter(adapter);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt("mode", mode);
        outState.putBoolean("isDeleteDialog", isDeleteDialog);
        outState.putBoolean("isNewEventDialog", isNewEventDialog);
        outState.putInt("deleteEventIndex", deleteEventIndex);
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

        showDeleteDialog(fachIndex, eventIndex, recyclerIndex);
    }

    @Subscribe
    public void onEvent(EventsChangedEvent event) {

        ((EventRecyclerAdapter) eventRecycler.getAdapter()).notifyEventsChanged(event);
    }

    private void showDeleteDialog(final int fachIndex, final int eventIndex, final int recyclerIndex) {

        isDeleteDialog = true;
        deleteEventIndex = eventIndex;
        deleteFachIndex = fachIndex;
        deleteRecyclerIndex = recyclerIndex;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Löschen bestätigen");
        dialogBuilder.setMessage("Bist du sicher, dass du dieses Event löschen willst?");
        dialogBuilder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isDeleteDialog = false;

                boolean removeAbove = ((EventRecyclerAdapter) eventRecycler.getAdapter()).isOnlyEventOfThatDay(recyclerIndex);

                if (fachIndex > -1) {
                    Event event = LocalData.getInstance().getFächer().get(fachIndex).getEvents().get(eventIndex);
                    LocalData.removeEventReminder(getActivity().getApplicationContext(), event, fachIndex, eventIndex);
                    LocalData.getInstance().getFächer().get(fachIndex).getEvents().remove(eventIndex);
                } else {
                    Event event = LocalData.getInstance().getNoFachEvents().get(eventIndex);
                    LocalData.removeEventReminder(getActivity().getApplicationContext(), event, fachIndex, eventIndex);
                    LocalData.getInstance().getNoFachEvents().remove(eventIndex);
                }

                //decide whether the item above should also be removed
                EventsChangedEvent eventsChangedEvent = new EventsChangedEvent(EventsChangedEvent.TYPE_REMOVED, recyclerIndex);
                eventsChangedEvent.setRemoveAbove(removeAbove);
                eventBus.post(eventsChangedEvent);

                LocalData.saveToFile(getActivity().getApplicationContext());
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isDeleteDialog = false;
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isDeleteDialog = false;
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
        eventBus.unregister(this);
        super.onDestroyView();
    }
}
