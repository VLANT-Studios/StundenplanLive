package de.conradowatz.jkgvertretung.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Calendar;
import java.util.Date;

public class Utilities {

    /**
     * Vergleicht zwei gegebene Tage in ihrer Kalenderreihenfolge
     *
     * @param day1
     * @param day2
     * @return 0 bei gleichem Tag; <0 wenn day1 vor day2; >0 wenn day1 nach day2
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
     * @return 0 bei gleichem Tag; <0 wenn date1 vor date2; >0 wenn day1 nach day2
     */
    public static int compareDays(Date date1, Date date2) {

        Calendar day1 = Calendar.getInstance();
        Calendar day2 = Calendar.getInstance();
        day1.setTime(date1);
        day2.setTime(date2);

        return compareDays(day1, day2);

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

    public static Gson getDefaultGson() {

        return new GsonBuilder().setDateFormat("dd.MM.yyyy HH:mm:ss").create();
    }

}
