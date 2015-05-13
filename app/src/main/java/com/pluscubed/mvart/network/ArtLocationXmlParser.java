package com.pluscubed.mvart.network;

import android.util.Xml;

import com.pluscubed.mvart.model.ArtLocation;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Xml parse
 */
public class ArtLocationXmlParser {
    // We don't use namespaces
    private static final String ns = null;

    public List<ArtLocation> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            List<ArtLocation> artLocations = new ArrayList<>();

            parser.require(XmlPullParser.START_TAG, ns, "artwork");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                // Starts by looking for the entry tag
                if (name.equals("item")) {
                    artLocations.add(readItem(parser));
                } else {
                    skip(parser);
                }
            }


            return artLocations;
        } finally {
            in.close();
        }
    }

    private ArtLocation readItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "item");
        ArtLocation artLocation = new ArtLocation();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tag = parser.getName();
            switch (tag) {
                case "name":
                    artLocation.title = readText(parser, tag);
                    break;
                case "latitude":
                    artLocation.latitude = Double.parseDouble(readText(parser, tag));
                    break;
                case "longitude":
                    artLocation.longitude = Double.parseDouble(readText(parser, tag));
                    break;
                case "artist":
                    artLocation.artist = readText(parser, tag);
                    break;
                case "address":
                    artLocation.address = readText(parser, tag);
                    break;
                case "description":
                    artLocation.description = readText(parser, tag);
                    break;
                case "dedicationyear":
                    artLocation.dedicationYear = readText(parser, tag);
                    break;
                case "picurls":
                    artLocation.picUrls = readArtPicUrlItem(parser);
                    break;
                case "thumburl":
                    artLocation.thumbnailPicUrl = readText(parser, tag).trim();
                    break;
                case "startdate":
                    artLocation.setStartDate(readText(parser, tag));
                    break;
                case "enddate":
                    artLocation.setEndDate(readText(parser, tag));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return artLocation;
    }

    private List<String> readArtPicUrlItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "picurls");
        List<String> urls = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tag = parser.getName();
            switch (tag) {
                case "url":
                    urls.add(readText(parser, tag).trim());
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return urls;
    }

    private String readText(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tag);
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, ns, tag);
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}
