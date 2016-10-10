package de.conradowatz.jkgvertretung.fragments;


import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.StundenplanPagerAdapter;
import de.conradowatz.jkgvertretung.events.DataReadyEvent;
import de.conradowatz.jkgvertretung.events.DayUpdatedEvent;
import de.conradowatz.jkgvertretung.events.FerienChangedEvent;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.Klasse;

public class StundenplanFragment extends Fragment {

    public static final int MODE_STUNDENPLAN = 1;
    public static final int MODE_VERTRETUNGSPLAN = 2;
    public static final int MODE_ALGVERTRETUNGSPLAN = 3;
    public static final int MODE_KLASSENPLAN = 4;
    boolean tabsNoSelect = false;
    private View contentView;
    private ViewPager viewPager;
    private TabLayout tabs;
    private Spinner spinner;
    private int mode;
    private int viewpagerCount;
    private Integer klassenIndex;
    private EventBus eventBus = EventBus.getDefault();
    private boolean waitingForData = false;

    public StundenplanFragment() {
        // Required empty public constructor
    }

    public static StundenplanFragment newInstance(int mode) {

        Bundle args = new Bundle();
        args.putInt("mode", mode);

        StundenplanFragment fragment = new StundenplanFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (savedInstanceState == null) {

            Bundle arguments = getArguments();
            mode = arguments.getInt("mode");
        } else {

            mode = savedInstanceState.getInt("mode");
            viewpagerCount = savedInstanceState.getInt("viewpagerCount");
            klassenIndex = savedInstanceState.getInt("klassenIndex");
        }

        //Analytics
        MyApplication analytics = (MyApplication) getActivity().getApplication();
        switch (mode) {
            case MODE_STUNDENPLAN:
                analytics.fireScreenHit("Stundenplan");
                break;
            case MODE_VERTRETUNGSPLAN:
                analytics.fireScreenHit("Vertretungsplan");
                break;
            case MODE_ALGVERTRETUNGSPLAN:
                analytics.fireScreenHit("Allgemeiner Vertretungsplan");
                break;
            case MODE_KLASSENPLAN:
                analytics.fireScreenHit("Klassenplan");
        }

        if (mode == MODE_KLASSENPLAN)
            contentView = inflater.inflate(R.layout.fragment_klassenplan, container, false);
        else contentView = inflater.inflate(R.layout.fragment_stundenplan, container, false);
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

        if (mode == MODE_KLASSENPLAN) {

            spinner = (Spinner) contentView.findViewById(R.id.spinner);
            setUpSpinner();
            if (klassenIndex != null) setUpViewPager(klassenIndex, null);
        } else {
            setUpViewPager(null, null);
        }
    }

    private void setUpSpinner() {

        ArrayList<String> klassennamenListe = new ArrayList<>();
        for (Klasse klasse : VertretungsData.getInstance().getKlassenList()) {
            klassennamenListe.add(klasse.getName());
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.klassenplan_spinner_text, klassennamenListe);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (klassenIndex == null || position != klassenIndex) {
                    int lastposition = viewPager.getCurrentItem();
                    setUpViewPager(position, lastposition);
                    klassenIndex = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        int meineKlasseInt = PreferenceHelper.readIntFromPreferences(getActivity(), "meineKlasseInt", -1);
        if (meineKlasseInt >= 0) {
            spinner.setSelection(meineKlasseInt);
        }

    }

    private void setUpViewPager(Integer klassenIndex, Integer lastPosition) {

        final boolean firstStart = viewPager.getAdapter() == null;
        if (VertretungsData.getInstance().getTagList() == null) return;
        final StundenplanPagerAdapter adapter = new StundenplanPagerAdapter(getChildFragmentManager(), mode, klassenIndex);
        viewPager.setAdapter(adapter);

        if (firstStart) {
            tabs.setTabTextColors(ContextCompat.getColor(getContext(), R.color.tabs_unselected), ContextCompat.getColor(getContext(), R.color.white));
            if (mode != MODE_STUNDENPLAN) {
                tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
                tabs.setupWithViewPager(viewPager);
            } else {
                tabs.setTabMode(TabLayout.MODE_FIXED);
                tabs.addTab(tabs.newTab().setText("AKTUELL"));
                tabs.addTab(tabs.newTab().setText(adapter.getPageTitle(0)));
                tabs.addTab(tabs.newTab().setText(adapter.getPageTitle(1)));
                tabs.addTab(tabs.newTab().setText(adapter.getPageTitle(2)));
                tabs.getTabAt(1).select();
                tabsNoSelect = false;
                tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {

                        if (tabsNoSelect) {
                            tabsNoSelect = false;
                            return;
                        }

                        if (tab.getPosition() == 0) {
                            aktuell(true);
                        } else if (tab.getPosition() == 1) {
                            if (viewPager.getCurrentItem() > 1) prevDay(true);
                            else if (viewPager.getCurrentItem() == 1) aktuell(true);
                        } else if (tab.getPosition() == 2) {
                            if (viewPager.getCurrentItem() == 0) nextDay(1, true);
                        } else {
                            if (viewPager.getCurrentItem() == 0) nextDay(2, true);
                            else nextDay(1, true);
                        }

                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        onTabSelected(tab);
                    }
                });
                viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                    private int prevPosition = viewPager.getCurrentItem();

                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    }

                    @Override
                    public void onPageSelected(int position) {

                        if (position < prevPosition) { //zurück
                            if (position > 0) prevDay(false);
                            else aktuell(false);
                        } else if (position > prevPosition) { //vor
                            nextDay(1, false);
                        }
                        prevPosition = position;

                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                    }
                });
            }
        }

        if (lastPosition != null) viewPager.setCurrentItem(lastPosition);

        viewpagerCount = adapter.getCount();
    }

    private void aktuell(boolean scroll) {
        if (scroll) viewPager.setCurrentItem(0, true);
        tabsNoSelect = true;
        tabs.removeAllTabs();
        tabs.addTab(tabs.newTab().setText("AKTUELL"));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(0)));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(1)));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(2)));
        tabsNoSelect = true;
        tabs.getTabAt(1).select();
    }

    private void nextDay(int pages, boolean scroll) {
        if (scroll) viewPager.setCurrentItem(viewPager.getCurrentItem() + pages, true);
        tabsNoSelect = true;
        tabs.removeAllTabs();
        tabs.addTab(tabs.newTab().setText("AKTUELL"));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem() - 1)));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem())));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem() + 1)));
        tabsNoSelect = true;
        tabs.getTabAt(2).select();
    }

    private void prevDay(boolean scroll) {
        if (scroll) viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
        tabsNoSelect = true;
        tabs.removeAllTabs();
        tabs.addTab(tabs.newTab().setText("AKTUELL"));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem() - 1)));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem())));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem() + 1)));
        tabsNoSelect = true;
        tabs.getTabAt(2).select();
    }

    /**
     * Läd den ViewPager neu, wenn Tage hinzugefügt wurden
     */
    @Subscribe
    public void onEvent(DayUpdatedEvent event) {

        if (viewPager == null) return;

        if (event.getPosition() > viewPager.getAdapter().getCount() - 1) {

            ((StundenplanPagerAdapter) viewPager.getAdapter()).dayAdded();
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

    @Subscribe
    public void onEvent(FerienChangedEvent event) {

        showData();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt("mode", mode);
        outState.putInt("viewpagerCount", viewpagerCount);
        if (klassenIndex != null) outState.putInt("klassenIndex", klassenIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        eventBus.unregister(this);
    }
}
