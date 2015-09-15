package de.conradowatz.jkgvertretung.adapters;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import de.conradowatz.jkgvertretung.fragments.StundenplanPageFragment;
import de.conradowatz.jkgvertretung.tools.VertretungsData;

public class StundenplanPagerAdapter extends FragmentStatePagerAdapter {

    private int mode;
    private Integer klassenIndex;
    private int count;

    public StundenplanPagerAdapter(FragmentManager fm, int mode, Integer klassenIndex) {
        super(fm);
        this.mode = mode;
        this.klassenIndex = klassenIndex;
        count = VertretungsData.getInstance().getTagList().size();
    }

    public void dayAdded() {
        count = VertretungsData.getInstance().getTagList().size();
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        return StundenplanPageFragment.newInstance(position, mode, klassenIndex);
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return VertretungsData.getInstance().getTagList().get(position).getDatumString().split(",")[0];
    }


}
