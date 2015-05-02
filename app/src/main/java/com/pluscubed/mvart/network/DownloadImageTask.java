package com.pluscubed.mvart.network;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.gms.maps.model.Marker;
import com.pluscubed.mvart.model.ArtLocation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Download image from Url
 */
public class DownloadImageTask extends AsyncTask<SparseArray<Object>, Void, Void> {

    public static final int DL_IMAGE_ARGS_ARTLOCATION = 0;
    public static final int DL_IMAGE_ARGS_PIC_INDEX = 1;
    public static final int DL_IMAGE_ARGS_MARKER = 2;
    public static final int DL_IMAGE_ARGS_IMAGEVIEW = 3;
    public static final int DL_IMAGE_ARGS_PROGRESSBAR = 4;
    public static final int DL_IMAGE_ARGS_THUMBNAIL_BOOL = 5;

    private Marker mMarker;
    private boolean mThumbnailMode;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private Drawable mDrawable;


    @SafeVarargs
    @Override
    protected final Void doInBackground(SparseArray<Object>... objects) {
        SparseArray<Object> downloadArgs = objects[0];

        ArtLocation artLocation = (ArtLocation) downloadArgs.get(DL_IMAGE_ARGS_ARTLOCATION);
        mMarker = (Marker) downloadArgs.get(DL_IMAGE_ARGS_MARKER);
        mThumbnailMode = (boolean) downloadArgs.get(DL_IMAGE_ARGS_THUMBNAIL_BOOL);
        mImageView = (ImageView) downloadArgs.get(DL_IMAGE_ARGS_IMAGEVIEW);
        mProgressBar = (ProgressBar) downloadArgs.get(DL_IMAGE_ARGS_PROGRESSBAR);
        int picIndex = (int) downloadArgs.get(DL_IMAGE_ARGS_PIC_INDEX);

        try {
            InputStream stream = null;

            try {
                if (mThumbnailMode) {
                    stream = (InputStream) new URL(artLocation.thumbnailPicUrl).getContent();
                    artLocation.thumbnailPic = Drawable.createFromStream(stream, null);
                } else {
                    stream = (InputStream) new URL(artLocation.picUrls.get(picIndex)).getContent();
                    mDrawable = Drawable.createFromStream(stream, null);
                }
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            //return getResources().getString(R.string.connection_error);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void uselessThing) {
        if (mThumbnailMode) {
            if (mMarker.isInfoWindowShown()) {
                mMarker.showInfoWindow();
            }
        } else {
            mImageView.setImageDrawable(mDrawable);
            mImageView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
        }
    }
}
