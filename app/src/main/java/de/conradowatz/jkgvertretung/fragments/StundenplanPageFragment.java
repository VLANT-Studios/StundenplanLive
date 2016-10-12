package de.conradowatz.jkgvertretung.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.EventActivity;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.activities.FerienActivity;
import de.conradowatz.jkgvertretung.activities.MainActivity;
import de.conradowatz.jkgvertretung.adapters.StundenPlanRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.DataReadyEvent;
import de.conradowatz.jkgvertretung.events.DayUpdatedEvent;
import de.conradowatz.jkgvertretung.events.EventsChangedEvent;
import de.conradowatz.jkgvertretung.events.FaecherUpdateEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.Tag;

public class StundenplanPageFragment extends Fragment implements StundenPlanRecyclerAdapter.Callback {

    private int mode;
    private int position;
    private int onlinePosition;
    private Integer klassenPlanIndex;
    private Date date;

    private RecyclerView recyclerView;

    private EventBus eventBus = EventBus.getDefault();
    private boolean waitingForData = false;

    public StundenplanPageFragment() {

    }

    public static StundenplanPageFragment newInstance(int position, int mode, Integer klassenPlanIndex, Date date, int onlinePosition) {

        Bundle args = new Bundle();
        args.putInt("position", position);
        args.putInt("mode", mode);
        args.putInt("onlinePosition", onlinePosition);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        args.putLong("date", calendar.getTimeInMillis());
        if (klassenPlanIndex != null) args.putInt("klassenPlanIndex", klassenPlanIndex);

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
            onlinePosition = arguments.getInt("onlinePosition");
            mode = arguments.getInt("mode");
            klassenPlanIndex = arguments.getInt("klassenPlanIndex");
            calendar.setTimeInMillis(arguments.getLong("date"));
            date = calendar.getTime();
        } else {

            position = savedInstanceState.getInt("position");
            mode = savedInstanceState.getInt("mode");
            onlinePosition = savedInstanceState.getInt("onlinePosition");
            klassenPlanIndex = savedInstanceState.getInt("klassenPlanIndex");
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

        if (!VertretungsData.getInstance().isReady()) {
            waitingForData = true;
            return;
        }

        setUpRecycler();
    }

    private void setUpRecycler() {

        int meinKlassenIndex = PreferenceHelper.readIntFromPreferences(getActivity(), "meineKlasseInt", 0);
        ArrayList<String> nichtKurse = PreferenceHelper.readStringListFromPreferences(getActivity(), "meineNichtKurse");
        if (nichtKurse == null) nichtKurse = new ArrayList<>();
        String meinKlassenString = VertretungsData.getInstance().getKlassenList().get(meinKlassenIndex).getName();
        ArrayList<Tag> tagList = VertretungsData.getInstance().getTagList();

        StundenPlanRecyclerAdapter adapter;
        if (mode == StundenplanFragment.MODE_STUNDENPLAN)
            if (onlinePosition > -1)
                adapter = StundenPlanRecyclerAdapter.newOnlineStundenplanInstance(tagList.get(onlinePosition), meinKlassenIndex, nichtKurse, this);
            else
                adapter = StundenPlanRecyclerAdapter.newOfflineStundenplanInstance(date, this);
        else if (mode == StundenplanFragment.MODE_VERTRETUNGSPLAN)
            adapter = StundenPlanRecyclerAdapter.newVertretungsplanInstance(tagList.get(position), meinKlassenString, nichtKurse);
        else if (mode == StundenplanFragment.MODE_ALGVERTRETUNGSPLAN)
            adapter = StundenPlanRecyclerAdapter.newAllgvertretungsplanInstance(tagList.get(position));
        else
            adapter = StundenPlanRecyclerAdapter.newKlassenplanplanInstance(tagList.get(position), klassenPlanIndex);
        recyclerView.setAdapter(adapter);

    }

    @Subscribe
    public void onEvent(DayUpdatedEvent event) {

        if (recyclerView == null) return;

        if (event.getPosition() == position) {

            setUpRecycler();
        }


    }

