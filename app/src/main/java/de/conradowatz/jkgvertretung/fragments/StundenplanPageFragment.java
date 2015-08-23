package de.conradowatz.jkgvertretung.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.StundenPlanRecyclerAdapter;
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.DayUpdatedEvent;
import de.conradowatz.jkgvertretung.variables.Tag;
import de.greenrobot.event.EventBus;

public class StundenplanPageFragment extends Fragment {

    private int mode;
    private int position;

    private RecyclerView recyclerView;

    private EventBus eventBus = EventBus.getDefault();

    public StundenplanPageFragment() {

    }

    public static StundenplanPageFragment newInstance(int position, int mode) {

        Bundle args = new Bundle();
        args.putInt("position", position);
        args.putInt("mode", mode);

        StundenplanPageFragment fragment = new StundenplanPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View contentView = inflater.inflate(R.layout.stundenplan_page, container, false);

        eventBus.register(this);

        if (savedInstanceState == null) {

            Bundle arguments = getArguments();
            position = arguments.getInt("position");
            mode = arguments.getInt("mode");
        } else {

            position = savedInstanceState.getInt("position");
            mode = savedInstanceState.getInt("mode");
        }

        recyclerView = (RecyclerView) contentView.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        setUpRecycler();

        return contentView;
    }

    private void setUpRecycler() {

        int klassenIndex = PreferenceReader.readIntFromPreferences(getActivity(), "meineKlasseInt", 0);
        ArrayList<String> nichtKurse = PreferenceReader.readStringListFromPreferences(getActivity(), "meineNichtKurse");
        String klassenString = VertretungsData.getsInstance().getKlassenList().get(klassenIndex).getName();
        ArrayList<Tag> tagList = VertretungsData.getsInstance().getTagList();

        StundenPlanRecyclerAdapter adapter;
        if (mode == StundenplanFragment.MODE_STUNDENPLAN)
            adapter = StundenPlanRecyclerAdapter.newStundenplanInstance(tagList.get(position), klassenIndex, nichtKurse);
        else if (mode == StundenplanFragment.MODE_VERTRETUNGSPLAN)
            adapter = StundenPlanRecyclerAdapter.newVertretungsplanInstance(tagList.get(position), klassenString, nichtKurse);
        else
            adapter = StundenPlanRecyclerAdapter.newAllgvertretungsplanInstance(tagList.get(position));
        recyclerView.setAdapter(adapter);

    }

    public void onEvent(DayUpdatedEvent event) {

        if (recyclerView == null) return;

        if (event.getPosition() == position) {

            setUpRecycler();
        }


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt("mode", mode);
        outState.putInt("position", position);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        eventBus.unregister(this);
        super.onPause();
    }
}
