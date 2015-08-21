package de.conradowatz.jkgvertretung.fragments;


import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.MainActivity;
import de.conradowatz.jkgvertretung.adapters.StundenPageAdapter;
import de.conradowatz.jkgvertretung.variables.Tag;

public class StundenplanFragment extends Fragment {

    public static final int MODE_STUNDENPLAN = 1;
    public static final int MODE_VERTRETUNGSPLAN = 2;
    public static final int MODE_ALGVERTRETUNGSPLAN = 3;

    private View contentView;

    private ViewPager viewPager;
    private TabLayout tabs;

    private int mode;

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

        if (savedInstanceState == null) {

            Bundle arguments = getArguments();
            mode = arguments.getInt("mode");
        } else {

            mode = savedInstanceState.getInt("mode");
        }

        contentView = inflater.inflate(R.layout.fragment_stundenplan, container, false);
        viewPager = (ViewPager) contentView.findViewById(R.id.viewPager);
        tabs = (TabLayout) contentView.findViewById(R.id.materialTabs);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity.vertretungsAPI==null) return contentView;

        ArrayList<String> titles = new ArrayList<>();
        for (Tag tag : mainActivity.vertretungsAPI.getTagList()) {
            titles.add(tag.getDatumString().split(",")[0]);
        }
        StundenPageAdapter adapter = new StundenPageAdapter(getChildFragmentManager(), mode, titles);
        viewPager.setAdapter(adapter);

        tabs.setTabTextColors(getResources().getColor(R.color.white), getResources().getColor(R.color.white));
        tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabs.setupWithViewPager(viewPager);

        return contentView;
    }

    /**
     * Läd den ViewPager neu, wenn Tage hinzugefügt wurden
     */
    public void onDayAdded() {

        if (viewPager!=null) {
            viewPager.getAdapter().notifyDataSetChanged();
            tabs.setupWithViewPager(viewPager);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        Log.d("JKGDEBUG", "saveinstance parent");

        outState.putInt("mode", mode);
        super.onSaveInstanceState(outState);
    }
}
