package com.pluscubed.mvart.model;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Art location data object
 */
public class ArtLocation {
    public String title;
    public double latitude;
    public double longitude;
    public String picUrl;
    public String picUrlThumbnail;
    public Drawable picThumbnail;
    public Drawable pic;
    public String description;
    public long startDate;
    public long endDate;

    private static long parseString(String string) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return format.parse(string).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setStartDate(String string) {
        startDate = parseString(string);
    }

    public void setEndDate(String string) {
        endDate = parseString(string);
    }

    public String getStartDateString(Context context) {
        return DateFormat.getDateFormat(context).format(new Date(startDate));
    }

    public String getEndDateString(Context context) {
        return DateFormat.getDateFormat(context).format(new Date(endDate));
    }

}