package de.conradowatz.jkgvertretung.adapters;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.Calendar;
import java.util.Date;

import de.conradowatz.jkgvertretung.fragments.StundenplanFragment;
import de.conradowatz.jkgvertretung.fragments.StundenplanPageFragment;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.Tag;

public class StundenplanPagerAdapter extends FragmentStatePagerAdapter {

    private int mode;
    private Integer klassenIndex;

    private int onlineCount;

    private Date nextSchoolDay;
    private int nextSchoolDayOfWeek;

    public StundenplanPagerAdapter(FragmentManager fm, int mode, Integer klassenIndex) {
        super(fm);
        this.mode = mode;
        this.klassenIndex = klassenIndex;
        onlineCount = VertretungsData.getInstance().getTagList().size();

        Calendar calendar = Calendar.getInstance();
        nextSchoolDay = calendar.getTime();
        if (VertretungsAPI.isntSchoolDay(nextSchoolDay))
            nextSchoolDay = VertretungsAPI.nextSchoolDay(nextSchoolDay);
        nextSchoolDayOfWeek = LocalData.getDayOfWeek(nextSchoolDay); //starting at 1 = Monday

    }

    public void dayAdded() {
        if (mode != StundenplanFragment.MODE_STUNDENPLAN)
            onlineCount = VertretungsData.getInstance().getTagList().size();
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nextSchoolDay);
        int dayToAdd = position + ((position + nextSchoolDayOfWeek - 1) / 5) * 2;
        calendar.add(Calendar.DATE, dayToAdd);
        int onlinePosition = -1;
        for (int i = 0; i < VertretungsData.getInstance().getTagList().size(); i++) {
            Tag t = VertretungsData.getInstance().getTagList().get(i);
            Calendar tc = Calendar.getInstance();
            tc.setTime(t.getDatum());
            if (Utilities.compareDays(calendar, tc) == 0) {
                onlinePosition = i;
                break;
            }
        }
        return StundenplanPageFragment.newInstance(position, mode, klassenIndex, calendar.getTime(), onlinePosition);
    }

    @Override
    public int getCount() {
        if (mode == StundenplanFragment.MODE_STUNDENPLAN)
            return 300;
        else return onlineCount;
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
            return VertretungsData.getInstance().getTagList().get(position).getDatumString().split(",")[0];
        }
    }


}
