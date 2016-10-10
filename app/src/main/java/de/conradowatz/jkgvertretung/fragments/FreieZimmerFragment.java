package de.conradowatz.jkgvertretung.fragments;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.FreieZimmerPagerAdapter;
import de.conradowatz.jkgvertretung.events.DataReadyEvent;
import de.conradowatz.jkgvertretung.events.DayUpdatedEvent;
import de.conradowatz.jkgvertretung.tools.VertretungsData;

public class FreieZimmerFragment extends Fragment {

    private View contentView;

    private ViewPager viewPager;
    private TabLayout tabs;

    private EventBus eventBus = EventBus.getDefault();
    private boolean waitingForData = false;

    public FreieZimmerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Analytics
        MyApplication analytics = (MyApplication) getActivity().getApplication();
        analytics.fireScreenHit("Freie Zimmer");

        contentView = inflater.inflate(R.layout.fragment_stundenplan, container, false);
        viewPager = (ViewPager) contentView.findViewById(R.id.viewPager);
        tabs = (TabLayout) contentView.findViewById(R.id.materialTabs);

        eventBus.register(this);

        return contentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (!VertretungsData.getInstance().isReady()) {
            waitingForData = true;
            return;
        }

        showData();
    }


    private void showData() {

        setUpViewPager();
    }

    private void setUpViewPager() {

        boolean firstStart = viewPager.getAdapter() == null;
        if (VertretungsData.getInstance().getTagList() == null) return;
        FreieZimmerPagerAdapter adapter = new FreieZimmerPagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(adapter);

        if (firstStart) {
            tabs.setTabTextColors(ContextCompat.getColor(getContext(), R.color.tabs_unselected), ContextCompat.getColor(getContext(), R.color.white));
            tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
            tabs.setupWithViewPager(viewPager);
        }
    }

    /**
     * Läd den ViewPager neu, wenn Tage hinzugefügt wurden
     */
    @Subscribe
    public void onEvent(DayUpdatedEvent event) {

        if (viewPager == null) return;

        if (event.getPosition() > viewPager.getAdapter().getCount() - 1) {

            ((FreieZimmerPagerAdapter) viewPager.getAdapter()).dayAdded();
            tabs.setupWithViewPager(viewPager);
        }

    }

    @Subscribe
    public void onEvent(DataReadyEvent event) {

        if (waitingForData) {
            waitingForData = false;

            showData();
        }
    }

    @Override
    public void onStop() {
        eventBus.unregister(this);
        super.onStop();
    }
}

