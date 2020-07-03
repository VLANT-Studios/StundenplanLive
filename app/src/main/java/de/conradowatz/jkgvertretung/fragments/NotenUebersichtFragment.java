package de.conradowatz.jkgvertretung.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.DecimalFormat;
import java.util.List;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.activities.ManagerActivity;
import de.conradowatz.jkgvertretung.adapters.NotenUebersichtRecyclerAdpater;
import de.conradowatz.jkgvertretung.events.FaecherUpdateEvent;
import de.conradowatz.jkgvertretung.events.KursChangedEvent;
import de.conradowatz.jkgvertretung.events.NotenChangedEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.variables.Fach;

public class NotenUebersichtFragment extends Fragment implements NotenUebersichtRecyclerAdpater.Callback {

    private View contentView;
    private CheckBox leistungskurseCheckbox;
    private RecyclerView recyclerView;
    private TextView durchschnittText;
    private FloatingActionButton fab;

    private EventBus eventBus = EventBus.getDefault();

    public NotenUebersichtFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_notenuebersicht, container, false);

        leistungskurseCheckbox = (CheckBox) contentView.findViewById(R.id.leistungskurseCheckbox);
        recyclerView = (RecyclerView) contentView.findViewById(R.id.recyclerView);
        durchschnittText = (TextView) contentView.findViewById(R.id.durchschnittText);
        fab = (FloatingActionButton) contentView.findViewById(R.id.fab);

        eventBus.register(this);
        setUp();


        return contentView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        eventBus.unregister(this);
    }

    @Subscribe
    public void onEvent(NotenChangedEvent event) {

        if (recyclerView!=null) {
            ((NotenUebersichtRecyclerAdpater) recyclerView.getAdapter()).updateData();
            calculateAverage();
        }
    }

    @Subscribe
    public void onEvent(FaecherUpdateEvent event) {

        if (recyclerView!=null) {
            ((NotenUebersichtRecyclerAdpater) recyclerView.getAdapter()).updateData();
            calculateAverage();
        }
    }

    @Subscribe
    public void onEvent(KursChangedEvent event) {

        setUpLeistungskurs();
    }

    private void setUp() {

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        NotenUebersichtRecyclerAdpater adpater = new NotenUebersichtRecyclerAdpater(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adpater);

        setUpLeistungskurs();

        calculateAverage();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startManagerActivity();
            }
        });
    }

    private void setUpLeistungskurs() {

        if (LocalData.isOberstufe()) {

            leistungskurseCheckbox.setVisibility(View.VISIBLE);

            boolean countLKdouble = PreferenceHelper.readBooleanFromPreferences(getActivity().getApplicationContext(), "countLKdouble", true);
            leistungskurseCheckbox.setChecked(countLKdouble);
            leistungskurseCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                    PreferenceHelper.saveBooleanToPrefernces(getActivity().getApplicationContext(), "countLKdouble", b);
                    calculateAverage();
                }
            });

        } else leistungskurseCheckbox.setVisibility(View.GONE);

    }

    private void calculateAverage() {

        new AsyncTask<Boolean, Integer, Double>() {

            int n = 0;
            @Override
            protected Double doInBackground(Boolean... params) {

                boolean countLKdouble = LocalData.isOberstufe() && PreferenceHelper.readBooleanFromPreferences(MyApplication.getAppContext(), "countLKdouble", true);

                double gesamtAverage = 0;
                List<Fach> faecher = SQLite.select().from(Fach.class).queryList();
                for (Fach f : faecher) {
                    Double average = f.getZensurenDurchschnitt();
                    if (average != null) {
                        gesamtAverage += average;
                        n++;

                        if (countLKdouble && f.isLeistungskurs()) { //Zählt Leistungskurse doppelt
                            gesamtAverage += average;
                            n++;
                        }
                    }
                }
                if (n > 0) gesamtAverage /= n;
                return gesamtAverage;
            }

            @Override
            protected void onPostExecute(Double gesamtAverage) {
                durchschnittText.setText(n > 0 ? new DecimalFormat("#0.00").format(gesamtAverage) : "n.A.");
            }
        }.execute();

    }

    private void startFachActivity(long fachId) {

        Intent openFachIntent = new Intent(getContext(), FachActivity.class);
        openFachIntent.putExtra("fachId", fachId);
        openFachIntent.putExtra("tab", FachActivity.TAB_NOTEN);
        startActivity(openFachIntent);

    }

    private void startManagerActivity() {

        Intent openManagerIntent = new Intent(getContext(), ManagerActivity.class);
        startActivity(openManagerIntent);

    }


    @Override
    public void onFachClicked(long fachId) {

        startFachActivity(fachId);
    }
}
