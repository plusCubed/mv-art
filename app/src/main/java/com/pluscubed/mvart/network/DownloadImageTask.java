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
    public static final int DL_IMAGE_ARGS_MARKER = 1;
    public static final int DL_IMAGE_ARGS_IMAGEVIEW = 2;
    public static final int DL_IMAGE_ARGS_PROGRESSBAR = 3;
    public static final int DL_IMAGE_ARGS_THUMBNAIL_BOOL = 4;

    private ArtLocation mArtLocation;
    private Marker mMarker;
    private boolean mThumbnailMode;
    private ImageView mImageView;
    private ProgressBar mProgressBar;


    @SafeVarargs
    @Override
    protected final Void doInBackground(SparseArray<Object>... objects) {
        SparseArray<Object> downloadArgs = objects[0];

        mArtLocation = (ArtLocation) downloadArgs.get(DL_IMAGE_ARGS_ARTLOCATION);
        mMarker = (Marker) downloadArgs.get(DL_IMAGE_ARGS_MARKER);
        mThumbnailMode = (boolean) downloadArgs.get(DL_IMAGE_ARGS_THUMBNAIL_BOOL);
        mImageView = (ImageView) downloadArgs.get(DL_IMAGE_ARGS_IMAGEVIEW);
        mProgressBar = (ProgressBar) downloadArgs.get(DL_IMAGE_ARGS_PROGRESSBAR);

        try {
            InputStream stream = null;

            try {
                if (mThumbnailMode) {
                    stream = (InputStream) new URL(mArtLocation.picUrlThumbnail).getContent();
                    mArtLocation.picThumbnail = Drawable.createFromStream(stream, null);
                } else {
                    stream = (InputStream) new URL(mArtLocation.picUrl).getContent();
                    mArtLocation.pic = Drawable.createFromStream(stream, null);
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
            mImageView.setImageDrawable(mArtLocation.pic);
            mImageView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
        }
    }
}
