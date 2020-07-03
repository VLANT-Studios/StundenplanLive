package de.conradowatz.jkgvertretung.adapters;

import android.os.AsyncTask;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.fragments.FreieZimmerPageFragment;
import de.conradowatz.jkgvertretung.variables.OnlineTag;

public class FreieZimmerPagerAdapter extends FragmentStatePagerAdapter {

    private List<OnlineTag> onlineTagList;

    public FreieZimmerPagerAdapter(FragmentManager fm) {
        super(fm);

        updateData();
    }

    public void updateData() {

        new AsyncTask<Boolean, Integer, List<OnlineTag>>() {
            @Override
            protected List<OnlineTag> doInBackground(Boolean... params) {
                return OnlineTag.getAllOnlineTagSorted();
            }

            @Override
            protected void onPostExecute(List<OnlineTag> o) {
                onlineTagList = o;
                notifyDataSetChanged();
            }
        }.execute();
    }

    @Override
    public Fragment getItem(int position) {
        return FreieZimmerPageFragment.newInstance(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return new SimpleDateFormat("EEEE", Locale.GERMAN).format(onlineTagList.get(position).getDate());
    }

    @Override
    public int getCount() {
        return onlineTagList==null?0:onlineTagList.size();
    }
}
