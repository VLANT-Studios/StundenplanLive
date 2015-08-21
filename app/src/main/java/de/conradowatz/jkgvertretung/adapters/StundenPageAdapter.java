package de.conradowatz.jkgvertretung.adapters;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

import de.conradowatz.jkgvertretung.fragments.StundenplanPageFragment;

public class StundenPageAdapter extends FragmentStatePagerAdapter {

    private int mode;
    private ArrayList<String> titles;

    public StundenPageAdapter(FragmentManager fm, int mode, ArrayList<String> titles) {
        super(fm);
        this.mode = mode;
        this.titles = titles;
    }

    @Override
    public Fragment getItem(int position) {
        return StundenplanPageFragment.newInstance(position, mode);
    }

    @Override
    public int getCount() {
        return titles.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles.get(position);
    }
}
