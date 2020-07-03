package de.conradowatz.jkgvertretung.fragments;

import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.FreieZimmerPagerAdapter;
import de.conradowatz.jkgvertretung.events.DaysUpdatedEvent;

public class FreieZimmerFragment extends Fragment {

    private View contentView;

    private ViewPager viewPager;
    private TabLayout tabs;

    private EventBus eventBus = EventBus.getDefault();

    public FreieZimmerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_stundenplan, container, false);
        viewPager = (ViewPager) contentView.findViewById(R.id.viewPager);
        tabs = (TabLayout) contentView.findViewById(R.id.materialTabs);

        eventBus.register(this);

        setUpViewPager();

        return contentView;
    }

    private void setUpViewPager() {

        boolean firstStart = viewPager.getAdapter() == null;
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
    public void onEvent(DaysUpdatedEvent event) {

        if (viewPager == null) return;

        ((FreieZimmerPagerAdapter) viewPager.getAdapter()).updateData();
        tabs.setupWithViewPager(viewPager);


    }

    @Override
    public void onStop() {
        super.onStop();

        eventBus.unregister(this);
    }
}

