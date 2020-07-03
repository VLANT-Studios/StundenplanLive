package de.conradowatz.jkgvertretung.adapters;


import android.os.AsyncTask;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.fragments.StundenplanFragment;
import de.conradowatz.jkgvertretung.fragments.StundenplanPageFragment;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.variables.OnlineTag;

public class StundenplanPagerAdapter extends FragmentStatePagerAdapter {

    private int mode;
    private String klassenName;

    private List<OnlineTag> onlineTagList = new ArrayList<>();

    private Date nextSchoolDay;
    private int nextSchoolDayOfWeek;

    public StundenplanPagerAdapter(FragmentManager fm, int mode, String klassenName) {
        super(fm);
        this.mode = mode;
        this.klassenName = klassenName;

        updateNextSchoolDay();

        updateData();

    }

    private void updateNextSchoolDay() {

        Calendar calendar = Utilities.getToday();
        nextSchoolDay = calendar.getTime();
        if (VertretungsAPI.isntSchoolDay(nextSchoolDay))
            nextSchoolDay = VertretungsAPI.nextSchoolDay(nextSchoolDay);
        nextSchoolDayOfWeek = Utilities.getDayOfWeek(nextSchoolDay); //starting at 1 = Monday

    }

    public void updateData() {

        new AsyncTask<Boolean, Integer, List<OnlineTag>>() {
            @Override
            protected List<OnlineTag> doInBackground(Boolean... params) {
                if (mode != StundenplanFragment.MODE_STUNDENPLAN)
                    return OnlineTag.getAllOnlineTagSorted();
                return null;
            }

            @Override
            protected void onPostExecute(List<OnlineTag> o) {
                onlineTagList = o;
                updateNextSchoolDay();
                notifyDataSetChanged();
            }
        }.execute();

    }

    @Override
    public Fragment getItem(int position) {

        Calendar calendar = Calendar.getInstance();

        if (mode == StundenplanFragment.MODE_STUNDENPLAN) {
            //get Datum
            calendar.setTime(nextSchoolDay);
            int dayToAdd = position + ((position + nextSchoolDayOfWeek - 1) / 5) * 2;
            calendar.add(Calendar.DATE, dayToAdd);
        }
        return StundenplanPageFragment.newInstance(position, mode, klassenName, calendar.getTime());
    }

    @Override
    public int getCount() {
        if (mode == StundenplanFragment.MODE_STUNDENPLAN)
            return 300;
        else return onlineTagList.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {

        if (mode == StundenplanFragment.MODE_STUNDENPLAN) {
            int dayOfWeek = (nextSchoolDayOfWeek + position) % 5;
            switch (dayOfWeek) {
                case 1:
                    return "Mo";
                case 2:
                    return "Di";
                case 3:
                    return "Mi";
                case 4:
                    return "Do";
                default:
                    return "Fr";
            }
        } else {
            return new SimpleDateFormat("EEEE", Locale.GERMAN).format(onlineTagList.get(position).getDate());
        }
    }


}
