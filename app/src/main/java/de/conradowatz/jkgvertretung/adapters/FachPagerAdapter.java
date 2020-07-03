package de.conradowatz.jkgvertretung.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import de.conradowatz.jkgvertretung.fragments.FachNotenFragment;
import de.conradowatz.jkgvertretung.fragments.FachSettingsFragment;
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
            case 2:
                return new FachNotenFragment();
        }
        return new FachSettingsFragment();

    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Events";
            case 1:
                return "Stunden";
            case 2:
                return "Noten";
        }
        return "Einstellungen";
    }
}
