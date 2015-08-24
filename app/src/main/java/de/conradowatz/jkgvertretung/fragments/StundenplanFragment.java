package de.conradowatz.jkgvertretung.fragments;


import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.StundenplanPagerAdapter;
import de.conradowatz.jkgvertretung.variables.DayUpdatedEvent;
import de.greenrobot.event.EventBus;

public class StundenplanFragment extends Fragment {

    public static final int MODE_STUNDENPLAN = 1;
    public static final int MODE_VERTRETUNGSPLAN = 2;
    public static final int MODE_ALGVERTRETUNGSPLAN = 3;

    private View contentView;

    private ViewPager viewPager;
    private TabLayout tabs;

    private int mode;

    private EventBus eventBus = EventBus.getDefault();

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

        //Analytics
        MyApplication analytics = (MyApplication) getActivity().getApplication();
        analytics.fireScreenHit("Stundenplan");

        eventBus.register(this);

        if (savedInstanceState == null) {

            Bundle arguments = getArguments();
            mode = arguments.getInt("mode");
        } else {

            mode = savedInstanceState.getInt("mode");
        }

        contentView = inflater.inflate(R.layout.fragment_stundenplan, container, false);
        viewPager = (ViewPager) contentView.findViewById(R.id.viewPager);
        tabs = (TabLayout) contentView.findViewById(R.id.materialTabs);

        setUpViewPager();

        return contentView;
    }

    private void setUpViewPager() {

        StundenplanPagerAdapter adapter = new StundenplanPagerAdapter(getChildFragmentManager(), mode);
        viewPager.setAdapter(adapter);

        tabs.setTabTextColors(ContextCompat.getColor(getContext(), R.color.white), ContextCompat.getColor(getContext(), R.color.white));
        tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabs.setupWithViewPager(viewPager);
    }

    /**
     * Läd den ViewPager neu, wenn Tage hinzugefügt wurden
     */
    public void onEvent(DayUpdatedEvent event) {

        if (viewPager == null) return;

        if (event.getPosition() > viewPager.getAdapter().getCount()) {

            viewPager.getAdapter().notifyDataSetChanged();
            tabs.setupWithViewPager(viewPager);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt("mode", mode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        eventBus.unregister(this);
        super.onStop();
    }
}
