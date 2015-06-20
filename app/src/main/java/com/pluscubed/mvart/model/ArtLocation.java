package com.pluscubed.mvart.model;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateFormat;

import com.pluscubed.mvart.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Art location data object
 */
public class ArtLocation {
    public String title;
    public double latitude;
    public double longitude;
    public List<String> picUrls;
    public String thumbnailPicUrl;
    public String artist;
    public String address;
    public String description;
    public String dedicationYear;
    public long startDate;
    public long endDate;

    public ArtLocation() {
        picUrls = new ArrayList<>();
    }

    private static long parseString(String string) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return format.parse(string).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean matchFilter(String string) {
        String lowercase = string.toLowerCase();
        return title.toLowerCase().contains(lowercase) ||
                artist.toLowerCase().contains(lowercase) ||
                address.toLowerCase().contains(lowercase) ||
                description.toLowerCase().contains(lowercase);
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

    public Spanned getFormattedDesc(Context context) {
        String string = "";
        if (!artist.isEmpty()) {
            string += context.getString(R.string.details_artist, artist);
        }
        if (!address.isEmpty()) {
            string += context.getString(R.string.details_address, address);
        }
        if (!description.isEmpty()) {
            string += context.getString(R.string.details_desc, description);
        }
        if (!dedicationYear.isEmpty()) {
            string += context.getString(R.string.details_dedication, dedicationYear);
        } else if (startDate != endDate) {
            string += context.getString(R.string.details_dates, getStartDateString(context), getEndDateString(context));
        }
        return Html.fromHtml(string);
    }

}