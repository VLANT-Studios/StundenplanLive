package de.conradowatz.jkgvertretung.adapters;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import de.conradowatz.jkgvertretung.fragments.StundenplanPageFragment;
import de.conradowatz.jkgvertretung.tools.VertretungsData;

public class StundenplanPagerAdapter extends FragmentStatePagerAdapter {

    private int mode;
    private Integer klassenIndex;

    public StundenplanPagerAdapter(FragmentManager fm, int mode, Integer klassenIndex) {
        super(fm);
        this.mode = mode;
        this.klassenIndex = klassenIndex;
    }

    @Override
    public Fragment getItem(int position) {
        return StundenplanPageFragment.newInstance(position, mode, klassenIndex);
    }

    @Override
    public int getCount() {
        return VertretungsData.getsInstance().getTagList().size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return VertretungsData.getsInstance().getTagList().get(position).getDatumString().split(",")[0];
    }


}
