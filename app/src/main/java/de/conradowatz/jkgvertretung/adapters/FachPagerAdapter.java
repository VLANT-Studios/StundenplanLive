package de.conradowatz.jkgvertretung.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.conradowatz.jkgvertretung.fragments.FachNotenFragment;
import de.conradowatz.jkgvertretung.fragments.FachStundenFragment;
import de.conradowatz.jkgvertretung.fragments.TerminFragment;

public class FachPagerAdapter extends FragmentPagerAdapter {

    public FachPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return TerminFragment.newInstance(TerminFragment.MODE_FACH);
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
