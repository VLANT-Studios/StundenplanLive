package de.conradowatz.jkgvertretung.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.EventActivity;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.activities.FerienActivity;
import de.conradowatz.jkgvertretung.adapters.TerminRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.EventsChangedEvent;
import de.conradowatz.jkgvertretung.events.FerienChangedEvent;
import de.conradowatz.jkgvertretung.events.TermineChangedEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.Ferien;

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
    private long deleteEventId;
    private long deleteFerienId;
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
        } else {
            mode = savedInstanceState.getInt("mode");
            if (savedInstanceState.getBoolean("isDeleteEventDialog"))
                showDeleteEventDialog(savedInstanceState.getLong("deleteEventId"), savedInstanceState.getInt("deleteRecyclerIndex"));
            if (savedInstanceState.getBoolean("isDeleteFerienDialog"))
                showDeleteFerienDialog(savedInstanceState.getLong("deleteFerienId"), savedInstanceState.getInt("deleteRecyclerIndex"));
            if (savedInstanceState.getBoolean("isNewEventDialog")) newEvent();
        }

        if (mode == MODE_FACH) fach = ((FachActivity) getActivity()).getFach();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mode == MODE_FACH)
                    startNewEventActivity(fach.getId());
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
        final List<Fach> faecher = Fach.getSortedFaecherList();
        int anzFaecher = faecher.size();
        String[] entries = new String[anzFaecher + 2];
        entries[0] = "Ferien hinzufügen";
        entries[1] = "Event ohne Fach";
        for (int i = 0; i < anzFaecher; i++) {
            entries[i + 2] = String.format(Locale.GERMANY, "%s Event", faecher.get(i).getName());
        }
        builder.setItems(entries, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int pos) {

                isNewEventDialog = false;

                if (pos == 0) showFerienActivity(-1);
                else if (pos == 1) startNewEventActivity(-1);
                else startNewEventActivity(faecher.get(pos - 2).getId());
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

    private void startEventActivity(long eventId) {

        Intent startEventActivityIntent = new Intent(getActivity(), EventActivity.class);
        startEventActivityIntent.putExtra("eventId", eventId);
        getActivity().startActivity(startEventActivityIntent);
    }

    private void startNewEventActivity(long fachId) {

        Intent startEventActivityIntent = new Intent(getActivity(), EventActivity.class);
        startEventActivityIntent.putExtra("fachId", fachId);
        startEventActivityIntent.putExtra("eventId", (long) -1);
        getActivity().startActivity(startEventActivityIntent);
    }

    private void showFerienActivity(long ferienId) {

        Intent newFerienActivityIntent = new Intent(getActivity().getApplicationContext(), FerienActivity.class);
        newFerienActivityIntent.putExtra("ferienId", ferienId);
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
        outState.putLong("deleteEventId", deleteEventId);
        outState.putLong("deleteFerienId", deleteFerienId);
        outState.putInt("deleteRecyclerIndex", deleteRecyclerIndex);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onEditEvent(long eventId) {

        startEventActivity(eventId);
    }

    @Override
    public void onDeleteEvent(long eventId, int recyclerIndex) {

        showDeleteEventDialog(eventId, recyclerIndex);
    }

    @Override
    public void onEditFerien(long ferienId) {

        showFerienActivity(ferienId);
    }

    @Override
    public void onDeleteFerien(long ferienId, int recyclerIndex) {

        showDeleteFerienDialog(ferienId, recyclerIndex);
    }

    @Subscribe
    public void onEvent(TermineChangedEvent event) {

        ((TerminRecyclerAdapter) terminRecycler.getAdapter()).updateData();
    }

    @Subscribe
    public void onEvent(FerienChangedEvent event) {

        if (mode==MODE_ALLGEMEIN || mode==MODE_FERIEN)
            ((TerminRecyclerAdapter) terminRecycler.getAdapter()).updateData();
    }

    private void showDeleteEventDialog(final long eventId, final int recyclerIndex) {

        isDeleteEventDialog = true;
        deleteEventId = eventId;
        deleteRecyclerIndex = recyclerIndex;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Löschen bestätigen");
        dialogBuilder.setMessage("Bist du sicher, dass du dieses Event löschen willst?");
        dialogBuilder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isDeleteEventDialog = false;

                Event event = Event.getEvent(eventId);
                LocalData.removeEventReminder(MyApplication.getAppContext(), event);
                event.delete();
                eventBus.post(new EventsChangedEvent());
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

    private void showDeleteFerienDialog(long ferienId, final int recyclerIndex) {

        isDeleteFerienDialog = true;
        deleteFerienId = ferienId;
        deleteRecyclerIndex = recyclerIndex;
        final Ferien f = Ferien.getFerien(ferienId);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Löschen bestätigen");
        dialogBuilder.setMessage("Bist du sicher, dass du '" + f.getName() + "' löschen willst?");
        dialogBuilder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isDeleteFerienDialog = false;

                f.delete();
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
