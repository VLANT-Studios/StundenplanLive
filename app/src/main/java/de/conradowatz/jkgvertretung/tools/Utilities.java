package de.conradowatz.jkgvertretung.tools;

import java.util.Calendar;

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

}
