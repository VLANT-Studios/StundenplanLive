package de.conradowatz.jkgvertretung.tools;

import android.text.format.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utilities {

    /**
     * Vergleicht zwei gegebene Tage in ihrer Kalenderreihenfolge
     *
     * @param day1
     * @param day2
     * @return 0 bei gleichem OnlineTag; <0 wenn day1 vor day2; >0 wenn day1 nach day2
     */
    public static int compareDays(Calendar day1, Calendar day2) {

        if (day1.get(Calendar.YEAR) != day2.get(Calendar.YEAR)) {
            return ((Integer) day1.get(Calendar.YEAR)).compareTo(day2.get(Calendar.YEAR));
        } else {
            return ((Integer) day1.get(Calendar.DAY_OF_YEAR)).compareTo(day2.get(Calendar.DAY_OF_YEAR));
        }

    }

    /**
     * Vergleicht zwei gegebene Tage in ihrer Kalenderreihenfolge
     *
     * @param date1
     * @param date2
     * @return 0 bei gleichem OnlineTag; <0 wenn date1 vor date2; >0 wenn day1 nach day2
     */
    public static int compareDays(Date date1, Date date2) {

        Calendar day1 = Calendar.getInstance();
        Calendar day2 = Calendar.getInstance();
        day1.setTime(date1);
        day2.setTime(date2);

        return compareDays(day1, day2);

    }

    public static Calendar getToday() {

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /**
     * @param date das Datum
     * @return 1-7
     */
    public static int getDayOfWeek(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return 1;
            case Calendar.TUESDAY:
                return 2;
            case Calendar.WEDNESDAY:
                return 3;
            case Calendar.THURSDAY:
                return 4;
            case Calendar.FRIDAY:
                return 5;
            case Calendar.SATURDAY:
                return 6;
            case Calendar.SUNDAY:
                return 7;
        }
        return 0;
    }

    public static int getDayDifference(Date date1, Date date2) {

        Calendar day1 = Calendar.getInstance();
        Calendar day2 = Calendar.getInstance();
        day1.setTime(date1);
        day2.setTime(date2);

        return getDayDifference(day1, day2);

    }

    public static int getDayDifference(Calendar day1, Calendar day2) {

        //day1 kommt vor day2
        int compareDays = compareDays(day1, day2);
        if (compareDays == 0) return 0;
        else if (compareDays > 1) {
            Calendar temp = day1;
            day1 = day2;
            day2 = temp;
        }

        int yearDiff = day2.get(Calendar.YEAR) - day1.get(Calendar.YEAR);
        if (yearDiff == 0) {

            return day2.get(Calendar.DAY_OF_YEAR) - day1.get(Calendar.DAY_OF_YEAR);

        } else {

            int diff = day1.getActualMaximum(Calendar.DAY_OF_YEAR) - day1.get(Calendar.DAY_OF_YEAR) + day2.get(Calendar.DAY_OF_YEAR);
            for (int i = 0; i < yearDiff - 1; i++) {
                day1.add(Calendar.YEAR, 1);
                diff += day1.getActualMaximum(Calendar.DAY_OF_YEAR);
            }
            return diff;

        }

    }

    public static String dayDifferenceToString(int diff) {

        if (diff == 0) return "heute";
        if (diff == 1) return "morgen";
        return String.format(Locale.GERMANY, "in %s Tagen", diff);
    }

}
