package de.conradowatz.jkgvertretung.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.conradowatz.jkgvertretung.fragments.FaecherFragment;
import de.conradowatz.jkgvertretung.fragments.TerminFragment;

public class ManagerPagerAdapter extends FragmentPagerAdapter {

    public ManagerPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) return new FaecherFragment();
        else return TerminFragment.newInstance(TerminFragment.MODE_FERIEN);
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0) return "Fächer";
        else return "Ferien";
    }
}
