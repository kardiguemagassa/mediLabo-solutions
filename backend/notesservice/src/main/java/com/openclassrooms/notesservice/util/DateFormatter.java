package com.openclassrooms.notesservice.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateFormatter {

    public static final String DATE_FORMATTER = "yyyy-MM-dd HH:mm:ss";

    public static String shortDate(String date) {
        return date.split(" ")[0];
    }

    public static String today(String formatter) {
        var cal = Calendar.getInstance();
        var dateFormat = new SimpleDateFormat(formatter, Locale.FRENCH);
        return dateFormat.format(cal.getTime());
    }

    public static String formattedDate() {
        var cal = Calendar.getInstance();

        // Exemple : 17 février 2026 - 14:30:25
        var dateFormat = new SimpleDateFormat("d MMMM yyyy - HH:mm:ss", Locale.FRENCH);
        return dateFormat.format(cal.getTime());
    }
}
