package de.conradowatz.jkgvertretung.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;
import java.util.Date;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.EventActivity;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.activities.FerienActivity;
import de.conradowatz.jkgvertretung.activities.MainActivity;
import de.conradowatz.jkgvertretung.adapters.AllVertretungsplanRecyclerAdapter;
import de.conradowatz.jkgvertretung.adapters.KlassenplanRecyclerAdapter;
import de.conradowatz.jkgvertretung.adapters.StundenPlanRecyclerAdapter;
import de.conradowatz.jkgvertretung.adapters.VertretungsplanRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.DaysUpdatedEvent;
import de.conradowatz.jkgvertretung.events.EventsChangedEvent;
import de.conradowatz.jkgvertretung.events.FaecherUpdateEvent;
import de.conradowatz.jkgvertretung.events.KursChangedEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.OnlineTag_Table;

public class StundenplanPageFragment extends Fragment implements StundenPlanRecyclerAdapter.Callback {

    private int mode;
    private int position;
    private String klassenName;
    private Date date;

    private RecyclerView recyclerView;

    private EventBus eventBus = EventBus.getDefault();

    public StundenplanPageFragment() {

    }

    public static StundenplanPageFragment newInstance(int position, int mode, String klassenName, Date date) {

        Bundle args = new Bundle();
        args.putInt("position", position);
        args.putInt("mode", mode);
        Calendar calendar = Utilities.getToday();
        calendar.setTime(date);

        args.putLong("date", calendar.getTimeInMillis());
        if (klassenName != null) args.putString("klassenName", klassenName);

        StundenplanPageFragment fragment = new StundenplanPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View contentView = inflater.inflate(R.layout.fragment_stundenplan_page, container, false);

        Calendar calendar = Calendar.getInstance();

        if (savedInstanceState == null) {

            Bundle arguments = getArguments();
            position = arguments.getInt("position");
            mode = arguments.getInt("mode");
            klassenName = arguments.getString("klassenName");
            calendar.setTimeInMillis(arguments.getLong("date"));
            date = calendar.getTime();
        } else {

            position = savedInstanceState.getInt("position");
            mode = savedInstanceState.getInt("mode");
            klassenName = savedInstanceState.getString("klassenName");
            calendar.setTimeInMillis(savedInstanceState.getLong("date"));
            date = calendar.getTime();
        }

        recyclerView = (RecyclerView) contentView.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        eventBus.register(this);

        return contentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setUpRecycler();
    }

    private void setUpRecycler() {

        switch (mode) {
            case StundenplanFragment.MODE_STUNDENPLAN:
                recyclerView.setAdapter(new StundenPlanRecyclerAdapter(date, this));
                break;
            case StundenplanFragment.MODE_VERTRETUNGSPLAN:
                recyclerView.setAdapter(new VertretungsplanRecyclerAdapter(position));
                break;
            case StundenplanFragment.MODE_ALGVERTRETUNGSPLAN:
                recyclerView.setAdapter(new AllVertretungsplanRecyclerAdapter(position));
                break;
            case StundenplanFragment.MODE_KLASSENPLAN:
                recyclerView.setAdapter(new KlassenplanRecyclerAdapter(position, klassenName));
                break;
        }

    }

