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
    private Integer klassenPlanIndex;

    private RecyclerView recyclerView;

    private EventBus eventBus = EventBus.getDefault();

    public StundenplanPageFragment() {

    }

    public static StundenplanPageFragment newInstance(int position, int mode, Integer klassenPlanIndex) {

        Bundle args = new Bundle();
        args.putInt("position", position);
        args.putInt("mode", mode);
        if (klassenPlanIndex != null) args.putInt("klassenPlanIndex", klassenPlanIndex);

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
            klassenPlanIndex = arguments.getInt("klassenPlanIndex");
        } else {

            position = savedInstanceState.getInt("position");
            mode = savedInstanceState.getInt("mode");
            klassenPlanIndex = savedInstanceState.getInt("klassenPlanIndex");
        }

        recyclerView = (RecyclerView) contentView.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        setUpRecycler();

        return contentView;
    }

    private void setUpRecycler() {

        int meinKlassenIndex = PreferenceReader.readIntFromPreferences(getActivity(), "meineKlasseInt", 0);
        ArrayList<String> nichtKurse = PreferenceReader.readStringListFromPreferences(getActivity(), "meineNichtKurse");
        String meinKlassenString = VertretungsData.getsInstance().getKlassenList().get(meinKlassenIndex).getName();
        ArrayList<Tag> tagList = VertretungsData.getsInstance().getTagList();

        StundenPlanRecyclerAdapter adapter;
        if (mode == StundenplanFragment.MODE_STUNDENPLAN)
            adapter = StundenPlanRecyclerAdapter.newStundenplanInstance(tagList.get(position), meinKlassenIndex, nichtKurse);
        else if (mode == StundenplanFragment.MODE_VERTRETUNGSPLAN)
            adapter = StundenPlanRecyclerAdapter.newVertretungsplanInstance(tagList.get(position), meinKlassenString, nichtKurse);
        else if (mode == StundenplanFragment.MODE_ALGVERTRETUNGSPLAN)
            adapter = StundenPlanRecyclerAdapter.newAllgvertretungsplanInstance(tagList.get(position));
        else
            adapter = StundenPlanRecyclerAdapter.newKlassenplanplanInstance(tagList.get(position), klassenPlanIndex);
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
        if (klassenPlanIndex != null) outState.putInt("klassenPlanIndex", klassenPlanIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        eventBus.unregister(this);
        super.onPause();
    }
}
