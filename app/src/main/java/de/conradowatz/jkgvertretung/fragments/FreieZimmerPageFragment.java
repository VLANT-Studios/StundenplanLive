package de.conradowatz.jkgvertretung.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.FreieZimmerRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.DataReadyEvent;
import de.conradowatz.jkgvertretung.events.DayUpdatedEvent;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.Tag;

public class FreieZimmerPageFragment extends Fragment {

    private int position;

    private RecyclerView recyclerView;

    private EventBus eventBus = EventBus.getDefault();
    private boolean waitingForData = false;

    public FreieZimmerPageFragment() {

    }

    public static FreieZimmerPageFragment newInstance(int position) {

        Bundle args = new Bundle();
        args.putInt("position", position);

        FreieZimmerPageFragment fragment = new FreieZimmerPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View contentView = inflater.inflate(R.layout.fragment_stundenplan_page, container, false);

        if (savedInstanceState == null) {

            Bundle arguments = getArguments();
            position = arguments.getInt("position");
        } else {

            position = savedInstanceState.getInt("position");
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

        ArrayList<Tag> tagList = VertretungsData.getInstance().getTagList();

        FreieZimmerRecyclerAdapter adapter = new FreieZimmerRecyclerAdapter(tagList.get(position));

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
    public void onEvent(DataReadyEvent event) {

        if (waitingForData) {
            waitingForData = false;

            setUpRecycler();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt("position", position);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        super.onStop();

        eventBus.unregister(this);
    }
}