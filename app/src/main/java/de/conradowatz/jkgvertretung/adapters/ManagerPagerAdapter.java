package de.conradowatz.jkgvertretung.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.conradowatz.jkgvertretung.fragments.FaecherFragment;
import de.conradowatz.jkgvertretung.fragments.FerienFragment;

public class ManagerPagerAdapter extends FragmentPagerAdapter {

    public ManagerPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) return new FaecherFragment();
        else return new FerienFragment();
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