    @Subscribe
    public void onEvent(DaysUpdatedEvent event) {

        if (recyclerView == null || recyclerView.getAdapter() == null) return;

        switch (mode) {
            case StundenplanFragment.MODE_STUNDENPLAN:
                ((StundenPlanRecyclerAdapter) recyclerView.getAdapter()).updateData();
                break;
            case StundenplanFragment.MODE_VERTRETUNGSPLAN:
                ((VertretungsplanRecyclerAdapter) recyclerView.getAdapter()).updateData();
                break;
            case StundenplanFragment.MODE_ALGVERTRETUNGSPLAN:
                ((AllVertretungsplanRecyclerAdapter) recyclerView.getAdapter()).updateData();
                break;
            case StundenplanFragment.MODE_KLASSENPLAN:
                ((KlassenplanRecyclerAdapter) recyclerView.getAdapter()).updateData();
                break;
        }


    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventsChangedEvent event) {

        if (recyclerView != null && recyclerView.getAdapter() != null && mode == StundenplanFragment.MODE_STUNDENPLAN)
            ((StundenPlanRecyclerAdapter) recyclerView.getAdapter()).updateData();


    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(FaecherUpdateEvent event) {

        if (recyclerView != null && recyclerView.getAdapter() != null && mode == StundenplanFragment.MODE_STUNDENPLAN)
            ((StundenPlanRecyclerAdapter) recyclerView.getAdapter()).updateData();
    }

    @Subscribe
    public void onEvent(KursChangedEvent event) {

        if (recyclerView == null || recyclerView.getAdapter() == null) return;

        switch (mode) {
            case StundenplanFragment.MODE_STUNDENPLAN:
                ((StundenPlanRecyclerAdapter) recyclerView.getAdapter()).updateData();
                break;
            case StundenplanFragment.MODE_VERTRETUNGSPLAN:
                ((VertretungsplanRecyclerAdapter) recyclerView.getAdapter()).updateData();
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt("mode", mode);
        outState.putInt("position", position);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        outState.putLong("date", calendar.getTimeInMillis());
        if (klassenName != null) outState.putString("klassenName", klassenName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        eventBus.unregister(this);
    }

    @Override
    public void onFachClicked(final Fach fach, final Date date) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(fach.getName());
        String[] optionen = new String[]{"Neues Event erstellen", "Fach bearbeiten"};
        dialogBuilder.setItems(optionen, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if (i == 0) { //neues Event

                    startEventActivity(fach.getId(), date);

                } else if (i == 1) { //Fach bearbeiten

                    startFachActivity(fach.getId());

                }
            }
        });

        dialogBuilder.setNeutralButton("Abbrechen", null);
        dialogBuilder.show();

    }

    private void startEventActivity(long fachId, Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Intent startEventActivityIntent = new Intent(getActivity(), EventActivity.class);
        startEventActivityIntent.putExtra("fachId", fachId);
        startEventActivityIntent.putExtra("eventId", -1);
        startEventActivityIntent.putExtra("date", calendar.getTimeInMillis());
        getActivity().startActivity(startEventActivityIntent);
    }

    private void startEventActivity(long eventId) {

        Intent startEventActivityIntent = new Intent(getActivity(), EventActivity.class);
        startEventActivityIntent.putExtra("eventId", eventId);
        getActivity().startActivity(startEventActivityIntent);
    }

    private void startFachActivity(long fachId) {

        Intent openFachIntent = new Intent(getContext(), FachActivity.class);
        openFachIntent.putExtra("fachId", fachId);
        openFachIntent.putExtra("tab", FachActivity.TAB_STUNDEN);
        startActivity(openFachIntent);
    }

    @Override
    public void onNewStundeClicked(final String fachName) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(fachName);
        dialogBuilder.setMessage("Mit dieser Stunde ist kein Fach verknüpft. Möchtest du den Smart Import benutzen um fehlende Fächer zu importieren?");
        dialogBuilder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                LocalData.smartImport(getActivity());
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", null);
        dialogBuilder.show();

    }

    @Override
    public void onManagerClicked() {

        ((MainActivity) getActivity()).openManager();

    }

    @Override
    public void onFerienClicked(long ferienId) {

        Intent newFerienActivityIntent = new Intent(getActivity().getApplicationContext(), FerienActivity.class);
        newFerienActivityIntent.putExtra("ferienId", ferienId);
        startActivity(newFerienActivityIntent);

    }

    @Override
    public void onEventClicked(final Event event, final Date date) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        final Fach fach = event.getFach();
        if (fach==null) dialogBuilder.setTitle(event.getName());
        else dialogBuilder.setTitle(fach.getName());
        String[] optionen = fach!=null?new String[]{"Event bearbeiten", "Neues Event erstellen", "Fach bearbeiten"}:new String[]{"Event bearbeiten"};
        dialogBuilder.setItems(optionen, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if (i == 0) { //Event bearbeiten

                    startEventActivity(event.getId());

                } else if (i == 1) { //neues Event

                    startEventActivity(fach.getId(), date);

                } else if (i == 2) { //Fach bearbeiten

                    startFachActivity(fach.getId());

                }
            }
        });

        dialogBuilder.setNeutralButton("Abbrechen", null);
        dialogBuilder.show();

    }
}