    @Subscribe
    public void onEvent(EventsChangedEvent event) {

        if (recyclerView != null) setUpRecycler();


    }

    @Subscribe
    public void onEvent(DataReadyEvent event) {

        if (waitingForData) {
            waitingForData = false;

            setUpRecycler();
        }
    }

    @Subscribe
    public void onEvent(FaecherUpdateEvent event) {

        setUpRecycler();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt("mode", mode);
        outState.putInt("position", position);
        outState.putInt("onlinePosition", onlinePosition);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        outState.putLong("date", calendar.getTimeInMillis());
        if (klassenPlanIndex != null) outState.putInt("klassenPlanIndex", klassenPlanIndex);
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

                int fachInt = LocalData.getInstance().getFächer().indexOf(fach);

                if (i == 0) { //neues Event

                    startEventActivity(fachInt, date);

                } else if (i == 1) { //Fach bearbeiten

                    startFachActivity(fachInt);

                }
            }
        });

        dialogBuilder.setNeutralButton("Abbrechen", null);
        dialogBuilder.show();

    }

    private void startEventActivity(int fachIndex, Date datum) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(datum);
        Intent startEventActivityIntent = new Intent(getActivity(), EventActivity.class);
        startEventActivityIntent.putExtra("fachInt", fachIndex);
        startEventActivityIntent.putExtra("eventInt", -1);
        startEventActivityIntent.putExtra("date", calendar.getTimeInMillis());
        getActivity().startActivity(startEventActivityIntent);
    }

    private void startEventActivity(int fachIndex, int eventInt) {

        Intent startEventActivityIntent = new Intent(getActivity(), EventActivity.class);
        startEventActivityIntent.putExtra("fachInt", fachIndex);
        startEventActivityIntent.putExtra("eventInt", eventInt);
        getActivity().startActivity(startEventActivityIntent);
    }

    private void startFachActivity(int fachIndex) {

        Intent openFachIntent = new Intent(getContext(), FachActivity.class);
        openFachIntent.putExtra("fachIndex", fachIndex);
        openFachIntent.putExtra("tab", FachActivity.TAB_STUNDEN);
        startActivity(openFachIntent);
    }

    @Override
    public void onNewStundeClicked(final String kursName, final boolean aWoche, final int tag, final int stunde) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(kursName);
        dialogBuilder.setMessage("Zu dieser Stunde ist noch kein Fach eingetragen. Möchtest du ein neues Fach erstellen?");
        dialogBuilder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                //neues Fach hinzufügen
                Fach newFach = new Fach(kursName);
                newFach.getStunden(aWoche)[tag][stunde] = true;
                LocalData.getInstance().getFächer().add(newFach);
                LocalData.getInstance().sortFächer();
                LocalData.saveToFile(getActivity().getApplicationContext());
                int fachInt = LocalData.getInstance().getFächer().indexOf(newFach);
                startFachActivity(fachInt);
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
    public void onFerienClicked(int ferienIndex) {

        Intent newFerienActivityIntent = new Intent(getActivity().getApplicationContext(), FerienActivity.class);
        newFerienActivityIntent.putExtra("ferienInt", ferienIndex);
        startActivity(newFerienActivityIntent);

    }

    @Override
    public void onEventClicked(final Fach fach, final Event event, final Date date) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(fach.getName());
        String[] optionen = new String[]{"Event bearbeiten", "Neues Event erstellen", "Fach bearbeiten"};
        dialogBuilder.setItems(optionen, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                int fachInt = LocalData.getInstance().getFächer().indexOf(fach);

                if (i == 0) { //Event bearbeiten

                    int eventInt = fach.getEvents().indexOf(event);
                    startEventActivity(fachInt, eventInt);

                } else if (i == 1) { //neues Event

                    startEventActivity(fachInt, date);

                } else if (i == 2) { //Fach bearbeiten

                    startFachActivity(fachInt);

                }
            }
        });

        dialogBuilder.setNeutralButton("Abbrechen", null);
        dialogBuilder.show();

    }
}
