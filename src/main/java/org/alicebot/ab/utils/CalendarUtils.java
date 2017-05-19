package org.alicebot.ab.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CalendarUtils {

    public static int timeZoneOffset() {
        Calendar cal = Calendar.getInstance();
        return (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000);
    }

    public static String year() {
        Calendar cal = Calendar.getInstance();
        return String.valueOf(cal.get(Calendar.YEAR));
    }

    public static String date() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMMMMMM dd, yyyy");
        dateFormat.setCalendar(cal);
        return dateFormat.format(cal.getTime());
    }

    public static String date(String jformat, String locale, String timezone) {
        if (jformat == null) {
            jformat = "EEE MMM dd HH:mm:ss zzz yyyy";
        }

        return new SimpleDateFormat(jformat).format(new Date());
    }
}
