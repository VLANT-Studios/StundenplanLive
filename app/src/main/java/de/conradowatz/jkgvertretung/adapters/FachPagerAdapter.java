package de.conradowatz.jkgvertretung.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.conradowatz.jkgvertretung.fragments.EventFragment;
import de.conradowatz.jkgvertretung.fragments.FachNotenFragment;
import de.conradowatz.jkgvertretung.fragments.FachStundenFragment;

public class FachPagerAdapter extends FragmentPagerAdapter {

    public FachPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return EventFragment.newInstance(EventFragment.MODE_FACH);
            case 1:
                return new FachStundenFragment();
        }
        return new FachNotenFragment();
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Events";
            case 1:
                return "Stunden";
        }
        return "Noten";
    }
}
