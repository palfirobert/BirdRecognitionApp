package com.example.birdrecognitionapp.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    /**
     * Converts a date string in the format "yyyy-MM-dd HH:mm:ss" to a Unix timestamp.
     * @param dateString The date string to convert.
     * @return The Unix timestamp equivalent of the date string.
     */
    public static long convertToTimestamp(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            Date parsedDate = dateFormat.parse(dateString);
            return parsedDate.getTime() / 1000; // Divide by 1000 to convert milliseconds to seconds
        } catch (ParseException e) {
            System.err.println("Failed to parse date: " + e.getMessage());
            return -1; // Return -1 or any other error code as per your application's needs
        }
    }
}
