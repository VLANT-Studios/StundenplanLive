package de.conradowatz.jkgvertretung.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import de.conradowatz.jkgvertretung.fragments.FreieZimmerPageFragment;
import de.conradowatz.jkgvertretung.tools.VertretungsData;

public class FreieZimmerPagerAdapter extends FragmentStatePagerAdapter {

    private int count;

    public FreieZimmerPagerAdapter(FragmentManager fm) {
        super(fm);

        count = VertretungsData.getInstance().getTagList().size();
    }

    public void dayAdded() {
        count = VertretungsData.getInstance().getTagList().size();
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        return FreieZimmerPageFragment.newInstance(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return VertretungsData.getInstance().getTagList().get(position).getDatumString().split(",")[0];
    }

    @Override
    public int getCount() {
        return count;
    }
}
