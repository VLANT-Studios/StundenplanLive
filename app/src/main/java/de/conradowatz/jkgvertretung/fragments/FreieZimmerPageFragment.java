package de.conradowatz.jkgvertretung.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.FreieZimmerRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.DaysUpdatedEvent;
import de.conradowatz.jkgvertretung.variables.OnlineTag;
import de.conradowatz.jkgvertretung.variables.OnlineTag_Table;

public class FreieZimmerPageFragment extends Fragment {

    private int position;

    private RecyclerView recyclerView;

    private EventBus eventBus = EventBus.getDefault();

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

        setUpRecycler();
    }

    private void setUpRecycler() {

        new AsyncTask<Boolean, Integer, List<OnlineTag>>() {
            @Override
            protected List<OnlineTag> doInBackground(Boolean... params) {
                return SQLite.select().from(OnlineTag.class).orderBy(OnlineTag_Table.date, true).queryList();
            }

            @Override
            protected void onPostExecute(List<OnlineTag> onlineTagList) {

                FreieZimmerRecyclerAdapter adapter = new FreieZimmerRecyclerAdapter(onlineTagList.get(position));
                if (recyclerView!=null) recyclerView.setAdapter(adapter);

            }
        }.execute();

    }

    @Subscribe
    public void onEvent(DaysUpdatedEvent event) {

        if (recyclerView == null) return;

        setUpRecycler();


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